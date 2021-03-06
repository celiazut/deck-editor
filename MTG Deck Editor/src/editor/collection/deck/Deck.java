package editor.collection.deck;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import editor.collection.CardList;
import editor.collection.category.CategoryListener;
import editor.collection.category.CategorySpec;
import editor.database.card.Card;

/**
 * This class represents a deck which can have cards added and removed (in quantity) and
 * have several category views (from which cards can also be added or removed).
 * 
 * @author Alec Roelke
 */
public class Deck implements CardList
{
	/**
	 * This class represents a category of a deck.  It looks like a deck since it
	 * contains a list of cards and can report how many copies of them there are, 
	 * so it extends Deck.  If a card is added or removed using the add and remove
	 * methods, the master list will be updated to reflect this only if the card
	 * passes through the Category's filter.
	 * TODO: Throw exceptions when trying to get information about cards that don't pass through the filter
	 * @author Alec Roelke
	 */
	private class Category implements CardList
	{
		/**
		 * Specification for the cards contained in this Category.
		 */
		private CategorySpec spec;
		/**
		 * List representing the filtered view of the master list.
		 */
		private List<Card> filtrate;
		/**
		 * Listener for changes in this category's CategorySpec.
		 */
		public CategoryListener listener;
		/**
		 * Rank of this Category.
		 */
		public int rank;
		
		/**
		 * Create a new Category.
		 * 
		 * @param s Specifications for the new Category
		 */
		private Category(CategorySpec s)
		{
			spec = s;
			rank = categories.size();
			update();
		}
		
		/**
		 * If the given Card belongs to this Category and isn't in the Deck, add it to the
		 * Deck.
		 * 
		 * @param c Card to add
		 * @return <code>true</code> if the Card was added and <code>false</code> otherwise.
		 */
		@Override
		public boolean add(Card c)
		{
			return includes(c) && Deck.this.add(c);
		}
		
		/**
		 * Add some number of copies of a Card to this Category if it passes
		 * through this Category's filter.
		 * 
		 * @param c Card to add
		 * @param n Number of copies to add
		 * @return <code>true</code> if the Deck was changed as a result, and
		 * <code>false</code> otherwise.
		 */
		@Override
		public boolean add(Card c, int n)
		{
			if (includes(c))
				return Deck.this.add(c, n);
			else
				throw new IllegalArgumentException("Category " + spec.getName() + " cannot contain card " + c);
		}
		
		/**
		 * Add all of the given Cards that can belong to this Category and aren't already
		 * in the Deck to the Deck.
		 * 
		 * @param coll Collection of Cards to add
		 * @return <code>true</code> if the Deck changed as a result, and <code>false</code>
		 * otherwise.
		 */
		@Override
		public boolean addAll(Collection<? extends Card> coll)
		{
			return Deck.this.addAll(coll.stream().filter(this::includes).collect(Collectors.toList()));
		}
		
		@Override
		public boolean addAll(Collection<? extends Card> coll, Collection<? extends Integer> n)
		{
			// TODO Auto-generated method stub
			return false;
		}
		
		@Override
		public void clear()
		{
			throw new UnsupportedOperationException();
		}
		
		/**
		 * @param o Object to look for
		 * @return <code>true</code> if the given Object is in this Category, and
		 * <code>false</code> otherwise.
		 */
		@Override
		public boolean contains(Object o)
		{
			return filtrate.contains(o);
		}
		
		/**
		 * @param coll Collection of objects to look for
		 * @return <code>true</code> if this Category contains all of the given objects,
		 * and <code>false</code> otherwise.
		 */
		@Override
		public boolean containsAll(Collection<?> coll)
		{
			for (Object o: coll)
				if (!contains(o))
					return false;
			return true;
		}
		
		/**
		 * @param index Index into this Category's view of the master list to
		 * look at
		 * @return The Card at the given index.
		 */
		@Override
		public Card get(int index)
		{
			return filtrate[index];
		}
		
		@Override
		public Entry getData(Card c)
		{
			if (includes(c))
				return Deck.this.getData(c);
			else
				throw new IllegalArgumentException("Category " + spec.getName() + " does not contain card " + c);
		}
		
		@Override
		public Entry getData(int index)
		{
			return getData(this[index]);
		}
		
		/**
		 * @param c Card to test
		 * @return <code>true</code> if the given Card can belong to this Category and
		 * <code>false</code> otherwise.
		 */
		public boolean includes(Card c)
		{
			return spec.includes(c);
		}
		
		/**
		 * @param o Object to look for
		 * @return The index of that Object in this Category's view of the master
		 * list.
		 */
		@Override
		public int indexOf(Object o)
		{
			return filtrate.indexOf(o);
		}
		
		/**
		 * @return <code>true</code> if there are no cards in this Category, and
		 * <code>false</code> otherwise.
		 */
		@Override
		public boolean isEmpty()
		{
			return size() == 0;
		}
		
		/**
		 * @return An iterator over this Category's Cards.
		 */
		@Override
		public Iterator<Card> iterator()
		{
			return filtrate.iterator();
		}
		
		@Override
		public Stream<Card> parallelStream()
		{
			// TODO Auto-generated method stub
			return null;
		}
		
