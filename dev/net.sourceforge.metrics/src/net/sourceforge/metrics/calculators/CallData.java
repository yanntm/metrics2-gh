/*
 * Copyright (c) 2010 Keith Cassell. All rights reserved.
 *
 * Licensed under CPL 1.0 (Common Public License Version 1.0).
 * The license is available at http://www.eclipse.org/legal/cpl-v10.html.
 *
 *
 * DISCLAIMER OF WARRANTIES AND LIABILITY:
 *
 * THE SOFTWARE IS PROVIDED "AS IS".  THE AUTHOR MAKES  NO REPRESENTATIONS OR WARRANTIES,
 * EITHER EXPRESS OR IMPLIED.  TO THE EXTENT NOT PROHIBITED BY LAW, IN NO EVENT WILL THE
 * AUTHOR  BE LIABLE FOR ANY DAMAGES, INCLUDING WITHOUT LIMITATION, LOST REVENUE,  PROFITS
 * OR DATA, OR FOR SPECIAL, INDIRECT, CONSEQUENTIAL, INCIDENTAL  OR PUNITIVE DAMAGES,
 * HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF  LIABILITY, ARISING OUT OF OR RELATED TO
 * ANY FURNISHING, PRACTICING, MODIFYING OR ANY USE OF THE SOFTWARE, EVEN IF THE AUTHOR
 * HAVE BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 */
package net.sourceforge.metrics.calculators;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.sourceforge.metrics.calculators.CohesionCalculator.CohesionPreferences;
import net.sourceforge.metrics.core.Log;
import net.sourceforge.metrics.core.sources.TypeMetrics;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

/**
 * This class maintains information about the members accessed by methods within
 * a class, for example, the attributes that are directly accessed by a method.
 * This information is particularly useful for computing several cohesion
 * metrics.
 * 
 * @author Keith Cassell
 */
public class CallData {
	/**
	 * A ConnectivityMatrix keeps track of connectivity data, i.e. what objects
	 * are connected to what other objects. The meaning of "connected" is
	 * determined by the user of the class. For example, connected might mean
	 * "calls directly", "reachable from", or any other relation.
	 * 
	 * The underlying data is stored in a two dimensional array. A nonzero entry
	 * in position matrix[x,y] indicates that x is connected to y.
	 * 
	 * @author Keith Cassell
	 * 
	 */
	static public class ConnectivityMatrix {

		public static final int CONNECTED = 1;
		public static final int DISCONNECTED = 0;

		/** The raw data. For now at least, a square matrix. */
		protected int[][] matrix;

		/** The members used for row and column headers. */
		protected List<IMember> headers;

		/** keeps track of which index in the array corresponds to each member. */
		protected HashMap<IMember, Integer> memberIndex = new HashMap<IMember, Integer>();

		/**
		 * Builds a connectivity matrix using the headers provided. All entries
		 * in the matrix are initialized to "not connected".
		 * 
		 * @param headers
		 *            the members to use as headers
		 */
		public ConnectivityMatrix(List<IMember> headers) {
			this.headers = headers;
			int index = 0;

			// Keep track of which index in the array corresponds to each member
			for (IMember member : headers) {
				memberIndex.put(member, Integer.valueOf(index++));
			}

			int size = headers.size();
			matrix = new int[size][size];

			// Initialize matrix to no connections
			for (int i = 0; i < size; i++) {
				for (int j = 0; j < size; j++) {
					matrix[i][j] = DISCONNECTED;
				}
			}
		}

		/**
		 * Builds a connectivity matrix using the headers provided. All entries
		 * in the matrix are initialized to "not connected".
		 * 
		 * @param headers
		 *            the members to use as headers
		 */
		public ConnectivityMatrix(ConnectivityMatrix original) {
			this.headers = original.headers;
			int index = 0;

			// Keep track of which index in the array corresponds to each member
			for (IMember member : headers) {
				memberIndex.put(member, Integer.valueOf(index++));
			}

			int size = headers.size();
			this.matrix = new int[size][size];

			// Initialize matrix to no connections
			for (int i = 0; i < size; i++) {
				for (int j = 0; j < size; j++) {
					this.matrix[i][j] = original.matrix[i][j];
				}
			}
		}

		/**
		 * Builds a matrix where the element at [row, column] is 1 if the member
		 * at [row] is a method that directly accesses the member at [column],
		 * and 0 otherwise.
		 * 
		 * If connectInterfaceMethods is true, it will also mark as connected
		 * those methods that are required from the same interface.
		 * 
		 * @param callData the data from which to build the matrix 
		 * @return the adjacency matrix
		 */
		private static ConnectivityMatrix buildAdjacencyMatrix(
				CallData callData) {
			Map<IField, HashSet<IMethod>> attributeAccessedByMap =
				callData.getAttributeAccessedByMap();
			Map<IMethod, HashSet<IMethod>> methodCalledByMap =
				callData.getMethodCalledByMap();
			Set<IField> attributeKeySet = attributeAccessedByMap.keySet();
			ArrayList<IMember> headers = new ArrayList<IMember>(attributeKeySet);
			Set<IMethod> methodKeySet = methodCalledByMap.keySet();
			headers.addAll(methodKeySet);
			ConnectivityMatrix matrix = new ConnectivityMatrix(headers);

			matrix.populateAdjacencies(callData);
			return matrix;
		}

