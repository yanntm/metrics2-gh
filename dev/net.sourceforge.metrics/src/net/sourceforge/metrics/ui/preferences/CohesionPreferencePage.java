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

import net.sourceforge.metrics.core.Constants;
import net.sourceforge.metrics.core.MetricsPlugin;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
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
implements Constants, IWorkbenchPreferencePage {

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
		preferenceStore.setDefault(CONNECT_INTERFACE_METHODS, false);
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
		preferenceStore.setDefault(IGNORE_MEMBERS_PATTERN, "");
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
					"Use original definitions (overrides the settings below)", parent);
		// TODO figure out how to show/hide editors based on
		// the value stored in the originalDefinitionEditor
//		if (originalDefinitionEditor.getPreferenceStore() != null)
		addField(originalDefinitionEditor);
		addField(new BooleanFieldEditor(CONNECT_INTERFACE_METHODS,
				"Connect interface methods", parent));
		addField(new BooleanFieldEditor(COUNT_ABSTRACT_METHODS,
				"Include abstract methods", parent));
		addField(new BooleanFieldEditor(COUNT_CONSTRUCTORS,
				"Include constructors", parent));
		addField(new BooleanFieldEditor(COUNT_DEPRECATED_METHODS,
				"Include deprecated methods", parent));
//		addField(new BooleanFieldEditor(COUNT_INHERITED_ATTRIBUTES,
//				"Include inherited attributes", parent));
//		addField(new BooleanFieldEditor(COUNT_INHERITED_METHODS,
//				"Include inherited methods", parent));
//		addField(new BooleanFieldEditor(COUNT_INNERS,
//				"Include inner class methods", parent));
		addField(new BooleanFieldEditor(COUNT_LOGGERS, "Include loggers",
				parent));
		addField(new BooleanFieldEditor(COUNT_OBJECTS_METHODS,
				"Include methods from Object", parent));
		addField(new BooleanFieldEditor(COUNT_STATIC_ATTRIBUTES,
				"Include static attributes", parent));
		addField(new BooleanFieldEditor(COUNT_STATIC_METHODS,
				"Include static methods", parent));
		addField(new StringFieldEditor(IGNORE_MEMBERS_PATTERN,
				"Ignore members matching (Java) pattern:", parent));
	}

}
