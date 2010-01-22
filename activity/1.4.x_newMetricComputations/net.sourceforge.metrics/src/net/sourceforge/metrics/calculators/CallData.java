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
import net.sourceforge.metrics.core.sources.AbstractMetricSource;
import net.sourceforge.metrics.core.sources.TypeMetrics;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
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
 * This information is particularly useful for computing several cohesion metrics.
 * 
 * @author Keith Cassell
 */
public class CallData {
    /**
     * A ConnectivityMatrix keeps track of connectivity data, i.e. what objects are
     * connected to what other objects.  The meaning of "connected" is
     * determined by the user of the class.  For example, connected might mean
     * "calls directly", "reachable from", or any other relation.
     * 
     * The underlying data is stored in a two dimensional array.  A nonzero
     * entry in position matrix[x,y] indicates that x is connected to y. 
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
	protected HashMap<IMember, Integer> memberIndex =
	    new HashMap<IMember, Integer>();

	/**
	 * Builds a connectivity matrix using the headers provided.
	 * All entries in the matrix are initialized to "not connected".
	 * @param headers the members to use as headers
	 */
	public ConnectivityMatrix(List<IMember> headers) {
	    this.headers = headers;
	    int index = 0;

	    // Keep track of which index in the array corresponds to each member
	    for (IMember member : headers) {
		memberIndex.put(member, index++);
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
	 * Builds a connectivity matrix using the headers provided.
	 * All entries in the matrix are initialized to "not connected".
	 * @param headers the members to use as headers
	 */
	public ConnectivityMatrix(ConnectivityMatrix original) {
	    this.headers = original.headers;
	    int index = 0;

	    // Keep track of which index in the array corresponds to each member
	    for (IMember member : headers) {
		memberIndex.put(member, index++);
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
	 * Builds a matrix where the element at [row, column] is 1 if
	 * the member at [row] is a method that directly accesses the
	 * member at [column], and 0 otherwise.
	 * @param attributeAccessedByMap lists the methods that access an attribute
	 * @param methodCalledByMap  lists the methods that access a method
	 * @return the adjacency matrix
	 */
	public static ConnectivityMatrix buildAdjacencyMatrix(
		HashMap<IField, HashSet<IMethod>> attributeAccessedByMap,
		HashMap<IMethod, HashSet<IMethod>> methodCalledByMap) {
	    Set<IField> attributeKeySet = attributeAccessedByMap.keySet();
	    ArrayList<IMember> headers = new ArrayList<IMember>(attributeKeySet);
	    Set<IMethod> methodKeySet = methodCalledByMap.keySet();
	    headers.addAll(methodKeySet);
	    ConnectivityMatrix matrix = new ConnectivityMatrix(headers);

	    matrix.populateAdjacencies(attributeAccessedByMap,
		    methodCalledByMap);
	    return matrix;
	}

	/**
	 * Populates the matrix using data from the maps
	 * 
	 * @param attributeAccessedByMap
	 *            info about the methods that access attributes
	 * @param methodCalledByMap
	 *            info about the methods that call other methods
	 */
	private void populateAdjacencies(
		Map<IField, HashSet<IMethod>> attributeAccessedByMap,
		Map<IMethod, HashSet<IMethod>> methodCalledByMap) {
	    Set<Entry<IField, HashSet<IMethod>>> attributeEntrySet =
		attributeAccessedByMap.entrySet();

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
	    Set<Entry<IMethod, HashSet<IMethod>>> methodEntrySet =
		methodCalledByMap.entrySet();

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
	    }
	}

	/**
	 * Builds a reachability matrix from an adjacency matrix where the element at
	 * [row, column] is 1 if the member at [row] is a method that (directly
	 * or indirectly) accesses the member at [column], and 0 otherwise.
	 * This method uses Warshall's algorithm for transitive closure, 
	 * 
	 * @param adjMatrix adjacency matrix containing information about direct connections
	 * @return the reachability matrix
	 */
	public static ConnectivityMatrix buildReachabilityMatrix(ConnectivityMatrix adjMatrix) {
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
	 * Gets the index corresponding to the supplied member.
	 * @param member the member whose index value is being searched for
	 * @return the nonnegative index if the index exists; negative otherwise
	 */
	public int getIndex(IMember member) {
	    int index = -1;

	    Integer indexInteger = memberIndex.get(member);
	    // For some reason, some methods within anonymous classes don't get
	    // indexed properly.
	    // TODO figure out why error can occur and/or write to log
	    if (indexInteger != null) {
		index = indexInteger;
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
	    StringBuffer buf =
		new StringBuffer("ConnectivityMatrix@" + hashCode() + "\n");
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

    }	// class ConnectivityMatrix
    
    
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
    protected HashMap<IMethod, HashSet<IField>> attributesAccessedMap = new HashMap<IMethod, HashSet<IField>>();

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
    
    /** Keeps track of the direct connections between methods and
     * other methods and attributes.     */
    protected ConnectivityMatrix adjacencyMatrix = null;
    
    /** Keeps track of the indirect connections between methods and
     * other methods and attributes.     */
    protected ConnectivityMatrix reachabilityMatrix = null;
    
    /** Flag to determine whether information about static attributes should be kept. */
    protected static boolean countStaticAttributes = true;

    /** Flag to determine whether information about static methods should be kept. */
    protected static boolean countStaticMethods = true;
    
    /**
     * Gathers call information about the given class and stores the
     * information about the members, attributes (fields), and call
     * relationships between them.  This information is obtainable
     * via the various get* methods.
     * @param source the class to analyze
     */
    public void collectCallData(TypeMetrics source, CohesionPreferences prefs) {
	if (prefs != null) {
	    countStaticAttributes = prefs.countStaticAttributes();
	    countStaticMethods = prefs.countStaticMethods();
	}
	IType type = (IType) source.getJavaElement();

	try {
	    collectMethodCallData(source, type);
	    collectFieldCallData(source, type);
	} catch (JavaModelException e) {
	    Log.logError("collectCallData failed for " + type.getElementName()
		    + ":\n", e);
	    e.printStackTrace();
	}
    }

    /**
     * Gathers call information about methods calling methods within the given class.
     * This information is stored in the methodCalledByMap and methodsCalledMap.
     * @param source the metric source (class)
     * @param type the class to analyze
     */
    private void collectMethodCallData(AbstractMetricSource source, IType type)
	    throws JavaModelException {
	IMethod[] typeMethods = type.getMethods();
	
	// Add the method to the methods field
	// unless it is static and the user has specified no statics
	for (int i = 0; i < typeMethods.length; i++) {
	    int flags = typeMethods[i].getFlags();
	    if (countStaticMethods || !Flags.isStatic(flags)) {
		methods.add(typeMethods[i]);
	    }
	}

	// Update the stored information about the methods of the class
	for (IMethod method : methods) {
	    // Update the methodCalledByMap for typeMethods[i]
	    Set<IMethod> callers =
		getCallingMethods(source, type, method);
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
     * Gathers call information about methods accessing
     * fields within the given class.  This information is stored in the
     * attributeAccessedByMap and attributesAccessedMap.
     * @param source the metric source (class)
     * @param type the class to analyze
     */
    private void collectFieldCallData(AbstractMetricSource source, IType type)
	    throws JavaModelException {
	IField[] typeFields = type.getFields();
	
	// Add the attribute to the attributes field
	// unless it is static and the user has specified no statics
	for (int i = 0; i < typeFields.length; i++) {
	    int flags = typeFields[i].getFlags();
	    if (countStaticAttributes || !Flags.isStatic(flags)) {
		attributes.add(typeFields[i]);
	    }
	}

	// Update the stored information about the fields of the class
	for (IField attribute : attributes) {
	    // Update the fieldCalledByMap for attribute
	    Set<IMethod> callers = getCallingMethods(source, type, attribute);
	    attributeAccessedByMap.put(attribute, new HashSet<IMethod>(callers));

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
     * Save this call graph to a file in a tabular form, where the first
     * column consists of the method signature and the second column
     * consists of either (1) a called method's signature or (2) a field
     * accessed by the method
     * @param fileName the file to write
     * @param sep the separator between the columns, e.g. a ","
     * for a CSV file
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
     * Collects the intraclass methods that access the specified member
     * 
     * @param source
     *            the code being examined
     * @param type
     *            the class being examined
     * @param member
     *            the field or method whose accessors are being determined
     * @return the collection of methods that access the indicated member
     */
    protected Set<IMethod> getCallingMethods(AbstractMetricSource source,
	    IType type, IMember member) {
	SearchPattern callingMethodPattern = SearchPattern.createPattern(
		member, IJavaSearchConstants.REFERENCES);
	IJavaSearchScope scope = SearchEngine
		.createJavaSearchScope(new IJavaElement[] { type });
	MethodCollector methodCollector = new MethodCollector(source);
	SearchEngine searchEngine = new SearchEngine();
	try {
	    SearchParticipant[] participants = new SearchParticipant[] { SearchEngine
		    .getDefaultSearchParticipant() };
	    searchEngine.search(callingMethodPattern, participants, scope,
		    methodCollector, null);
	} catch (CoreException e) {
	    Log.logError("getCallingMethods failed for " +
		    type.getElementName() + "." + member.getElementName(), e);
	}
	Set<IMethod> callers = methodCollector.getResults();
	return callers;
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
    public static class MethodCollector extends SearchRequestor {
	protected Set<IMethod> results = null;

	public MethodCollector(AbstractMetricSource source) {
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
		int flags = method.getFlags();
		if (countStaticMethods || !Flags.isStatic(flags)) {
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

    }
    
    
    /**
     * This provides a mechanism for writing out the methodsCalledMap in a
     * tabular manner suitable for databases.
     * 
     * @param sep the separator string, e.g. "," for a CSV file
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
     * @param sep the separator string, e.g. "," for a CSV file
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
	String result =
	    "CallData@" + hashCode() + "\n" +
	    attributesAccessedMapToString() +
	    methodsCalledMapToString();
	return result;
    }

    /**
     * Return the adjacency matrix, constructing it if necessary.
     * @return the adjacency matrix
     */
    public ConnectivityMatrix getAdjacencyMatrix() {
	if (adjacencyMatrix == null) {
	    adjacencyMatrix = ConnectivityMatrix.buildAdjacencyMatrix(
		    attributeAccessedByMap, methodCalledByMap);
	}
	return adjacencyMatrix;
    }

    /**
     * Return the reachability matrix, constructing it if necessary.
     * @return the reachability matrix
     */
    public ConnectivityMatrix getReachabilityMatrix() {
	if (reachabilityMatrix == null) {
	    getAdjacencyMatrix();
	    reachabilityMatrix =
		ConnectivityMatrix.buildReachabilityMatrix(adjacencyMatrix);
	}
	return reachabilityMatrix;
    }

}
