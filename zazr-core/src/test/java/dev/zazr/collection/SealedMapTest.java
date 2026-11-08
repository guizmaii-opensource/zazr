package dev.zazr.collection;

import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// `Map` and `SortedMap` are implemented by Zazr only: they are sealed, and every class they permit is final, so no
/// code outside the library can implement them, at compile time or through a dynamic proxy.
class SealedMapTest {

    @Test
    void mapPermitsTheHashMapsAndSortedMapOnly() {
        assertThat(Map.class.isSealed()).isTrue();
        assertThat(Map.class.getPermittedSubclasses())
                .containsExactlyInAnyOrder(HashMap.class, LinkedHashMap.class, SortedMap.class);
    }

    @Test
    void sortedMapPermitsTreeMapOnly() {
        assertThat(SortedMap.class.isSealed()).isTrue();
        assertThat(SortedMap.class.getPermittedSubclasses()).containsExactly(TreeMap.class);
    }

    @Test
    void theMapClassesAreFinal() {
        for (Class<?> map : new Class<?>[] {HashMap.class, LinkedHashMap.class, TreeMap.class}) {
            assertThat(Modifier.isFinal(map.getModifiers()))
                    .as(map.getSimpleName())
                    .isTrue();
        }
    }

    @Test
    void aProxyCannotImplementMap() {
        for (Class<?> map : new Class<?>[] {Map.class, SortedMap.class}) {
            assertThatThrownBy(() -> Proxy.newProxyInstance(
                            map.getClassLoader(), new Class<?>[] {map}, (proxy, method, args) -> null))
                    .as(map.getSimpleName())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sealed");
        }
    }
}
