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
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Calculates cohesion based on Badri and Badri's Degree of Cohesion in a class
 * (Direct) DCD metric. DCD measures the proportion of connected methods to the
 * maximum possible number of connected methods.
 * 
 * In the 2004 paper, classes with fewer than two methods were considered as
 * special classes and excluded from the measurements, as were abstract classes.
 * Overloaded methods within the same class were treated as one method.
 * Moreover, all special methods (constructor, destructor) were removed.
 * 
 * This class modifies the original DCD in the following ways: (1) classes with
 * fewer than two methods receive a value of 1.0 (max. cohesion) (2) Overloaded
 * methods within the same class are treated as separate methods.
 * 
 * With DCD, large numbers indicate more cohesive classes.
 * 
 * BADRI, L., AND BADRI, M. A proposal of a new class cohesion criterion: An
 * empirical study. Journal of Object Technology 3, 4 (2004).
 * 
 * @author Keith Cassell
 */
public class CohesionDCD extends CohesionCalculator {
	// TODO Determine how to handle non-existent values in the metrics2
	// framework

	/**
	 * Constructor for LackOfCohesion.
	 */
	public CohesionDCD() {
		super(DCD);
	}

	/**
	 * Calculates Degree of Cohesion (Direct) of a Class (DCD).
	 * 
	 * @param source
	 *            the class being evaluated
	 */
	public void calculate(AbstractMetricSource source)
			throws InvalidSourceException {
		CallData callData = getCallDataFromSource(source);
		List<Integer> methodsToEval = 
				getEvaluableMethodReachabilityIndices(callData);
		int n = methodsToEval.size();
		double npc = n * (n - 1) / 2;
		double value = 0;

		// Avoid dividing by zero
		if (npc != 0) {
			int dcd = calculateDCD(callData, methodsToEval);
			value = dcd / npc;
		} else {
			// TODO According to the Badris, this should be undefined.
			value = 1.0;
		}
		setResult(source, value);
	}

	/**
	 * Calculates Degree of Cohesion (Direct) of a Class (DCD). i.e. when
	 * methods directly or indirectly access a common member.
	 * 
	 * @param callData
	 *            method call data
	 * @param methodsToEval
	 *            the methods involved in the calculation
	 * @return the number of connections
	 */
	private int calculateDCD(CallData callData, List<Integer> methodsToEval) {
		int dcd = 0;
		ConnectivityMatrix directMatrix =
			callData.getBadriDirectlyConnectedMatrix(methodsToEval);
		int size = directMatrix.matrix.length;

		for (int i = 0; i < size; i++) {
			for (int j = i + 1; j < size; j++) {
				if (directMatrix.matrix[i][j] == ConnectivityMatrix.CONNECTED) {
					dcd++;
				}
			}
		}
		return dcd;
	}

    /**
     * This method returns the methods that are public and not constructors.
     * This is used for DCD, DCI.
     * @param callData call data containing method information
     * @return the public non-constructor methods
     */
	public static List<Integer> getEvaluableMethodReachabilityIndices(
			CallData callData) {
		ArrayList<Integer> methodsToEval = new ArrayList<Integer>();
		ConnectivityMatrix reachabilityMatrix = callData
				.getReachabilityMatrix();

		// Remove constructors from consideration
		for (IMethod method : callData.getMethods()) {
			try {
				int flags = method.getFlags();
				if (!method.isConstructor() && (Flags.isPublic(flags))) {
					int index = reachabilityMatrix.getIndex(method);
					methodsToEval.add(index);
				}
			} catch (JavaModelException e) {
				Log.logError("Unable to get information on " + method, e);
			}
		}
		return methodsToEval;
	}


}
