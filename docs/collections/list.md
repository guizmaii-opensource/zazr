---
description: List, the cons list - O(1) prepend, head and tail, and a sealed Cons/Nil pair to pattern match on.
---

# `List`

A linked list: a sealed interface with two records, `Cons(T head, List<T> tail)` and `Nil()`.

Adding at the front, `head` and `tail` are O(1) and share the rest of the list. Anything that reaches the end walks
the whole list.

## When to choose it

When you take a sequence apart from the front, recursively or with pattern matching, or need a stack (`push`, `pop`,
`peek`). For access by index, adding at the end or `size`, choose [`Vector`](vector.md).

```java
var list = List.of(1, 2, 3);
var first = switch (list) {
    case Cons(var head, var tail) -> "head " + head + ", then " + tail.size() + " more";
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

- `size()` is O(n): the list does not store its size. `equals`, `toArray()` and `stream()` count
  the elements first too.
- `append`, `appendAll`, `last`, `init`, `takeRight` and `dropRight` walk the whole list, and all but `last` copy it.
- `get(i)` walks the list up to position i; `update`, `insert` and `removeAt` also copy the elements before it.
- `take`, `drop`, `slice` and `splitAt` walk only the elements they take or skip, never the rest of the list.
- A loop by index over `asJava()` is O(n^2), because each `get(i)` walks the list: use its iterator.
- `containsAll` is O(n * m): each element of the argument is looked for by a walk.
- `push` is `prepend` under its stack name.
