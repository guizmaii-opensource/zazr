package outside_of_vavr;

import io.vavr.collection.HashMap;
import io.vavr.collection.Vector;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IllegalAccessErrorTest {

    @Test
    public void shouldNotThrowIllegalAccessErrorWhenUsingHashMapMergeMethodReference() {
        final BiFunction<HashMap<String, String>, HashMap<String, String>, HashMap<String, String>> merge = HashMap::merge;
        final HashMap<String, String> reduced = Vector.of("a", "b", "c")
                .map(t -> HashMap.of(t, t))
                .reduce(merge);
        assertThat(reduced).isEqualTo(HashMap.of("a", "a", "b", "b", "c", "c"));
    }
}
