package gui;

import gui.editor.CategoryEditorPanel;
import gui.editor.CategoryPanel;
import gui.editor.EditorFrame;
import gui.filter.FilterGroupPanel;
import gui.inventory.InventoryDownloadDialog;
import gui.inventory.InventoryLoadDialog;
import gui.inventory.InventoryTableModel;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyVetoException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.text.html.HTMLDocument;

import util.StripedTable;
import util.TableMouseAdapter;
import database.Card;
import database.Inventory;
import database.ManaCost;
import database.characteristics.CardCharacteristic;

/**
 * This class represents the main frame of the editor.  It contains several tabs that display information
 * about decks.
 * 
 * The editor tab is divided into three sections:  On the left side is a database of all cards that can be
 * added to decks with a window below it that displays the Oracle text of the currently-selected card.  On
 * the right side is a pane which contains internal frames that allow the user to open, close, and edit
 * multiple decks at once.  See @link{gui.editor.EditorFrame} for details on the editor frames.
 * 
 * TODO: Add some indication that a card is in the deck in the inventory table if it is
 * TODO: Remove terminal output showing status of initialization
 * 
 * @author Alec Roelke
 */
@SuppressWarnings("serial")
public class MainFrame extends JFrame
{
	/**
	 * Update status value: update needed.
	 */
	public static final int UPDATE_NEEDED = 0;
	/**
	 * Update status value: update not needed.
	 */
	public static final int NO_UPDATE = 1;
	/**
	 * Update status value: update needed, but was not requested.
	 */
	public static final int UPDATE_CANCELLED = 2;
	/**
	 * Delimiter for preset categories in the settings file.
	 */
	public static final String CATEGORY_DELIMITER = "∎";
	
	/**
	 * Inventory of all cards.
	 */
	private Inventory inventory;
	/**
	 * Table displaying the inventory of all cards.
	 */
	private StripedTable inventoryTable;
	/**
	 * Model for the table displaying the inventory of all cards.
	 */
	private InventoryTableModel inventoryModel;
	/**
	 * Panel for editing the inventory filter.
	 */
	private FilterGroupPanel filter;
	/**
	 * Currently-selected card which will be added to decks.
	 */
	private Card selectedCard;
	/**
	 * Pane for showing the Oracle text of the currently-selected card.
	 */
	private JTextPane oracleTextPane;
	/**
	 * Desktop pane containing internal editor frames.
	 */
	private JDesktopPane decklistDesktop;
	/**
	 * Number to append to the end of untitled decks that have just been created.
	 */
	private int untitled;
	/**
	 * Currently-selected editor frame.
	 */
	private EditorFrame selectedFrame;
	/**
	 * List of open editor frames.
	 */
	private List<EditorFrame> editors;
	/**
	 * File chooser for opening and saving.
	 */
	private JFileChooser fileChooser;
	/**
	 * Properties object storing settings.
	 */
	private Properties properties;
	/**
	 * URL pointing to the site to get the latest version of the
	 * inventory from.
	 */
	private URL versionSite;
	/**
	 * File to store the inventory in.
	 */
	private File inventoryFile;
	/**
	 * URL pointing to the site to get the inventory from.
	 */
	private URL inventorySite;
	/**
	 * Number of recent files to display.
	 */
	private int recentCount;
	/**
	 * Menu items showing recent files to open.
	 */
	private Queue<JMenuItem> recentItems;
	/**
	 * Map of those menu items onto the files they should open.
	 */
	private Map<JMenuItem, File> recents;
	/**
	 * Menu containing the recent menu items.
	 */
	private JMenu recentsMenu;
	/**
	 * Newest version number of the inventory.
	 */
	private String newestVersion;
	/**
	 * Menu showing preset categories.
	 */
	private JMenu presetMenu;
	
