package dev.zazr.collection.internal;

import org.jspecify.annotations.Nullable;

/// The positions behind `combinations` and `permutations` of the sequences: each cursor walks its arrangements in
/// order, one array of positions updated in place, and the caller builds each result from the positions.
public interface Arrangements {

    /// The k-combinations of n positions, in lexicographic order: the index array of Scala's `CombinationsItr`
    /// (`scala/collection/Seq.scala`) with every position counted once, since the sequences choose by position
    /// and keep equal elements apart. From one combination, the next increments the last index that can still move,
    /// and puts the following ones right after it: O(k) per combination, and no choice is ever abandoned.
    final class Combinations {

        private final int n;
        private final int[] indices;
        private boolean started;

        /// The cursor before the first combination; `0 <= k <= n`.
        public Combinations(int n, int k) {
            this.n = n;
            this.indices = new int[k];
        }

        /// Moves to the next combination, the first one on the first call: false when there is none left.
        public boolean advance() {
            int k = indices.length;
            if (!started) {
                started = true;
                for (int j = 0; j < k; j++) {
                    indices[j] = j;
                }
                return true;
            }
            // the last index that can move: index j may reach n - k + j, leaving room for the ones after it
            @SuppressWarnings("Var")
            int j = k - 1;
            while (j >= 0 && indices[j] == n - k + j) {
                j--;
            }
            if (j < 0) {
                return false;
            }
            indices[j]++;
            for (int i = j + 1; i < k; i++) {
                indices[i] = indices[i - 1] + 1;
            }
            return true;
        }

        /// The position of the `j`-th element of the current combination.
        public int index(int j) {
            return indices[j];
        }
    }

    /// The distinct permutations of a sequence, in the order of a choice by first occurrence: the first element of
    /// a permutation is each distinct element in the order it first occurs, and the rest are the permutations of
    /// what is left once that occurrence is removed, chosen the same way. Scala's `PermutationsItr` orders by the
    /// first occurrence in the whole sequence instead, which gives another order when an element repeats, so the
    /// walk is a depth-first search over the positions.
    ///
    /// Equal elements form a class, and the occurrences of a class are taken in order: the positions in use are
    /// always the first ones of their class. So a position is the first occurrence of its class among those left
    /// exactly when its rank in the class is the number of positions of the class in use, an O(1) test. Each step
    /// walks the positions of one level, O(n), and a permutation is built in O(n).
    final class Permutations {

        // the class of each position, numbered in order of first occurrence
        private final int[] classes;
        // the rank of each position among those of its class
        private final int[] ranks;
        // the number of positions of each class in use
        private final int[] used;
        // the position chosen at each level
        private final int[] chosen;
        private boolean started;

        /// The cursor before the first permutation of `elements`, compared with `equals` and `hashCode`.
        public Permutations(@Nullable Object[] elements) {
            int n = elements.length;
            this.classes = new int[n];
            this.ranks = new int[n];
            this.chosen = new int[n];
            java.util.HashMap<@Nullable Object, Integer> classOf = new java.util.HashMap<>();
            int[] counts = new int[n];
            for (int p = 0; p < n; p++) {
                Integer known = classOf.get(elements[p]);
                int c = (known == null) ? classOf.size() : known;
                if (known == null) {
                    classOf.put(elements[p], c);
                }
                classes[p] = c;
                ranks[p] = counts[c]++;
            }
            this.used = new int[classOf.size()];
        }

        /// Moves to the next permutation, the first one on the first call: false when there is none left.
        public boolean advance() {
            int n = chosen.length;
            if (!started) {
                started = true;
                fill(0);
                return true;
            }
            // the deepest level whose position can move to a later candidate; the levels below it are filled again
            for (int level = n - 1; level >= 0; level--) {
                int current = chosen[level];
                used[classes[current]]--;
                int next = candidate(current + 1);
                if (next < n) {
                    take(level, next);
                    fill(level + 1);
                    return true;
                }
            }
            return false;
        }

        /// The position of the `level`-th element of the current permutation.
        public int position(int level) {
            return chosen[level];
        }

        // the first position from `from` on that is the first occurrence of its class among the positions left; n if
        // none
        @SuppressWarnings("Var")
        private int candidate(int from) {
            int n = classes.length;
            int p = from;
            while (p < n && ranks[p] != used[classes[p]]) {
                p++;
            }
            return p;
        }

        private void take(int level, int position) {
            chosen[level] = position;
            used[classes[position]]++;
        }

        // the first choice at each level from `level` on; one position is always left per level
        private void fill(int level) {
            for (int l = level; l < chosen.length; l++) {
                take(l, candidate(0));
            }
        }
    }
}
