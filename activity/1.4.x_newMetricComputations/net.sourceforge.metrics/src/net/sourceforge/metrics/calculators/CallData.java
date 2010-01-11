/*
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
 *
 * $Id: LackOfCohesion.java,v 1.15 2005/01/16 21:32:04 sauerf Exp $
 */
package net.sourceforge.metrics.calculators;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sourceforge.metrics.core.Log;
import net.sourceforge.metrics.core.sources.AbstractMetricSource;
import net.sourceforge.metrics.core.sources.TypeMetrics;

import org.eclipse.core.runtime.CoreException;
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

    public void collectCallData(TypeMetrics source) {
	IType type = (IType) source.getJavaElement();

	try {
	    collectMethodCallData(source, type);
	    collectFieldCallData(source, type);
	} catch (JavaModelException e) {
		Log.logError("collectCallData failed for " +
			type.getElementName() + ":\n", e);
	    e.printStackTrace();
	}
    }

    private void collectMethodCallData(AbstractMetricSource source, IType type)
	    throws JavaModelException {
	IMethod[] typeMethods = type.getMethods();
	List<IMethod> methodList = Arrays.asList(typeMethods);
	methods.addAll(methodList);

	// Update the stored information about the methods of the class
	for (int i = 0; i < typeMethods.length; i++) {
	    // Update the methodCalledByMap for typeMethods[i]
	    Set<IMethod> callers =
		getCallingMethods(source, type, typeMethods[i]);
	    methodCalledByMap.put(typeMethods[i], new HashSet<IMethod>(callers));

	    // Update the methodsCalledMap for typeMethods[i]
	    for (IMethod caller : callers) {
		HashSet<IMethod> calleesL = methodsCalledMap.get(caller);
		if (calleesL == null) {
		    calleesL = new HashSet<IMethod>();
		}
		calleesL.add(typeMethods[i]);
		methodsCalledMap.put(caller, calleesL);
	    }
	}
    }

    private void collectFieldCallData(AbstractMetricSource source, IType type)
	    throws JavaModelException {
	IField[] typeFields = type.getFields();
	attributes.addAll(Arrays.asList(typeFields));

	// Update the stored information about the fields of the class
	for (int i = 0; i < typeFields.length; i++) {
	    // Update the fieldCalledByMap for typeFields[i]
	    Set<IMethod> callers = getCallingMethods(source, type,
		    typeFields[i]);
	    attributeAccessedByMap.put(typeFields[i], new HashSet<IMethod>(
		    callers));

	    // Update the fieldsCalledMap for typeFields[i]
	    for (IMethod caller : callers) {
		HashSet<IField> calleesL = attributesAccessedMap.get(caller);
		if (calleesL == null) {
		    calleesL = new HashSet<IField>();
		}
		calleesL.add(typeFields[i]);
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
    public HashSet<IField> getAttributes() {
	return attributes;
    }

    /**
     * @return the methods
     */
    public HashSet<IMethod> getMethods() {
	return methods;
    }

    /**
     * @return the methodsCalledMap
     */
    public HashMap<IMethod, HashSet<IMethod>> getMethodsCalledMap() {
	return methodsCalledMap;
    }

    /**
     * @return the attributesAccessedMap
     */
    public HashMap<IMethod, HashSet<IField>> getAttributesAccessedMap() {
	return attributesAccessedMap;
    }

    /**
     * @return the methodCalledByMap
     */
    public HashMap<IMethod, HashSet<IMethod>> getMethodCalledByMap() {
	return methodCalledByMap;
    }

    /**
     * @return the attributeAccessedByMap
     */
    public HashMap<IField, HashSet<IMethod>> getAttributeAccessedByMap() {
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
		results.add((IMethod) matchingElement);
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

}
