package com.guizmaii.zazr.collection.internal;

import org.jspecify.annotations.Nullable;

/// The walk of a [ChampNode] trie with a fixed stack: a depth-first pre-order traversal that yields the entries held
/// inline by a node before those of its children, left to right. A subclass reads the entry at [#currentValueCursor]
/// of [#currentValueNode] and advances the cursor. Ported from `ChampBaseIterator` in
/// `scala/collection/immutable/ChampCommon.scala` of the Scala 3 standard library (the Scala 2.13 collection library,
/// which Scala 3 ships unchanged).
///
/// @param <T> the element type
/// @param <N> the node type
public abstract class ChampIterator<T extends @Nullable Object, N extends ChampNode<N>> extends AbstractIterator<T> {

    protected int currentValueCursor;
    protected int currentValueLength;
    // null only while currentValueLength is 0, so never read then
    @SuppressWarnings("NullAway.Init")
    protected N currentValueNode;

    private int currentStackLevel = -1;
    // the cursor and the number of children of each node on the stack, allocated with the first node that has children
    private int @Nullable [] nodeCursorsAndLengths;
    private @Nullable Object @Nullable [] nodes;

    protected ChampIterator(N rootNode) {
        if (rootNode.hasNodes()) {
            pushNode(rootNode);
        }
        if (rootNode.hasPayload()) {
            setupPayloadNode(rootNode);
        }
    }

    private void setupPayloadNode(N node) {
        currentValueNode = node;
        currentValueCursor = 0;
        currentValueLength = node.payloadArity();
    }

    private void pushNode(N node) {
        @Nullable Object[] stack = nodes;
        int[] cursors = nodeCursorsAndLengths;
        if (stack == null || cursors == null) {
            stack = nodes = new Object[ChampNode.MAX_DEPTH];
            cursors = nodeCursorsAndLengths = new int[ChampNode.MAX_DEPTH * 2];
        }
        currentStackLevel++;
        final int cursorIndex = currentStackLevel * 2;
        stack[currentStackLevel] = node;
        cursors[cursorIndex] = 0;
        cursors[cursorIndex + 1] = node.nodeArity();
    }

    // the stack arrays are allocated by the first push, and read only while the stack is not empty
    @SuppressWarnings({"unchecked", "NullAway"})
    private boolean searchNextValueNode() {
        while (currentStackLevel >= 0) {
            final int cursorIndex = currentStackLevel * 2;
            final int nodeCursor = nodeCursorsAndLengths[cursorIndex];
            if (nodeCursor < nodeCursorsAndLengths[cursorIndex + 1]) {
                nodeCursorsAndLengths[cursorIndex] = nodeCursor + 1;
                final N nextNode = ((N) nodes[currentStackLevel]).getNode(nodeCursor);
                if (nextNode.hasNodes()) {
                    pushNode(nextNode);
                }
                if (nextNode.hasPayload()) {
                    setupPayloadNode(nextNode);
                    return true;
                }
            } else {
                // drop the reference, so a long-lived iterator does not keep a walked subtree alive
                nodes[currentStackLevel] = null;
                currentStackLevel--;
            }
        }
        return false;
    }

    @Override
    public final boolean hasNext() {
        return currentValueCursor < currentValueLength || searchNextValueNode();
    }
}
