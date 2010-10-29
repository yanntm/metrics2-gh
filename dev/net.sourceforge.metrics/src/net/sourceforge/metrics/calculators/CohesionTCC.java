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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sourceforge.metrics.calculators.CallData.ConnectivityMatrix;
import net.sourceforge.metrics.core.Log;
import net.sourceforge.metrics.core.sources.AbstractMetricSource;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Calculates cohesion using Bieman and Kang's TCC metric.  TCC measures the
 * proportion of connected methods to the maximum possible number of connected
 * methods.  It considers only "visible" methods and further omits constructors
 * from consideration.  By visible, we assume they consider
 * all non-private methods.  TCC considers methods to be connected when they
 * directly access a common attribute.  With TCC, large numbers indicate more
 * cohesive classes.
 * 
 * Bieman and Kang do not specify how to treat the "abnormal" case of two or
 * fewer methods, so we give the maximal cohesion score of one for that case.
 * 
 * @see BIEMAN, J. M., AND KANG, B.-K. Cohesion and reuse in an object oriented
 *    system. SIGSOFT Softw. Eng. Notes 20, SI (1995), 259–262.
 * 
 * @author Keith Cassell
 */
public class CohesionTCC extends CohesionCalculator
{
    /**
     * Constructor for LackOfCohesion.
     */
    public CohesionTCC() {
	super(TCC);
    }
    
    /**
     * Calculates Tight Cohesion of a Class (TCC).
     * Let NP(C) be the total number of pairs of abstracted methods
     * in an abstracted class. NP is the maximum possible number
     * of direct or indirect connections in a class. If there
     * are N methods in a class C, NP(C) is N * (N – 1)/2.
     * Let NDC(C) be the number of direct connections in an abstracted class.
     * Tight class cohesion (TCC) is the relative number of
     * directly connected methods:
     *     TCC(C) = NDC(C)/NP(C)
     * @param source the class being evaluated
     */
    public void calculate(AbstractMetricSource source)
	    throws InvalidSourceException {
	CallData callData = getCallDataFromSource(source);
	List<Integer> methodsToEval = getEvaluableMethodReachabilityIndices(callData);
	int n = methodsToEval.size();
	double npc = n * (n - 1) / 2;
	double value = 0;

	// Avoid dividing by zero
	if (npc != 0) {
	    int ndc = calculateNDC(callData, methodsToEval);
	    value = ndc / npc;
	}
	// If 0 - 1 methods, make maximally cohesive
	else {
	    value = 1.0;
	}
	setResult(source, value);
    }

    /**
     * Calculates the number of direct connections (NDC) in a class,
     * i.e. when methods directly access a common attribute or (transitively) 
     * call methods that access a common attribute.
     * @param callData contains information about which
     *   methods access which attributes
     * @param methodsToEval the indices of the methods involved in the calculation
     * @return the number of connections
     */
    private int calculateNDC(CallData callData, List<Integer> methodsToEval) {
	int ndc = 0;
	ConnectivityMatrix directMatrix =
	    buildDirectlyConnectedMatrix(callData, methodsToEval);
	int size = directMatrix.matrix.length;

	for (int i = 0; i < size; i++) {
	    for (int j = i + 1; j < size; j++) {
		if (directMatrix.matrix[i][j] ==
		        ConnectivityMatrix.CONNECTED) {
		    ndc++;
		}
	    }
	}
	return ndc;
    }

