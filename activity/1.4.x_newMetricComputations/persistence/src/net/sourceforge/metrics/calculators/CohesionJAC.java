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
import java.util.List;

import net.sourceforge.metrics.calculators.CallData.ConnectivityMatrix;
import net.sourceforge.metrics.core.Log;
import net.sourceforge.metrics.core.sources.AbstractMetricSource;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

/**
 * JAC - Java Aware Cohesion This is similar to Badri and Badri's Degree of
 * Cohesion in a class (Indirect) DCI metric, but takes advantage of some Java
 * specific information.
 * 
 * excluded from the measurements: ? abstract classes. constructor
 * 
 * This class modifies the original DCI in the following ways: (1) classes with
 * fewer than two methods receive a value of 1.0 (max. cohesion) (2) Overloaded
 * methods within the same class are treated as separate methods.
 * 
 * With JAC, large numbers indicate more cohesive classes.
 * 
 * BADRI, L., AND BADRI, M. A proposal of a new class cohesion criterion: An
 * empirical study. Journal of Object Technology 3, 4 (2004).
 * 
 * @author Keith Cassell
 */
public class CohesionJAC extends CohesionCalculator {
	
	//private IType loggerType = null;
	private static String loggerSignature = null;

	public CohesionJAC() {
		super(JAC);
		
		if (loggerSignature == null) {
			loggerSignature =
				Signature.createTypeSignature("java.util.logging.Logger", false);
		}
	}

	/**
	 * Calculates Cohesion of a Java Class (JAC).
	 * 
	 * @param source
	 *            the class being evaluated
	 */
	public void calculate(AbstractMetricSource source)
			throws InvalidSourceException {
		CallData callData = getCallDataFromSource(source);
		List<Integer> methodsToEval =
				getEvaluableReachabilityIndices(callData);
		int n = methodsToEval.size();
		double npc = n * (n - 1) / 2;
		double value = 0;

		// Avoid dividing by zero
		if (npc != 0) {
			int nic = calculateNIC(callData, methodsToEval);
			value = nic / npc;
		} else {
			value = 1.0;
		}
		setResult(source, value);
	}

    /**
     * This method returns the methods that are public and not constructors.
     * @param callData call data containing method information
     * @return the public non-constructor methods
     * @see CohesionDCD#getEvaluableMethodReachabilityIndices(CallData)
     */
	public List<Integer> getEvaluableReachabilityIndices(
			CallData callData) {
		ArrayList<Integer> methodsToEval = new ArrayList<Integer>();
		ConnectivityMatrix reachabilityMatrix = callData
				.getReachabilityMatrix();
		//TODO make configurable which methods, fields get removed

		// Remove methods from consideration: constructors, Object's methods,
		for (IMethod method : callData.getMethods()) {
			try {
				int flags = method.getFlags();
				if (!method.isConstructor()
						&& !Flags.isAbstract(flags)
						//&& !Flags.isStatic(flags)
						//&& !Flags.isDeprecated(flags)
						//&& Flags.isPublic(flags)
						&& !isObjectMethod(method)
						) {
					int index = reachabilityMatrix.getIndex(method);
					methodsToEval.add(index);
				}
			} catch (JavaModelException e) {
				Log.logError("Unable to get information on " + method, e);
			}
		}

		// Remove from consideration: serialVersionUID, loggers
		for (IField field : callData.getAttributes()) {
			String fieldName = field.getElementName();
			if (!isLogger(field) && !"serialVersionUID".equals(fieldName)) {
				int index = reachabilityMatrix.getIndex(field);
				methodsToEval.add(index);
			}
		}
		return methodsToEval;
	}

	/**
	 * Calculates the number of indirect connections (NIC) in a class, i.e. when
	 * methods indirectly access a common member or (transitively) call methods
	 * that access a common member.
	 * 
	 * @param callData
	 *            contains information about which methods access which members
	 * @param methodsToEval
	 *            the methods involved in the calculation
	 * @return the number of connections
	 */
	private int calculateNIC(CallData callData, List<Integer> methodsToEval) {
		int nic = 0;
		ConnectivityMatrix directMatrix = ConnectivityMatrix
				.buildDirectlyConnectedMatrix(callData, methodsToEval);
		ConnectivityMatrix indirectMatrix = ConnectivityMatrix
				.buildReachabilityMatrix(directMatrix);

		for (int i = 0; i < indirectMatrix.matrix.length; i++) {
			for (int j = i + 1; j < indirectMatrix.matrix.length; j++) {
				if (indirectMatrix.matrix[i][j] == ConnectivityMatrix.CONNECTED) {
					nic++;
				}
			}
		}
		return nic;
	}

	/**
	 * Determines whether the supplied handle matches one of the methods
	 * defined on Object that can be overridden (clone, equals, hashCode,
	 * toString).
	 * @param sig the Eclipse handle
	 * @return true if an Object method; false otherwise
	 * @throws JavaModelException 
	 */
	private static boolean isObjectMethod(IMethod method) throws JavaModelException {
		// method.isSimilar(superMethod)
		String sig = method.getSignature();
		boolean result =
			sig.endsWith("~hashCode")
			|| sig.endsWith("~equals~QObject;")
			|| sig.endsWith("~clone")
			|| sig.endsWith("~toString");
		return result;
	}

	/**
	 * Determines whether this field is a logger
	 * @param field
	 * @return true if a logger, false otherwise
	 */
	private boolean isLogger(IField field) {
		boolean isLogger =  false;
		try {
			String typeSignature = field.getTypeSignature();
			isLogger = (loggerSignature != null)
				&& loggerSignature.equals(typeSignature);
		} catch (JavaModelException e) {
			Log.logError("isLogger() failure: ", e);
		}
		return isLogger;
	}


}
