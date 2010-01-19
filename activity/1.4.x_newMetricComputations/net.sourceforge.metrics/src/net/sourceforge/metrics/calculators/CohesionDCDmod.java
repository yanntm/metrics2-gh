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
import java.util.List;
import java.util.Set;

import net.sourceforge.metrics.calculators.CallData.ConnectivityMatrix;
import net.sourceforge.metrics.core.Log;
import net.sourceforge.metrics.core.Metric;
import net.sourceforge.metrics.core.sources.AbstractMetricSource;
import net.sourceforge.metrics.core.sources.TypeMetrics;

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
 * This class modifies the original DCD in the following ways:
 * (1) classes with fewer than two methods receive a value of 1.0 (max. cohesion)
 * (2) Overloaded methods within the same class are treated as separate methods.
 * 
 * With DCD, large numbers indicate more cohesive classes.
 * 
 * BADRI, L., AND BADRI, M. A proposal of a new class cohesion criterion: An
 * empirical study. Journal of Object Technology 3, 4 (2004).
 * 
 * @author Keith Cassell
 */
public class CohesionDCDmod extends CohesionCalculator
{
    //TODO Determine how to handle non-existent values in the metrics2 framework

    /**
     * Constructor for LackOfCohesion.
     */
    public CohesionDCDmod() {
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
	List<Integer> methodsToEval = getEvaluableMethodReachabilityIndices(callData);
	int n = methodsToEval.size();
	double npc = n * (n - 1) / 2;
	double value = 0;

	// Avoid dividing by zero
	if (npc != 0) {
	    int dcd = calculateDCD(callData, methodsToEval);
	    value = dcd / npc;
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
    private int calculateDCD(CallData callData, List<Integer> methodsToEval) {
	int dcd = 0;
	ConnectivityMatrix directMatrix =
	    buildDirectlyConnectedMatrix(callData, methodsToEval);
	int size = directMatrix.matrix.length;

	for (int i = 0; i < size; i++) {
	    for (int j = i + 1; j < size; j++) {
		if (directMatrix.matrix[i][j] ==
		        ConnectivityMatrix.CONNECTED) {
		    dcd++;
		}
	    }
	}
	return dcd;
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
	    IMember member = reachabilityHeaders.get(method);
	    headers.add(member);
	}
	return headers;
    }
    
    /**
     * Builds the matrix indicating methods that are directly connected, i.e,
     * methods that either directly or indirectly access at least one common
     * attribute.
     * @param callData contains information about which
     *   methods access which members
     * @param methodsToEval
     * @return the matrix indicating methods that are directly connected
     */
    public static ConnectivityMatrix buildDirectlyConnectedMatrix(CallData callData,
	    List<Integer> methodsToEval) {
	//TODO cache this matrix, so it can be built once and then used by both DCD and DCI.
	ConnectivityMatrix reachabilityMatrix = callData.getReachabilityMatrix();
	List<IMember> headers = getMatrixHeaders(reachabilityMatrix, methodsToEval);
	ConnectivityMatrix directlyConnectedMatrix =
	    new ConnectivityMatrix(headers);
	
	for (int i = 0; i < methodsToEval.size(); i++) {
	    Integer methodIndexI = methodsToEval.get(i);
	    Set<Integer> iFields =
		getMembersAccessedBy(methodIndexI, reachabilityMatrix);

	    if (iFields != null) {
		for (int j = i + 1; j < methodsToEval.size(); j++) {
		    Integer methodIndexJ = methodsToEval.get(j);
		    Set<Integer> jFields =
			getMembersAccessedBy(methodIndexJ, reachabilityMatrix);
		    markDirectlyConnectedMethods(directlyConnectedMatrix, i,
			    iFields, j, jFields);
		} // for j
	    } // if iFields != null
	} // for i
	return directlyConnectedMatrix;
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
	HashSet<Integer> intersection =
	    new HashSet<Integer>(jMembers);
	intersection.retainAll(iMembers);

	// Mark connected if the methods access some of the
	// same members.  This is nondirectional, so we
	// mark the matrix in two places
	if (intersection.size() != 0) {
	    directlyConnectedMatrix.matrix[i][j] =
		ConnectivityMatrix.CONNECTED;
	    directlyConnectedMatrix.matrix[j][i] =
		ConnectivityMatrix.CONNECTED;
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
		memberIndices.add(i);
	    }
	}
	return memberIndices;
    }
    
    /**
     * This method returns the methods that are public and not constructors.
     * @param callData call data containing method information
     * @return the public non-constructor methods
     */
    public static List<Integer> getEvaluableMethodReachabilityIndices(CallData callData) {
	ArrayList<Integer> methodsToEval = new ArrayList<Integer>();
	ConnectivityMatrix reachabilityMatrix = callData.getReachabilityMatrix();
	
	// Remove constructors from consideration
	for (IMethod method : callData.getMethods()) {
	    try {
		int flags = method.getFlags();
		if (!method.isConstructor()
			&& (Flags.isPublic(flags))) {
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
