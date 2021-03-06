package editor.collection;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

import editor.collection.category.CategorySpec;
import editor.database.card.Card;
import editor.database.characteristics.CardData;

/**
 * This class represents a collection of Cards.  The collection can choose whether or
 * not each card can be represented by multiple entries (add/remove) or through some
 * other means (increase/decrease).
 * TODO: Correct comments
 * TODO: Clarify why this doesn't extend Collection<Card> (it has to do with contracts and remove)
 * @author Alec Roelke
 */
public interface CardList extends Iterable<Card>
{
	/**
	 * TODO: Comment this
	 * TODO: Make this able to return every characteristic of a card
	 * @author Alec
	 *
	 */
	public interface Entry
	{
		public Card card();
		public Set<CategorySpec> categories();
		public int count();
		public Date dateAdded();
		public default Object get(CardData data)
		{
			switch (data)
			{
			case NAME:
				return card().unifiedName();
			case LAYOUT:
				return card().layout();
			case MANA_COST:
				return card().manaCost();
			case CMC:
				return card().cmc();
			case COLORS:
				return card().colors();
			case COLOR_IDENTITY:
				return card().colorIdentity();
			case TYPE_LINE:
				return card().unifiedTypeLine();
			case EXPANSION_NAME:
				return card().expansion().toString();
			case RARITY:
				return card().rarity();
			case POWER:
				return card().power();
			case TOUGHNESS:
				return card().toughness();
			case LOYALTY:
				return card().loyalty();
			case ARTIST:
				return card().artist()[0];
			case LEGAL_IN:
				return card().legalIn();
			case COUNT:
				return count();
			case CATEGORIES:
				return categories();
			case DATE_ADDED:
				return dateAdded();
			default:
				throw new IllegalArgumentException("Unknown data type " + data);
			}
		}
	}
	
	/**
	 * Add a new Card to this CardCollection (optional operation).  This should
	 * return <code>true</code> if a new entry in the list is created, and
	 * <code>false</code> otherwise.  If a Card can only have one entry, but multiple
	 * copies are allowed, <code>false</code> should still be returned on an attempt
	 * to add another copy of a Card, and its count should not increase.
	 * 
	 * @param c Card to add
	 * @return <code>true</code> if the Card was added, and <code>false</code>
	 * otherwise.
	 * @throws UnsupportedOperationException if this operation is not supported
	 */
	public boolean add(Card c);
	
	/**
	 * TODO: Comment this
	 * @param c
	 * @param n
	 * @return
	 */
	public boolean add(Card c, int n);

	/**
	 * Add several new Cards to this CardCollection (optional operation).
	 * 
	 * @param coll Cards to add
	 * @return <code>true</code> if any of the Cards were successfully added to this
	 * CardCollection, and <code>false</code> otherwise.
	 * @throws UnsupportedOperationException if this operation is not supported
	 * @see CardList#add(Card)
	 */
	public boolean addAll(Collection<? extends Card> coll);
	
	/**
	 * TODO: Comment this
	 * @param coll
	 * @param n
	 * @return
	 */
	public boolean addAll(Collection<? extends Card> coll, Collection<? extends Integer> n);

	/**
	 * Remove all entries from this CardCollection (optional operation).
	 * @throws UnsupportedOperationException if this operation is not supported
	 */
	public void clear();

	/**
	 * @param o Object to look for
	 * @return <code>true</code> if this CardCollection contains the specified object,
	 * and <code>false</code> otherwise.
	 */
	public boolean contains(Object o);

	/**
	 * @param coll Collection of objects to look for
	 * @return <code>true</code> if this CardCollection contains all of the specified
	 * objects, and <code>false</code> otherwise.
	 */
	public boolean containsAll(Collection<?> coll);

	/**
	 * @param index Index of the Card to look for
	 * @return The Card at the given index
	 * @throws IndexOutOfBoundsException if the index is less than 0 or is too big
	 */
	public Card get(int index);

