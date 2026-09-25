---
description: Queue, a banker's queue over two lists - amortised O(1) enqueue and dequeue.
---

# `Queue`

A first-in, first-out sequence built from two `List`s: the front, in order, and the rear, reversed. `enqueue` prepends
to the rear; `dequeue` takes the head of the front and, when the front runs out, reverses the rear into a new front.
Each element is reversed once, so `enqueue` and `dequeue` are amortised O(1).

## When to choose it

For first in, first out: a work list, a breadth-first walk. It also has the full sequence API, but positional access
walks the lists; choose [`Vector`](vector.md) for that.

```java
Queue<String> queue = Queue.of("a", "b").enqueue("c");
Tuple2<String, Queue<String>> next = queue.dequeue();
// next is (a, Queue(b, c))
```

```java
Queue<Integer> work = Queue.of(1);
int visited = 0;
while (!work.isEmpty() && visited < 5) {
    Tuple2<Integer, Queue<Integer>> step = work.dequeue();
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
- An amortised bound holds over a sequence of operations on one queue. Dequeuing repeatedly from the same old version
  of a queue repeats the reversal each time.
- `iterator()` reverses the rear list when it is created (O(m) for m rear elements).
