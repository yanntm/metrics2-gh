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
import net.sourceforge.metrics.core.Log;
import net.sourceforge.metrics.core.Metric;
import net.sourceforge.metrics.core.sources.AbstractMetricSource;
import net.sourceforge.metrics.core.sources.TypeMetrics;
import net.sourceforge.metrics.ui.preferences.CohesionPreferencePage;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jface.preference.IPreferenceStore;
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
	 * Checks to make sure that the calculator is working with a type/class. If
	 * not, it throws an exception
	 * 
	 * @param source
	 *            the metric source to check
	 * @throws InvalidSourceException
	 *             when the source's level isn't TYPE
	 */
	protected void checkApplicability(AbstractMetricSource source)
			throws InvalidSourceException {
		if (source.getLevel() != TYPE) {
			throw new InvalidSourceException(name + " only applicable to types");
		}
	}

	/**
	 * Gets the call data associated with a particular class.
	 * 
	 * @param source
	 *            the class's metric data, including call data
	 * @return the callData
	 * @throws InvalidSourceException
	 *             when the source's level isn't TYPE
	 */
	protected CallData getCallDataFromSource(AbstractMetricSource source)
			throws InvalidSourceException {
		getPrefs();
		checkApplicability(source);
		TypeMetrics metrics = (TypeMetrics) source;
		CallData callData = metrics.getCallData();
		if (callData == null) {
			callData = new CallData();
			try {
				callData.collectCallData(metrics, prefs);
				metrics.setCallData(callData);
			} catch (JavaModelException e) {
				Log.logError("Unable to collect call data", e);
			}
		}
		return callData;
	}

	/**
	 * Save the measurement for the indicated source
	 * 
	 * @param source
	 *            the code being measured
	 * @param value
	 *            the measurement
	 */
	protected void setResult(AbstractMetricSource source, double value) {
		// System.out.println("Setting " + name + " to " + value + " for "
		// + source.getName());
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
	 * they get updated when they change. By default, all methods and attributes
	 * declared in the local class are used in the cohesion calculation, but
	 * none that are inherited from superclasses. Members defined in inner
	 * classes are also ignored by default. The fields below allow the user to
	 * change the default behavior. "NYI" indicates "Not Yet Implemented".
	 */
	public static class CohesionPreferences implements IPropertyChangeListener {

		// TODO Options to (possibly) implement

		/** If this is "true", the original metric definitions are
		 * used; otherwise, the user has the options of deciding
		 * which members are considered in the calculations by setting
		 * the other preferences.  */
		protected boolean useOriginalDefinitions = true;

		/** Indicates whether methods imposed by interfaces should be
		 * considered as connected (as though they called each other). */
		protected boolean connectInterfaceMethods = false;

		/** Indicates whether abstract methods should be considered. */
		protected boolean countAbstractMethods = false;

		/** Indicates whether constructors should be considered. */
		protected boolean countConstructors = false;

		/** Indicates whether deprecated methods should be considered. */
		protected boolean countDeprecatedMethods = false;

		/** NYI - Indicates whether inherited attributes should be included. */
		protected boolean countInheritedAttributes = false;

		/** NYI - Indicates whether inherited methods should be included. */
		protected boolean countInheritedMethods = false;

		/** Indicates whether logger fields should be included. */
		protected boolean countLoggers = false;

		/** Indicates whether methods declared by the Object Class
		 * (toString, etc.) should be considered. */
		protected boolean countObjectsMethods = false;

		/** Indicates whether only public methods should be considered
		 * in relationships with the attributes. */
		protected boolean countPublicMethodsOnly = false;

		/** Indicates whether static attributes should be considered. */
		protected boolean countStaticAttributes = true;

		/** Indicates whether static methods should be considered. */
		protected boolean countStaticMethods = true;
		
		/**
		 * NYI - Indicates whether members of inner classes should be treated the same as
		 * members of the outer class.
		 */
		protected boolean countInners = false;

		/** Members matching this pattern should not be considered in the cohesion
		 * calculation.	 */
		protected String ignoreMembersPattern = "";
		
		public CohesionPreferences() {
			init();
			getPreferences().addPropertyChangeListener(this);
		}

		protected void init() {
			IPreferenceStore preferences = getPreferences();
			connectInterfaceMethods = preferences.getBoolean(
					CohesionPreferencePage.CONNECT_INTERFACE_METHODS);
			countAbstractMethods = preferences.getBoolean(
					CohesionPreferencePage.COUNT_ABSTRACT_METHODS);
			countConstructors = preferences.getBoolean(
					CohesionPreferencePage.COUNT_CONSTRUCTORS);
			countDeprecatedMethods = preferences.getBoolean(
					CohesionPreferencePage.COUNT_DEPRECATED_METHODS);
			countInheritedAttributes = preferences.getBoolean(
					CohesionPreferencePage.COUNT_INHERITED_ATTRIBUTES);
			countInheritedMethods = preferences.getBoolean(
					CohesionPreferencePage.COUNT_INHERITED_METHODS);
			countInners = preferences.getBoolean(
					CohesionPreferencePage.COUNT_INNERS);
			countLoggers = preferences.getBoolean(
					CohesionPreferencePage.COUNT_LOGGERS);
			countObjectsMethods = preferences.getBoolean(
					CohesionPreferencePage.COUNT_OBJECTS_METHODS);
			countPublicMethodsOnly = preferences.getBoolean(
					CohesionPreferencePage.COUNT_PUBLIC_METHODS_ONLY);
			countStaticAttributes = preferences.getBoolean(
					CohesionPreferencePage.COUNT_STATIC_ATTRIBUTES);
			countStaticMethods = preferences.getBoolean(
					CohesionPreferencePage.COUNT_STATIC_METHODS);
			ignoreMembersPattern = preferences.getString(
					CohesionPreferencePage.IGNORE_MEMBERS_PATTERN);
			useOriginalDefinitions = preferences.getBoolean(
					CohesionPreferencePage.USE_ORIGINAL_DEFINITIONS);
		}

		public boolean getUseOriginalDefinitions() {
			return useOriginalDefinitions;
		}

		public boolean getConnectInterfaceMethods() {
			return connectInterfaceMethods;
		}

		public boolean getCountAbstractMethods() {
			return countAbstractMethods;
		}

		public boolean getCountConstructors() {
			return countConstructors;
		}

		public boolean getCountDeprecatedMethods() {
			return countDeprecatedMethods;
		}

		public boolean getCountInheritedAttributes() {
			return countInheritedAttributes;
		}

		public boolean getCountInheritedMethods() {
			return countInheritedMethods;
		}

		public boolean getCountLoggers() {
			return countLoggers;
		}

		public boolean getCountObjectsMethods() {
			return countObjectsMethods;
		}

		public boolean getCountPublicMethodsOnly() {
			return countPublicMethodsOnly;
		}

		public boolean getCountInners() {
			return countInners;
		}

		public boolean getCountStaticMethods() {
			return countStaticMethods;
		}

		public boolean getCountStaticAttributes() {
			return countStaticAttributes;
		}

		public String getIgnoreMembersPattern() {
			return ignoreMembersPattern;
		}

		/**
		 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
		 */
		public void propertyChange(PropertyChangeEvent event) {
			String property = event.getProperty();
			if (property.startsWith(CohesionPreferencePage.COHESION_PREFERENCE_PREFIX)) {
				init();
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "CohesionPreferences: "
			+ "\n [countAbstractMethods=\t" + countAbstractMethods
			+ "\n countConstructors=\t" + countConstructors
			+ "\n countDeprecatedMethods=\t" + countDeprecatedMethods
			+ "\n countInheritedAttributes=\t" + countInheritedAttributes
			+ "\n countInheritedMethods=\t" + countInheritedMethods
			+ "\n countInners=\t" + countInners
			+ "\n countLoggers=\t" + countLoggers
			+ "\n countObjectsMethods=\t" + countObjectsMethods
			+ "\n countPublicMethodsOnly=\t" + countPublicMethodsOnly
			+ "\n countStaticAttributes=\t" + countStaticAttributes
			+ "\n countStaticMethods=\t" + countStaticMethods
			+ "\n ignoreMembersPattern=\t" + ignoreMembersPattern
			+ "\n useOriginalDefinitions=\t" + useOriginalDefinitions + "]";
		}

	}

}