		/**
		 * Populates the matrix using data from the maps
		 * The element at [row, column] is 1 if the member
		 * at [row] is a method that directly accesses the member at [column],
		 * and 0 otherwise.
		 * 
		 * If connectInterfaceMethods is true, it will also mark as connected
		 * those methods that are required from the same interface.
		 * @param callData the class's call data
		 */
		private void populateAdjacencies(CallData callData) {
			markAccessedAttributesConnected(callData);
			markCalledMethodsConnected(callData);
			
			if (!callData.useOriginalDefinitions && callData.connectInterfaceMethods) {
				markInterfaceMethodsConnected(callData);
			}
		}

		/**
		 * Connect methods with the attributes they access by putting an
		 * entry in the matrix.
		 * @param callData the class's call data
		 */
		private void markAccessedAttributesConnected(CallData callData) {
			Map<IField, HashSet<IMethod>> attributeAccessedByMap =
				callData.getAttributeAccessedByMap();
			Set<Entry<IField, HashSet<IMethod>>> attributeEntrySet =
				attributeAccessedByMap.entrySet();

			// Connect methods with the attributes they access
			for (Entry<IField, HashSet<IMethod>> entry : attributeEntrySet) {
				HashSet<IMethod> callers = entry.getValue();
				IField field = entry.getKey();
				int fieldIndex = getIndex(field);

				for (IMethod caller : callers) {
					if (caller != null) {
						int methodIndex = getIndex(caller);

						if (methodIndex >= 0) {
							matrix[methodIndex][fieldIndex] = CONNECTED;
						}
					}
				}
			}
		}

		/**
		 * Connect methods with the methods they call by putting an
		 * entry in the matrix.
		 * @param callData the class's call data
		 */
		private void markCalledMethodsConnected(CallData callData) {
			Map<IMethod, HashSet<IMethod>> methodCalledByMap =
				callData.getMethodCalledByMap();
			Set<Entry<IMethod, HashSet<IMethod>>> methodEntrySet = methodCalledByMap
					.entrySet();

			for (Entry<IMethod, HashSet<IMethod>> entry : methodEntrySet) {
				HashSet<IMethod> callers = entry.getValue();
				IMethod callee = entry.getKey();
				int calleeIndex = getIndex(callee);

				for (IMethod caller : callers) {
					if (caller != null) {
						int callerIndex = getIndex(caller);
						if (callerIndex >= 0) {
							matrix[callerIndex][calleeIndex] = CONNECTED;
						}
					}
				}
			}	// for - connect methods
		}

