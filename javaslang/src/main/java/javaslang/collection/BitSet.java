/*     / \____  _    _  ____   ______  / \ ____  __    _______
 *    /  /    \/ \  / \/    \ /  /\__\/  //    \/  \  //  /\__\   JΛVΛSLΛNG
 *  _/  /  /\  \  \/  /  /\  \\__\\  \  //  /\  \ /\\/ \ /__\ \   Copyright 2014-2016 Javaslang, http://javaslang.io
 * /___/\_/  \_/\____/\_/  \_/\__\/__/\__\_/  \_//  \__/\_____/   Licensed under the Apache License, Version 2.0
 */
package javaslang.collection;

import javaslang.Function1;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.Tuple3;
import javaslang.control.Option;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.*;
import java.util.stream.Collector;

import static javaslang.collection.Comparators.naturalComparator;

/**
 * An immutable {@code BitSet} implementation.
 *
 * @author Ruslan Sennov
 * @since 2.1.0
 */
public interface BitSet<T> extends SortedSet<T> {

    long serialVersionUID = 1L;

    class Builder<T> implements Serializable {

        private static final long serialVersionUID = 1L;

        final static Builder<Integer> DEFAULT = new Builder<>(i -> i, i -> i);

        final Function1<Integer, T> fromInt;
        final Function1<T, Integer> toInt;

        Builder(Function1<Integer, T> fromInt, Function1<T, Integer> toInt) {
            this.fromInt = fromInt;
            this.toInt = toInt;
        }

        public Collector<T, ArrayList<T>, BitSet<T>> collector() {
            final BinaryOperator<ArrayList<T>> combiner = (left, right) -> {
                left.addAll(right);
                return left;
            };
            return Collector.of(ArrayList::new, ArrayList::add, combiner, this::ofAll);
        }

        public BitSet<T> empty() {
            return new BitSetModule.BitSet1<>(this, 0L);
        }

        public BitSet<T> of(T t) {
            final int value = toInt.apply(t);
            if(value < BitSetModule.BITS_PER_WORD) {
                return new BitSetModule.BitSet1<>(this, 1L << value);
            } else if(value < 2 * BitSetModule.BITS_PER_WORD) {
                return new BitSetModule.BitSet2<>(this, 0L, 1L << value);
            } else {
                return empty().add(t);
            }
        }

        @SuppressWarnings("varargs")
        @SafeVarargs
        public final BitSet<T> of(T... values) {
            if(values.length == 0) {
                return empty();
            } else if(values.length == 1) {
                return of(values[0]);
            } else {
                return empty().addAll(Array.wrap(values));
            }
        }

        public BitSet<T> ofAll(Iterable<? extends T> values) {
            Objects.requireNonNull(values, "values is null");
            return empty().addAll(values);
        }

        public BitSet<T> tabulate(int n, Function<? super Integer, ? extends T> f) {
            Objects.requireNonNull(f, "f is null");
            return empty().addAll(Collections.tabulate(n, f));
        }

        public BitSet<T> fill(int n, Supplier<? extends T> s) {
            Objects.requireNonNull(s, "s is null");
            return empty().addAll(Collections.fill(n, s));
        }
    }

    static <T> Builder<T> withRelations(Function1<Integer, T> fromInt, Function1<T, Integer> toInt) {
        return new Builder<>(fromInt, toInt);
    }

    static <T extends Enum<T>> Builder<T> withEnum(Class<T> clz) {
        final Function1<Integer, T> fromInt = i -> clz.getEnumConstants()[i];
        final Function1<T, Integer> toInt = Enum<T>::ordinal;
        return new Builder<>(fromInt, toInt);
    }

    static Builder<Character> withCharacters() {
        return new Builder<>(i -> (char) i.intValue(), c -> (int) c);
    }

    static Builder<Byte> withBytes() {
        return new Builder<>(Integer::byteValue, Byte::intValue);
    }

    static Builder<Long> withLongs() {
        return new Builder<>(Integer::longValue, Long::intValue);
    }

    static Builder<Short> withShorts() {
        return new Builder<>(Integer::shortValue, Short::intValue);
    }

    /**
     * Returns a {@link java.util.stream.Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(java.util.stream.Collector)} to obtain a {@link javaslang.collection.BitSet}.
     *
     * @return A javaslang.collection.List Collector.
     */
    static Collector<Integer, ArrayList<Integer>, BitSet<Integer>> collector() {
        return Builder.DEFAULT.collector();
    }

