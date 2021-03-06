package editor.gui.filter.editor;

import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import editor.collection.category.CategorySpec;
import editor.filter.Filter;
import editor.filter.leaf.FilterLeaf;
import editor.gui.SettingsDialog;
import editor.gui.generic.ComboBoxPanel;

/**
 * This class represents a panel that presents the user with a
 * single combo box showing the default categories in the
 * settings dialog and returns the filter for the selected one.
 * It is replaced with the contents of the selected filter in the
 * filter tree.
 * 
 * @author Alec Roelke
 */
@SuppressWarnings("serial")
public class DefaultsFilterPanel extends FilterEditorPanel<FilterLeaf<?>>
{
	/**
	 * Combo box showing the default categories.
	 */
	private ComboBoxPanel<String> defaults;
	/**
	 * Map of category names onto their String representations.
	 */
	private Map<String, String> categories;
	
	/**
	 * Create a new DefaultsFilterPanel.
	 */
	public DefaultsFilterPanel()
	{
		super();
		setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		
		categories = new HashMap<String, String>();
		
		List<CategorySpec> presets = SettingsDialog.getPresetCategories();
		String[] names = presets.stream().map(CategorySpec::getName).toArray(String[]::new);
		for (int i = 0; i < presets.size(); i++)
			categories[names[i]] = presets[i].toListlessString();
		
		defaults = new ComboBoxPanel<String>(names);
		add(defaults);
	}
	
	/**
	 * @return A FilterGroup containing the parsed contents of the selected
	 * default category. 
	 */
	@Override
	public Filter filter()
	{
		return new CategorySpec(categories[defaults.getSelectedItem()]).getFilter();
	}

	/**
	 * Since this panel doesn't correspond to a filter, there are no contents
	 * to set.
	 */
	@Override
	public void setContents(FilterLeaf<?> filter)
	{}
}
