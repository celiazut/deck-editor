package editor.gui.filter;

import java.util.HashSet;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import editor.filter.Filter;

/**
 * This class represents a panel that corresponds to a filter but
 * allows the user to edit its contents.
 * 
 * @author Alec Roelke
 *
 * @param <F> Type of filter being edited
 */
@SuppressWarnings("serial")
public abstract class FilterPanel<F extends Filter> extends JPanel
{
	/**
	 * Group that this FilterPanel belongs to.
	 */
	protected FilterGroupPanel group;
	/**
	 * Change listeners that have been registered with this FilterPanel.
	 */
	private Set<ChangeListener> listeners;
	
	/**
	 * Create a new FilterPanel that belongs to no group.
	 */
	public FilterPanel()
	{
		super();
		group = null;
		listeners = new HashSet<ChangeListener>();
	}
	
	/**
	 * @return The filter currently being edited by this FilterPanel.
	 * If the panel was created from an already-existing filter, that
	 * filter will not reflect changes made in the panel!  This function
	 * returns a copy of that filter modified according to the fields.
	 */
	public abstract Filter filter();
	
	/**
	 * Set the contents of this FilterPanel.
	 * 
	 * @param filter Filter containing the information that should
	 * be displayed by this FilterPanel.
	 */
	public abstract void setContents(F filter);
	
	/**
	 * Register a new ChangeListener with this FilterPanel, which will fire
	 * when panels or groups are added or removed.
	 * 
	 * @param listener ChangeListener to register
	 */
	public void addChangeListener(ChangeListener listener)
	{
		listeners.add(listener);
	}
	
	/**
	 * De-register a ChangeListener with this FilterPanel, so it will no longer
	 * fire.
	 * 
	 * @param listener ChangeListener to remove
	 */
	public void removeChangeListener(ChangeListener listener)
	{
		listeners.remove(listener);
	}
	
	/**
	 * Indicate that a group or panel has been added or removed, and fire this
	 * FilterPanel's listeners and all the listeners of its parents up the tree.
	 */
	public void firePanelsChanged()
	{
		if (group != null)
			group.firePanelsChanged();
		for (ChangeListener listener: listeners)
			listener.stateChanged(new ChangeEvent(this));
	}
}
