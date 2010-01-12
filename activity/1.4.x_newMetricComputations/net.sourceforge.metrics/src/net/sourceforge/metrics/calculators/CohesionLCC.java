// TODO Distribute responsibilities between CohesionCalculator and this class
/*
 * Copyright (c) 2003 Frank Sauer. All rights reserved.
 *
 * Licenced under CPL 1.0 (Common Public License Version 1.0).
 * The licence is available at http://www.eclipse.org/legal/cpl-v10.html.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import net.sourceforge.metrics.calculators.CallData.ConnectivityMatrix;
import net.sourceforge.metrics.core.Metric;
import net.sourceforge.metrics.core.sources.AbstractMetricSource;
import net.sourceforge.metrics.core.sources.TypeMetrics;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;

/**
 * Calculates cohesion using Bieman and Kang's LCC metric. LCC measures the
 * proportion of connected methods to the maximum possible number of connected
 * methods. It considers only "visible" methods and further omits constructors
 * and destructors from consideration. By visible, we assume they consider all
 * non-private methods. LCC considers methods to be connected when they access a
 * common attribute either directly of indirectly. With LCC, large numbers
 * indicate more cohesive classes.
 * 
 * @see BIEMAN, J. M., AND KANG, B.-K. Cohesion and reuse in an object oriented
 *      system. SIGSOFT Softw. Eng. Notes 20, SI (1995), 259–262.
 * 
 * @author Keith Cassell
 */
public class CohesionLCC extends CohesionCalculator
{
    //TODO Options to (possibly) implement
    /* 
     * "A subclass inherits methods and instance variables from its superclass. 
     * We have several options for evaluating cohesion of a subclass. We can 
     * (1) include all inherited components in the subclass in our evaluation, 
     * (2) include only methods and instance variables defined in the subclass, or 
     * (3) include inherited instance variables but not inherited methods. 
     * The class cohesion measures that we develop can be applied using any one 
     * of these options."
     */

    /**
     * Constructor for LackOfCohesion.
     */
    public CohesionLCC() {
	super(LCC);
    }
    
    /**
     * Calculates Loose Cohesion of a Class (LCC).
     * Let NP(C) be the total number of pairs of abstracted methods
     * in an abstracted class. NP is the maximum possible number
     * of direct or indirect connections in a class. If there
     * are N methods in a class C, NP(C) is N * (N – 1)/2.
     * Let NIC(C) be the number of indirect connections in an abstracted class.
     * Loose class cohesion (LCC) is the relative number of
     * directly connected methods:
     *     LCC(C) = NIC(C)/NP(C)
     * @param source the class being evaluated
     * @see net.sourceforge.metrics.calculators.Calculator#calculate(net.sourceforge.metrics.core.sources.AbstractMetricSource)
     */
    public void calculate(AbstractMetricSource source)
	    throws InvalidSourceException {
	if (source.getLevel() != TYPE) {
	    throw new InvalidSourceException("LCC only applicable to types");
	}
	
	TypeMetrics typeSource = (TypeMetrics) source;
	CallData callData = typeSource.getCallData();
	HashSet<IMethod> methodsToEval = callData.getNonConstructorMethods();
	int n = methodsToEval.size();
	double npc = n * (n - 1) / 2;
	double value = 0;

	// Avoid dividing by zero
	if (npc != 0) {
	    int nic = calculateNIC(callData, methodsToEval);
	    value = nic / npc;
	}
	// TODO remove
	System.out.println("Setting LCC to " + value + " for "
		+ source.getName());
	source.setValue(new Metric(LCC, value));
    }

    /**
     * Calculates the number of indirect connections (NIC) in a class,
     * i.e. when methods directly access a common attribute or (transitively) 
     * call methods that access a common attribute.
     * @param callData contains information about which
     *   methods access which attributes
     * @param methodsToEval the methods involved in the calculation
     * @return the number of connections
     */
    private int calculateNIC(CallData callData, HashSet<IMethod> methodsToEval) {
	int nic = 0;
	HashMap<IField,HashSet<IMethod>> attributeAccessedByMap =
	    callData.getAttributeAccessedByMap();
	HashMap<IMethod,HashSet<IMethod>> methodCalledByMap = callData.getMethodCalledByMap();
	ConnectivityMatrix adjacencyMatrix =
	    ConnectivityMatrix.buildAdjacencyMatrix(
		    attributeAccessedByMap, methodCalledByMap);
	ConnectivityMatrix reachabilityMatrix =
	    adjacencyMatrix.buildReachabilityMatrix();
	ArrayList<Integer> attributeIndices =
	    getAttributeIndices(callData, adjacencyMatrix);
	IMethod[] methodArray =
	    methodsToEval.toArray(new IMethod[methodsToEval.size()]);
	
	for (int i = 0; i < methodArray.length; i++) {
	    int iIndex = adjacencyMatrix.getIndex(methodArray[i]);
	    HashSet<Integer> iFields =
		getFieldsAccessedBy(iIndex, attributeIndices, reachabilityMatrix);

	    if (iFields != null) {
		for (int j = i + 1; j < methodArray.length; j++) {
		    int jIndex = adjacencyMatrix.getIndex(methodArray[j]);
		    HashSet<Integer> jFields =
			getFieldsAccessedBy(jIndex, attributeIndices, reachabilityMatrix);

		    // Determine whether there are commonly accessed attributes
		    if (jFields != null) {
			HashSet<Integer> intersection = new HashSet<Integer>(jFields);
			intersection.retainAll(iFields);

			// Increment the count if the methods access some of the
			// same attributes.
			if (intersection.size() != 0) {
			    nic++;
			}
		    } // if jFields != null
		} // for j
	    } // if iFields != null
	} // for i
	return nic;
    }
    
    /**
     * @param methodIndex the index of a method in the reachability matrix
     * @param attributeIndices the indices of all of the methods
     * in the reachability matrix
     * @param reachabilityMatrix
     * @return the indices of the attributes accessed by the method
     */
    private HashSet<Integer> getFieldsAccessedBy(
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

    private ArrayList<Integer> getAttributeIndices(
	    CallData callData, ConnectivityMatrix adjacencyMatrix) {
	ArrayList<Integer> indices = new ArrayList<Integer>();
	HashSet<IField> attributes = callData.getAttributes();
	for (IField field : attributes) {
	    int index = adjacencyMatrix.getIndex(field);
	    indices.add(index);
	}
	return indices;
    }
    
    

}
