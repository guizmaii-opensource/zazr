---
description: Queue, a first-in, first-out sequence - O(1) enqueue, amortised O(1) dequeue.
---

# `Queue`

A first-in, first-out sequence. `enqueue` adds at the end in O(1). `dequeue` takes from the front in amortised O(1):
most calls are O(1), and now and then one pays O(n) to put the elements added at the end in order.

## When to choose it

For first in, first out: a work list, a breadth-first walk. It also has the other sequence methods, but access by
index walks the queue; choose [`Vector`](vector.md) for that.

```java
var queue = Queue.of("a", "b").enqueue("c"); // Queue<String>
var next = queue.dequeue(); // Tuple2<String, Queue<String>>
// next is (a, Queue(b, c))
```

```java
var work = Queue.of(1);
var visited = 0;
while (!work.isEmpty() && visited < 5) {
    var step = work.dequeue(); // Tuple2<Integer, Queue<Integer>>
    work = step._2().enqueue(step._1() * 2, step._1() * 2 + 1);
    visited++;
}
// visited is 5, work is Queue(6, 7, 8, 9, 10, 11)
```

## Costs

--8<-- "Queue.md"

Every method: [complexity page](complexity.md#queue).

## Sharp edges

- `dequeue()` on an empty queue throws; `dequeueOption()` returns an `Option`.
- The amortised cost holds when each `dequeue` works on the queue the previous one returned. Calling `dequeue`
  again and again on the same old queue can pay the O(n) step every time.
- Creating an `iterator()` can cost O(n), and so can `get(i)` for a small `i`: on a queue built by `enqueue`, most
  elements are still waiting at the end, in reverse order. `size()` counts the elements; `isEmpty()` is O(1).