		/**
		 * Remove some number of copies of a Card from this Category if it passes
		 * through this Category's filter.
		 * 
		 * @param c Card to add
		 * @param n Number of copies to remove
		 * @return The numbe of copies of the Card that were actually removed.
		 */
		@Override
		public int remove(Card c, int n)
		{
			if (includes(c))
				return Deck.this.remove(c, n);
			else
				throw new IllegalArgumentException("Category " + spec.getName() + " does not contain card " + c);
		}

		/**
		 * Remove the given object from the Deck if it is in this Category.
		 * 
		 * @param o Object to remove
		 * @return <code>true</code> if the given object was successfully remove, and
		 * <code>false</code> otherwise.
		 */
		@Override
		public boolean remove(Object o)
		{
			return contains(o) && Deck.this.remove(o);
		}

		@Override
		public boolean removeAll(Collection<? extends Card> coll, Collection<? extends Integer> n)
		{
			// TODO Auto-generated method stub
			return false;
		}

		/**
		 * Remove all objects from the given collection that are in this Category
		 * from the Deck.
		 * 
		 * @param coll Collection of objects to remove
		 * @return <code>true</code> if any of the objects were successfully removed
		 * from the Deck, and <code>false</code> otherwise.
		 */
		@Override
		public boolean removeAll(Collection<?> coll)
		{
			boolean changed = false;
			for (Object o: coll)
				changed |= remove(o);
			return changed;
		}

		/**
		 * Set the number of copies of the given Card to be the given value.  If the card
		 * isn't in the deck, it will be added.  If it isn't included in the category,
		 * then nothing will happen.
		 * 
		 * @param c Card to change
		 * @param n Number of copies to change to
		 * @return <code>true</code> if the number of copies was changed or if the card was
		 * added, and <code>false</code> otherwise.
		 */
		@Override
		public boolean set(Card c, int n)
		{
			return includes(c) & Deck.this.set(c, n);
		}
		
		/**
		 * Set the number of copies of the Card at the given index to be the given value.
		 * 
		 * @param index Index to find the Card at
		 * @param n Number of copies to change to
		 * @return <code>true</code> if the Card is in the Category and if the number of copies
		 * was changed, and <code>false</code> otherwise.
		 */
		@Override
		public boolean set(int index, int n)
		{
			return this[index] != null && set(this[index], n);
		}
		
		/**
		 * @return The number of unique Cards in this Category.
		 */
		@Override
		public int size()
		{
			return filtrate.size();
		}

		/**
		 * @return This Category's specifications.
		 */
		public CategorySpec spec()
		{
			return spec;
		}

		@Override
		public Spliterator<Card> spliterator()
		{
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * TODO: Override spliterator instead of this
		 * @return A sequential Stream whose source is this Category.
		 */
		@Override
		public Stream<Card> stream()
		{
			return filtrate.stream();
		}

		/**
		 * @return An array containing all of the Cards in this Category.
		 */
		@Override
		public Card[] toArray()
		{
			return filtrate.toArray(new Card[filtrate.size()]);
		}

		/**
		 * @return This Category's String representation.
		 * @see editor.gui.filter.original.editor.FilterEditorPanel#setContents(String)
		 * @see gui.editor.CategoryDialog#setContents(String)
		 */
		@Override
		public String toString()
		{
			return spec.toString();
		}

		/**
		 * @return the total number of Cards in this Category.
		 */
		@Override
		public int total()
		{
			return filtrate.stream().map(Deck.this::getEntry).mapToInt((e) -> e == null ? 0 : e.count).sum();
		}

		/**
		 * Update this category so its filtrate reflects the new filter,
		 * whitelist, and blacklist.
		 */
		public void update()
		{
			filtrate = masterList.stream().map((e) -> e.card).filter(spec::includes).collect(Collectors.toList());
			for (DeckEntry e: masterList)
				if (spec.includes(e.card))
					e.categories.add(this);
				else
					e.categories.remove(this);
		}
	}
	
	/**
	 * This class represents an entry into a deck.  It has a card and a
	 * number of copies.
	 * TODO: Correct comments
	 * @author Alec Roelke
	 */
	private class DeckEntry implements Entry
	{
		/**
		 * Card in this Entry.  It can't be changed.
		 */
		private final Card card;
		/**
		 * Number of copies of the Card.
		 */
		private int count;
		/**
		 * Date this Entry was created (the Card was originally added).
		 */
		private final Date date;
		/**
		 * Set of categories this Entry's Card belongs to.  Implemented using a
		 * LinkedHashSet, so it will maintain the ordering that categories were
		 * added.
		 */
		private Set<Category> categories;
		
		/**
		 * Create a new Entry.
		 * 
		 * @param c Card for this Entry
		 * @param n Number of initial copies in this Entry
		 * @param d Date the Card was added
		 */
		private DeckEntry(Card c, int n, Date d)
		{
			card = c;
			count = n;
			date = d;
			categories = new LinkedHashSet<Category>();
		}
		
		/**
		 * Copy constructor for Entry.
		 * 
		 * @param e Original Entry to copy
		 */
		private DeckEntry(DeckEntry e)
		{
			card = e.card;
			count = e.count;
			date = e.date;
			categories = new LinkedHashSet<Category>(e.categories);
		}
		
