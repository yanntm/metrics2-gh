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
import net.sourceforge.metrics.core.sources.TypeMetrics;

/**
 * Calculates cohesion based on Badri and Badri's Degree of Cohesion in a class
 * (Indirect) DCI metric. DCI measures the proportion of connected methods to the
 * maximum possible number of connected methods.
 * 
 * In the 2004 paper, classes with fewer than two methods were considered as
 * special classes and excluded from the measurements, as were abstract classes.
 * Overloaded methods within the same class were treated as one method.
 * Moreover, all special methods (constructor, destructor) were removed.
 * 
 * This class modifies the original DCI in the following ways:
 * (1) classes with fewer than two methods receive a value of 1.0 (max. cohesion)
 * (2) Overloaded methods within the same class are treated as separate methods.
 * 
 * With DCI, large numbers indicate more cohesive classes.
 * 
 * BADRI, L., AND BADRI, M. A proposal of a new class cohesion criterion: An
 * empirical study. Journal of Object Technology 3, 4 (2004).
 * 
 * @author Keith Cassell
 */
public class CohesionDCI extends CohesionCalculator
{
    public CohesionDCI() {
	super(DCI);
    }
    
    /**
     * Calculates Degree of Cohesion (Indirect) of a Class (DCI).
     * @param source the class being evaluated
     */
    public void calculate(AbstractMetricSource source)
	    throws InvalidSourceException {
	if (source.getLevel() != TYPE) {
	    throw new InvalidSourceException("DCI only applicable to types");
	}
	
	TypeMetrics typeSource = (TypeMetrics) source;
	CallData callData = typeSource.getCallData();
	List<Integer> methodsToEval =
	    CohesionDCD.getEvaluableMethodReachabilityIndices(callData);
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
//	System.out.println("Setting DCI to " + value + " for "
//		+ source.getName());
	source.setValue(new Metric(DCI, value));
    }

    /**
     * Calculates the number of indirect connections (NIC) in a class,
     * i.e. when methods indirectly access a common member or (transitively) 
     * call methods that access a common member.
     * @param callData contains information about which
     *   methods access which members
     * @param methodsToEval the methods involved in the calculation
     * @return the number of connections
     */
    private int calculateNIC(CallData callData, List<Integer> methodsToEval) {
	int nic = 0;
	ConnectivityMatrix directMatrix =
	    CohesionDCD.buildDirectlyConnectedMatrix(callData, methodsToEval);
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
