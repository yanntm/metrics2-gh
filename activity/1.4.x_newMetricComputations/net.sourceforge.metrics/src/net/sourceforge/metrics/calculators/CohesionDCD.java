// TODO Distribute responsibilities between CohesionCalculator and this class
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
import java.util.Set;

import net.sourceforge.metrics.calculators.CallData.ConnectivityMatrix;
import net.sourceforge.metrics.core.Log;
import net.sourceforge.metrics.core.Metric;
import net.sourceforge.metrics.core.sources.AbstractMetricSource;
import net.sourceforge.metrics.core.sources.TypeMetrics;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Calculates cohesion using Badri and Badri's Degree of Cohesion in a class
 * (Direct) DCD metric.  DCD measures the proportion of connected methods to the
 * maximum possible number of connected methods.  Classes with fewer than two
 * methods were considered as special classes and excluded from the Badris'
 * measurements.  They also excluded all abstract classes.  Overloaded methods
 * within the same class were treated as one method. Moreover, all special
 * methods (constructor, destructor) are removed in our approach.  TCC considers
 * methods to be connected when they directly access a common attribute.  With
 * DCD, large numbers indicate more cohesive classes.
 * 
 * BADRI, L., AND BADRI, M. A proposal of a new class cohesion criterion:
 *   An empirical study. Journal of Object Technology 3, 4 (2004).
 * 
 * @author Keith Cassell
 */
public class CohesionDCD extends CohesionCalculator
{
    //TODO Determine how to handle non-existent values in the metrics2 framework

    /**
     * Constructor for LackOfCohesion.
     */
    public CohesionDCD() {
	super(DCD);
    }
    
    /**
     * Calculates Degree of Cohesion (Direct) of a Class (DCD).
     * @param source the class being evaluated
     */
    public void calculate(AbstractMetricSource source)
	    throws InvalidSourceException {
	if (source.getLevel() != TYPE) {
	    throw new InvalidSourceException("DCD only applicable to types");
	}
	
	TypeMetrics typeSource = (TypeMetrics) source;
	CallData callData = typeSource.getCallData();
	HashSet<IMethod> methodsToEval = getEvaluableMethods(callData);
	// TODO only consider public methods in calculation
	int n = methodsToEval.size();
	double npc = n * (n - 1) / 2;
	double value = 0;

	// Avoid dividing by zero
	if (npc != 0) {
	    int ndc = calculateNDC(callData, methodsToEval);
	    value = ndc / npc;
	}
	else {
	    // TODO According to the Badris, this should be undefined
	    value = 1.0;
	}
	// TODO remove
	System.out.println("Setting DCD to " + value + " for "
		+ source.getName());
	source.setValue(new Metric(DCD, value));
    }

    /**
     * Calculates Degree of Cohesion (Direct) of a Class (DCD).
     * i.e. when methods directly or indirectly access a common member.
     * @param callData method call data
     * @param methodsToEval the methods involved in the calculation
     * @return the number of connections
     */
    private int calculateNDC(CallData callData, HashSet<IMethod> methodsToEval) {
	int ndc = 0;
	IMethod[] methodArray =
	    methodsToEval.toArray(new IMethod[methodsToEval.size()]);
	ConnectivityMatrix directMatrix =
	    buildDirectlyConnectedMatrix(callData, methodArray);
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
     * @param methodArray
     * @return the matrix indicating methods that are directly connected
     */
    public static ConnectivityMatrix buildDirectlyConnectedMatrix(CallData callData,
	    IMethod[] methodArray) {
	//TODO cache this matrix, so it can be built once and then used by both DCD and DCI.
	ConnectivityMatrix reachabilityMatrix = callData.getReachabilityMatrix();
	ArrayList<IMember> headers =
	    new ArrayList<IMember>(reachabilityMatrix.headers);
	ConnectivityMatrix directlyConnectedMatrix =
	    new ConnectivityMatrix(headers);
	ArrayList<Integer> attributeIndices =
	    getAttributeIndices(callData, reachabilityMatrix);
	
	for (int i = 0; i < methodArray.length; i++) {
	    int iIndex = reachabilityMatrix.getIndex(methodArray[i]);
	    HashSet<Integer> iFields =
		getFieldsAccessedBy(iIndex, attributeIndices, reachabilityMatrix);

	    if (iFields != null) {
		for (int j = i + 1; j < methodArray.length; j++) {
		    int jIndex = reachabilityMatrix.getIndex(methodArray[j]);
		    HashSet<Integer> jFields =
			getFieldsAccessedBy(
				jIndex, attributeIndices, reachabilityMatrix);

		    // Determine whether there are commonly accessed attributes
		    if (jFields != null) {
			HashSet<Integer> intersection =
			    new HashSet<Integer>(jFields);
			intersection.retainAll(iFields);

			// Mark connected if the methods access some of the
			// same attributes.  This is nondirectional, so we
			// mark the matrix in two places
			if (intersection.size() != 0) {
			    directlyConnectedMatrix.matrix[iIndex][jIndex] =
				ConnectivityMatrix.CONNECTED;
			    directlyConnectedMatrix.matrix[jIndex][iIndex] =
				ConnectivityMatrix.CONNECTED;
			}
		    } // if jFields != null
		} // for j
	    } // if iFields != null
	} // for i
	return directlyConnectedMatrix;
    }
    
    /**
     * @param methodIndex the index of a method in the reachability matrix
     * @param attributeIndices the indices of all of the methods
     * in the reachability matrix
     * @param reachabilityMatrix
     * @return the indices of the attributes accessed by the method
     */
    private static HashSet<Integer> getFieldsAccessedBy(
	    int methodIndex,
	    ArrayList<Integer> attributeIndices,
	    ConnectivityMatrix reachabilityMatrix) {
	HashSet<Integer> iFields = new HashSet<Integer>();
	for (Integer aIndex: attributeIndices) {
	    if (reachabilityMatrix.matrix[methodIndex][aIndex] ==
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
    private static ArrayList<Integer> getAttributeIndices(
	    CallData callData, ConnectivityMatrix matrix) {
	ArrayList<Integer> indices = new ArrayList<Integer>();
	Set<IField> attributes = callData.getAttributes();
	for (IField field : attributes) {
	    int index = matrix.getIndex(field);
	    indices.add(index);
	}
	return indices;
    }
    
    /**
     * This method returns the methods that are public and not constructors.
     * @param callData call data containing method information
     * @return the public non-constructor methods
     */
    public static HashSet<IMethod> getEvaluableMethods(CallData callData) {
	HashSet<IMethod> methodsToEval = new HashSet<IMethod>();
	
	// Remove constructors and nonpublic methods from consideration
	for (IMethod method : callData.getMethods()) {
	    try {
		int flags = method.getFlags();
		if (!method.isConstructor()
			&& Flags.isPublic(flags)) {
		    methodsToEval.add(method);
		}
	    } catch (JavaModelException e) {
		Log.logError("Unable to determine if " + method.toString()
			+ " is a constructor.", e);
	    }
	}
	return methodsToEval;
    }

}
