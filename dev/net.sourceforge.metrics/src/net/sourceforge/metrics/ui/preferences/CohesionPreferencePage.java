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

	/** If this is "true", the original metric definitions are
	 * used; otherwise, the user has the options of deciding
	 * which members are considered in the calculations by setting
	 * the other preferences.
	 */
	public static final String USE_ORIGINAL_DEFINITIONS =
		"Cohesion.UseOriginals";

	/** The beginning of all cohesion preference strings. */
	public static final String COHESION_PREFIX = "Cohesion";
	
	public static final String COUNT_ABSTRACT_METHODS =
		COHESION_PREFIX + ".AbstractMethods";
	public static final String COUNT_CONSTRUCTORS =
		COHESION_PREFIX + ".Constructors";
	public static final String COUNT_DEPRECATED_METHODS =
		COHESION_PREFIX + ".Deprecated";
	public static final String COUNT_INHERITED_ATTRIBUTES =
		COHESION_PREFIX + ".InheritedAttributes";
	public static final String COUNT_INHERITED_METHODS =
		COHESION_PREFIX + ".InheritedMethods";
	public static final String COUNT_INNERS =
		COHESION_PREFIX + ".Inners";
	public static final String COUNT_LOGGERS =
		COHESION_PREFIX + ".Loggers";
	public static final String COUNT_OBJECTS_METHODS =
		COHESION_PREFIX + ".ObjectsMethods";
	public static final String COUNT_PUBLIC_METHODS_ONLY =
		COHESION_PREFIX + ".PublicMethodsOnly";
	public static final String COUNT_STATIC_ATTRIBUTES =
		COHESION_PREFIX + ".StaticAttributes";
	public static final String COUNT_STATIC_METHODS =
		COHESION_PREFIX + ".StaticMethods";


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
		preferenceStore.setDefault(USE_ORIGINAL_DEFINITIONS, true);
		preferenceStore.setDefault(COUNT_ABSTRACT_METHODS, true);
		preferenceStore.setDefault(COUNT_CONSTRUCTORS, false);
		preferenceStore.setDefault(COUNT_DEPRECATED_METHODS, true);
		preferenceStore.setDefault(COUNT_INHERITED_ATTRIBUTES, false);
		preferenceStore.setDefault(COUNT_INHERITED_METHODS, false);
		preferenceStore.setDefault(COUNT_INNERS, false);
		preferenceStore.setDefault(COUNT_LOGGERS, true);
		preferenceStore.setDefault(COUNT_OBJECTS_METHODS, true);
		preferenceStore.setDefault(COUNT_PUBLIC_METHODS_ONLY, false);
		preferenceStore.setDefault(COUNT_STATIC_ATTRIBUTES, false);
		preferenceStore.setDefault(COUNT_STATIC_METHODS, false);
	}

	/**
	 * Add check boxes to the cohesion preferences screen.
	 */
	@Override
	public void createFieldEditors() {
		Composite parent = getFieldEditorParent();
// TODO KAC uncomment these when functionality is in place.
		BooleanFieldEditor originalDefinitionEditor =
			new BooleanFieldEditor(USE_ORIGINAL_DEFINITIONS,
					"Use original definitions (overrides other settings)", parent);
		// TODO figure out how to show/hide editors based on
		// the value stored in the originalDefinitionEditor
//		if (originalDefinitionEditor.getPreferenceStore() != null)
		addField(originalDefinitionEditor);
		addField(new BooleanFieldEditor(COUNT_ABSTRACT_METHODS,
				"Abstract methods", parent));
		addField(new BooleanFieldEditor(COUNT_CONSTRUCTORS,
				"Constructors", parent));
		addField(new BooleanFieldEditor(COUNT_DEPRECATED_METHODS,
				"Deprecated methods", parent));
//		addField(new BooleanFieldEditor(COUNT_INHERITED_ATTRIBUTES,
//				"Inherited attributes", parent));
//		addField(new BooleanFieldEditor(COUNT_INHERITED_METHODS,
//				"Inherited methods", parent));
//		addField(new BooleanFieldEditor(COUNT_INNERS,
//				"Inner class methods", parent));
		addField(new BooleanFieldEditor(COUNT_LOGGERS, "Loggers",
				parent));
		addField(new BooleanFieldEditor(COUNT_OBJECTS_METHODS,
				"Methods from Object", parent));
		addField(new BooleanFieldEditor(COUNT_STATIC_ATTRIBUTES,
				"Static attributes", parent));
		addField(new BooleanFieldEditor(COUNT_STATIC_METHODS,
				"Static methods", parent));
	}

}
