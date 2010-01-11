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

import net.sourceforge.metrics.core.Metric;
import net.sourceforge.metrics.core.sources.AbstractMetricSource;
import net.sourceforge.metrics.core.sources.TypeMetrics;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;

/**
 * Calculates the Lack of Cohesion of Methods (LCOM) metric using Chidamber and
 * Kemerer's method. This metric returns the maximum of zero and the number of
 * dissimilar pairs of methods minus the number of similar pairs of methods.
 * (Two methods are similar if they each access a common attribute directly.)
 * Being a "lack of cohesion" metric, large numbers indicate less cohesive
 * classes.
 * 
 * The values returned by this metric are dependent on the number of methods in
 * the classes being measured. Therefore, there is no upper bound on these
 * measurements, and comparing the measurements from two differently sized
 * classes has little meaning.
 * 
 * @see CHIDAMBER, S., AND KEMERER, C. A metrics suite for object oriented
 *      design. IEEE Transactions on Software Engineering 20, 6 (1994)
 * 
 * @author Keith Cassell
 */
public class LCOMCK extends CohesionCalculator {

    /**
     * Constructor for LackOfCohesion.
     */
    public LCOMCK() {
	super(LCOMCK);
    }

    /**
     * This metric calculates the number of dissimilar pairs of methods minus
     * the number of similar pairs of methods where two methods are similar if
     * they each access a common attribute directly. Being a "lack of cohesion"
     * metric, large numbers indicate less cohesive classes.
     * 
     * @param source
     *            the class being evaluated
     * @see net.sourceforge.metrics.calculators.Calculator#calculate(net.sourceforge.metrics.core.sources.AbstractMetricSource)
     */
    public void calculate(AbstractMetricSource source)
	    throws InvalidSourceException {
	if (source.getLevel() != TYPE) {
	    throw new InvalidSourceException("LCOMCK only applicable to types");
	}
	TypeMetrics typeSource = (TypeMetrics) source;
	CallData callData = typeSource.getCallData();
	HashSet<IField> attributes = callData.getAttributes();
	HashSet<IMethod> methods = callData.getMethods();
	HashMap<IMethod, HashSet<IField>> accessedMap =
	    callData.getAttributesAccessedMap();
	double value = 0;

	// If there are no attributes or less than two methods, then
	// LCOM = 0.
	if ((attributes.size() == 0) || (methods.size() < 2)) {
	    value = 0;
	}
	// The "normal" case - multiple methods and attributes.
	else {
	    value = calculateCommonCase(methods, accessedMap);
	} // else
	// TODO remove
	System.out.println("Setting CK to " + value + " for "
		+ source.getName());
	source.setValue(new Metric(LCOMCK, value));
    }

    /**
     * This metric calculates the number of dissimilar pairs of methods minus
     * the number of similar pairs of methods where two methods are similar if
     * they each access a common attribute directly.
     * 
     * @param methods
     *            the methods in the class being evaluated
     * @param accessedMap
     *            indicates which fields are directly accessed by each method
     * @return the number of dissimilar pairs of methods minus the number of
     *         similar pairs of methods
     */
    private double calculateCommonCase(HashSet<IMethod> methods,
	    HashMap<IMethod, HashSet<IField>> accessedMap) {
	double value = 0;
	int similar = 0;
	int dissimilar = 0;
	Object[] methodArray = methods.toArray();
	for (int i = 0; i < methodArray.length; i++) {
	    HashSet<IField> iFields = accessedMap.get(methodArray[i]);
	    if (iFields == null) {
		dissimilar++;
	    } else
		for (int j = i + 1; j < methodArray.length; j++) {
		    // Cloned here because we use retainAll to determine the
		    // intersection
		    HashSet<IField> jFields = accessedMap.get(methodArray[j]);

		    if (jFields == null) {
			dissimilar++;
		    } else {
			HashSet<IField> intersection =
			    new HashSet<IField>(jFields);
			intersection.retainAll(iFields);

			// If the methods don't access any of the same
			// attributes,
			// they are dissimilar
			if (intersection.size() == 0) {
			    dissimilar++;
			} else {
			    similar++;
			}
		    } // else fields != null
		} // inner for
	} // outer for
	if (dissimilar > similar) {
	    value = dissimilar - similar;
	}
	return value;
    }

}
