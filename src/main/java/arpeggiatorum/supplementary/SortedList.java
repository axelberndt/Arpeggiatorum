package arpeggiatorum.supplementary;

import java.util.*;
import java.util.function.Consumer;

/**
 * A sorted list with increasing values and no double entries.
 *
 * @param <E>
 * @author Axel Berndt
 */
public class SortedList<E extends Comparable<E>> implements Iterable<E> {
    private final ArrayList<E> list;    // the actual list

    /**
     * default constructor
     */
    public SortedList() {
        this.list = new ArrayList<>();
    }

    /**
     * constructor with initial capacity
     *
     * @param initialCapacity
     */
    public SortedList(int initialCapacity) {
        this.list = new ArrayList<>(initialCapacity);
    }

    /**
     * insert an item in the list
     *
     * @param item
     * @return
     */
    public boolean add(E item) {
        int index = Collections.binarySearch(this.list, item);  // this works only if the list is sorted in ascending order

        if (index >= 0)                         // item is already in the list
            return false;                       // we cannot have two items of the same value

        this.list.add(-(index + 1), item);      // add item to the list at the determined insertion position
        return true;
    }

    public boolean addAll(Collection<? extends E> collection) {
        boolean success = false;

        for (E item : collection)
            success |= this.add(item);

        return success;
    }

    /**
     * delete an item from the list
     *
     * @param item
     * @return
     */
    public E remove(E item) {
        int index = Collections.binarySearch(this.list, item);  // this works only if the list is sorted in ascending order

        if (index >= 0)                         // found the item
            return this.list.remove(index);     // remove it

        return null;                            // item is not in the list
    }

    /**
     * delete the item at the given index from the list
     *
     * @param index
     * @return
     */
    public E remove(int index) {
        if ((index < 0) || (index >= this.list.size()))
            return null;
        return this.list.remove(index);
    }

    /**
     * Removes all the items from this list. The list will be empty after this call returns.
     */
    public void clear() {
        this.list.clear();
    }

    /**
     * access the item at the specified index
     *
     * @param index
     * @return
     */
    public E get(int index) {
        return this.list.get(index);
    }

    /**
     * get the whole list as ArrayList
     *
     * @return
     */
    public ArrayList<E> toArrayList() {
        return this.list;
    }

    /**
     * get the length of the list
     *
     * @return
     */
    public int size() {
        return this.list.size();
    }

    /**
     * check if the list ist empty
     *
     * @return
     */
    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    /**
     * check if an item is in the list
     *
     * @param item
     * @return
     */
    public boolean contains(E item) {
        int index = Collections.binarySearch(this.list, item);  // this works only if the list is sorted in ascending order
        return (index >= 0);
    }

    /**
     * get index of item
     *
     * @param item
     * @return the index of the search key, if it is contained in the list; otherwise, (-(insertion point) - 1). The insertion point is defined as the point at which the key would be inserted into the list: the index of the first element greater than the key, or list.size() if all elements in the list are less than the specified key. Note that this guarantees that the return value will be >= 0 if and only if the key is found.
     */
    public int indexOf(E item) {
        return Collections.binarySearch(this.list, item);  // this works only if the list is sorted in ascending order
    }

    @Override
    public Iterator<E> iterator() {
        return this.list.iterator();
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        Iterable.super.forEach(action);
    }

    @Override
    public Spliterator<E> spliterator() {
        return Iterable.super.spliterator();
    }
}
