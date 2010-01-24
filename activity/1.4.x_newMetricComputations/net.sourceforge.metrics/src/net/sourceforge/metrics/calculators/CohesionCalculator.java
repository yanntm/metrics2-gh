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
import java.util.Set;

import net.sourceforge.metrics.core.Constants;
import net.sourceforge.metrics.core.Metric;
import net.sourceforge.metrics.core.sources.AbstractMetricSource;
import net.sourceforge.metrics.core.sources.TypeMetrics;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * Supplies the basis for several cohesion metrics.
 * 
 * @author Keith Cassell
 */
public abstract class CohesionCalculator extends Calculator implements
	Constants {

    protected static CohesionPreferences prefs;

    // TODO generate preferences mechanism for preferences that (1) apply to all
    // cohesion metrics and (2) apply to certain specific cohesion metrics

    /**
     * Constructor for Cohesion.
     */
    public CohesionCalculator() {
	super(LCOM);
    }

    /**
     * Constructor for Cohesion.
     * 
     * @param name
     */
    public CohesionCalculator(String name) {
	super(name);
    }

    /**
     * @see net.sourceforge.metrics.calculators.Calculator#calculate(net.sourceforge.metrics.core.sources.AbstractMetricSource)
     */
    public abstract void calculate(AbstractMetricSource source)
	    throws InvalidSourceException;

    /**
     * Returns the preferences.
     * 
     * @return Preferences
     */
    public static CohesionPreferences getPrefs() {
	if (prefs == null) {
	    prefs = new CohesionPreferences();
	}
	return prefs;
    }

    /**
     * Checks to make sure that the calculator is working with a type/class.
     * If not, it throws an exception
     * @param source the metric source to check
     * @throws InvalidSourceException when the source's level isn't TYPE
     */
    protected void checkApplicability(AbstractMetricSource source)
	    throws InvalidSourceException {
	if (source.getLevel() != TYPE) {
	    throw new InvalidSourceException(name + " only applicable to types");
	}
    }

    /**
     * Gets the call data associated with a particular class.
     * @param source the class's metric data, including call data
     * @return the callData
     * @throws InvalidSourceException when the source's level isn't TYPE
     */
    protected CallData getCallDataFromSource(AbstractMetricSource source)
	    throws InvalidSourceException {
	getPrefs();
	checkApplicability(source);
	TypeMetrics metrics = (TypeMetrics) source;
	CallData callData = metrics.getCallData();
	if (callData == null) {
	    callData = new CallData();
	    callData.collectCallData(metrics, prefs);
	    metrics.setCallData(callData);
	}
	return callData;
    }
    
    /**
     * Save the measurement for the indicated source
     * @param source the code being measured
     * @param value the measurement
     */
    protected void setResult(AbstractMetricSource source, double value) {
//	System.out.println("Setting " + name + " to " + value + " for "
//	+ source.getName());
	source.setValue(new Metric(name, value));
    }

    /**
     * Uses the jdt searchengine to collect all methods inside a class that call
     * the specified method
     * 
     * @author Frank Sauer
     * 
     */
    public static class MethodCollector extends SearchRequestor {
	protected Set<IMethod> results = null;

	public MethodCollector(AbstractMetricSource source) {
	}

	public Set<IMethod> getResults() {
	    return results;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.search.SearchRequestor#beginReporting()
	 */
	public void beginReporting() {
	    results = new HashSet<IMethod>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.core.search.SearchRequestor#acceptSearchMatch(org
	 * .eclipse.jdt.core.search.SearchMatch)
	 */
	public void acceptSearchMatch(SearchMatch match) throws CoreException {
	    Object matchingElement = match.getElement();

	    if (matchingElement instanceof IMethod) {
		results.add((IMethod) matchingElement);
	    }
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.search.SearchRequestor#endReporting()
	 */
	public void endReporting() {
	}

    }

    /**
     * Statically cache preference values, yet register for change events so
     * they get updated when they change.
     */
    public static class CohesionPreferences implements IPropertyChangeListener {
	// TODO Options to (possibly) implement
	/*
	 * "A subclass inherits methods and instance variables from its
	 * superclass. We have several options for evaluating cohesion of a
	 * subclass. We can (1) include all inherited components in the subclass
	 * in our evaluation, (2) include only methods and instance variables
	 * defined in the subclass, or (3) include inherited instance variables
	 * but not inherited methods.  The current default is (2)."
	 */

	private boolean countStaticMethods;
	private boolean countStaticAttributes;

	public CohesionPreferences() {
	    init();
	    getPreferences().addPropertyChangeListener(this);
	}

	protected void init() {
	    countStaticMethods = getPreferences().getBoolean(
		    "LCOM.StaticMethods");
	    countStaticAttributes = getPreferences().getBoolean(
		    "LCOM.StaticAttributes");
	}

	public boolean countStaticMethods() {
	    return countStaticMethods;
	}

	public boolean countStaticAttributes() {
	    return countStaticAttributes;
	}

	/**
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
	    if (event.getProperty().startsWith("LCOM")) {
		init();
	    }
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
	    return "CohesionPreferences [countStaticAttributes="
		    + countStaticAttributes + ", countStaticMethods="
		    + countStaticMethods + "]";
	}
    }
    
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
	return "CohesionCalculator [name=" + name + ", prefs = " + prefs + "]";
    }


}
