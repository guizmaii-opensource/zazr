package dev.zazr.jackson.internal;

import dev.zazr.Tuple0;
import dev.zazr.Tuple8;
import dev.zazr.collection.HashMap;
import dev.zazr.collection.HashSet;
import dev.zazr.collection.LazyList;
import dev.zazr.collection.Map;
import dev.zazr.collection.NonEmptyMap;
import dev.zazr.collection.NonEmptySet;
import dev.zazr.collection.NonEmptySortedMap;
import dev.zazr.collection.NonEmptySortedSet;
import dev.zazr.collection.NonEmptyVector;
import dev.zazr.collection.SortedMap;
import dev.zazr.collection.TreeSet;
import dev.zazr.collection.Vector;
import dev.zazr.control.Option;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.ReferenceType;
import tools.jackson.databind.type.SimpleType;
import tools.jackson.databind.type.TypeFactory;

import static org.assertj.core.api.Assertions.assertThat;

/// The registries answer for the Zazr types only, and recognise each of them.
class RegistriesTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final TypeFactory TYPES = MAPPER.getTypeFactory();

    /// A collection-like and map-like type of another library.
    static final class Other implements Iterable<Integer> {
        @Override
        public java.util.Iterator<Integer> iterator() {
            return java.util.Collections.emptyIterator();
        }
    }

    @Test
    void otherTypesAreLeftToJackson() {
        var config = MAPPER.deserializationConfig();
        var deserializers = new ZazrDeserializers();
        var collectionLike = TYPES.constructCollectionLikeType(Other.class, Integer.class);
        var mapLike = TYPES.constructMapLikeType(Other.class, String.class, Integer.class);
        var reference = (ReferenceType) TYPES.constructReferenceType(
                java.util.concurrent.atomic.AtomicReference.class, TYPES.constructType(String.class));
        assertThat(deserializers.findCollectionLikeDeserializer(collectionLike, config, null, null, null))
                .isNull();
        assertThat(deserializers.findMapLikeDeserializer(mapLike, config, null, null, null, null))
                .isNull();
        assertThat(deserializers.findReferenceDeserializer(reference, config, null, null, null))
                .isNull();
        assertThat(deserializers.findBeanDeserializer(TYPES.constructType(String.class), config, null))
                .isNull();
        assertThat(deserializers.findBeanDeserializer(TYPES.constructType(Tuple0.class), config, null))
                .isNull();

        var serializers = new ZazrSerializers();
        var serializationConfig = MAPPER.serializationConfig();
        assertThat(serializers.findCollectionLikeSerializer(
                        serializationConfig, collectionLike, null, null, null, null))
                .isNull();
        assertThat(serializers.findMapLikeSerializer(serializationConfig, mapLike, null, null, null, null, null))
                .isNull();
        assertThat(serializers.findReferenceSerializer(serializationConfig, reference, null, null, null, null))
                .isNull();
        assertThat(serializers.findSerializer(serializationConfig, TYPES.constructType(Tuple0.class), null, null))
                .isNull();
    }

    @Test
    void hasDeserializerFor() {
        var config = MAPPER.deserializationConfig();
        var deserializers = new ZazrDeserializers();
        for (var type : java.util.List.<Class<?>>of(
                Option.class,
                Vector.class,
                LazyList.class,
                TreeSet.class,
                NonEmptySortedSet.class,
                HashMap.class,
                SortedMap.class,
                NonEmptySortedMap.class,
                Tuple8.class)) {
            assertThat(deserializers.hasDeserializerFor(config, type))
                    .as(type.getName())
                    .isTrue();
        }
        for (var type : java.util.List.<Class<?>>of(String.class, Option.Some.class, Tuple0.class, Other.class)) {
            assertThat(deserializers.hasDeserializerFor(config, type))
                    .as(type.getName())
                    .isFalse();
        }
    }

    /// The types Jackson builds without the type modifiers (for a type id, or `@JsonDeserialize(as = ...)`).
    @Test
    void unshapedZazrTypesAreReshaped() {
        var config = MAPPER.deserializationConfig();
        var deserializers = new ZazrDeserializers();
        assertThat(deserializers.findBeanDeserializer(SimpleType.constructUnsafe(Vector.class), config, null))
                .isInstanceOf(CollectionDeserializer.class);
        assertThat(deserializers.findBeanDeserializer(SimpleType.constructUnsafe(HashMap.class), config, null))
                .isInstanceOf(ZazrMapDeserializer.class);
        assertThat(deserializers.findBeanDeserializer(SimpleType.constructUnsafe(Option.class), config, null))
                .isInstanceOf(OptionDeserializer.class);
        assertThat(deserializers.findBeanDeserializer(SimpleType.constructUnsafe(Option.Some.class), config, null))
                .isNull();
    }

    @Test
    void zazrTypes() {
        for (var type :
                java.util.List.<Class<?>>of(HashMap.class, Map.class, NonEmptyMap.class, NonEmptySortedMap.class)) {
            assertThat(ZazrTypes.isMap(type)).isTrue();
            assertThat(ZazrTypes.isCollection(type)).isFalse();
        }
        for (var type : java.util.List.<Class<?>>of(
                Vector.class, HashSet.class, NonEmptyVector.class, NonEmptySet.class, NonEmptySortedSet.class)) {
            assertThat(ZazrTypes.isMap(type)).isFalse();
            assertThat(ZazrTypes.isCollection(type)).isTrue();
        }
        for (var type : java.util.List.<Class<?>>of(Option.class, String.class, Iterable.class)) {
            assertThat(ZazrTypes.isMap(type)).isFalse();
            assertThat(ZazrTypes.isCollection(type)).isFalse();
        }
        assertThat(ZazrTypes.mapSupertype(HashMap.class)).isEqualTo(Map.class);
        assertThat(ZazrTypes.mapSupertype(NonEmptyMap.class)).isEqualTo(NonEmptyMap.class);
    }
}
