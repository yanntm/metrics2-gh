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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sourceforge.metrics.core.Metric;
import net.sourceforge.metrics.core.sources.AbstractMetricSource;
import net.sourceforge.metrics.core.sources.TypeMetrics;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;

/**
 * Calculates the Lack of Cohesion of Methods (LCOM*) metric using the
 * Henderson-Sellers method (See book page 147): (avg(m(a)) - m)/(1 - m) where
 * m(a) is the number of methods that access a.  The book does not
 * mention what to do with 0-1 attributes/methods.  In that case, we return
 * a value of 0 (maximally cohesive).  Note that whether static
 * attributes and static methods are considered is configurable from
 * preferences.
 * 
 * @see HENDERSON-SELLERS, B. Object-oriented metrics: measures of complexity.
 *      Prentice-Hall, Inc., 1996.
 * @author Frank Sauer
 */
public class LCOMHS extends CohesionCalculator {

    /**
     * Constructor for LackOfCohesion.
     */
    public LCOMHS() {
	super(LCOMHS);
    }

    /**
     * @see net.sourceforge.metrics.calculators.Calculator#calculate(net.sourceforge.metrics.core.sources.AbstractMetricSource)
     */
    public void calculate(AbstractMetricSource source)
	    throws InvalidSourceException {
	if (source.getLevel() != TYPE) {
	    throw new InvalidSourceException("LCOMHS only applicable to types");
	}
	TypeMetrics typeSource = (TypeMetrics) source;
	CallData callData = typeSource.getCallData();
	Set<IField> attributes = callData.getAttributes();
	Set<IMethod> methods = callData.getMethods();
	Map<IField, HashSet<IMethod>> attributeAccessedByMap =
	    callData.getAttributeAccessedByMap();

	double value = 0;

	// TODO check attributes > 1 with paper.  The book does not
	// mention what to do with 0-1 attributes/methods, but references
	// another Henderson-Sellers paper which is not easily obtainable.
	if ((attributes.size() > 1) && (methods.size() > 1)) {
	    if (attributeAccessedByMap.size() > 0) {
		// TODO boolean countStatics = getPrefs().countStaticMethods();
		value = calculateResult(attributeAccessedByMap);
	    }
	}
	System.out.println("Setting HS to " + value + " for "
		+ source.getName());
	source.setValue(new Metric(LCOMHS, value));
    }

    /**
     * @return double (avg(m(a)) - m)/(1 - m) where m(a) is the number of
     *         methods that access a
     */
    private double calculateResult(
	    Map<IField, HashSet<IMethod>> attributeAccessedByMap) {
	int sum = 0;
	int numAttributes = 0;
	Set<IMethod> allMethods = new HashSet<IMethod>();
	// TODO remove redundant calculation
	for (Iterator<HashSet<IMethod>> i = attributeAccessedByMap.values().iterator();
		i.hasNext(); numAttributes++) {
	    Set<IMethod> methods = (Set<IMethod>) i.next();
	    allMethods.addAll(methods);
	    sum += methods.size();
	}
	int numMethods = allMethods.size();
	if (numMethods == 1)
	    return 0;
	double avg = (double) sum / (double) numAttributes;
	double result = Math.abs((avg - numMethods) / (1 - numMethods));
	return result;
    }

}
