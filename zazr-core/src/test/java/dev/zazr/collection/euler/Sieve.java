package dev.zazr.collection.euler;

import dev.zazr.Function3;
import dev.zazr.collection.List;
import dev.zazr.collection.Set;
import dev.zazr.collection.Stream;
import dev.zazr.control.Option;
import java.util.function.BiFunction;

final class Sieve {

    private Sieve() {
    }

    private final static List<BiFunction<Integer, Integer, Option<Integer>>> RULES = List.of(
            (x, y) -> Option.some((4 * x * x) + (y * y)).filter(n -> n % 12 == 1 || n % 12 == 5),
            (x, y) -> Option.some((3 * x * x) + (y * y)).filter(n -> n % 12 == 7),
            (x, y) -> Option.some((3 * x * x) - (y * y)).filter(n -> x > y && n % 12 == 11)
    );

    private final static List<Function3<Set<Integer>, Integer, Integer, Set<Integer>>> STEPS = List.of(
            (sieve, limit, root) -> Stream.rangeClosed(1, root).crossProduct()
                    .foldLeft(sieve, (xs, xy) ->
                            RULES.foldLeft(xs, (ss, r) -> r.apply(xy._1(), xy._2())
                                    .filter(p -> p < limit)
                                    .map(p -> ss.contains(p) ? ss.remove(p) : ss.add(p))
                                    .getOrElse(ss)
                            )
                    ),
            (sieve, limit, root) -> Stream.rangeClosed(5, root)
                    .foldLeft(sieve, (xs, r) -> xs.contains(r)
                                                ? Stream.rangeBy(r * r, limit, r * r).foldLeft(xs, Set::remove)
                                                : xs
                    )
    );

    static Set<Integer> fillSieve(int limit, Set<Integer> empty) {
        return STEPS.foldLeft(empty.add(2).add(3), (s, step) ->
                step.apply(s, limit, (int) Math.ceil(Math.sqrt(limit)))
        );
    }

}
