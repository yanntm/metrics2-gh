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

import java.util.Map;
import java.util.Set;

import net.sourceforge.metrics.core.sources.AbstractMetricSource;

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
		CallData callData = getCallDataFromSource(source);
		Set<IField> attributes = callData.getAttributes();
		Set<IMethod> methods = callData.getMethods();
		Map<IMethod, Set<IField>> accessedMap = callData
				.getAttributesAccessedMap();
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
		setResult(source, value);
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
	private double calculateCommonCase(Set<IMethod> methods,
			Map<IMethod, Set<IField>> accessedMap) {
		double value = 0;
		int similar = 0;
		int dissimilar = 0;
		Object[] methodArray = methods.toArray();
		for (int i = 0; i < methodArray.length - 1; i++) {
			Set<IField> iFields = accessedMap.get(methodArray[i]);
			if (iFields == null) {
				dissimilar += (methodArray.length - (i + 1));
			} else
				for (int j = i + 1; j < methodArray.length; j++) {
					Set<IField> jFields = accessedMap.get(methodArray[j]);

					if (jFields == null) {
						dissimilar++;
					} else {
						// If the methods access some of the same
						// attributes, they are similar
						if (doesIntersect(iFields, jFields)) {
							similar++;
						} else {
							dissimilar++;
						}
					} // else fields != null
				} // inner for
		} // outer for
		if (dissimilar > similar) {
			value = dissimilar - similar;
		}
		return value;
	}

	private boolean doesIntersect(Set<IField> iFields, Set<IField> jFields) {
		boolean doesIntersect = false;
		
		// Check for intersection of the fields accessed
		for (IField iField : iFields) {
			if (jFields.contains(iField)) {
				doesIntersect = true;
				break;
			}
		}
		return doesIntersect;
	}

}
