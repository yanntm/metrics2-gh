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
 * $id$
 */
package net.sourceforge.metrics.ui.preferences;

import net.sourceforge.metrics.core.MetricsPlugin;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Configure settings for cohesion metrics, e.g. the methods/attributes not to be
 * considered.
 * 
 * @see PreferencePage
 * @author Frank Sauer
 */
public class CohesionPreferencePage extends FieldEditorPreferencePage
implements IWorkbenchPreferencePage {

	public static final String COHESION_COUNT_ABSTRACT_METHODS =
		"LCOM.AbstractMethods";
	public static final String COHESION_COUNT_CONSTRUCTORS =
		"LCOM.Constructors";
	public static final String COHESION_COUNT_DEPRECATED_METHODS =
		"LCOM.Deprecated";
	public static final String COHESION_COUNT_INHERITED_ATTRIBUTES =
		"LCOM.InheritedAttributes";
	public static final String COHESION_COUNT_INHERITED_METHODS =
		"LCOM.InheritedMethods";
	public static final String COHESION_COUNT_INNERS =
		"LCOM.Inners";
	public static final String COHESION_COUNT_LOGGERS =
		"LCOM.Loggers";
	public static final String COHESION_COUNT_OBJECTS_METHODS =
		"LCOM.ObjectsMethods";
	public static final String COHESION_COUNT_STATIC_ATTRIBUTES =
		"LCOM.StaticAttributes";
	public static final String COHESION_COUNT_STATIC_METHODS =
		"LCOM.StaticMethods";
	public static final String COHESION_LINK_SYNCHRONIZED_METHODS =
		"LCOM.Synchronized";

	/**
	 * The constructor.
	 */
	public CohesionPreferencePage() {
		super(GRID);
		setPreferenceStore(MetricsPlugin.getDefault().getPreferenceStore());
		String instructions = "Settings for Cohesion Metrics -\n"
			+ "check items to include in the cohesion calculation.\n\n"
			+ "WARNING: changes invalidate cache and force recalculation!\n";
		setDescription(instructions);
	}

	public void init(IWorkbench workbench) {
		IPreferenceStore preferenceStore = getPreferenceStore();
		preferenceStore.setDefault(COHESION_COUNT_ABSTRACT_METHODS, true);
		preferenceStore.setDefault(COHESION_COUNT_CONSTRUCTORS, false);
		preferenceStore.setDefault(COHESION_COUNT_DEPRECATED_METHODS, true);
		preferenceStore.setDefault(COHESION_COUNT_INHERITED_ATTRIBUTES, false);
		preferenceStore.setDefault(COHESION_COUNT_INHERITED_METHODS, false);
		preferenceStore.setDefault(COHESION_COUNT_INNERS, false);
		preferenceStore.setDefault(COHESION_COUNT_LOGGERS, true);
		preferenceStore.setDefault(COHESION_COUNT_OBJECTS_METHODS, true);
		preferenceStore.setDefault(COHESION_COUNT_STATIC_ATTRIBUTES, false);
		preferenceStore.setDefault(COHESION_COUNT_STATIC_METHODS, false);
		preferenceStore.setDefault(COHESION_LINK_SYNCHRONIZED_METHODS, false);
	}

	/**
	 * Add check boxes to the cohesion preferences screen.
	 */
	@Override
	public void createFieldEditors() {
		Composite parent = getFieldEditorParent();
		
		addField(new BooleanFieldEditor(COHESION_COUNT_ABSTRACT_METHODS,
				"Abstract methods", parent));
		addField(new BooleanFieldEditor(COHESION_COUNT_CONSTRUCTORS,
				"Constructors", parent));
		addField(new BooleanFieldEditor(COHESION_COUNT_DEPRECATED_METHODS,
				"Deprecated methods", parent));
		addField(new BooleanFieldEditor(COHESION_COUNT_INHERITED_ATTRIBUTES,
				"Inherited attributes", parent));
		addField(new BooleanFieldEditor(COHESION_COUNT_INHERITED_METHODS,
				"Inherited methods", parent));
		addField(new BooleanFieldEditor(COHESION_COUNT_INNERS,
				"Inner class methods", parent));
		addField(new BooleanFieldEditor(COHESION_COUNT_LOGGERS, "Loggers",
				parent));
		addField(new BooleanFieldEditor(COHESION_COUNT_OBJECTS_METHODS,
				"Methods from Object", parent));
		addField(new BooleanFieldEditor(COHESION_COUNT_STATIC_ATTRIBUTES,
				"Static attributes", parent));
		addField(new BooleanFieldEditor(COHESION_COUNT_STATIC_METHODS,
				"Static methods", parent));
		addField(new BooleanFieldEditor(COHESION_LINK_SYNCHRONIZED_METHODS,
				"Link synchronized methods", parent));

	}

}
