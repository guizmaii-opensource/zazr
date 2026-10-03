package dev.zazr.avaje.jsonb;

import dev.zazr.collection.Vector;
import io.avaje.jsonb.Jsonb;
import io.avaje.jsonb.spi.JsonbExtension;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// avaje-jsonb finds the component by itself: adding the dependency is enough.
class RegistrationTest {

    @Test
    void theComponentIsAServiceOfAvajeJsonb() {
        assertThat(ServiceLoader.load(JsonbExtension.class).stream().map(ServiceLoader.Provider::type))
                .contains(ZazrJsonbComponent.class);
    }

    @Test
    void theDefaultJsonbAndABuiltOneKnowTheZazrTypes() {
        assertThat(Jsonb.instance().toJson(Vector.of(1, 2))).isEqualTo("[1,2]");
        assertThat(Jsonb.builder().serializeNulls(true).build().toJson(Vector.of(1, 2)))
                .isEqualTo("[1,2]");
    }

    @Test
    void theComponentCanBeAddedByHand() {
        var jsonb = Jsonb.builder().add(new ZazrJsonbComponent()).build();
        assertThat(jsonb.type(Vector.class).fromJson("[1,2]")).isEqualTo(Vector.of(1L, 2L));
    }
}