		/**
		 * Add copies to this Entry.
		 * 
		 * @param n Copies to add
		 * @return The new number of copies in this Entry.
		 */
		private int add(int n)
		{
			return count += n;
		}
		
		@Override
		public Card card()
		{
			return card;
		}
		
		public Set<CategorySpec> categories()
		{
			return categories.stream().map(Category::spec).collect(Collectors.toSet());
		}
		
		public int count()
		{
			return count;
		}
		
		public Date dateAdded()
		{
			return date;
		}

		/**
		 * Remove copies from this Entry.  There can't be fewer than
		 * 0 copies.
		 * 
		 * @param n Number of copies to remove.
		 * @return The new number of copies in this Entry.
		 */
		private int remove(int n)
		{
			if (n > count)
				return count = 0;
			else
				return count -= n;
		}
	}
	/**
	 * This class represents an event during which a Deck may have changed.
	 * It can indicate how many copies of Cards may have been added to or
	 * removed from the Deck, how a category may have changed, or if any
	 * categories were removed.  If a parameter did not change and the contents
	 * of that parameter's change are requested, throw an IllegalStateException.
	 * 
	 * @author Alec Roelke
	 */
	@SuppressWarnings("serial")
	public class Event extends EventObject
	{
		/**
		 * If Cards were added to or removed from the Deck, this map
		 * contains which ones and how many copies.
		 */
		private Map<Card, Integer> cardsChanged;
		/**
		 * If a category's name was changed, its old name.
		 */
		private String changedName;
		/**
		 * CategoryEvent representing the changes to the CategorySpec corresponding
		 * to the category that was changed, if any was changed.
		 */
		private CategorySpec.Event categoryChanges;
		/**
		 * If a category was added to the deck, its name.
		 */
		private String addedCategory;
		/**
		 * Set of names of categories that have been removed, if any.
		 */
		private Set<String> removedCategories;
		/**
		 * Map of categories onto their old ranks, if they were changed.
		 */
		private Map<String, Integer> rankChanges;
		
		public Event()
		{
			super(Deck.this);
			cardsChanged = null;
			changedName = null;
			categoryChanges = null;
			addedCategory = null;
			removedCategories = null;
			rankChanges = null;
		}
		
		/**
		 * @return The name of the category that was added.
		 * @throws IllegalStateException If no category was added.
		 */
		public String addedName()
		{
			if (categoryAdded())
				return addedCategory;
			else
				throw new IllegalStateException("No category has been added to the deck");
		}
		
		/**
		 * @return A map containing the Cards that were added and the number of copies
		 * that were added.
		 * @throws IllegalStateException If no cards were added or removed during the
		 * event.
		 */
		public Map<Card, Integer> cardsAdded()
		{
			if (cardsChanged())
			{
				Map<Card, Integer> cards = new HashMap<Card, Integer>(cardsChanged);
				for (Card c: cardsChanged.keySet())
					if (cardsChanged[c].intValue() < 1)
						cards.remove(c);
				return cards;
			}
			else
				throw new IllegalStateException("Deck cards were not changed");
		}
		
		/**
		 * @return <code>true</code> if Cards were added to or removed from the Deck
		 * during the event.
		 */
		public boolean cardsChanged()
		{
			return cardsChanged != null;
		}
		
		/**
		 * Indicate that cards and/or counts of cards in the deck changed.
		 * 
		 * @param change Map of Cards onto their count changes
		 * @return The Event representing the change.
		 */
		private Event cardsChanged(Map<Card, Integer> change)
		{
			cardsChanged = change;
			return this;
		}
		
		/**
		 * @return A map of cards that were removed and the number of copies that
		 * were removed.  Positive numbers are used to indicate removed cards.
		 * @throws IllegalStateException If no cards were added or removed during
		 * the event.
		 */
		public Map<Card, Integer> cardsRemoved()
		{
			if (cardsChanged())
			{
				Map<Card, Integer> cards = new HashMap<Card, Integer>(cardsChanged);
				for (Card c: cardsChanged.keySet())
					if (cardsChanged[c].intValue() > -1)
						cards.remove(c);
					else
						cardsChanged.compute(c, (k, v) -> -v);
				return cards;
			}
			else
				throw new IllegalStateException("Deck cards were not changed");
		}
		
		/**
		 * @return <code>true</code> if any categories were removed during the
		 * event, and <code>false</code> otherwise.
		 */
		public boolean categoriesRemoved()
		{
			return removedCategories != null;
		}
		
		/**
		 * Indicate that categories were removed from the deck.
		 * 
		 * @param removed Collection of categories that were removed
		 * @return The Event representing the change.
		 */
		private Event categoriesRemoved(Collection<Category> removed)
		{
			removedCategories = new HashSet<String>(removed.stream().map((c) -> c.spec.getName()).collect(Collectors.toSet()));
			return this;
		}
		
		/**
		 * @return <code>true</code> if a category was added to the deck, and
		 * <code>false</code>.
		 */
		public boolean categoryAdded()
		{
			return addedCategory != null;
		}
		
		/**
		 * Indicate that a category was added to the deck.
		 * 
		 * @param added Category that was added
		 * @return The Event representing the change.
		 */
		private Event categoryAdded(Category added)
		{
			addedCategory = added.spec.getName();
			return this;
		}
		
