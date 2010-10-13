package net.sourceforge.metrics.core;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * To comply with Eclipse 3.0 API this class is needed to initialize the default values for the metrics preferences. It is references in the plugin.xml.
 * 
 * @author Frank Sauer
 */
public class MetricsPreferenceInitializer
extends AbstractPreferenceInitializer
implements Constants {

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.core.runtime.preferences.AbstractPreferenceInitializer# initializeDefaultPreferences()
	 */
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore prefStore = MetricsPlugin.getDefault().getPreferenceStore();
		prefStore.setDefault(METRICS_PREFERENCE_PREFIX + "." + "decimals", FRACTION_DIGITS);
		prefStore.setDefault(METRICS_PREFERENCE_PREFIX + "." + "xmlformat", "net.sourceforge.metrics.internal.xml.MetricsFirstExporter");
		prefStore.setDefault(METRICS_PREFERENCE_PREFIX + "." + "enablewarnings", false);
		prefStore.setDefault(METRICS_PREFERENCE_PREFIX + "." + "defaultColor", "0,0,0");
		prefStore.setDefault(METRICS_PREFERENCE_PREFIX + "." + "linkedColor", "0,0,255");
		prefStore.setDefault(METRICS_PREFERENCE_PREFIX + "." + "outOfRangeColor", "255,0,0");
		prefStore.setDefault(METRICS_PREFERENCE_PREFIX + "." + "depGR_background", "1,17,68");
		prefStore.setDefault(METRICS_PREFERENCE_PREFIX + "." + "showProject", true);
		prefStore.addPropertyChangeListener(MetricsPlugin.getDefault());
	}

}
