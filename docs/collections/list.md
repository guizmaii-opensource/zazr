---
description: List, the cons list - O(1) prepend, head and tail, and a sealed Cons/Nil pair to switch over.
---

# `List`

A linked list: a sealed interface with two records, `Cons(T head, List<T> tail)` and `Nil()`.

Adding at the front, `head` and `tail` are O(1) and share the rest of the list. Anything that reaches the end walks
the whole list.

## When to choose it

When you take a sequence apart from the front, recursively or with a `switch`, or need a stack (`push`, `pop`,
`peek`). For access by index, adding at the end or `length`, choose [`Vector`](vector.md).

```java
var list = List.of(1, 2, 3);
var first = switch (list) {
    case Cons(var head, var tail) -> "head " + head + ", then " + tail.length() + " more";
    case Nil() -> "empty";
};
// "head 1, then 2 more"
```

```java
var stack = List.<String>empty().push("a").push("b"); // List<String>
var top = stack.peek(); // String
var popped = stack.pop(); // List<String>
// top is "b", popped is List(a)
```

## Costs

--8<-- "List.md"

Every method: [complexity page](complexity.md#list).

## Sharp edges

- `length()` is O(n): the list does not store its length.
- `append`, `appendAll`, `get(i)`, `update`, `last` and `init` walk or copy the list up to the position.
- `push` is `prepend` under its stack name.