		/**
		 * @return <code>true</code> if a category in the Deck was changed, and
		 * <code>false</code> otherwise.
		 */
		public boolean categoryChanged()
		{
			return categoryChanges != null;
		}
		
		/**
		 * Indicate that a category was changed.
		 * 
		 * @param changeName Name of the category that was changed
		 * @param changes CategorySpec.Event indicating changes to the category
		 * @return The Event representing the change.
		 */
		private Event categoryChanged(String changeName, CategorySpec.Event changes)
		{
			changedName = changeName;
			categoryChanges = changes;
			return this;
		}
		
		/**
		 * @return A CategoryEvent detailing the changes to the category.
		 * @throws IllegalStateException if no category was changed during
		 * the event.
		 */
		public CategorySpec.Event categoryChanges()
		{
			if (categoryChanged())
				return categoryChanges;
			else
				throw new IllegalStateException("Category was not changed");
		}
		
		/**
		 * @return The name of the category that was changed before the event.
		 * Use this rather than the CategoryEvent returned by {@link categoryChanges()}
		 * to identify which category was changed if its name was not changed.
		 * @throws IllegalStateException If no category was changed during the
		 * event.
		 */
		public String categoryName()
		{
			if (categoryChanged())
				return changedName;
			else
				throw new IllegalStateException("Category was not changed");
		}
		
		/**
		 * Indicate that a category was removed from the deck.
		 * 
		 * @param removed Category that was removed
		 * @return The Event representing the change.
		 */
		private Event categoryRemoved(Category removed)
		{
			removedCategories = new HashSet<String>(Arrays.asList(removed.spec.getName()));
			return this;
		}
		
		/**
		 * @return The Deck that changed to create this DeckEvent.
		 */
		@Override
		public Deck getSource()
		{
			return Deck.this;
		}
		
		/**
		 * @return A map of category names onto their old ranks before they were
		 * changed.
		 * @throws IllegalStateException If no category ranks were changed.
		 */
		public Map<String, Integer> oldRanks()
		{
			if (ranksChanged())
				return rankChanges;
			else
				throw new IllegalStateException("No category's rank changed");
		}
		
		/**
		 * @return <code>true</code> if the ranks of any categories were changed, and
		 * <code>false</code> otherwise.
		 */
		public boolean ranksChanged()
		{
			return rankChanges != null;
		}
		
		/**
		 * Indicate that the ranks of categories were changed.
		 * 
		 * @param changed Map of category names onto their old ranks
		 * @return The Event representing the change.
		 */
		private Event ranksChanged(Map<String, Integer> changed)
		{
			rankChanges = changed;
			return this;
		}
		
		/**
		 * @return The set of names of the categories that were removed during the
		 * event.
		 * @throws IllegalStateException If no categories were removed during the
		 * event.
		 */
		public Set<String> removedNames()
		{
			if (categoriesRemoved())
				return removedCategories;
			else
				throw new IllegalStateException("No category has been removed from the deck");
		}
	}
	
	/**
	 * This class represents data being transferred via drag and drop or cut/copy/paste
	 * between this Deck and another object.  The Deck only supports importing Card or Entry
	 * data flavors, but can export Strings as well.
	 * 
	 * @author Alec Roelke
	 */
	public static class TransferData implements Transferable
	{
		/**
		 * Entries being exported.
		 */
		private Map<Card, Integer> transferData;
		
		/**
		 * Create a new TransferData containing the given Deck's entries for the given
		 * Card.
		 * 
		 * @param d Deck to get entries from
		 * @param cards Cards to find entries for
		 */
		public TransferData(Deck d, Card... cards)
		{
			transferData = Arrays.stream(cards).collect(Collectors.toMap(Function.identity(), (c) -> d.getEntry(c).count()));
		}
		
		/**
		 * Create a new TransferData containing the given Deck's entries for the given
		 * Card.
		 * 
		 * @param d Deck to get entries from
		 * @param cards Cards to find entries for
		 */
		public TransferData(Deck d, Collection<Card> cards)
		{
			this(d, cards.stream().toArray(Card[]::new));
		}
		
		/**
		 * @param flavor Flavor of data to retrieve
		 * @return Data of the given flavor corresponding to the Entries retrieved by this
		 * TransferData.
		 * @throws UnsupportedFlavorException If the given flavor is unsupported
		 */
		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
		{
			if (flavor.equals(entryFlavor))
				return transferData;
			else if (flavor.equals(Card.cardFlavor))
				return transferData.keySet().stream().sorted(Card::compareName).toArray(Card[]::new);
			else if (flavor.equals(DataFlavor.stringFlavor))
				return transferData.entrySet().stream().map((e) -> e.getValue() + "x " + e.getKey().unifiedName()).reduce("", (a, b) -> a + "\n" + b);
			else
				throw new UnsupportedFlavorException(flavor);
		}

		/**
		 * @return An Array containing the data flavors a Deck can support.  A Deck can
		 * only support Entry, Card, and String flavors.
		 */
		@Override
		public DataFlavor[] getTransferDataFlavors()
		{
			return new DataFlavor[] {entryFlavor, Card.cardFlavor, DataFlavor.stringFlavor};
		}