	/**
	 * Create a new MainFrame.
	 */
	public MainFrame()
	{
		super();
		
		selectedCard = null;
		untitled = 0;
		selectedFrame = null;
		editors = new ArrayList<EditorFrame>();
		filter = null;
		recentItems = new LinkedList<JMenuItem>();
		recents = new HashMap<JMenuItem, File>();
		
		// Initialize properties to their default values, then load the current values
		// from the properties file
		resetDefaultSettings();
		try (InputStreamReader in = new InputStreamReader(new FileInputStream(SettingsDialog.PROPERTIES_FILE)))
		{
			properties.load(in);
		}
		catch (FileNotFoundException e)
		{
//			JOptionPane.showMessageDialog(null, "File " + PROPERTIES_FILE + " not found.  Using default settings.", "Warning", JOptionPane.WARNING_MESSAGE);
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog(null, "Error opening " + SettingsDialog.PROPERTIES_FILE + ": " + e.getMessage() + ".", "Warning", JOptionPane.WARNING_MESSAGE);
		}
		try
		{
			versionSite = new URL(properties.getProperty(SettingsDialog.INVENTORY_SOURCE) + properties.getProperty(SettingsDialog.VERSION_FILE));
		}
		catch (MalformedURLException e)
		{
			JOptionPane.showMessageDialog(null, "Bad version URL: " + properties.getProperty(SettingsDialog.INVENTORY_SOURCE) + properties.getProperty(SettingsDialog.VERSION_FILE), "Warning", JOptionPane.WARNING_MESSAGE);
		}
		try
		{
			inventorySite = new URL(properties.getProperty(SettingsDialog.INVENTORY_SOURCE) + properties.getProperty(SettingsDialog.INVENTORY_FILE));
		}
		catch (MalformedURLException e)
		{
			JOptionPane.showMessageDialog(null, "Bad file URL: " + properties.getProperty(SettingsDialog.INVENTORY_SOURCE) + properties.getProperty(SettingsDialog.INVENTORY_FILE), "Warning", JOptionPane.WARNING_MESSAGE);
		}
		inventoryFile = new File(properties.getProperty(SettingsDialog.INVENTORY_LOCATION) + File.separator + properties.getProperty(SettingsDialog.INVENTORY_FILE));
		recentCount = Integer.valueOf(properties.getProperty(SettingsDialog.RECENT_COUNT));
		if (properties.getProperty(SettingsDialog.INVENTORY_COLUMNS).isEmpty())
			properties.put(SettingsDialog.INVENTORY_COLUMNS, "Name,Expansion,Mana Cost,Type");
		newestVersion = properties.getProperty(SettingsDialog.VERSION);
		
		System.out.println("Loaded properties");
		
		// TODO: Pick a title
		setTitle("MTG Deck Editor");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		Dimension screenRes = Toolkit.getDefaultToolkit().getScreenSize();
		setBounds(50, 50, screenRes.width - 100, screenRes.height - 100);
		
		/* MENU BAR */
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		// File menu
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		System.out.println("Created file menu");
		// TODO: Add items for importing and exporting from/to different deck formats
		
		// New file menu item
		JMenuItem newItem = new JMenuItem("New");
		newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK));
		newItem.addActionListener((e) -> newEditor());
		fileMenu.add(newItem);
		System.out.println("Created new file item");
		
		// Open file menu item
		JMenuItem openItem = new JMenuItem("Open...");
		openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK));
		openItem.addActionListener((e) -> open());
		fileMenu.add(openItem);
		System.out.println("Created open file item");
		
		// Close file menu item
		JMenuItem closeItem = new JMenuItem("Close");
		closeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_MASK));
		closeItem.addActionListener((e) -> {if (selectedFrame != null) close(selectedFrame); else exit();});
		fileMenu.add(closeItem);
		System.out.println("Created close file item");
		
		// Close all files menu item
		JMenuItem closeAllItem = new JMenuItem("Close All");
		closeAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
		closeAllItem.addActionListener((e) -> closeAll());
		fileMenu.add(closeAllItem);
		System.out.println("Created close all files item");
		
		fileMenu.add(new JSeparator());
		
		// Save file menu item
		JMenuItem saveItem = new JMenuItem("Save");
		saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK));
		saveItem.addActionListener((e) -> {if (selectedFrame != null) save(selectedFrame);});
		fileMenu.add(saveItem);
		System.out.println("Created save file item");
		
		// Save file as menu item
		JMenuItem saveAsItem = new JMenuItem("Save As...");
		saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0));
		saveAsItem.addActionListener((e) -> {if (selectedFrame != null) saveAs(selectedFrame);});
		fileMenu.add(saveAsItem);
		System.out.println("Created save as item");
		
		// Save all files menu item
		JMenuItem saveAllItem = new JMenuItem("Save All");
		saveAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
		saveAllItem.addActionListener((e) -> saveAll());
		fileMenu.add(saveAllItem);
		System.out.println("Created save all item");
		
		// Recent files menu
		recentsMenu = new JMenu("Open Recent");
		recentsMenu.setEnabled(false);
		if (!properties.getProperty(SettingsDialog.RECENT_FILES).isEmpty())
			for (String fname: properties.getProperty(SettingsDialog.RECENT_FILES).split("\\|"))
				updateRecents(new File(fname));
		fileMenu.add(recentsMenu);
		System.out.println("Created recents menu");
		
		fileMenu.add(new JSeparator());
		
		// Exit menu item
		JMenuItem exitItem = new JMenuItem("Exit");
		exitItem.addActionListener((e) -> exit());
		exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, KeyEvent.ALT_MASK));
		fileMenu.add(exitItem);
		System.out.println("Created exit item");
		
		// Edit menu
		JMenu editMenu = new JMenu("Edit");
		menuBar.add(editMenu);
		System.out.println("Created edit menu");
		
		// Undo menu item
		JMenuItem undoItem = new JMenuItem("Undo");
		undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_MASK));
		undoItem.addActionListener((e) -> {if (selectedFrame != null) selectedFrame.undo();});
		editMenu.add(undoItem);
		System.out.println("Created undo item");
		
		// Redo menu item
		JMenuItem redoItem = new JMenuItem("Redo");
		redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_MASK));
		redoItem.addActionListener((e) -> {if (selectedFrame != null) selectedFrame.redo();});
		editMenu.add(redoItem);
		System.out.println("Created redo item");
		
		editMenu.add(new JSeparator());
		
		// Preferences menu item
		JMenuItem preferencesItem = new JMenuItem("Preferences...");
		preferencesItem.addActionListener((e) -> {
			SettingsDialog settings = new SettingsDialog(this, properties);
			settings.setVisible(true);
		});
		editMenu.add(preferencesItem);
		System.out.println("Created preferences menu");
		
		// Deck menu
		JMenu deckMenu = new JMenu("Deck");
		menuBar.add(deckMenu);
		System.out.println("Created deck menu");
		
		// Add card menu
		JMenu addMenu = new JMenu("Add Cards");
		deckMenu.add(addMenu);
		System.out.println("Created cards menu");
		
		// Add single copy item
		JMenuItem addSingleItem = new JMenuItem("Add Single Copy");
		addSingleItem.setAccelerator(KeyStroke.getKeyStroke('+'));
		addSingleItem.addActionListener((e) -> {if (selectedFrame != null) selectedFrame.addCards(getSelectedCards(), 1);});
		addMenu.add(addSingleItem);
		System.out.println("Created add single item");
		
		// Fill playset item
		JMenuItem playsetItem = new JMenuItem("Fill Playset");
		playsetItem.addActionListener((e) -> {
			if (selectedFrame != null)
				for (Card c: getSelectedCards())
					selectedFrame.addCard(c, 4 - selectedFrame.count(c));
		});
		addMenu.add(playsetItem);
		System.out.println("Created fill playset item");
		
		// Add variable item
		JMenuItem addNItem = new JMenuItem("Add Copies...");
		addNItem.addActionListener((e) -> {
			if (selectedFrame != null)
			{
				JPanel contentPanel = new JPanel(new BorderLayout());
				contentPanel.add(new JLabel("Copies to add:"), BorderLayout.WEST);
				JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1));
				contentPanel.add(spinner, BorderLayout.SOUTH);
				if (JOptionPane.showOptionDialog(null, contentPanel, "Add Cards", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null) == JOptionPane.OK_OPTION)
					selectedFrame.addCards(getSelectedCards(), (Integer)spinner.getValue());
			}
		});
		addMenu.add(addNItem);
		System.out.println("Created add N item");
		
		// Remove card menu
		JMenu removeMenu = new JMenu("Remove Cards");
		deckMenu.add(removeMenu);
		System.out.println("Created remove menu");
		
		// Remove single copy item
		JMenuItem removeSingleItem = new JMenuItem("Remove Single Copy");
		removeSingleItem.setAccelerator(KeyStroke.getKeyStroke('-'));
		removeSingleItem.addActionListener((e) -> {if (selectedFrame != null) selectedFrame.removeSelectedCards(1);});
		removeMenu.add(removeSingleItem);
		System.out.println("Created remove single item");
		
		// Remove all item
		JMenuItem removeAllItem = new JMenuItem("Remove All Copies");
		removeAllItem.addActionListener((e) -> {if (selectedFrame != null) selectedFrame.removeSelectedCards(Integer.MAX_VALUE);});
		removeMenu.add(removeAllItem);
		System.out.println("Created remove all item");
		
		// Remove variable item
		JMenuItem removeNItem = new JMenuItem("Remove Copies...");
		removeNItem.addActionListener((e) -> {
			if (selectedFrame != null)
			{
				JPanel contentPanel = new JPanel(new BorderLayout());
				contentPanel.add(new JLabel("Copies to remove:"), BorderLayout.WEST);
				JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1));
				contentPanel.add(spinner, BorderLayout.SOUTH);
				if (JOptionPane.showOptionDialog(null, contentPanel, "Add Cards", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null) == JOptionPane.OK_OPTION)
					selectedFrame.removeSelectedCards((Integer)spinner.getValue());
			}
		});
		removeMenu.add(removeNItem);
		System.out.println("Created remove N item");
		
		// Category menu
		JMenu categoryMenu = new JMenu("Category");
		deckMenu.add(categoryMenu);
		System.out.println("Created category menu");
		
		// Add category item
		JMenuItem addCategoryItem = new JMenuItem("Add...");
		addCategoryItem.addActionListener((e) -> {if (selectedFrame != null) selectedFrame.createCategory();});
		categoryMenu.add(addCategoryItem);
		System.out.println("Created add category item");
		
		// Edit category item
		JMenuItem editCategoryItem = new JMenuItem("Edit...");
		editCategoryItem.addActionListener((e) -> {
			if (selectedFrame != null)
			{
				JPanel contentPanel = new JPanel(new BorderLayout());
				contentPanel.add(new JLabel("Choose a category to edit:"), BorderLayout.NORTH);
				JList<String> categories = new JList<String>(selectedFrame.categoryNames());
				categories.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				contentPanel.add(new JScrollPane(categories), BorderLayout.CENTER);
				if (JOptionPane.showOptionDialog(null, contentPanel, "Edit Category", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null) == JOptionPane.OK_OPTION)
					selectedFrame.editCategory(categories.getSelectedValue());
			}
		});
		categoryMenu.add(editCategoryItem);
		System.out.println("Created edit category item");
		
		// Remove category item
		JMenuItem removeCategoryItem = new JMenuItem("Remove...");
		removeCategoryItem.addActionListener((e) -> {
			if (selectedFrame != null)
			{
				JPanel contentPanel = new JPanel(new BorderLayout());
				contentPanel.add(new JLabel("Choose a category to remove:"), BorderLayout.NORTH);
				JList<String> categories = new JList<String>(selectedFrame.categoryNames());
				categories.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				contentPanel.add(new JScrollPane(categories), BorderLayout.CENTER);
				if (JOptionPane.showOptionDialog(null, contentPanel, "Edit Category", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null) == JOptionPane.OK_OPTION)
					selectedFrame.removeCategory(categories.getSelectedValue());
			}
		});
		categoryMenu.add(removeCategoryItem);
		System.out.println("Created remove category item");
		
		// Preset categories menu
		presetMenu = new JMenu("Add Preset");
		categoryMenu.add(presetMenu);
		for (String category: properties.getProperty(SettingsDialog.EDITOR_PRESETS).split(CATEGORY_DELIMITER))
		{
			CategoryEditorPanel editor = new CategoryEditorPanel(category);
			JMenuItem categoryItem = new JMenuItem(editor.name());
			categoryItem.addActionListener((e) -> {
				if (selectedFrame != null && !selectedFrame.containsCategory(editor.name()))
					selectedFrame.addCategory(new CategoryPanel(editor.name(), editor.repr(), editor.filter(), selectedFrame));
			});
			presetMenu.add(categoryItem);
		}
		System.out.println("Created category presets menu");
		
		// Help menu
		JMenu helpMenu = new JMenu("Help");
		menuBar.add(helpMenu);
		System.out.println("Created help menu");
		// TODO: Add a help dialog
		
		JMenuItem updateInventoryItem = new JMenuItem("Check for inventory update...");
		updateInventoryItem.addActionListener((e) -> {
			switch (checkForUpdate())
			{
			case UPDATE_NEEDED:
				if (updateInventory())
				{
					properties.put(SettingsDialog.VERSION, newestVersion);
					loadInventory();
				}
				break;
			case NO_UPDATE:
				JOptionPane.showMessageDialog(null, "Inventory is up to date.");
				break;
			case UPDATE_CANCELLED:
				break;
			default:
				break;
			}
		});
		helpMenu.add(updateInventoryItem);
		System.out.println("Created update inventory item");
		
		JMenuItem reloadInventoryItem = new JMenuItem("Reload inventory...");
		reloadInventoryItem.addActionListener((e) -> loadInventory());
		helpMenu.add(reloadInventoryItem);
		System.out.println("Created reload inventory item");
		
		/* CONTENT PANE */
		// Panel containing all content
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, ActionEvent.CTRL_MASK), "Next Frame");
		contentPane.getActionMap().put("Next Frame", new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (!editors.isEmpty() && selectedFrame != null)
					selectFrame(editors.get((editors.indexOf(selectedFrame) + 1)%editors.size()));
			}
		});
		contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, ActionEvent.CTRL_MASK), "Previous Frame");
		contentPane.getActionMap().put("Previous Frame", new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (!editors.isEmpty() && selectedFrame != null)
				{
					int next = editors.indexOf(selectedFrame) - 1;
					selectFrame(editors.get(next < 0 ? editors.size() - 1 : next));
				}
			}
		});
		setContentPane(contentPane);
		System.out.println("Created content pane");
		
		// DesktopPane containing editor frames
		decklistDesktop = new JDesktopPane();
		decklistDesktop.setBackground(SystemColor.controlShadow);
		System.out.println("Created desktop pane");
		
		// Panel containing inventory and image of currently-selected card
		JPanel inventoryPanel = new JPanel(new BorderLayout(0, 0));
		inventoryPanel.setPreferredSize(new Dimension(getWidth()/4, getHeight()*3/4));
		System.out.println("Created cards panel");
		
		// Panel showing the image of the currently-selected card
		JPanel imagePanel = new JPanel();
		inventoryPanel.add(imagePanel, BorderLayout.NORTH);
		System.out.println("Created card image panel");
		
		// Panel containing the inventory and the quick-filter bar
		JPanel tablePanel = new JPanel(new BorderLayout(0, 0));
		inventoryPanel.add(tablePanel, BorderLayout.CENTER);
		System.out.println("Created inventory table panel");
		
		// Panel containing the quick-filter bar
		JPanel filterPanel = new JPanel();
		filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.X_AXIS));
		System.out.println("Created quick-filter bar panel");
		
		// Text field for quickly filtering by name
		JTextField nameFilterField = new JTextField();
		filterPanel.add(nameFilterField);
		System.out.println("Created quick-filter text field");
		
		// Button for clearing the filter
		JButton clearButton = new JButton("X");
		filterPanel.add(clearButton);
		System.out.println("Created clear filter button");
		
		// Button for opening the advanced filter dialog
		JButton advancedFilterButton = new JButton("Advanced...");
		filterPanel.add(advancedFilterButton);
		tablePanel.add(filterPanel, BorderLayout.NORTH);
		System.out.println("Created advanced filter button");
		
		// Panel displaying the Oracle text of the currently-selected card
		JPanel textPanel = new JPanel(new BorderLayout());
		System.out.println("Created oracle text panel");
		
		// Label saying "Oracle Text:"
		JPanel oracleLabelPanel = new JPanel(new BorderLayout(0, 0));
		textPanel.add(oracleLabelPanel, BorderLayout.NORTH);
		oracleLabelPanel.add(new JLabel("Oracle Text:"));
		System.out.println("Created oracle text label");
		
		// Pane displaying the Oracle text
		oracleTextPane = new JTextPane();
		oracleTextPane.setEditable(false);
		oracleTextPane.setContentType("text/html");
		oracleTextPane.setFont(UIManager.getFont("Label.font"));
		oracleTextPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
		try
		{
			((HTMLDocument)oracleTextPane.getDocument()).setBase(new File(".").toURI().toURL());
		}
		catch (MalformedURLException e)
		{}
		textPanel.add(new JScrollPane(oracleTextPane), BorderLayout.CENTER);
		System.out.println("Created oracle text pane");
		
		// Create the inventory and put it in the table
		inventoryTable = new StripedTable();
		inventoryTable.setAutoCreateRowSorter(true);
		inventoryTable.setDefaultRenderer(ManaCost.class, new ManaCostRenderer());
		inventoryTable.setFillsViewportHeight(true);
		inventoryTable.setShowGrid(false);
		inventoryTable.setStripeColor(SettingsDialog.stringToColor(properties.getProperty(SettingsDialog.INVENTORY_STRIPE)));
		tablePanel.add(new JScrollPane(inventoryTable), BorderLayout.CENTER);
		System.out.println("Created inventory table");
		
		// Table popup menu
		JPopupMenu inventoryMenu = new JPopupMenu();
		inventoryTable.addMouseListener(new TableMouseAdapter(inventoryTable, inventoryMenu));
		System.out.println("Created inventory table popup menu");
		
		// Add single copy item
		JMenuItem addSinglePopupItem = new JMenuItem("Add Single Copy");
		addSinglePopupItem.addActionListener((e) -> {if (selectedFrame != null) selectedFrame.addCards(getSelectedCards(), 1);});
		inventoryMenu.add(addSinglePopupItem);
		System.out.println("Created add single popup item");
		
		// Fill playset item
		JMenuItem playsetPopupItem = new JMenuItem("Fill Playset");
		playsetPopupItem.addActionListener((e) -> {
			if (selectedFrame != null)
				for (Card c: getSelectedCards())
					selectedFrame.addCard(c, 4 - selectedFrame.count(c));
		});
		inventoryMenu.add(playsetPopupItem);
		System.out.println("Created fill playset popup item");
		
		// Add variable item
		JMenuItem addNPopupItem = new JMenuItem("Add Copies...");
		addNPopupItem.addActionListener((e) -> {
			if (selectedFrame != null)
			{
				JPanel contentPanel = new JPanel(new BorderLayout());
				contentPanel.add(new JLabel("Copies to add:"), BorderLayout.WEST);
				JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1));
				contentPanel.add(spinner, BorderLayout.SOUTH);
				if (JOptionPane.showOptionDialog(null, contentPanel, "Add Cards", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null) == JOptionPane.OK_OPTION)
					selectedFrame.addCards(getSelectedCards(), (Integer)spinner.getValue());
			}
		});
		inventoryMenu.add(addNPopupItem);
		System.out.println("Created add N popup item");
		
		inventoryMenu.add(new JSeparator());
		
		// Remove single copy item
		JMenuItem removeSinglePopupItem = new JMenuItem("Remove Single Copy");
		removeSinglePopupItem.addActionListener((e) -> {if (selectedFrame != null) selectedFrame.removeCards(getSelectedCards(), 1);});
		inventoryMenu.add(removeSinglePopupItem);
		System.out.println("Created remove single popup item");
		
		// Remove all item
		JMenuItem removeAllPopupItem = new JMenuItem("Remove All Copies");
		removeAllPopupItem.addActionListener((e) -> {if (selectedFrame != null) selectedFrame.removeCards(getSelectedCards(), Integer.MAX_VALUE);});
		inventoryMenu.add(removeAllPopupItem);
		System.out.println("Created remove all popup item");
		
		// Remove variable item
		JMenuItem removeNPopupItem = new JMenuItem("Remove Copies...");
		removeNPopupItem.addActionListener((e) -> {
			if (selectedFrame != null)
			{
				JPanel contentPanel = new JPanel(new BorderLayout());
				contentPanel.add(new JLabel("Copies to remove:"), BorderLayout.WEST);
				JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1));
				contentPanel.add(spinner, BorderLayout.SOUTH);
				if (JOptionPane.showOptionDialog(null, contentPanel, "Add Cards", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null) == JOptionPane.OK_OPTION)
					selectedFrame.removeCards(getSelectedCards(), (Integer)spinner.getValue());
			}
		});
		inventoryMenu.add(removeNPopupItem);
		System.out.println("Created remove N popup item");
		
		// Action to be taken when the user presses the Enter key after entering text into the quick-filter
		// bar
		nameFilterField.addActionListener((e) -> {
			filter = new FilterGroupPanel();
			inventory.updateFilter((c) -> c.normalizedName().contains(nameFilterField.getText().toLowerCase()));
			inventoryModel.fireTableDataChanged();
		});
		System.out.println("Created quick-filter text field action");
		
		// Action to be taken when the clear button is pressed (reset the filter)
		clearButton.addActionListener((e) -> {
			nameFilterField.setText("");
			filter = new FilterGroupPanel();
			inventory.updateFilter((c) -> true);
			inventoryModel.fireTableDataChanged();
		});
		System.out.println("Created clear filter button action");
		
		// Action to be taken when the advanced filter button is pressed (show the advanced filter
		// dialog)
		advancedFilterButton.addActionListener((e) -> {
			if (JOptionPane.showOptionDialog(null, filter, "Advanced Filter", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null) == JOptionPane.OK_OPTION)
			{
				nameFilterField.setText("");
				inventory.updateFilter(filter.filter());
				inventoryModel.fireTableDataChanged();
			}
		});
		System.out.println("Created advanced filter button action");
		
		// Action to be taken when a selection is made in the inventory table (update the relevant
		// panels)
		inventoryTable.getSelectionModel().addListSelectionListener((e) -> {
			if (!e.getValueIsAdjusting())
			{
				ListSelectionModel lsm = (ListSelectionModel)e.getSource();
				if (!lsm.isSelectionEmpty())
					selectCard(inventory.get(inventoryTable.convertRowIndexToModel(lsm.getMinSelectionIndex())));
			}
		});
		System.out.println("Created inventory table selection action");
		
		// Split panes dividing the panel into three sections.  They can be resized at will.
		JSplitPane inventorySplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inventoryPanel, textPanel);
		inventorySplit.setOneTouchExpandable(true);
		inventorySplit.setContinuousLayout(true);
		JSplitPane editorSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inventorySplit, decklistDesktop);
		editorSplit.setOneTouchExpandable(true);
		editorSplit.setContinuousLayout(true);
		contentPane.add(editorSplit, BorderLayout.CENTER);
		System.out.println("Created split panes");
		
		// File chooser
		fileChooser = new JFileChooser(properties.getProperty(SettingsDialog.INITIALDIR));
		fileChooser.setMultiSelectionEnabled(false);
		System.out.println("Created file chooser");
		
		// Handle what happens when the window tries to close and when it opens.
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowOpened(WindowEvent e)
			{
				if (Boolean.valueOf(properties.getProperty(SettingsDialog.INITIALDIR)) && checkForUpdate() == UPDATE_NEEDED)
					if (updateInventory())
						properties.put(SettingsDialog.VERSION, newestVersion);
				loadInventory();
			}
			
			@Override
			public void windowClosing(WindowEvent e)
			{
				exit();
			}
		});
		System.out.println("Initialization complete");
	}
	
	/**
	 * Exit the application if all open editors successfully close.
	 */
	public void exit()
	{
		if (closeAll())
		{
			saveSettings();
			System.exit(0);
		}
	}
	
	/**
	 * Load the inventory and initialize the inventory table.
	 * @see InventoryLoadDialog
	 */
	public void loadInventory()
	{
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		InventoryLoadDialog loadDialog = new InventoryLoadDialog(this);
		loadDialog.setLocationRelativeTo(this);
		inventory = loadDialog.createInventory(inventoryFile);
		inventory.sort((a, b) -> a.compareName(b));
		inventoryModel = new InventoryTableModel(inventory, Arrays.stream(properties.getProperty(SettingsDialog.INVENTORY_COLUMNS).split(",")).map(CardCharacteristic::get).collect(Collectors.toList()));
		inventoryTable.setModel(inventoryModel);
		filter = new FilterGroupPanel();
		
		setCursor(Cursor.getDefaultCursor());
	}
	
	/**
	 * Check to see if the inventory needs to be updated.  If it does, ask the user if it should be.
	 * 
	 * @return an integer value representing the state of the update.  It can be:
	 * UPDATE_NEEDED
	 * NO_UPDATE
	 * UPDATE_CANCELLED
	 */
	public int checkForUpdate()
	{
		if (!inventoryFile.exists())
		{
			JOptionPane.showMessageDialog(null, inventoryFile.getName() + " not found.  It will be downloaded.", "Update", JOptionPane.WARNING_MESSAGE);
			try (BufferedReader in = new BufferedReader(new InputStreamReader(versionSite.openStream())))
			{
				newestVersion = in.readLine();
				newestVersion = newestVersion.substring(1, newestVersion.length() - 1);
			}
			catch (IOException e)
			{
				JOptionPane.showMessageDialog(null, "Error connecting to server: " + e.getMessage() + ".", "Connection Error", JOptionPane.ERROR_MESSAGE);
				return NO_UPDATE;
			}
			return UPDATE_NEEDED;
		}
		else
		{
			try (BufferedReader in = new BufferedReader(new InputStreamReader(versionSite.openStream())))
			{
				newestVersion = in.readLine();
				newestVersion = newestVersion.substring(1, newestVersion.length() - 1);
				if (!newestVersion.equals(properties.get(SettingsDialog.VERSION)))
				{
					if (JOptionPane.showConfirmDialog(null, "Inventory is out of date.  Download update?", "Update", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
						return UPDATE_NEEDED;
					else
						return UPDATE_CANCELLED;
				}
				else
					return NO_UPDATE;
			}
			catch (IOException e)
			{
				JOptionPane.showMessageDialog(null, "Error connecting to server: " + e.getMessage() + ".", "Connection Error", JOptionPane.ERROR_MESSAGE);
				return NO_UPDATE;
			}
		}
	}
	
	/**
	 * Download the latest list of cards from the inventory site (default mtgjson.com).  If the
	 * download is taking a while, a progress bar will appear.
	 * 
	 * TODO: If the inventory cannot be found, give the user the option to search for it
	 * 
	 * @return <code>true</code> if the download was successful, and <code>false</code>
	 * otherwise.
	 */
	public boolean updateInventory()
	{
		InventoryDownloadDialog downloadDialog = new InventoryDownloadDialog(this);
		downloadDialog.setLocationRelativeTo(this);
		return downloadDialog.downloadInventory(inventorySite, inventoryFile);
	}
	
	/**
	 * Set program settings back to their default values
	 */
	public void resetDefaultSettings()
	{
		properties = new Properties();
		properties.put(SettingsDialog.VERSION_FILE, "version.json");
		properties.put(SettingsDialog.INVENTORY_SOURCE, "http://mtgjson.com/json/");
		properties.put(SettingsDialog.VERSION, "");
		properties.put(SettingsDialog.INVENTORY_FILE, "AllSets-x.json");
		properties.put(SettingsDialog.INITIALDIR, "true");
		properties.put(SettingsDialog.INVENTORY_LOCATION, "./");
		properties.put(SettingsDialog.INVENTORY_COLUMNS, "Name,Mana Cost,Type,Expansion");
		properties.put(SettingsDialog.INVENTORY_STRIPE, "#FFCCCCCC");
		properties.put(SettingsDialog.INITIALDIR, "./");
		properties.put(SettingsDialog.RECENT_COUNT, "4");
		properties.put(SettingsDialog.RECENT_FILES, "");
		properties.put(SettingsDialog.EDITOR_COLUMNS, "Name,Count,Mana Cost,Type,Expansion,Rarity");
		properties.put(SettingsDialog.EDITOR_STRIPE, "#FFCCCCCC");
		properties.put(SettingsDialog.EDITOR_PRESETS, "Artifacts \u00AB\u00BB \u00AB\u00BB \u00ABAND \u00ABtype:contains any of\"artifact\"\u00BB \u00ABtype:contains none of\"creature\"\u00BB\u00BB\u220ECreatures \u00AB\u00BB \u00AB\u00BB \u00ABAND \u00ABtype:contains any of\"creature\"\u00BB\u00BB\u220ELands \u00AB\u00BB \u00AB\u00BB \u00ABAND \u00ABtype:contains any of\"land\"\u00BB\u00BB\u220EInstants/Sorceries \u00AB\u00BB \u00AB\u00BB \u00ABAND \u00ABtype:contains any of\"instant sorcery\"\u00BB\u00BB");
	}
	
	/**
	 * Apply the settings in the given Properties.
	 * 
	 * @param p Properties containing the settings to apply.
	 */
	public void applySettings(Properties p)
	{
		for (String key: p.stringPropertyNames())
			properties.put(key, p.getProperty(key));
		try
		{
			inventorySite = new URL(properties.getProperty(SettingsDialog.INVENTORY_SOURCE) + properties.getProperty(SettingsDialog.INVENTORY_FILE));
		}
		catch (MalformedURLException e)
		{
			JOptionPane.showMessageDialog(null, "Bad file URL: " + properties.getProperty(SettingsDialog.INVENTORY_SOURCE) + properties.getProperty(SettingsDialog.INVENTORY_FILE), "Warning", JOptionPane.WARNING_MESSAGE);
		}
		inventoryFile = new File(properties.getProperty(SettingsDialog.INVENTORY_LOCATION) + properties.getProperty(SettingsDialog.INVENTORY_FILE));
		recentCount = Integer.valueOf(properties.getProperty(SettingsDialog.RECENT_COUNT));
		// TODO: If recentCount has gotten smaller, update the menu accordingly
		if (properties.getProperty(SettingsDialog.INVENTORY_COLUMNS).isEmpty())
			properties.put(SettingsDialog.INVENTORY_COLUMNS, "Name,Expansion,Mana Cost,Type");
		inventoryModel.setColumns(Arrays.stream(properties.getProperty(SettingsDialog.INVENTORY_COLUMNS).split(",")).map(CardCharacteristic::get).collect(Collectors.toList()));
		inventoryTable.setStripeColor(SettingsDialog.stringToColor(properties.getProperty(SettingsDialog.INVENTORY_STRIPE)));
		if (properties.getProperty(SettingsDialog.EDITOR_COLUMNS).isEmpty())
			properties.put(SettingsDialog.EDITOR_COLUMNS, "Name,Count,Mana Cost,Type,Expansion,Rarity");
		for (EditorFrame frame: editors)
			frame.setSettings(properties);
		presetMenu.removeAll();
		for (String category: properties.getProperty(SettingsDialog.EDITOR_PRESETS).split(CATEGORY_DELIMITER))
		{
			CategoryEditorPanel editor = new CategoryEditorPanel(category);
			JMenuItem categoryItem = new JMenuItem(editor.name());
			categoryItem.addActionListener((e) -> {
				if (selectedFrame != null)
					selectedFrame.addCategory(new CategoryPanel(editor.name(), editor.repr(), editor.filter(), selectedFrame));
			});
			presetMenu.add(categoryItem);
		}
		
		revalidate();
		repaint();
	}
	
	/**
	 * Write the latest values of the settings to the settings file.
	 */
	public void saveSettings()
	{
		try (FileOutputStream out = new FileOutputStream(SettingsDialog.PROPERTIES_FILE))
		{
			// TODO: Write a header comment
			StringJoiner str = new StringJoiner("|");
			for (JMenuItem recent: recentItems)
				str.add(recents.get(recent).getPath());
			properties.put(SettingsDialog.RECENT_FILES, str.toString());
			properties.store(out, "");
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog(null, "Error writing " + SettingsDialog.PROPERTIES_FILE + ": " + e.getMessage() + ".", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * @return The Properties containing the program's settings.
	 */
	public Properties getSettings()
	{
		return properties;
	}
	
	/**
	 * @param name Name of the property to get
	 * @return A String containing the value of the setting with the given name,
	 * or null if there is no such setting.
	 */
	public String getSetting(String name)
	{
		return properties.getProperty(name);
	}
	
	/**
	 * Update the recently-opened files to add the most recently-opened one, and delete
	 * the oldest one if too many are there.
	 * 
	 * @param f File to add to the list
	 */
	public void updateRecents(File f)
	{
		if (!recents.containsValue(f))
		{
			recentsMenu.setEnabled(true);
			if (recentItems.size() >= recentCount)
			{
				JMenuItem eldest = recentItems.poll();
				recents.remove(eldest);
				recentsMenu.remove(eldest);
			}
			JMenuItem mostRecent = new JMenuItem(f.getPath());
			recentItems.offer(mostRecent);
			recents.put(mostRecent, f);
			mostRecent.addActionListener((e) -> open(f));
			recentsMenu.add(mostRecent);
		}
	}
	
	/**
	 * Create a new editor frame.
	 * 
	 * @param name Name of the new frame (also the name of the file)
	 * @see gui.editor.EditorFrame
	 */
	public void newEditor()
	{
		EditorFrame frame = new EditorFrame(++untitled, this);
		frame.setVisible(true);
		editors.add(frame);
		decklistDesktop.add(frame);
		selectFrame(frame);
	}
	
	/**
	 * Open the file chooser to select a file, and if a file was selected,
	 * parse it and initialize a Deck from it.
	 */
	public void open()
	{
		switch (fileChooser.showOpenDialog(this))
		{
		case JFileChooser.APPROVE_OPTION:
			open(fileChooser.getSelectedFile());
			updateRecents(fileChooser.getSelectedFile());
			break;
		case JFileChooser.CANCEL_OPTION:
		case JFileChooser.ERROR_OPTION:
			break;
		default:
			break;
		}
	}
	
	/**
	 * Open the specified file and create an editor for it.
	 * 
	 * @param f File to open.
	 */
	public void open(File f)
	{
		EditorFrame frame = null;
		for (EditorFrame e: editors)
		{
			if (e.file() != null && e.file().equals(f))
			{
				frame = e;
				break;
			}
		}
		if (frame == null)
		{
			frame = new EditorFrame(f, ++untitled, this);
			frame.setVisible(true);
			editors.add(frame);
			decklistDesktop.add(frame);
		}
		properties.put(SettingsDialog.INITIALDIR, fileChooser.getCurrentDirectory().getPath());
		try
		{
			frame.setSelected(true);
		}
		catch (PropertyVetoException e)
		{
			JOptionPane.showMessageDialog(null, "Error creating new editor: " + e.getMessage() + ".", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * Attempt to close the specified frame.
	 * 
	 * @param frame Frame to close
	 * @return <code>true</code> if the frame was closed, and <code>false</code>
	 * otherwise.
	 */
	public boolean close(EditorFrame frame)
	{
		if (!editors.contains(frame) || !frame.close())
			return false;
		else
		{
			editors.remove(frame);
			if (editors.size() > 0)
				selectFrame(editors.get(0));
			else
				selectedFrame = null;
			return true;
		}
	}
	
	/**
	 * Attempts to close all of the open editors.  If any can't be closed for
	 * whatever reason, they will remain open, but the rest will still be closed.
	 * 
	 * @return <code>true</code> if all open editors were successfully closed, and
	 * <code>false</code> otherwise.
	 */
	public boolean closeAll()
	{
		List<EditorFrame> e = new ArrayList<EditorFrame>(editors);
		boolean closedAll = true;
		for (EditorFrame editor: e)
			closedAll &= close(editor);
		return closedAll;
	}
	
	/**
	 * If specified editor frame has a file associated with it, save
	 * it to that file.  Otherwise, open the file dialog and save it
	 * to whatever is chosen (save as).
	 * 
	 * @param frame EditorFrame to save
	 */
	public void save(EditorFrame frame)
	{
		if (!frame.save())
			saveAs(frame);
	}
	
	/**
	 * Save the specified editor frame to a file chosen from a JFileChooser.
	 * 
	 * @param frame Frame to save.
	 */
	public void saveAs(EditorFrame frame)
	{
		// If the file exists, let the user choose whether or not to overwrite.  If he or she chooses not to,
		// ask for a new file.  If he or she cancels at any point, stop asking and don't open a file.
		boolean done;
		do
		{
			done = false;
			switch (fileChooser.showSaveDialog(this))
			{
			case JFileChooser.APPROVE_OPTION:
				File f = fileChooser.getSelectedFile();
				boolean write;
				if (f.exists())
				{
					int option = JOptionPane.showConfirmDialog(null, "File " + f.getName() + " already exists.  Overwrite?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION);
					write = (option == JOptionPane.YES_NO_CANCEL_OPTION);
					done = (option != JOptionPane.NO_OPTION);
				}
				else
				{
					write = true;
					done = true;
				}
				if (write)
					frame.save(f);
				break;
			case JFileChooser.CANCEL_OPTION:
			case JFileChooser.ERROR_OPTION:
				done = true;
				break;
			default:
				break;
			}
		} while (!done);
		properties.put(SettingsDialog.INITIALDIR, fileChooser.getCurrentDirectory().getPath());
	}
	
	/**
	 * Attempt to save all open editors.  For each that needs a file, ask for a file
	 * to save to.
	 */
	public void saveAll()
	{
		for (EditorFrame editor: editors)
			save(editor);
	}
	
	/**
	 * @param id UID of the Card to look for
	 * @return The Card with the given UID.
	 */
	public Card getCard(String id)
	{
		return inventory.get(id);
	}
	
	/**
	 * Update the currently-selected card.
	 * 
	 * @param card Card to select
	 */
	public void selectCard(Card card)
	{
		if (selectedCard == null || !selectedCard.equals(card))
		{
			selectedCard = card;
			oracleTextPane.setText("<html>" + card.toHTMLString() + "</html>");
			oracleTextPane.setCaretPosition(0);
		}
	}
	
	/**
	 * @return The currently-selected Card.
	 */
	public Card getSelectedCard()
	{
		return selectedCard;
	}
	
	/**
	 * @return A List containing each currently-selected card in the inventory table.
	 */
	public List<Card> getSelectedCards()
	{
		return Arrays.stream(inventoryTable.getSelectedRows())
					 .mapToObj((r) -> inventory.get(inventoryTable.convertRowIndexToModel(r)))
					 .collect(Collectors.toList());
	}
	
	/**
	 * Set the currently-active frame.  This is the one that will be operated on
	 * when single-deck actions are taken from the main frame, such as saving
	 * and closing.
	 * 
	 * @param frame EditorFrame to operate on from now on
	 */
	public void selectFrame(EditorFrame frame)
	{
		try
		{
			frame.setSelected(true);
			selectedFrame = frame;
		}
		catch (PropertyVetoException e)
		{}
	}
}
