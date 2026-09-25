package com.guizmaii.zazr.test.docs;

import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.test.CheckConfig;
import com.guizmaii.zazr.test.Gen;
import com.guizmaii.zazr.test.laws.MapLaws;
import com.guizmaii.zazr.test.laws.MapSubject;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The fenced {@code java} blocks of the laws sections of docs/testing.md, pasted verbatim, compiled and run. They use
 * {@link Gen}, whose name the other examples of the page give to another type, so they live in their own file.
 */
public class DocsLawsExamplesTest {

    @Test
    void checkingYourOwnType() {
        record Box(Vector<Object> items) {
            Box map(Function<Object, Object> f) { return new Box(items.map(f)); }
        }
        var boxes = new MapSubject<Box>() {
            public Gen<Box> values() { return Gen.vector(Gen.intValue(-100, 100)).map(v -> new Box(v.map(x -> (Object) x))); }
            public Box map(Box box, Function<Object, Object> f) { return box.map(f); }
        };
        MapLaws.<Box>all().assertSatisfied(boxes);

        MapSubject<Box> broken = new MapSubject<>() {
            public Gen<Box> values() { return boxes.values(); }
            public Box map(Box box, Function<Object, Object> f) { return new Box(box.map(f).items().dropRight(1)); }
        };
        assertThatThrownBy(() -> MapLaws.<Box>all().assertSatisfied(broken, new CheckConfig(200, 100, 42, 1000)))
            .isInstanceOf(AssertionError.class)
            .hasMessage("""
                2 law(s) failed:
                mapIdentity: falsified at sample 5 by (Box[items=Vector(6)]): left = Box[items=Vector()], right = Box[items=Vector(6)] (seed 42, replay with -Dzazr.check.seed=42)
                mapComposition: falsified at sample 7 by (Box[items=Vector(99, -6)], x -> -1 * x + -9, x -> -10 * x + 80): left = Box[items=Vector()], right = Box[items=Vector(1160)] (seed 42, replay with -Dzazr.check.seed=42)""");
    }
}
