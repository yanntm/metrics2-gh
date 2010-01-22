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

import java.util.List;

import net.sourceforge.metrics.calculators.CallData.ConnectivityMatrix;
import net.sourceforge.metrics.core.Metric;
import net.sourceforge.metrics.core.sources.AbstractMetricSource;

/**
 * Calculates cohesion using Bieman and Kang's LCC metric. LCC measures the
 * proportion of connected methods to the maximum possible number of connected
 * methods. It considers only "visible" methods and further omits constructors
 * from consideration. By visible, we assume they consider all
 * non-private methods. LCC considers methods to be connected when they access a
 * common attribute either directly of indirectly. With LCC, large numbers
 * indicate more cohesive classes.
 * 
 * Bieman and Kang do not specify how to treat the "abnormal" case of two or
 * fewer methods, so we give the maximal cohesion score of one for that case.
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
     */
    public void calculate(AbstractMetricSource source)
	    throws InvalidSourceException {
	CallData callData = getCallDataFromSource(source);
	List<Integer> methodsToEval =
	    CohesionTCC.getEvaluableMethodReachabilityIndices(callData);
	int n = methodsToEval.size();
	double npc = n * (n - 1) / 2;
	double value = 0;

	// Avoid dividing by zero
	if (npc != 0) {
	    int nic = calculateNIC(callData, methodsToEval);
	    value = nic / npc;
	}
	else {
	    value = 1.0;
	}
//	System.out.println("Setting LCC to " + value + " for "
//		+ source.getName());
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
    private int calculateNIC(CallData callData, List<Integer> methodsToEval) {
	int nic = 0;
	ConnectivityMatrix directMatrix =
	    CohesionTCC.buildDirectlyConnectedMatrix(callData, methodsToEval);
	ConnectivityMatrix indirectMatrix =
	    ConnectivityMatrix.buildReachabilityMatrix(directMatrix);
	
	for (int i = 0; i < indirectMatrix.matrix.length; i++) {
	    for (int j = i + 1; j < indirectMatrix.matrix.length; j++) {
		if (indirectMatrix.matrix[i][j] ==
		        ConnectivityMatrix.CONNECTED) {
		    nic++;
		}
	    }
	}
	return nic;
    }
    
    

}
