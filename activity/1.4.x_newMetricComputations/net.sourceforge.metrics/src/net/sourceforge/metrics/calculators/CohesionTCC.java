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

import java.util.HashMap;
import java.util.HashSet;

import net.sourceforge.metrics.core.Log;
import net.sourceforge.metrics.core.Metric;
import net.sourceforge.metrics.core.sources.AbstractMetricSource;
import net.sourceforge.metrics.core.sources.TypeMetrics;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Calculates cohesion using Bieman and Kang's TCC metric.  TCC measures the
 * proportion of connected methods to the maximum possible number of connected
 * methods.  It considers only "visible" methods and further omits constructors
 * and destructors from consideration.  By visible, we assume they consider
 * all non-private methods.  TCC considers methods to be connected when they
 * directly access a common attribute.  With TCC, large numbers indicate more
 * cohesive classes.
 * 
 * @see BIEMAN, J. M., AND KANG, B.-K. Cohesion and reuse in an object oriented
 *    system. SIGSOFT Softw. Eng. Notes 20, SI (1995), 259–262.
 * 
 * @author Keith Cassell
 */
public class CohesionTCC extends CohesionCalculator
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
    public CohesionTCC() {
	super(TCC);
    }
    
    /**
     * TCC doesn't consider constructors in its calculations.  This method
     * returns the methods that are not constructors.
     * @param callData the method call data
     * @return the non-constructor methods
     */
    private HashSet<IMethod> getMethodsToEval(CallData callData) {
	HashSet<IMethod> methods = callData.getMethods();
	HashSet<IMethod> methodsToEval = new HashSet<IMethod>();
	
	// Remove constructors from consideration
	for (IMethod method : methods) {
	    try {
		if (!method.isConstructor()) {
		    methodsToEval.add(method);
		}
	    } catch (JavaModelException e) {
		Log.logError("Unable to determine if " + method.toString()
			+ " is a constructor.", e);
	    }
	}
	return methodsToEval;
    }

    /**
     * Calculates Tight Cohesion of a Class (TCC).
     * Let NP(C) be the total number of pairs of abstracted methods
     * in an abstracted class. NP is the maximum possible number
     * of direct or indirect connections in a class. If there
     * are N methods in a class C, NP(C) is N * (N – 1)/2.
     * Let NDC(C) be the number of direct connections and
     * NIC(C) be the number of indirect connections in an abstracted class.
     * Tight class cohesion (TCC) is the relative number of
     * directly connected methods:
     *     TCC(C) = NDC(C)/NP(C)
     * @param source the class being evaluated
     * @see net.sourceforge.metrics.calculators.Calculator#calculate(net.sourceforge.metrics.core.sources.AbstractMetricSource)
     */
    public void calculate(AbstractMetricSource source)
	    throws InvalidSourceException {
	if (source.getLevel() != TYPE) {
	    throw new InvalidSourceException("TCC only applicable to types");
	}
	TypeMetrics typeSource = (TypeMetrics) source;
	CallData callData = typeSource.getCallData();
	HashSet<IMethod> methodsToEval = getMethodsToEval(callData);
	int n = methodsToEval.size();
	double npc = n * (n - 1) / 2;
	double value = 0;

	// Avoid dividing by zero
	if (npc != 0) {
	    int ndc = calculateNDC(callData, methodsToEval);
	    value = ndc / npc;
	}
	// TODO remove
	System.out.println("Setting TCC to " + value + " for "
		+ source.getName());
	source.setValue(new Metric(TCC, value));
    }

    /**
     * Calculates the number of direct connections (NDC) in a class,
     * i.e. when methods directly access a common attribute.
     * @param callData contains information about which
     *   methods access which attributes
     * @param methodsToEval the methods involved in the calculation
     * @return the number of direct connections
     */
    private int calculateNDC(CallData callData, HashSet<IMethod> methodsToEval) {
	int ndc = 0;
	Object[] methodArray = methodsToEval.toArray();
	HashMap<IMethod, HashSet<IField>> accessedMap =
	callData.getAttributesAccessedMap();

	for (int i = 0; i < methodArray.length; i++) {
	    HashSet<IField> iFields = accessedMap.get(methodArray[i]);

	    for (int j = i + 1; j < methodArray.length; j++) {
		// Cloned here because we use retainAll to determine the
		// intersection
		HashSet<IField> jFields = accessedMap.get(methodArray[j]);

		// Determine whether there are commonly accessed attributes
		if (jFields != null) {
		    HashSet<IField> intersection = new HashSet<IField>(jFields);
		    intersection.retainAll(iFields);

		    // Increment the count if the methods access some of the
		    // same attributes.
		    if (intersection.size() != 0) {
			ndc++;
		    }
		} // else fields != null
	    } // inner for
	} // outer for
	return ndc;
    }

}
