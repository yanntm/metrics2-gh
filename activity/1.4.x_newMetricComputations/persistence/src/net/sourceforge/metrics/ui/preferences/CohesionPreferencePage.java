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

	public static final String COUNT_ABSTRACT_METHODS =
		"Cohesion.AbstractMethods";
	public static final String COUNT_CONSTRUCTORS =
		"Cohesion.Constructors";
	public static final String COUNT_DEPRECATED_METHODS =
		"Cohesion.Deprecated";
	public static final String COUNT_INHERITED_ATTRIBUTES =
		"Cohesion.InheritedAttributes";
	public static final String COUNT_INHERITED_METHODS =
		"Cohesion.InheritedMethods";
	public static final String COUNT_INNERS =
		"Cohesion.Inners";
	public static final String COUNT_LOGGERS =
		"Cohesion.Loggers";
	public static final String COUNT_OBJECTS_METHODS =
		"Cohesion.ObjectsMethods";
	public static final String COUNT_STATIC_ATTRIBUTES =
		"Cohesion.StaticAttributes";
	public static final String COUNT_STATIC_METHODS =
		"Cohesion.StaticMethods";

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
		preferenceStore.setDefault(COUNT_ABSTRACT_METHODS, true);
		preferenceStore.setDefault(COUNT_CONSTRUCTORS, false);
		preferenceStore.setDefault(COUNT_DEPRECATED_METHODS, true);
		preferenceStore.setDefault(COUNT_INHERITED_ATTRIBUTES, false);
		preferenceStore.setDefault(COUNT_INHERITED_METHODS, false);
		preferenceStore.setDefault(COUNT_INNERS, false);
		preferenceStore.setDefault(COUNT_LOGGERS, true);
		preferenceStore.setDefault(COUNT_OBJECTS_METHODS, true);
		preferenceStore.setDefault(COUNT_STATIC_ATTRIBUTES, false);
		preferenceStore.setDefault(COUNT_STATIC_METHODS, false);
	}

	/**
	 * Add check boxes to the cohesion preferences screen.
	 */
	@Override
	public void createFieldEditors() {
		Composite parent = getFieldEditorParent();
// TODO uncomment these when functionality is in place.
//		addField(new BooleanFieldEditor(COUNT_ABSTRACT_METHODS,
//				"Abstract methods", parent));
//		addField(new BooleanFieldEditor(COUNT_CONSTRUCTORS,
//				"Constructors", parent));
//		addField(new BooleanFieldEditor(COUNT_DEPRECATED_METHODS,
//				"Deprecated methods", parent));
//		addField(new BooleanFieldEditor(COUNT_INHERITED_ATTRIBUTES,
//				"Inherited attributes", parent));
//		addField(new BooleanFieldEditor(COUNT_INHERITED_METHODS,
//				"Inherited methods", parent));
//		addField(new BooleanFieldEditor(COUNT_INNERS,
//				"Inner class methods", parent));
//		addField(new BooleanFieldEditor(COUNT_LOGGERS, "Loggers",
//				parent));
//		addField(new BooleanFieldEditor(COUNT_OBJECTS_METHODS,
//				"Methods from Object", parent));
		addField(new BooleanFieldEditor(COUNT_STATIC_ATTRIBUTES,
				"Static attributes", parent));
		addField(new BooleanFieldEditor(COUNT_STATIC_METHODS,
				"Static methods", parent));
	}

}