    /**
     * Builds the matrix indicating methods that are directly connected, i.e,
     * methods that either directly or indirectly access at least one common
     * attribute.
     * @param callData contains information about which
     *   methods access which attributes
     * @param methodsToEval
     * @return the matrix indicating methods that are directly connected
     */
    public static ConnectivityMatrix buildDirectlyConnectedMatrix(CallData callData,
	    List<Integer> methodsToEval) {
	//TODO cache this matrix, so it can be built once and then used by both TCC. LCC
	ConnectivityMatrix reachabilityMatrix = callData.getReachabilityMatrix();
	List<IMember> headers = getMatrixHeaders(reachabilityMatrix, methodsToEval);
	ConnectivityMatrix directlyConnectedMatrix =
	    new ConnectivityMatrix(headers);
	List<Integer> attributeIndices =
	    getAttributeIndices(callData, reachabilityMatrix);
	
	for (int i = 0; i < methodsToEval.size(); i++) {
	    int iIndex = methodsToEval.get(i).intValue();
	    Set<Integer> iFields =
		getAttributesAccessedBy(iIndex, attributeIndices, reachabilityMatrix);

	    if (iFields != null) {
		for (int j = i + 1; j < methodsToEval.size(); j++) {
		    int jIndex = methodsToEval.get(j).intValue();
		    Set<Integer> jFields =
			getAttributesAccessedBy(
				jIndex, attributeIndices, reachabilityMatrix);

		    // Determine whether there are commonly accessed attributes
		    if (jFields != null) {
			Set<Integer> intersection = new HashSet<Integer>(jFields);
			intersection.retainAll(iFields);

			// Mark connected if the methods access some of the
			// same attributes.  This is nondirectional, so we
			// mark the matrix in two places
			if (intersection.size() != 0) {
			    directlyConnectedMatrix.matrix[i][j] =
				ConnectivityMatrix.CONNECTED;
			    directlyConnectedMatrix.matrix[j][i] =
				ConnectivityMatrix.CONNECTED;
			}
		    } // if jFields != null
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
	List<IMember> headers = new ArrayList<IMember>();
	for (int i = 0; i < methodsToEval.size(); i++) {
	    Integer method = methodsToEval.get(i);
	    IMember member = reachabilityHeaders.get(method.intValue());
	    headers.add(member);
	}
	return headers;
    }
    
    /**
     * @param methodIndex the index of a method in the reachability matrix
     * @param attributeIndices the indices of all of the attributes
     * in the reachability matrix
     * @param reachabilityMatrix
     * @return the indices of the attributes accessed by the method
     */
    private static Set<Integer> getAttributesAccessedBy(
	    int methodIndex,
	    List<Integer> attributeIndices,
	    ConnectivityMatrix reachabilityMatrix) {
	Set<Integer> iFields = new HashSet<Integer>();
	for (Integer aIndex: attributeIndices) {
	    if (reachabilityMatrix.matrix[methodIndex][aIndex.intValue()] ==
	            CallData.ConnectivityMatrix.CONNECTED) {
		iFields.add(aIndex);
	    }
	}
	return iFields;
    }

    /**
     * Get the numeric indices that indicate the positions of the
     * attributes in the matrix
     * @param callData the class's call data
     * @param the matrix indicating relationships
     *   between methods and attributes
     * @return the indices of the attributes in the matrix
     */
    private static List<Integer> getAttributeIndices(
	    CallData callData, ConnectivityMatrix matrix) {
	List<Integer> indices = new ArrayList<Integer>();
	Set<IField> attributes = callData.getAttributes();
	for (IField field : attributes) {
	    int index = matrix.getIndex(field);
	    indices.add(Integer.valueOf(index));
	}
	return indices;
    }
    
    /**
     * This method returns indices within the reachability matrix of
     * the methods that are visible and not constructors.
     * @param callData call data containing method information
     * @return the indices within the reachability matrix
     *   of the visible, non-constructor methods
     */
    public static List<Integer> getEvaluableMethodReachabilityIndices(CallData callData) {
	List<Integer> methodsToEval = new ArrayList<Integer>();
	ConnectivityMatrix reachabilityMatrix = callData.getReachabilityMatrix();
	
	// Remove constructors from consideration
	for (IMethod method : callData.getMethods()) {
	    try {
		int flags = method.getFlags();
		if (!method.isConstructor()
			&& (!Flags.isPrivate(flags))) {
		    int index = reachabilityMatrix.getIndex(method);
		    methodsToEval.add(Integer.valueOf(index));
		}
	    } catch (JavaModelException e) {
		Log.logError("Unable to get information on " + method, e);
	    }
	}
	return methodsToEval;
    }


}