	/**
	 * TODO: Comment this
	 * @param c
	 * @return
	 */
	public Entry getData(Card c);

	/**
	 * TODO: Comment this
	 * @param index
	 * @return
	 */
	public Entry getData(int index);
	
	/**
	 * @param o Object to look for
	 * @return The index of the first occurrence of the given object in this
	 * CardCollection, or -1 if there is none of them.
	 */
	public int indexOf(Object o);

	/**
	 * @return <code>true</code> if this CardCollection contains no Cards, and
	 * <code>false</code> otherwise.
	 */
	public boolean isEmpty();
	
	/**
	 * @return An iterator over the Cards in this CardCollection.
	 */
	@Override
	public Iterator<Card> iterator();

	/**
	 * TODO: Comment this
	 * TODO: Potentially make this default
	 * @return
	 */
	public Stream<Card> parallelStream();

	/**
	 * TODO: Comment this
	 * @param c
	 * @param n
	 * @return
	 */
	public int remove(Card c, int n);

	/**
	 * Remove an object from this CardCollection (optional operation).  If multiple
	 * copies of a Card are represented by a single entry, then an implementation of
	 * this should remove the entire entry.
	 * 
	 * @param o Object to remove
	 * @return <code>true</code> if the object was remove, and <code>false</code>
	 * otherwise.
	 * @throws UnsuportedOperationException if this operation is not supported
	 */
	public boolean remove(Object o);

	/**
	 * TODO: Comment this
	 * @param coll
	 * @param n
	 * @return
	 */
	public boolean removeAll(Collection<? extends Card> coll, Collection<? extends Integer> n);
	
	/**
	 * Remove all of the given objects from this CardCollection (optional
	 * operation).
	 * 
	 * @param coll Collection containing objects to remove
	 * @return <code>true</code> if any of the objects were removed, and
	 * <code>false</code> otherwise
	 * @throws UnsupportedOperationException if this operation is not supported
	 * @see CardList#remove(Object)
	 */
	public boolean removeAll(Collection<?> coll);
	
	/**
	 * Set the number of copies of a Card to the specified number (optional
	 * operation).  If the number is 0, then there should be no entries representing
	 * the Card left.  If there are no entries representing the Card and the number
	 * is greater than 0, then entries should be created.
	 * 
	 * @param c Card to set the count of
	 * @param n Number of copies to set
	 * @return <code>true</code> if this CardCollection changed as a result, and
	 * <code>false</code> otherwise.
	 * @throws UnsupportedOperationException if this operation is not supported
	 */
	public boolean set(Card c, int n);
	
	/**
	 * Set the number of copies of the Card at the specified index (optional
	 * operation).
	 * 
	 * @param index Index of the Card to set the number of
	 * @param n Number of copies of the Card to set
	 * @return <code>true</code> if this CardCollection changed as a result, and
	 * <code>false</code> otherwise.
	 * @throws UnsupportedOperationException if this operation is not supported
	 * @throws IndexOutOfBoundsException if the index is less than 0 or is too big
	 * @see CardList#set(Card, int)
	 */
	public boolean set(int index, int n);
	
	/**
	 * @return The number of entries in this CardCollection.  If multiple copies
	 * of a Card are to be represented by a single entry, then all copies of that
	 * Card count only once for this method.
	 * @see CardList#total()
	 */
	public int size();
	
	/**
	 * TODO: Comment this
	 * TODO: Potentially make this default
	 * @return
	 */
	public Stream<Card> stream();
	
	/**
	 * @return An array containing all of the Cards in this CardCollection.  If
	 * multiple copies of a Card are represented by a single entry, then each entry
	 * should only appear once.
	 */
	public Card[] toArray();
	
	/**
	 * @return The total number of Cards in this CardCollection.  If multiple copies of a
	 * Card are represented as a single entry, this counts each copy separately.  If they
	 * are not, this should return the same value as {@link CardList#size()}.
	 * @see CardList#size()
	 */
	public int total();
}