    static BitSet<Integer> empty() {
        return Builder.DEFAULT.empty();
    }

    static BitSet<Integer> of(Integer value) {
        return Builder.DEFAULT.of(value);
    }

    static BitSet<Integer> of(Integer... values) {
        return Builder.DEFAULT.of(values);
    }

    /**
     * Returns a BitSet containing {@code n} values of a given Function {@code f}
     * over a range of integer values from 0 to {@code n - 1}.
     *
     * @param n The number of elements in the BitSet
     * @param f The Function computing element values
     * @return A BitSet consisting of elements {@code f(0),f(1), ..., f(n - 1)}
     * @throws NullPointerException if {@code f} are null
     */
    static BitSet<Integer> tabulate(int n, Function<Integer, Integer> f) {
        return Builder.DEFAULT.tabulate(n, f);
    }

    /**
     * Returns a BitSet containing {@code n} values supplied by a given Supplier {@code s}.
     *
     * @param n The number of elements in the BitSet
     * @param s The Supplier computing element values
     * @return A BitSet of size {@code n}, where each element contains the result supplied by {@code s}.
     * @throws NullPointerException if {@code s} are null
     */
    static BitSet<Integer> fill(int n, Supplier<Integer> s) {
        return Builder.DEFAULT.fill(n, s);
    }

    static BitSet<Integer> ofAll(Iterable<Integer> values) {
        return Builder.DEFAULT.ofAll(values);
    }

    /**
     * Creates a BitSet based on the elements of a boolean array.
     *
     * @param array a boolean array
     * @return A new BitSet of Boolean values
     */
    static BitSet<Boolean> ofAll(boolean[] array) {
        Objects.requireNonNull(array, "array is null");
        return BitSet.withRelations(i -> i != 0, b -> b ? 1 : 0).ofAll(Iterator.ofAll(array));
    }

    /**
     * Creates a BitSet based on the elements of a byte array.
     *
     * @param array a byte array
     * @return A new BitSet of Byte values
     */
    static BitSet<Byte> ofAll(byte[] array) {
        Objects.requireNonNull(array, "array is null");
        return BitSet.withBytes().ofAll(Iterator.ofAll(array));
    }

    /**
     * Creates a BitSet based on the elements of a char array.
     *
     * @param array a char array
     * @return A new BitSet of Character values
     */
    static BitSet<Character> ofAll(char[] array) {
        Objects.requireNonNull(array, "array is null");
        return BitSet.withCharacters().ofAll(Iterator.ofAll(array));
    }

    /**
     * Creates a BitSet based on the elements of an int array.
     *
     * @param array an int array
     * @return A new BitSet of Integer values
     */
    static BitSet<Integer> ofAll(int[] array) {
        Objects.requireNonNull(array, "array is null");
        return BitSet.ofAll(Iterator.ofAll(array));
    }

    /**
     * Creates a BitSet based on the elements of a long array.
     *
     * @param array a long array
     * @return A new BitSet of Long values
     */
    static BitSet<Long> ofAll(long[] array) {
        Objects.requireNonNull(array, "array is null");
        return BitSet.withLongs().ofAll(Iterator.ofAll(array));
    }

    /**
     * Creates a BitSet based on the elements of a short array.
     *
     * @param array a short array
     * @return A new BitSet of Short values
     */
    static BitSet<Short> ofAll(short[] array) {
        Objects.requireNonNull(array, "array is null");
        return BitSet.withShorts().ofAll(Iterator.ofAll(array));
    }