		/**
		 * @param flavor DataFlavor to check
		 * @return <code>true</code> if a Deck can support the given data flavor, and
		 * <code>false</code> otherwise.
		 */
		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor)
		{
			return Arrays.asList(getTransferDataFlavors()).contains(flavor);
		}
	}
	
	/**
	 * Data flavor representing entries in a deck.  Transfer data will appear as a
	 * map of cards onto an integer representing the number of copies to transfer.
	 */
	public static final DataFlavor entryFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" + Map.class.getName() + "\"", "Deck Entries");
	/**
	 * Formatter for dates, usually for formatting the add date of a card.
	 */
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM d, yyyy");
	/**
	 * List of cards in this Deck.
	 */
	private List<DeckEntry> masterList;
	/**
	 * Categories in this Deck.
	 */
	private Map<String, Category> categories;
	/**
	 * Total number of cards in this Deck, accounting for multiples.
	 */
	private int total;
	
	/**
	 * Number of land cards in this Deck, accounting for multiples.
	 */
	private int land;
	
	/**
	 * List of listeners for changes in this Deck.
	 */
	private Collection<DeckListener> listeners;
	
	/**
	 * Create a new, empty Deck with no categories.
	 */
	public Deck()
	{
		masterList = new ArrayList<DeckEntry>();
		categories = new LinkedHashMap<String, Category>();
		total = 0;
		land = 0;
		listeners = new HashSet<DeckListener>();
	}
	
	/**
	 * Create a new Deck with the given cards. There will be no
	 * categories.
	 * 
	 * @param cards Cards to add to the new Deck.
	 */
	public Deck(Collection<Card> cards)
	{
		this();
		for (Card c: cards)
			add(c, 1);
	}
	
	/**
	 * If the given Card isn't in this Deck, add a copy of it to the Deck.
	 * 
	 * @param c Card to add
	 * @return <code>true</code> if the card wasn't in the deck and was successfully added,
	 * and <code>false</code> otherwise.
	 */
	@Override
	public boolean add(Card c)
	{
		return !contains(c) && increase(c);
	}
	
	/**
	 * Add some number of Cards to this Deck.  If the number is not positive,
	 * then no changes are made.
	 * 
	 * @param c Card to add
	 * @param n Number of copies to add
	 * @return <code>true</code> if the Deck changed as a result, and
	 * <code>false</code> otherwise, which is when the number to add
	 * is less than 1.
	 */
	@Override
	public boolean add(Card c, int n)
	{
		return add(c, n, new Date());
	}
	
	/**
	 * Add some number of Cards to this Deck.  If the number is not positive,
	 * then no changes are made.
	 * 
	 * @param c Card to add
	 * @param n Number of copies to add
	 * @param d Date the card was originally added
	 * @return <code>true</code> if the Deck changed as a result, and
	 * <code>false</code> otherwise, which is when the number to add
	 * is less than 1.
	 */
	public boolean add(Card c, int n, Date d)
	{
		if (n < 1)
			return false;
		else
		{
			DeckEntry e = getEntry(c);
			if (e == null)
			{
				masterList.add(e = new DeckEntry(c, n, d));
				for (Category category: categories.values())
				{
					if (category.includes(c))
					{
						category.filtrate.add(c);
						e.categories.add(category);
					}
				}
			}
			else
				e.add(n);
			total += n;
			if (c.typeContains("land"))
				land += n;
			
			Map<Card, Integer> added = new HashMap<Card, Integer>();
			added[c] = n;
			Event event = new Event().cardsChanged(added);
			for (DeckListener listener: listeners)
				listener.deckChanged(event);
			
			return true;
		}
	}
	
	/**
	 * Add each of the given collection of Cards that aren't already in the Deck to the Deck.
	 * 
	 * @param coll Collection of Cards to add.
	 * @return <code>true</code> if any of the Cards were successfully added, and
	 * <code>false</code> otherwise.
	 */
	@Override
	public boolean addAll(Collection<? extends Card> coll)
	{
		boolean changed = false;
		for (Card c: coll)
			changed |= add(c);
		return changed;
	}
	
	@Override
	public boolean addAll(Collection<? extends Card> coll, Collection<? extends Integer> n)
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	/**
	 * Add all of the cards in the given Deck to this Deck.
	 * 
	 * @param d Deck to copy
	 * @return <code>true</code> if cards were successfully added, and <code>false</code>
	 * otherwise.
	 */
	public boolean addAll(Deck d)
	{
		boolean changed = false;
		for (DeckEntry e: d.masterList)
			changed |= add(e.card, e.count);
		return changed;
	}
	
	/**
	 * Add a new Category.  If there is already a Category with the same name,
	 * instead do nothing.
	 * 
	 * @param spec Specifications for the new Category
	 * if there already was one with that name.
	 */
	public CardList addCategory(CategorySpec spec)
	{
		if (!categories.containsKey(spec.getName()))
		{
			Category c = new Category(spec);
			categories[spec.getName()] = c;
			c.update();
			
			spec.addCategoryListener(c.listener = (e) -> {
				if (e.nameChanged())
				{
					categories.remove(e.oldName());
					categories[e.newName()] = c;
				}
				if (e.filterChanged() || e.whitelistChanged() || e.blacklistChanged())
					c.update();
				
				Event event = new Event().categoryChanged(e.nameChanged() ? e.oldName() : e.getSource().getName(), e);
				for (DeckListener listener: new HashSet<DeckListener>(listeners))
					listener.deckChanged(event);
			});

			Event event = new Event().categoryAdded(c);
			for (DeckListener listener: new HashSet<DeckListener>(listeners))
				listener.deckChanged(event);
			
			return c;
		}
		else
			return categories[spec.getName()];
	}
	
	/**
	 * Add a new listener for listening to changes in this Deck.
	 * 
	 * @param listener Listener to add.
	 */
	public void addDeckListener(DeckListener listener)
	{
		listeners.add(listener);
	}
	
	/**
	 * @return A Collection containing all of the specifications of the categories
	 * in this Deck, in no particular order.
	 */
	public Collection<CategorySpec> categories()
	{
		return categories.values().stream().map(Category::spec).collect(Collectors.toList());
	}
	
	/**
	 * Reset this Deck to being empty and having no categories.
	 */
	@Override
	public void clear()
	{
		Map<Card, Integer> removed = masterList.stream().collect(Collectors.toMap((c) -> c.card, (c) -> -c.count));
		Collection<Category> categoriesRemoved = categories.values();
		
		masterList.clear();
		categories.clear();
		total = 0;
		land = 0;
		
		Event event = new Event().cardsChanged(removed).categoriesRemoved(categoriesRemoved);
		for (DeckListener listener: new HashSet<DeckListener>(listeners))
			listener.deckChanged(event);
	}
	
	/**
	 * @param o Object to look for
	 * @return <code>true</code> if this Deck contains one or more copies
	 * of the given Object, and <code>false</code> otherwise.
	 */
	@Override
	public boolean contains(Object o)
	{
		return o instanceof Card && getEntry((Card)o) != null;
	}
	
	/**
	 * @param name Name of the category to look in
	 * @param o Card to look for in the category
	 * @return <code>true</code> if the given Card is in the category,
	 * and <code>false</code> otherwise.
	 */
	public boolean contains(String name, Object o)
	{
		return o instanceof Card && categories[name].contains(o);
	}
	
	/**
	 * @param coll Collection of objects to look for
	 * @return <code>true</code> if all of the objects in the given collection are present
	 * in this Deck, and <code>false</code> otherwise.
	 */
	@Override
	public boolean containsAll(Collection<?> coll)
	{
		for (Object o: coll)
			if (!contains(o))
				return false;
		return true;
	}
	
	/**
	 * @param name Name of the Category to look for
	 * @return <code>true</code> if this Deck has a Category with the given
	 * name, and <code>false</code> otherwise.
	 */
	public boolean containsCategory(String name)
	{
		return categories.containsKey(name);
	}
	
	/**
	 * Remove one copy of the given Card from this Deck.
	 * 
	 * @param c Card to remove
	 * @return 0 if no copies were removed, and 1 if a copy was removed.
	 */
	public int decrease(Card c)
	{
		return remove(c, 1);
	}
	
	/**
	 * Exclude a Card from a category, even if it passes through its
	 * filter.
	 * 
	 * @param name Name of the category to exclude from
	 * @param c Card to exclude
	 * @return <code>true</code> if the Card was successfully excluded
	 * from the category, and <code>false</code> otherwise.
	 */
	public boolean exclude(String name, Card c)
	{
		return contains(c) && categories[name].spec.exclude(c);
	}
	
	/**
	 * @param index Index to look at in the list
	 * @return The Card at the given index.
	 */
	@Override
	public Card get(int index)
	{
		return masterList[index].card;
	}
	
	/**
	 * Get the Card at the given index in the category.
	 * 
	 * @param name Category to look for a card in
	 * @param index Index to find the Card at
	 * @return The Card at the given index.
	 */
	public Card get(String name, int index)
	{
		return categories[name][index];
	}
	
	/**
	 * @param name Name of the category to get Cards from
	 * @return The CardCollection containing Cards in the category with the
	 * given name.
	 */
	public CardList getCategoryCards(String name)
	{
		return categories[name];
	}
	
	/**
	 * @param name Name of the category to search for
	 * @return The user-defined rank of the given category, or -1 if no
	 * category with the given name exists.
	 */
	public int getCategoryRank(String name)
	{
		return containsCategory(name) ? categories[name].rank : -1;
	}
	
	/**
	 * @param name Name of the category whose CategorySpec is desired
	 * @return The CategorySpec of the category with the given name.
	 */
	public CategorySpec getCategorySpec(String name)
	{
		if (categories[name] != null)
			return categories[name].spec;
		else
			throw new IllegalArgumentException("No category named " + name + " found");
	}
	
	/**
	 * TODO: Comment this
	 */
	@Override
	public Entry getData(Card c)
	{
		return getEntry(c);
	}
	
	/**
	 * TODO: Comment this
	 */
	@Override
	public Entry getData(int index)
	{
		return masterList[index];
	}
	
	/**
	 * @param c Card to search for an Entry.
	 * @return The Entry corresponding to the Card, or <code>null</code>
	 * if there is none.
	 */
	private DeckEntry getEntry(Card c)
	{
		for (DeckEntry e: masterList)
			if (e.card.equals(c))
				return e;
		return null;
	}
	
	/**
	 * Include a Card in a category, even if it doesn't pass through
	 * the category's filter.
	 * 
	 * @param name Name of the category to include a Card in
	 * @param c Card to include in the category
	 * @return <code>true</code> if the Card was successfully included,
	 * and <code>false</code> otherwise.
	 */
	public boolean include(String name, Card c)
	{
		return contains(c) && categories[name].spec.include(c);
	}
	
	/**
	 * Add a single copy of a Card to this Deck.
	 * 
	 * @param c Card to add
	 * @return <code>true</code>, since the Deck will always change as
	 * a result.
	 */
	public boolean increase(Card c)
	{
		return add(c, 1);
	}
	
	/**
	 * Add some number of copies of a collection of Cards to this Deck.  If
	 * the number is not positive, then no changes are made.
	 * 
	 * @param coll Collection of Cards to add
	 * @param n Number of copies of each card to add
	 * @return <code>true</code> if the Deck was changed as a result, and
	 * <code>false</code> otherwise.
	 */
	public boolean increaseAll(Collection<? extends Card> coll, int n)
	{
		boolean changed = false;
		for (Card c: coll)
			changed |= add(c, n);
		return changed;
	}
	
	/**
	 * @param o Object to look for
	 * @return Index of that Object in the master list.
	 */
	@Override
	public int indexOf(Object o)
	{
		return o instanceof Card ? masterList.indexOf(getEntry((Card)o)) : -1;
	}
	
	/**
	 * @return <code>true</code> if there are no cards in this Deck, and
	 * <code>false</code> otherwise.
	 */
	@Override
	public boolean isEmpty()
	{
		return size() == 0;
	}
	
	/**
	 * @return An Iterator over the list of Cards in this Deck.
	 */
	@Override
	public Iterator<Card> iterator()
	{
		return stream().iterator();
	}
	
	/**
	 * @return The number of land cards in this Deck.
	 */
	public int land()
	{
		return land;
	}
	
	/**
	 * @return The number of nonland cards in this Deck.
	 */
	public int nonland()
	{
		return total - land;
	}
	
	/**
	 * @return The number of categories in this Deck
	 */
	public int numCategories()
	{
		return categories.size();
	}
	
	@Override
	public Stream<Card> parallelStream()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Remove some number of copies of the given Card from this Deck.  If that
	 * number is less than one, no changes are made.
	 * 
	 * @param c Card to remove
	 * @param n Number of copies to remove
	 * @return The number of copies of the Card that were actually removed.
	 */
	@Override
	public int remove(Card c, int n)
	{
		if (n < 1)
			return 0;
		else
		{
			DeckEntry e = getEntry(c);
			if (e == null)
				return 0;
			else
			{
				if (n > e.count)
					n = e.count;
				e.remove(n);
				if (e.count == 0)
				{
					for (Category category: categories.values())
					{
						category.filtrate.remove(c);
						if (category.spec.getWhitelist().contains(c))
							category.spec.exclude(c);
						if (category.spec.getBlacklist().contains(c))
							category.spec.include(c);
					}
					masterList.remove(e);
				}
				total -= n;
				if (c.typeContains("land"))
					land -= n;
				
				Map<Card, Integer> removed = new HashMap<Card, Integer>();
				removed[c] = -n;
				Event event = new Event().cardsChanged(removed);
				for (DeckListener listener: listeners)
					listener.deckChanged(event);
				
				return n;
			}
		}
	}

	/**
	 * @param o Object to remove
	 * @return <code>true</code> if the object is a Card and if one or more copies were
	 * removed, and <code>false</code> otherwise.
	 */
	@Override
	public boolean remove(Object o)
	{
		return o instanceof Card && remove((Card)o, Integer.MAX_VALUE) > 0;
	}
	
	@Override
	public boolean removeAll(Collection<? extends Card> coll, Collection<? extends Integer> n)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Remove as many of the objects in the given list from this Deck as possible.
	 * 
	 * @param coll Collection of objects to remove
	 * @return <code>true</code> if any of the given objects were remove, and
	 * <code>false</code> otherwise.
	 */
	@Override
	public boolean removeAll(Collection<?> coll)
	{
		boolean changed = false;
		for (Object o: coll)
			changed |= remove(o);
		return changed;
	}

	/**
	 * Remove a Category from this Deck.
	 * 
	 * @param name Name of the Category to remove.
	 * @return <code>true</code> if the deck changed as a result, and
	 * <code>false</code> otherwise.
	 */
	public boolean removeCategory(String name)
	{
		Category c = categories[name];
		if (c != null)
		{
			for (DeckEntry e: masterList)
				e.categories.remove(c);
			Map<String, Integer> oldRanks = new HashMap<String, Integer>();
			for (Category category: categories.values())
			{
				if (category.rank > c.rank)
				{
					oldRanks[category.spec.getName()] = category.rank;
					category.rank--;
				}
			}
			categories.remove(name);
			c.spec.removeCategoryListener(c.listener);
			
			Event event = new Event().categoryRemoved(c).ranksChanged(oldRanks);
			for (DeckListener listener: new HashSet<DeckListener>(listeners))
				listener.deckChanged(event);
			
			return true;
		}
		else
			return false;
	}

	/**
	 * Remove a listener so it no longer receives alerts to changes in this
	 * Deck.
	 * 
	 * @param listener Listener to remove
	 * @return <code>true</code> if the given listener was successfully
	 * removed, and <code>false</code> otherwise.
	 */
	public boolean removeDeckListener(DeckListener listener)
	{
		return listeners.remove(listener);
	}

	/**
	 * Write this Deck to a file.  The format will appear like this:
	 * [Number of unique cards]
	 * [Card 1 UID]\t[count]
	 * [Card 2 UID]\t[count]
	 * 		.			.
	 * 		.			.
	 * 		.			.
	 * [Number of categories]
	 * [Category 1 String representation]
	 * [Category 2 String representation]
	 * 				  .
	 * 				  .
	 * 				  .
	 * 
	 * @param file File to save to
	 * @throws IOException
	 */
	public void save(File file) throws IOException
	{
		try (PrintWriter wr = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8")))
		{
			wr.println(String.valueOf(size()));
			for (DeckEntry e: masterList)
				wr.println(e.card.id() + "\t" + e.count + "\t" + DATE_FORMAT.format(e.date));
			wr.println(String.valueOf(categories.size()));
			for (Category c: categories.values())
				wr.println(c.toString());
		} 
	}
	
	/**
	 * Set the number of copies of the given Card to be the given value.  If the card
	 * isn't in the deck, it will be added.
	 * 
	 * @param c Card to change
	 * @param n Number of copies to change to
	 * @return <code>true</code> if the number of copies was changed or if the card was
	 * added, and <code>false</code> otherwise.
	 */
	@Override
	public boolean set(Card c, int n)
	{
		if (n < 0)
			n = 0;
		DeckEntry e = getEntry(c);
		if (e == null)
			return add(c, n);
		else if (e.count == n)
			return false;
		else
		{
			total += n - e.count;
			if (e.card.typeContains("land"))
				land += n - e.count;
			
			Map<Card, Integer> change = new HashMap<Card, Integer>();
			change[c] = n - e.count;
			Event event = new Event().cardsChanged(change);
			
			e.count = n;
			if (e.count == 0)
			{
				masterList.remove(e);
				for (Category category: categories.values())
				{
					category.filtrate.remove(e.card);
					category.spec.getWhitelist().remove(e.card);
					category.spec.getBlacklist().remove(e.card);
				}
			}
			
			for (DeckListener listener: listeners)
				listener.deckChanged(event);
			
			return true;
		}
	}
	
	/**
	 * Set the number of copies of the Card at the given index to be the given value.
	 * 
	 * @param index Index to find the Card at
	 * @param n Number of copies to change to
	 * @return <code>true</code> if the Card is in the Deck and if the number of copies
	 * was changed, and <code>false</code> otherwise.
	 */
	@Override
	public boolean set(int index, int n)
	{
		return set(masterList[index].card, n);
	}
	
	/**
	 * @return The number of unique Cards in this Deck.
	 */
	@Override
	public int size()
	{
		return masterList.size();
	}
	
	/**
	 * @param name Name of the desired category.
	 * @return The number of unique Cards in the desired category.
	 */
	public int size(String name)
	{
		return categories[name].size();
	}
	
	@Override
	public Spliterator<Card> spliterator()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * @return A sequential Stream whose source is this Deck.
	 */
	@Override
	public Stream<Card> stream()
	{
		return masterList.stream().map((e) -> e.card);
	}

	/**
	 * Change the rank of the category with the given name to the target value.  The
	 * category that has that value will have its rank changed to that of the one with
	 * the given name.
	 * 
	 * @param name Name of the category whose rank should be changed
	 * @param target New rank for the category
	 * @return <code>true</code> if ranks were successfully changed, and <code>false</code>
	 * otherwise (such as if the named category doesn't exist, the target rank is too
	 * high, the target rank is negative, or the target rank is the named category's rank).
	 */
	public boolean swapCategoryRanks(String name, int target)
	{
		if (!categories.containsKey(name) || categories[name].rank == target
				|| target >= categories.size() || target < 0)
			return false;
		else
		{
			for (Category second: categories.values())
			{
				if (second.rank == target)
				{
					Map<String, Integer> oldRanks = new HashMap<String, Integer>();
					oldRanks[name] = categories[name].rank;
					oldRanks[second.spec.getName()] = second.rank;
					
					second.rank = categories[name].rank;
					categories[name].rank = target;
					
					Event event = new Event().ranksChanged(oldRanks);
					for (DeckListener listener: listeners)
						listener.deckChanged(event);
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * @return An array containin all of the Cards in this Deck.
	 */
	@Override
	public Card[] toArray()
	{
		return stream().toArray(Card[]::new);
	}

	/**
	 * @return The number of Cards in this Deck.
	 */
	@Override
	public int total()
	{
		return total;
	}

	/**
	 * @param name Name of the desired category.
	 * @return The total number of cards, including copies, in the
	 * category.
	 */
	public int total(String name)
	{
		return containsCategory(name) ? categories[name].total() : 0;
	}
}