		/**
		 * Mark as connected methods that are from the same interface by putting an
		 * entry in the matrix.
		 * @param callData the class's call data
		 */
		private void markInterfaceMethodsConnected(CallData callData) {
			Set<IMethod> methodSet = callData.getMethods();
			ArrayList<IMethod> methodList = new ArrayList<IMethod>(methodSet);
			try {
				IType[] interfaces = callData.getInterfaces();

				if (interfaces.length > 0) {
					int arraySize = methodList.size();
					for (int i = 0; i < arraySize - 1; i++) {
						IMethod iMethod = methodList.get(i);
						IType iType = getDeclaringInterface(iMethod, interfaces);

						if ((iType != null) && iType.isInterface()) {
							for (int j = i + 1; j < arraySize; j++) {
								IMethod jMethod = methodList.get(j);
								IType jType = getDeclaringInterface(jMethod,
										interfaces);
								if (iType.equals(jType)) {
									int iIndex = getIndex(iMethod);
									int jIndex = getIndex(jMethod);
									matrix[iIndex][jIndex] = CONNECTED;
									matrix[jIndex][iIndex] = CONNECTED;
								}
							}
						}
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		/**
		 * Builds a reachability matrix from an adjacency matrix where the
		 * element at [row, column] is 1 if the member at [row] is a method that
		 * (directly or indirectly) accesses the member at [column], and 0
		 * otherwise. This method uses Warshall's algorithm for transitive
		 * closure,
		 * 
		 * @param adjMatrix
		 *            adjacency matrix containing information about direct
		 *            connections
		 * @return the reachability matrix
		 */
		public static ConnectivityMatrix buildReachabilityMatrix(
				ConnectivityMatrix adjMatrix) {
			ConnectivityMatrix rMatrix = new ConnectivityMatrix(adjMatrix);
			int max = rMatrix.headers.size();

			for (int k = 0; k < max; k++) {
				for (int i = 0; i < max; i++) {
					if (rMatrix.matrix[i][k] == 1) {
						for (int j = 0; j < max; j++) {
							if (rMatrix.matrix[k][j] == 1) {
								rMatrix.matrix[i][j] = 1;
							}
						} // for k
					} // if there is a connection
				} // for j
			} // for i
			return rMatrix;
		}

	    /**
	     * Builds the matrix indicating methods that are directly connected, i.e,
	     * methods that either directly or indirectly access at least one common
	     * member.
	     * @param callData contains information about which
	     *   methods access which members
	     * @param methodsToEval the members for which connectivity information
	     * is desired.  (Methods like constructors, toString, etc. may not be
	     * included in this list.)
	     * @return the matrix indicating methods that are directly connected
	     */
		private static ConnectivityMatrix buildDirectlyConnectedMatrix(
				CallData callData, List<Integer> methodsToEval) {
			ConnectivityMatrix reachabilityMatrix = callData
					.getReachabilityMatrix();
			List<IMember> headers = getMatrixHeaders(reachabilityMatrix,
					methodsToEval);
			ConnectivityMatrix directlyConnectedMatrix = new ConnectivityMatrix(
					headers);

			for (int i = 0; i < methodsToEval.size(); i++) {
				Integer methodIndexI = methodsToEval.get(i);
				Set<Integer> iFields = getMembersAccessedBy(methodIndexI.intValue(),
						reachabilityMatrix);

				if (iFields != null) {
					for (int j = i + 1; j < methodsToEval.size(); j++) {
						Integer methodIndexJ = methodsToEval.get(j);
						Set<Integer> jFields = getMembersAccessedBy(
								methodIndexJ.intValue(), reachabilityMatrix);
						markDirectlyConnectedMethods(directlyConnectedMatrix,
								i, iFields, j, jFields);
					} // for j
				} // if iFields != null
			} // for i
			return directlyConnectedMatrix;
		}

	    /**
	     * Return the methods that will serve as headers for the matrix.
	     * @param connectityMatrix the matrix containing
	     * the source information for headers
	     * @param methodsToEval the indices of the methods to evaluate in
	     *    the connectivity matrix
	     * @return the methods to evaluate
	     */
	    private static List<IMember> getMatrixHeaders(
		    ConnectivityMatrix connectityMatrix,
		    List<Integer> methodsToEval) {
		List<IMember> reachabilityHeaders = connectityMatrix.getHeaders();
		ArrayList<IMember> headers = new ArrayList<IMember>();
		for (int i = 0; i < methodsToEval.size(); i++) {
		    Integer method = methodsToEval.get(i);
		    IMember member = reachabilityHeaders.get(method.intValue());
		    headers.add(member);
		}
		return headers;
	    }
	    
	    /**
	     * Add an entry to the matrix if both methods access some common member
	     * @param directlyConnectedMatrix the matrix to modify
	     * @param i the index for method i
	     * @param iMembers the members accessed by method i
	     * @param j the index for method j
	     * @param jMembers the members accessed by method j
	     */
	    private static void markDirectlyConnectedMethods(
		    ConnectivityMatrix directlyConnectedMatrix, int i,
		    Set<Integer> iMembers, int j, Set<Integer> jMembers) {
		// Determine whether there are commonly accessed members
		if (jMembers != null) {
		    HashSet<Integer> intersection = new HashSet<Integer>(jMembers);
		    intersection.retainAll(iMembers);

		    // Mark connected if the methods access some of the
		    // same members. This is nondirectional, so we
		    // mark the matrix in two places
		    if (intersection.size() != 0) {
		    	directlyConnectedMatrix.matrix[i][j] = ConnectivityMatrix.CONNECTED;
		    	directlyConnectedMatrix.matrix[j][i] = ConnectivityMatrix.CONNECTED;
		    }
		} // if jFields != null
	    }
	    
	    /**
	     * Collects the members reachable from the indicated method.
	     * @param methodIndex the index of a method in the reachability matrix
	     * in the reachability matrix
	     * @param reachabilityMatrix
	     * @return the indices of the members accessed by the method
	     */
	    private static Set<Integer> getMembersAccessedBy(
		    int methodIndex,
		    ConnectivityMatrix reachabilityMatrix) {
		HashSet<Integer> memberIndices = new HashSet<Integer>();
		for (int i = 0; i < reachabilityMatrix.matrix.length; i++) {
		    if (reachabilityMatrix.matrix[methodIndex][i] ==
		            CallData.ConnectivityMatrix.CONNECTED) {
			memberIndices.add(Integer.valueOf(i));
		    }
		}
		return memberIndices;
	    }
	    
		/**
		 * Gets the index corresponding to the supplied member.
		 * 
		 * @param member
		 *            the member whose index value is being searched for
		 * @return the nonnegative index if the index exists; negative otherwise
		 */
		public int getIndex(IMember member) {
			int index = -1;

			Integer indexInteger = memberIndex.get(member);
			// For some reason, some methods within anonymous classes don't get
			// indexed properly.
			// TODO figure out why error can occur and/or write to log
			if (indexInteger != null) {
				index = indexInteger.intValue();
			}
			return index;
		}

		/**
		 * @return the headers
		 */
		public List<IMember> getHeaders() {
			return headers;
		}

		/**
		 * @return a human-readable form of the matrix
		 */
		public String toString() {
			StringBuffer buf = new StringBuffer("ConnectivityMatrix@"
					+ hashCode() + "\n");
			int size = headers.size();
			// print rows
			for (int i = 0; i < size; i++) {
				buf.append(" ").append(i).append(" ");
				String member = headers.get(i).getElementName();
				member = String.format("%-10.10s", member);
				buf.append(member);
				for (int j = 0; j < size; j++) {
					buf.append(" ").append(matrix[i][j]);
				}
				buf.append("\n");
			}
			return buf.toString();
		}

	} // class ConnectivityMatrix

	/** The Eclipse signature for the Java logger. */
	private static String loggerSignature = null;

	/** The type whose data we're collecting. */
	protected IType type = null;
	
	/** The set of known attributes (fields). */
	protected HashSet<IField> attributes = new HashSet<IField>();

	/** The set of known methods. */
	protected HashSet<IMethod> methods = new HashSet<IMethod>();

	/**
	 * The methods directly called by a method. The key is the calling method.
	 * The value is the set of methods that it calls.
	 */
	protected HashMap<IMethod, HashSet<IMethod>> methodsCalledMap =
		new HashMap<IMethod, HashSet<IMethod>>();

	/**
	 * The attributes directly accessed by a method. The key is the calling
	 * method. The value is the set of attributes that it accesses.
	 */
	protected HashMap<IMethod, HashSet<IField>> attributesAccessedMap =
		new HashMap<IMethod, HashSet<IField>>();

	/**
	 * The direct callers of a method. The key is a method that gets called by
	 * some method. The value is the set of methods that directly call it.
	 */
	protected HashMap<IMethod, HashSet<IMethod>> methodCalledByMap =
		new HashMap<IMethod, HashSet<IMethod>>();

	/**
	 * The direct accessors of an attribute (field). The key is an attribute
	 * that gets accessed by some method. The value is the set of methods that
	 * directly access it.
	 */
	protected HashMap<IField, HashSet<IMethod>> attributeAccessedByMap =
		new HashMap<IField, HashSet<IMethod>>();

	/**
	 * Keeps track of the direct connections between methods and other methods
	 * and attributes.
	 */
	protected ConnectivityMatrix adjacencyMatrix = null;

	/**
	 * Keeps track of the indirect connections between methods and other methods
	 * and attributes.
	 */
	protected ConnectivityMatrix reachabilityMatrix = null;
	
	/**
	 * Keeps track of the direct connections between methods and other methods
	 * and attributes for the Badri cohesion metrics (DCD, DCI).
	 */
	protected ConnectivityMatrix badriDirectlyConnectedMatrix = null;

	/** If this is "true", the original metric definitions are
	 * used; otherwise, the user has the options of deciding
	 * which members are considered in the calculations by setting
	 * the other preferences.  */
	protected boolean useOriginalDefinitions = true;

	/** Indicates whether methods imposed by interfaces should be
	 * considered as connected (as though they called each other). */
	protected boolean connectInterfaceMethods = false;

	/** Indicates whether abstract methods should be considered. */
	protected boolean countAbstractMethods = false;

	/** Indicates whether constructors should be considered. */
	protected boolean countConstructors = false;

	/** Indicates whether deprecated methods should be considered. */
	protected boolean countDeprecatedMethods = true;

	/** NYI - Indicates whether inherited attributes should be included. */
	protected boolean countInheritedAttributes = false;

	/** NYI - Indicates whether inherited methods should be included. */
	protected boolean countInheritedMethods = false;

	/** Indicates whether logger fields should be included. */
	protected boolean countLoggers = true;

	/** Indicates whether methods declared by the Object Class
	 * (toString, etc.) should be considered. */
	protected boolean countObjectsMethods = true;

	/** Indicates whether only public methods should be considered
	 * in relationships with the attributes. */
	protected boolean countPublicMethodsOnly = false;

	/** Indicates whether static attributes should be considered. */
	protected boolean countStaticAttributes = true;

	/** Indicates whether static methods should be considered. */
	protected boolean countStaticMethods = true;
	
	/**
	 * NYI - Indicates whether members of inner classes should be treated the same as
	 * members of the outer class. */
	protected boolean countInners = false;

	
	/**
	 * Gathers call information about the given class and stores the information
	 * about the members, attributes (fields), and call relationships between
	 * them. This information is obtainable via the various get* methods.
	 * 
	 * @param source
	 *            the class to analyze
	 * @throws JavaModelException generally when the project can not be found
	 */
	public void collectCallData(TypeMetrics source, CohesionPreferences prefs)
	throws JavaModelException {
		// Initialize the logger signature once only
		if (loggerSignature == null) {
			String loggerTypeSignature =
				Signature.createTypeSignature("java.util.logging.Logger", false);
			loggerSignature = Signature.getSignatureSimpleName(loggerTypeSignature);
		}
		
		setPreferences(prefs);
		IType type = (IType) source.getJavaElement();
		collectCallData(type);
	}

	private void setPreferences(CohesionPreferences prefs) {
		// Retrieve and cache the preferences
		if (prefs != null) {
			useOriginalDefinitions = prefs.getUseOriginalDefinitions();
			connectInterfaceMethods = prefs.getConnectInterfaceMethods();
			countAbstractMethods = prefs.getCountAbstractMethods();
			countConstructors = prefs.getCountConstructors();
			countDeprecatedMethods = prefs.getCountDeprecatedMethods();
			countInheritedAttributes = prefs.getCountInheritedAttributes();
			countInheritedMethods = prefs.getCountInheritedMethods();
			countInners = prefs.getCountInners();
			countLoggers = prefs.getCountLoggers();
			countObjectsMethods = prefs.getCountObjectsMethods();
			countPublicMethodsOnly = prefs.getCountPublicMethodsOnly();
			countStaticAttributes = prefs.getCountStaticAttributes();
			countStaticMethods = prefs.getCountStaticMethods();
		}
	}

	/**
	 * Gathers call information about the given class and stores the information
	 * about the members, attributes (fields), and call relationships between
	 * them. This information is obtainable via the various get* methods.
	 * 
	 * @param type
	 *            the class to analyze
	 * @throws JavaModelException generally when the project can not be found
     */
	public void collectCallData(IType type) throws JavaModelException {
		this.type = type;
		IJavaSearchScope scope =
			SearchEngine.createJavaSearchScope(new IJavaElement[] { type });
		MethodCollector methodCollector = new MethodCollector();
		SearchEngine searchEngine = new SearchEngine();
		SearchParticipant[] participants =
			new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
		try {
			collectMethodCallData(type, searchEngine, participants, scope, methodCollector);
			collectFieldCallData(type, searchEngine, participants, scope, methodCollector);
		} catch (JavaModelException e) {
			Log.logError("collectCallData failed for " + type.getElementName()
					+ ":\n", e);
			throw e;
		}
	}

	/**
	 * Gathers call information about methods calling methods within the given
	 * class. This information is stored in the methodCalledByMap and
	 * methodsCalledMap.
	 * 
	 * @param type the class to analyze
	 * @param searchEngine
	 * @param participants
	 * @param scope the elements being examined, e.g. this class or this package
	 * @param methodCollector gathers the search results
	 * @return the collection of methods that access the indicated member
	 */
	private void collectMethodCallData(IType type,
			SearchEngine searchEngine,
			SearchParticipant[] participants,
			IJavaSearchScope scope,
			MethodCollector methodCollector)
			throws JavaModelException {
		IMethod[] typeMethods = type.getMethods();

		// Add the method to the methods field
		// unless it is static and the user has specified no statics
		for (int i = 0; i < typeMethods.length; i++) {
			if (acceptMethod(typeMethods[i])) {
				methods.add(typeMethods[i]);
			}
		}

		// Update the stored information about the methods of the class
		for (IMethod method : methods) {
			// Update the methodCalledByMap for typeMethods[i]
			Set<IMethod> callers =
				getCallingMethods(method, searchEngine, participants, scope, methodCollector);
			methodCalledByMap.put(method, new HashSet<IMethod>(callers));

			// Update the methodsCalledMap for method
			for (IMethod caller : callers) {
				HashSet<IMethod> calleesL = methodsCalledMap.get(caller);
				if (calleesL == null) {
					calleesL = new HashSet<IMethod>();
				}
				calleesL.add(method);
				methodsCalledMap.put(caller, calleesL);
			}
		}
	}

	/**
	 * Decides whether to accept a method for use in the cohesion calculations
	 * based on the user preferences.
	 * NOTE:  Some methods can be filtered out at collection time, others
	 * must be filtered out at calculation time.  For example, a user may
	 * desire to base the cohesion scores on public methods; however, we
	 * still need to collect the non-public methods here, because they may
	 * connect the public methods to the attributes.
	 * @param method the method to check
	 * @return true if method is to be used; false otherwise.
	 * @throws JavaModelException
	 */
	private boolean acceptMethod(IMethod method) throws JavaModelException {
		boolean accept = useOriginalDefinitions;
		
		if (!useOriginalDefinitions) {
			int flags = method.getFlags();
			accept =
				(countAbstractMethods || !Flags.isAbstract(flags))
				&& (countConstructors || !method.isConstructor())
				&& (countDeprecatedMethods || !Flags.isDeprecated(flags))
				// TODO more filters
				// && (countInheritedMethods || !isInherited(method))
				// && countInners
				&& (countObjectsMethods || !CallData.isObjectMethod(method))
				&& (countStaticMethods || !Flags.isStatic(flags));
			}
		return accept;
	}

	/**
	 * Gathers call information about methods accessing fields within the given
	 * class. This information is stored in the attributeAccessedByMap and
	 * attributesAccessedMap.
	 * 
	 * @param type
	 *            the class to analyze
	 * @param searchEngine
	 * @param participants
	 * @param scope the elements being examined, e.g. this class or this package
	 * @param methodCollector gathers the search results
	 */
	private void collectFieldCallData(IType type,
			SearchEngine searchEngine,
			SearchParticipant[] participants,
			IJavaSearchScope scope,
			MethodCollector methodCollector)
			throws JavaModelException {
		IField[] typeFields = type.getFields();

		// Add the attribute to the attributes field
		// unless it is static and the user has specified no statics
		for (int i = 0; i < typeFields.length; i++) {
			if (acceptField(typeFields[i])) {
				attributes.add(typeFields[i]);
			}
		}

		// Update the stored information about the fields of the class
		for (IField attribute : attributes) {
			// Update the fieldCalledByMap for attribute
			Set<IMethod> callers =
				getCallingMethods(attribute, searchEngine, participants, scope, methodCollector);
			attributeAccessedByMap
					.put(attribute, new HashSet<IMethod>(callers));

			// Update the fieldsCalledMap for attribute
			for (IMethod caller : callers) {
				HashSet<IField> calleesL = attributesAccessedMap.get(caller);
				if (calleesL == null) {
					calleesL = new HashSet<IField>();
				}
				calleesL.add(attribute);
				attributesAccessedMap.put(caller, calleesL);
			}
		}
	}

	/**
	 * Decides whether to accept a field for use in the cohesion calculations
	 * based on the user preferences.
	 * @param field the field to check
	 * @return true if field is to be used; false otherwise.
	 * @throws JavaModelException
	 */
	private boolean acceptField(IField field) throws JavaModelException {
		boolean accept = useOriginalDefinitions;
		
		if (!useOriginalDefinitions) {
			int flags = field.getFlags();
			accept =
				// (countInheritedAttributes || !isInherited(field))
				// TODO countInners
				(countLoggers || !isLogger(field))
				&& (countStaticAttributes || !Flags.isStatic(flags));
		}
		return accept;
	}

	/**
	 * Save this call graph to a file in a tabular form, where the first column
	 * consists of the method signature and the second column consists of either
	 * (1) a called method's signature or (2) a field accessed by the method
	 * 
	 * @param fileName
	 *            the file to write
	 * @param sep
	 *            the separator between the columns, e.g. a "," for a CSV file
	 */
	public void saveCallGraph(String fileName, String sep) {
		FileWriter writer = null;
		try {
			writer = new FileWriter(new File(fileName));
			writer.write(attributesAccessedMapToTableString(sep));
			writer.write(methodsCalledMapToTableString(sep));
		} catch (IOException e) {
			Log.logError("unable to send output to file " + fileName, e);
		} catch (JavaModelException e) {
			Log.logError("JavaModelException: ", e);
		} finally {
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (IOException e) {
				Log.logError("unable to close file " + fileName, e);
			}
		}
	}
	
	/**
	 * Collects the methods that access the specified member
	 * @param member the field or method whose accessors are being determined
	 * @param scope the elements being examined, e.g. this class or this package
	 * @return the collection of methods that access the indicated member
	 */
	public Set<IMethod> getCallingMethods(
			IMember member,
			IJavaSearchScope scope) {
		MethodCollector methodCollector = new MethodCollector();
		SearchEngine searchEngine = new SearchEngine();
		SearchParticipant[] participants =
			new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
		Set<IMethod> callers =
			getCallingMethods(member, searchEngine, participants,
					scope, methodCollector);
		return callers;
	}

	/**
	 * Collects the methods that access the specified member
	 * @param member the field or method whose accessors are being determined
	 * @param searchEngine
	 * @param participants
	 * @param scope the elements being examined, e.g. this class or this package
	 * @param methodCollector gathers the search results
	 * @return the collection of methods that access the indicated member
	 */
	protected Set<IMethod> getCallingMethods(
			IMember member,
			SearchEngine searchEngine,
			SearchParticipant[] participants,
			IJavaSearchScope scope,
			MethodCollector methodCollector) {
		try {
			SearchPattern callingMethodPattern =
				SearchPattern.createPattern(member, IJavaSearchConstants.REFERENCES);
			searchEngine.search(callingMethodPattern, participants, scope,
					methodCollector, null);
		} catch (CoreException e) {
			Log.logError("getCallingMethods failed for "
					+ member.getElementName(), e);
		}
		Set<IMethod> callers = methodCollector.getResults();
		return callers;
	}
	
	/**
	 * Gets all the interfaces implemented by the type, either
	 * directly or indirectly via a superclass.
	 * @return the interfaces
	 * @throws JavaModelException
	 */
	public IType[] getInterfaces() throws JavaModelException {
		ITypeHierarchy typeHierarchy = type.newSupertypeHierarchy(null);
		IType[] interfaces = typeHierarchy.getAllSuperInterfaces(type);
		return interfaces;
	}
	
	/**
	 * Find the interface that defines the method, if any.
	 * @param method the method we're searching on
	 * @param interfaces the interfaces to consider
	 * @return the interface with a matching method or null if none exist
	 * @throws JavaModelException
	 */
	private static IType getDeclaringInterface(IMethod method, IType[] interfaces)
	throws JavaModelException {
		IType ifc = null;
		
		for (int i = 0; ((i < interfaces.length) && (ifc == null)); i++) {
			IMethod[] methods = interfaces[i].getMethods();
			for (IMethod ifcMethod : methods) {
				if (method.isSimilar(ifcMethod)) {
					ifc = interfaces[i];
				}
			}
		}
		return ifc;
	}

	/**
	 * Gets a human readable version of the method signature.
	 */
	protected String getSignature(IMethod method) throws JavaModelException {
		String methodName = method.getElementName();
		String sig = Signature.toString(method.getSignature(), methodName,
				null, // parameterNames,
				true, // fullyQualifyTypeNames,
				false); // includeReturnType
		return sig;
	}

	/**
	 * @return the attributes
	 */
	public Set<IField> getAttributes() {
		return attributes;
	}

	/**
	 * @return the methods
	 */
	public Set<IMethod> getMethods() {
		return methods;
	}

	/**
	 * @return the methodsCalledMap
	 */
	public Map<IMethod, HashSet<IMethod>> getMethodsCalledMap() {
		return methodsCalledMap;
	}

	/**
	 * @return the attributesAccessedMap
	 */
	public Map<IMethod, HashSet<IField>> getAttributesAccessedMap() {
		return attributesAccessedMap;
	}

	/**
	 * @return the methodCalledByMap
	 */
	public Map<IMethod, HashSet<IMethod>> getMethodCalledByMap() {
		return methodCalledByMap;
	}

	/**
	 * @return the attributeAccessedByMap
	 */
	public Map<IField, HashSet<IMethod>> getAttributeAccessedByMap() {
		return attributeAccessedByMap;
	}

	/**
	 * Uses the jdt searchengine to collect all methods inside a class that call
	 * the specified method
	 * 
	 * @author Frank Sauer
	 * 
	 */
	private class MethodCollector extends SearchRequestor {
		protected Set<IMethod> results = null;

		public MethodCollector() {
		}

		public Set<IMethod> getResults() {
			return results;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jdt.core.search.SearchRequestor#beginReporting()
		 */
		public void beginReporting() {
			results = new HashSet<IMethod>();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jdt.core.search.SearchRequestor#acceptSearchMatch(org
		 * .eclipse.jdt.core.search.SearchMatch)
		 */
		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			Object matchingElement = match.getElement();

			if (matchingElement instanceof IMethod) {
				IMethod method = (IMethod) matchingElement;
				if (acceptMethod(method)) {
					results.add(method);
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jdt.core.search.SearchRequestor#endReporting()
		 */
		public void endReporting() {
		}

	}	// MethodCollector

	/**
	 * This provides a mechanism for writing out the methodsCalledMap in a
	 * tabular manner suitable for databases.
	 * 
	 * @param sep
	 *            the separator string, e.g. "," for a CSV file
	 * @return a string representation of the methodsCalledMap
	 * @throws JavaModelException
	 */
	private String methodsCalledMapToTableString(String sep)
			throws JavaModelException {
		StringBuffer buf = new StringBuffer();
		Set<IMethod> keySet = methodsCalledMap.keySet();

		for (IMethod caller : keySet) {
			String sCaller = getSignature(caller);
			HashSet<IMethod> callees = methodsCalledMap.get(caller);

			for (IMethod callee : callees) {
				buf.append(sCaller);
				buf.append(sep);
				buf.append(getSignature(callee));
				buf.append("\n");
			}
		}
		return buf.toString();
	}

	/**
	 * This provides a mechanism for generating a human-readable version of the
	 * methodsCalledMap
	 * 
	 * @return a string representation of the methodsCalledMap
	 */
	private String methodsCalledMapToString() {
		StringBuffer buf = new StringBuffer("methodsCalledMap:\n");
		Set<IMethod> keySet = methodsCalledMap.keySet();

		for (IMethod caller : keySet) {
			String sCaller;
			try {
				sCaller = getSignature(caller);
			} catch (JavaModelException e) {
				sCaller = caller.toString();
			}
			HashSet<IMethod> callees = methodsCalledMap.get(caller);
			buf.append(sCaller);
			buf.append(": ");
			buf.append(callees.toString());
			buf.append("\n");
		}
		return buf.toString();
	}

	/**
	 * This provides a mechanism for writing out the attributesAccessedMap in a
	 * tabular manner suitable for databases.
	 * 
	 * @param sep
	 *            the separator string, e.g. "," for a CSV file
	 * @return a string representation of the attributesAccessedMap
	 * @throws JavaModelException
	 */
	private String attributesAccessedMapToTableString(String sep)
			throws JavaModelException {
		StringBuffer buf = new StringBuffer();
		Set<IMethod> keySet = attributesAccessedMap.keySet();

		for (IMethod caller : keySet) {
			String sCaller = getSignature(caller);
			HashSet<IField> callees = attributesAccessedMap.get(caller);

			for (IField callee : callees) {
				buf.append(sCaller);
				buf.append(sep);
				buf.append(callee.getElementName());
				buf.append("\n");
			}
		}
		return buf.toString();
	}

	/**
	 * This provides a mechanism for generating a human-readable version of the
	 * attributesAccessedMap
	 * 
	 * @return a string representation of the attributesAccessedMap
	 */
	private String attributesAccessedMapToString() {
		StringBuffer buf = new StringBuffer("attributesAccessedMap:\n");
		Set<IMethod> keySet = attributesAccessedMap.keySet();

		for (IMethod caller : keySet) {
			String sCaller;
			try {
				sCaller = getSignature(caller);
			} catch (JavaModelException e) {
				sCaller = caller.toString();
			}
			HashSet<IField> callees = attributesAccessedMap.get(caller);
			buf.append(sCaller);
			buf.append(": ");
			buf.append(callees.toString());
			buf.append("\n");
		}
		return buf.toString();
	}

	/**
	 * @return a human-readable form of the call data
	 */
	@Override
	public String toString() {
		String result = "CallData@" + hashCode() + "\n"
				+ attributesAccessedMapToString() + methodsCalledMapToString();
		return result;
	}

	/**
	 * Return the adjacency matrix, constructing it if necessary.
	 * 
	 * @return the adjacency matrix
	 */
	public ConnectivityMatrix getAdjacencyMatrix() {
		if (adjacencyMatrix == null) {
			adjacencyMatrix = ConnectivityMatrix.buildAdjacencyMatrix(this);
		}
		return adjacencyMatrix;
	}

	/**
	 * Return the reachability matrix, constructing it if necessary.
	 * 
	 * @return the reachability matrix
	 */
	public ConnectivityMatrix getReachabilityMatrix() {
		if (reachabilityMatrix == null) {
			getAdjacencyMatrix();
			reachabilityMatrix = ConnectivityMatrix
					.buildReachabilityMatrix(adjacencyMatrix);
		}
		return reachabilityMatrix;
	}

	/**
	 * Return the badriDirectlyConnected matrix, constructing it if necessary.
	 * @return the badriDirectlyConnected matrix
	 */
	public ConnectivityMatrix getBadriDirectlyConnectedMatrix(
			List<Integer> methodsToEval) {
		if (badriDirectlyConnectedMatrix == null) {
			badriDirectlyConnectedMatrix = ConnectivityMatrix
					.buildDirectlyConnectedMatrix(this, methodsToEval);
		}
		return badriDirectlyConnectedMatrix;
	}

	/**
	 * Determines whether the supplied handle matches one of the methods
	 * defined on Object that can be overridden (clone, equals, hashCode,
	 * toString).
	 * @param sig the Eclipse handle
	 * @return true if an Object method; false otherwise
	 * @throws JavaModelException 
	 */
	public static boolean isObjectMethod(IMethod method) throws JavaModelException {
		// method.isSimilar(superMethod)
		String sig = method.getHandleIdentifier();
		boolean result =
			sig.endsWith("~hashCode")
			|| sig.endsWith("~equals~QObject;")
			|| sig.endsWith("~clone")
			|| sig.endsWith("~toString");
		return result;
	}

	/**
	 * @param member the method or attribute whose status we're checking
	 * @return true if the member was declared on a supertype; false otherwise
	 * @throws JavaModelException
	 */
	private boolean isInherited(IMember member)
	throws JavaModelException {
		// TODO determine whether this will be true for members
		// defined locally but declared in a superclass
		IType declaringType = member.getDeclaringType();
		ITypeHierarchy typeHierarchy = type.newSupertypeHierarchy(null);
		IType[] supertypes = typeHierarchy.getAllSupertypes(type);
		boolean hasSuper = false;
		int i = 0;
		while (!hasSuper && i < supertypes.length) {
			hasSuper = declaringType.equals(supertypes[i]);
			i++;
		}
		return hasSuper;
	}
	
	/**
	 * Determines whether this field is a (java.util) logger
	 * @param field
	 * @return true if a logger, false otherwise
	 */
	public static boolean isLogger(IField field) {
		boolean isLogger =  false;
		try {
			String typeSignature = field.getTypeSignature();
			String simpleName = Signature.getSignatureSimpleName(typeSignature);
			isLogger = (loggerSignature != null)
				&& loggerSignature.equals(simpleName);
		} catch (JavaModelException e) {
			Log.logError("isLogger() failure: ", e);
		}
		return isLogger;
	}

}