    /**
     * Creates a BitSet of int numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @return a range of int values as specified or the empty range if {@code from >= toExclusive}
     */
    static BitSet<Integer> range(int from, int toExclusive) {
        return BitSet.ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a BitSet of int numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @param step        the step
     * @return a range of long values as specified or the empty range if<br>
     * {@code from >= toInclusive} and {@code step > 0} or<br>
     * {@code from <= toInclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    static BitSet<Integer> rangeBy(int from, int toExclusive, int step) {
        return BitSet.ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a BitSet of int numbers starting from {@code from}, extending to {@code toInclusive}.
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @return a range of int values as specified or the empty range if {@code from > toInclusive}
     */
    static BitSet<Integer> rangeClosed(int from, int toInclusive) {
        return BitSet.ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    static BitSet<Character> rangeClosed(char from, char toInclusive) {
        return BitSet.withCharacters().ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    static BitSet<Long> rangeClosed(long from, long toInclusive) {
        return BitSet.withLongs().ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    /**
     * Creates a BitSet of int numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @param step        the step
     * @return a range of int values as specified or the empty range if<br>
     * {@code from > toInclusive} and {@code step > 0} or<br>
     * {@code from < toInclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    static BitSet<Integer> rangeClosedBy(int from, int toInclusive, int step) {
        return BitSet.ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    static BitSet<Character> rangeClosedBy(char from, char toInclusive, int step) {
        return BitSet.withCharacters().ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    static BitSet<Long> rangeClosedBy(long from, long toInclusive, long step) {
        return BitSet.withLongs().ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    Builder<T> builder();

    @Override
    BitSet<T> add(T element);

    @Override
    BitSet<T> addAll(Iterable<? extends T> elements);

    @Override
    default BitSet<T> diff(Set<? extends T> elements) {
        return removeAll(elements);
    }

    @Override
    default BitSet<T> distinct() {
        return this;
    }

    @Override
    default BitSet<T> distinctBy(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return builder().ofAll(iterator().distinctBy(comparator));
    }

    @Override
    default <U> BitSet<T> distinctBy(Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        return builder().ofAll(iterator().distinctBy(keyExtractor));
    }

    @Override
    default BitSet<T> drop(long n) {
        if (n <= 0) {
            return this;
        } else if (n >= length()) {
            return builder().empty();
        } else {
            return builder().ofAll(iterator().drop(n));
        }
    }

    @Override
    default BitSet<T> dropRight(long n) {
        if (n <= 0) {
            return this;
        } else if (n >= length()) {
            return builder().empty();
        } else {
            return builder().ofAll(iterator().dropRight(n));
        }
    }

    @Override
    default BitSet<T> dropUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return dropWhile(predicate.negate());
    }

    @Override
    default BitSet<T> dropWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final BitSet<T> bitSet = builder().ofAll(iterator().dropWhile(predicate));
        return (bitSet.length() == length()) ? this : bitSet;
    }

    @Override
    default BitSet<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final BitSet<T> bitSet = builder().ofAll(iterator().filter(predicate));
        return (bitSet.length() == length()) ? this : bitSet;
    }

    @Override
    default <U> SortedSet<U> flatMap(Comparator<? super U> comparator, Function<? super T, ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return TreeSet.ofAll(comparator, iterator().flatMap(mapper));
    }

    @Override
    default <U> SortedSet<U> flatMap(Function<? super T, ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return TreeSet.ofAll(naturalComparator(), iterator().flatMap(mapper));
    }

    @Override
    default <U> U foldRight(U zero, BiFunction<? super T, ? super U, ? extends U> f) {
        Objects.requireNonNull(f, "f is null");
        return iterator().foldRight(zero, f);
    }

    @Override
    default <C> Map<C, BitSet<T>> groupBy(Function<? super T, ? extends C> classifier) {
        Objects.requireNonNull(classifier, "classifier is null");
        return iterator().groupBy(classifier).map((key, iterator) -> Tuple.of(key, builder().ofAll(iterator)));
    }

    @Override
    default Iterator<BitSet<T>> grouped(long size) {
        return sliding(size, size);
    }

    @Override
    default boolean hasDefiniteSize() {
        return true;
    }

    @Override
    BitSet<T> init();

    @Override
    default Option<BitSet<T>> initOption() {
        return isEmpty() ? Option.none() : Option.some(init());
    }

    @Override
    default boolean isTraversableAgain() {
        return true;
    }

    @Override
    Iterator<T> iterator();

    @Override
    default BitSet<T> intersect(Set<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (isEmpty() || elements.isEmpty()) {
            return builder().empty();
        } else {
            int size = size();
            if (size <= elements.size()) {
                return retainAll(elements);
            } else {
                BitSet<T> results = builder().ofAll(elements).retainAll(this);
                return (size == results.size()) ? this : results;
            }
        }
    }

    @Override
    default Tuple2<BitSet<T>, BitSet<T>> partition(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return iterator().partition(predicate).map(builder()::ofAll, builder()::ofAll);
    }

    @Override
    default BitSet<T> peek(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action is null");
        if (!isEmpty()) {
            action.accept(head());
        }
        return this;
    }

    @Override
    default String stringPrefix() {
        return "BitSet";
    }

    @Override
    default Comparator<T> comparator() {
        return (t1, t2) -> Integer.compare(builder().toInt.apply(t1), builder().toInt.apply(t2));
    }

    @Override
    default <U> SortedSet<U> map(Comparator<? super U> comparator, Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return TreeSet.ofAll(comparator, iterator().map(mapper));
    }

    @Override
    default <U> SortedSet<U> map(Function<? super T, ? extends U> mapper) {
        return map(naturalComparator(), mapper);
    }

    @Override
    BitSet<T> remove(T element);

    @Override
    BitSet<T> removeAll(Iterable<? extends T> elements);

    @Override
    default BitSet<T> replace(T currentElement, T newElement) {
        if (contains(currentElement)) {
            return remove(currentElement).add(newElement);
        } else {
            return this;
        }
    }

    @Override
    default BitSet<T> replaceAll(T currentElement, T newElement) {
        // a set has only one occurrence
        return replace(currentElement, newElement);
    }

    @Override
    default BitSet<T> retainAll(Iterable<? extends T> elements) {
        return Collections.retainAll(this, elements);
    }

    @Override
    default BitSet<T> scan(T zero, BiFunction<? super T, ? super T, ? extends T> operation) {
        Objects.requireNonNull(operation, "operation is null");
        return Collections.scanLeft(this, zero, operation, new java.util.ArrayList<T>(), (arr, t) -> {
            arr.add(t);
            return arr;
        }, builder()::ofAll);
    }

    @Override
    default <U> Set<U> scanLeft(U zero, BiFunction<? super U, ? super T, ? extends U> operation) {
        return Collections.scanLeft(this, zero, operation, new java.util.ArrayList<>(), (c, u) -> {
            c.add(u);
            return c;
        }, HashSet::ofAll);
    }

    @Override
    default <U> Set<U> scanRight(U zero, BiFunction<? super T, ? super U, ? extends U> operation) {
        return Collections.scanRight(this, zero, operation, new java.util.ArrayList<>(), (c, u) -> {
            c.add(u);
            return c;
        }, HashSet::ofAll);
    }

    @Override
    default Iterator<BitSet<T>> sliding(long size) {
        return sliding(size, 1);
    }

    @Override
    default Iterator<BitSet<T>> sliding(long size, long step) {
        return iterator().sliding(size, step).map(builder()::ofAll);
    }

    @Override
    default Tuple2<BitSet<T>, BitSet<T>> span(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return iterator().span(predicate).map(builder()::ofAll, builder()::ofAll);
    }

    @Override
    default BitSet<T> tail() {
        if (isEmpty()) {
            throw new UnsupportedOperationException("tail of empty BitSet");
        } else {
            return remove(head());
        }
    }

    @Override
    default Option<BitSet<T>> tailOption() {
        return isEmpty() ? Option.none() : Option.some(tail());
    }

    @Override
    default BitSet<T> take(long n) {
        if (n <= 0) {
            return builder().empty();
        } else if (n >= length()) {
            return this;
        } else {
            return builder().ofAll(iterator().take(n));
        }
    }

    @Override
    default BitSet<T> takeRight(long n) {
        if (n <= 0) {
            return builder().empty();
        } else if (n >= length()) {
            return this;
        } else {
            return builder().ofAll(iterator().takeRight(n));
        }
    }

    @Override
    default BitSet<T> takeUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final BitSet<T> result = takeWhile(predicate.negate());
        return (result.length() == length()) ? this : result;
    }

    @Override
    default BitSet<T> takeWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final BitSet<T> result = builder().ofAll(iterator().takeWhile(predicate));
        return (result.length() == length()) ? this : result;
    }

    @Override
    default java.util.SortedSet<T> toJavaSet() {
        return toJavaSet(() -> new java.util.TreeSet<>(comparator()));
    }

    @Override
    default BitSet<T> union(Set<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        return addAll(elements);
    }

    // TODO
    @Override
    default <T1, T2> Tuple2<TreeSet<T1>, TreeSet<T2>> unzip(
            Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) {
        Objects.requireNonNull(unzipper, "unzipper is null");
        return iterator().unzip(unzipper).map(i1 -> TreeSet.ofAll(naturalComparator(), i1),
                i2 -> TreeSet.ofAll(naturalComparator(), i2));
    }

    // TODO
    @Override
    default <T1, T2, T3> Tuple3<TreeSet<T1>, TreeSet<T2>, TreeSet<T3>> unzip3(
            Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
        Objects.requireNonNull(unzipper, "unzipper is null");
        return iterator().unzip3(unzipper).map(
                i1 -> TreeSet.ofAll(naturalComparator(), i1),
                i2 -> TreeSet.ofAll(naturalComparator(), i2),
                i3 -> TreeSet.ofAll(naturalComparator(), i3));
    }

    // TODO
    @Override
    default <U> TreeSet<Tuple2<T, U>> zip(Iterable<? extends U> that) {
        Objects.requireNonNull(that, "that is null");
        final Comparator<Tuple2<T, U>> tuple2Comparator = Tuple2.comparator(comparator(), naturalComparator());
        return TreeSet.ofAll(tuple2Comparator, iterator().zip(that));
    }

    // TODO
    @Override
    default <U> TreeSet<Tuple2<T, U>> zipAll(Iterable<? extends U> that, T thisElem, U thatElem) {
        Objects.requireNonNull(that, "that is null");
        final Comparator<Tuple2<T, U>> tuple2Comparator = Tuple2.comparator(comparator(), naturalComparator());
        return TreeSet.ofAll(tuple2Comparator, iterator().zipAll(that, thisElem, thatElem));
    }

    // TODO
    @Override
    default TreeSet<Tuple2<T, Long>> zipWithIndex() {
        final Comparator<? super T> component1Comparator = comparator();
        final Comparator<Tuple2<T, Long>> tuple2Comparator = (t1, t2) -> component1Comparator.compare(t1._1, t2._1);
        return TreeSet.ofAll(tuple2Comparator, iterator().zipWithIndex());
    }
}

interface BitSetModule {

    int ADDRESS_BITS_PER_WORD = 6;
    int BITS_PER_WORD = 64;

    abstract class AbstractBitSet<T> implements BitSet<T> {

        private static final long serialVersionUID = 1L;

        Builder<T> builder;

        AbstractBitSet(Builder<T> builder) {
            this.builder = builder;
        }

        public Builder<T> builder() {
            return builder;
        }

        abstract int getWordsNum();

        abstract long[] copyExpand(int wordsNum);

        abstract long getWord(int index);

        BitSet<T> fromBitMaskNoCopy(long[] elements) {
            final int len = elements.length;
            if (len == 0) {
                return builder.empty();
            }
            if (len == 1) {
                return new BitSet1<>(builder, elements[0]);
            }
            if (len == 2) {
                return new BitSet2<>(builder, elements[0], elements[1]);
            }
            return new BitSetN<>(builder, elements);
        }

        private void setElement(long[] words, int element) {
            final int index = element >> ADDRESS_BITS_PER_WORD;
            words[index] |= (1L << element);
        }

        private void unsetElement(long[] words, int element) {
            final int index = element >> ADDRESS_BITS_PER_WORD;
            words[index] &= ~(1L << element);
        }

        long[] shrink(long[] elements) {
            int newlen = elements.length;
            while (newlen > 0 && elements[newlen - 1] == 0) {
                newlen--;
            }
            long[] newelems = new long[newlen];
            System.arraycopy(elements, 0, newelems, 0, newlen);
            return newelems;
        }

        @Override
        public BitSet<T> add(T t) {
            if (contains(t)) {
                return this;
            } else {
                final int element = builder.toInt.apply(t);
                if (element < 0) {
                    throw new IllegalArgumentException("bitset element must be >= 0");
                }
                final long[] copy = copyExpand(1 + (element >> ADDRESS_BITS_PER_WORD));
                setElement(copy, element);
                return fromBitMaskNoCopy(copy);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public BitSet<T> addAll(Iterable<? extends T> elements) {
            final Stream<Integer> source = Stream.ofAll(elements).map(builder.toInt);
            final long[] copy = copyExpand(1 + (source.max().getOrElse(0) >> ADDRESS_BITS_PER_WORD));
            source.forEach(element -> {
                if (element < 0) {
                    throw new IllegalArgumentException("bitset element must be >= 0");
                }
                setElement(copy, element);
            });
            return fromBitMaskNoCopy(copy);
        }

        @Override
        public boolean contains(T t) {
            final int element = builder.toInt.apply(t);
            if (element < 0) {
                throw new IllegalArgumentException("bitset element must be >= 0");
            }
            final int index = element >> ADDRESS_BITS_PER_WORD;
            return (getWord(index) & (1L << element)) != 0;
        }

        @Override
        public BitSet<T> init() {
            if (isEmpty()) {
                throw new UnsupportedOperationException("init of empty TreeSet");
            } else {
                final long last = getWord(getWordsNum() - 1);
                final int element = BITS_PER_WORD * (getWordsNum() - 1) + BITS_PER_WORD - Long.numberOfLeadingZeros(last) - 1;
                return remove(builder.fromInt.apply(element));
            }
        }

        @Override
        public Iterator<T> iterator() {
            return Stream.range(0, getWordsNum() << ADDRESS_BITS_PER_WORD)
                    .filter(i -> (getWord(i >> ADDRESS_BITS_PER_WORD) & (1L << i)) != 0)
                    .map(builder.fromInt)
                    .iterator();
        }

        @Override
        public int length() {
            int len = 0;
            for (int i = 0; i < getWordsNum(); i++) {
                len += Long.bitCount(getWord(i));
            }
            return len;
        }

        @Override
        public BitSet<T> remove(T t) {
            if (contains(t)) {
                final int element = builder.toInt.apply(t);
                final long[] copy = copyExpand(getWordsNum());
                unsetElement(copy, element);
                return fromBitMaskNoCopy(shrink(copy));
            } else {
                return this;
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public BitSet<T> removeAll(Iterable<? extends T> elements) {
            final Stream<Integer> source = Stream.ofAll(elements).map(builder.toInt);
            final long[] copy = copyExpand(getWordsNum());
            source.forEach(element -> {
                unsetElement(copy, element);
            });
            return fromBitMaskNoCopy(shrink(copy));
        }

        @Override
        public String toString() {
            return mkString(stringPrefix() + "(", ", ", ")");
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof BitSet) {
                final BitSet<?> that = (BitSet<?>) o;
                return Collections.equals(this, that);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Collections.hash(this);
        }
    }

    class BitSet1<T> extends AbstractBitSet<T> {

        private static final long serialVersionUID = 1L;

        private final long elements;

        BitSet1(Builder<T> builder, long elements) {
            super(builder);
            this.elements = elements;
        }

        @Override
        int getWordsNum() {
            return 1;
        }

        @Override
        long[] copyExpand(int wordsNum) {
            if(wordsNum < 1) {
                wordsNum = 1;
            }
            long[] arr = new long[wordsNum];
            arr[0] = elements;
            return arr;
        }

        @Override
        long getWord(int index) {
            if (index == 0) {
                return elements;
            } else {
                return 0L;
            }
        }

        @Override
        public T head() {
            if (elements != 0) {
                return builder.fromInt.apply(Long.numberOfTrailingZeros(elements));
            }
            throw new NoSuchElementException("head of empty BitSet");
        }
    }

    class BitSet2<T> extends AbstractBitSet<T> {

        private static final long serialVersionUID = 1L;

        private final long elements1, elements2;

        BitSet2(Builder<T> builder, long elements1, long elements2) {
            super(builder);
            this.elements1 = elements1;
            this.elements2 = elements2;
        }

        @Override
        int getWordsNum() {
            return 2;
        }

        @Override
        long[] copyExpand(int wordsNum) {
            if(wordsNum < 2) {
                wordsNum = 2;
            }
            long[] arr = new long[wordsNum];
            arr[0] = elements1;
            arr[1] = elements2;
            return arr;
        }

        @Override
        long getWord(int index) {
            if (index == 0) {
                return elements1;
            } else {
                if (index == 1) {
                    return elements2;
                } else {
                    return 0L;
                }
            }
        }

        @Override
        public T head() {
            if (elements1 != 0) {
                return builder.fromInt.apply(Long.numberOfTrailingZeros(elements1));
            } else if (elements2 != 0) {
                return builder.fromInt.apply(BITS_PER_WORD + Long.numberOfTrailingZeros(elements2));
            }
            throw new NoSuchElementException("head of empty BitSet");
        }
    }

    class BitSetN<T> extends AbstractBitSet<T> {

        private static final long serialVersionUID = 1L;

        private final long[] elements;

        BitSetN(Builder<T> builder, long[] elements) {
            super(builder);
            this.elements = elements;
        }

        @Override
        int getWordsNum() {
            return elements.length;
        }

        @Override
        long[] copyExpand(int wordsNum) {
            if(wordsNum < elements.length) {
                wordsNum = elements.length;
            }
            long[] arr = new long[wordsNum];
            System.arraycopy(elements, 0, arr, 0, elements.length);
            return arr;
        }

        @Override
        long getWord(int index) {
            if (index < elements.length) {
                return elements[index];
            } else {
                return 0L;
            }
        }

        @Override
        public T head() {
            int offset = 0;
            for (int i = 0; i < getWordsNum(); i++) {
                if(elements[i] != 0) {
                    return builder.fromInt.apply(offset + Long.numberOfTrailingZeros(elements[i]));
                }
                offset += BITS_PER_WORD;
            }
            throw new NoSuchElementException("head of empty BitSet");
        }
    }
}
