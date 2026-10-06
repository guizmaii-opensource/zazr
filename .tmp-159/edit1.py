import sys
p='Vector.java'
s=open(p).read()
old_perm='''    /**
     * All distinct permutations of the elements, in the order the distinct elements first occur.
     * <p>
     * Complexity: O(n! * n^2) in the worst case (all elements distinct): there are n! permutations of n elements, and
     * every partial permutation is copied into a new Vector at each of the n levels of the recursion.
     *
     * @return the permutations; none for the empty Vector
     */
    public Vector<Vector<T>> permutations() {
        if (isEmpty()) {
            return empty();
        } else if (size() == 1) {
            return of(this);
        } else {
            @SuppressWarnings("Var")
            Vector<Vector<T>> results = empty();
            for (T t : distinct()) {
                for (Vector<T> ts : remove(t).permutations()) {
                    results = results.append(of(t).appendAll(ts));
                }
            }
            return results;
        }
    }'''
new_perm='''    /**
     * All distinct permutations of the elements, in the order the distinct elements first occur.
     * <p>
     * Complexity: O(n! * n) in the worst case (all elements distinct): there are n! permutations of n elements, each
     * built once, in O(n), from an array of positions that moves to the next permutation in O(n).
     *
     * @return the permutations; none for the empty Vector
     */
    public Vector<Vector<T>> permutations() {
        if (isEmpty()) {
            return empty();
        } else if (size() == 1) {
            return of(this);
        } else {
            return VectorModule.Permutations.apply(this);
        }
    }'''
assert old_perm in s
s=s.replace(old_perm,new_perm)
old_comb='''     * Complexity: O(k * C(n, k) + C(n, 0) + ... + C(n, k)): the C(n, k) combinations of k elements are built, and
     * every choice of fewer than k elements is visited on the way, even one that cannot be completed. That is
     * O(k * C(n, k)) for k up to n / 2; as k approaches n, the visited choices approach 2^n while the result shrinks:
     * {@code combinations(n)} does O(2^n) work to return one combination.'''
new_comb='''     * Complexity: O(n + k * C(n, k)): the elements are copied into an array once, then each of the C(n, k)
     * combinations is built from an array of k positions, which moves to the next combination in O(k).'''
assert old_comb in s
s=s.replace(old_comb,new_comb)
old_all='''     * Complexity: O(n * 2^n): the 2^n combinations hold n * 2^(n - 1) elements in all, and building them visits as
     * many partial choices.'''
new_all='''     * Complexity: O(n * 2^n): the 2^n combinations hold n * 2^(n - 1) elements in all, each built once.'''
assert old_all in s
s=s.replace(old_all,new_all)
open(p,'w').write(s)
