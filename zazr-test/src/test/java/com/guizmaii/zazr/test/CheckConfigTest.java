package com.guizmaii.zazr.test;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckConfigTest {

    private static CheckConfig from(Map<String, String> properties) {
        return CheckConfig.fromProperties(properties::get);
    }

    @Test
    void defaultsAreTheDocumentedOnes() {
        final CheckConfig config = from(Map.of());
        assertThat(config.samples()).isEqualTo(200).isEqualTo(CheckConfig.DEFAULT_SAMPLES);
        assertThat(config.size()).isEqualTo(100).isEqualTo(CheckConfig.DEFAULT_SIZE);
        assertThat(config.maxDiscards()).isEqualTo(1000).isEqualTo(CheckConfig.DEFAULT_MAX_DISCARDS);
    }

    @Test
    void defaultsDrawAFreshSeedEachTime() {
        final java.util.Set<Long> seeds = new java.util.HashSet<>();
        for (int i = 0; i < 20; i++) {
            seeds.add(from(Map.of()).seed());
        }
        assertThat(seeds).hasSizeGreaterThan(1);
    }

    @Test
    void systemPropertiesReplaceTheDefaults() {
        final CheckConfig config = from(Map.of(
                CheckConfig.SAMPLES_PROPERTY, "7",
                CheckConfig.SIZE_PROPERTY, " 3 ",
                CheckConfig.SEED_PROPERTY, "-42",
                CheckConfig.MAX_DISCARDS_PROPERTY, "0"));
        assertThat(config).isEqualTo(new CheckConfig(7, 3, -42L, 0));
    }

    @Test
    void propertyNamesAreTheDocumentedOnes() {
        assertThat(CheckConfig.SAMPLES_PROPERTY).isEqualTo("zazr.check.samples");
        assertThat(CheckConfig.SIZE_PROPERTY).isEqualTo("zazr.check.size");
        assertThat(CheckConfig.SEED_PROPERTY).isEqualTo("zazr.check.seed");
        assertThat(CheckConfig.MAX_DISCARDS_PROPERTY).isEqualTo("zazr.check.maxDiscards");
    }

    @Test
    void theSeedPropertyAcceptsTheWholeLongRange() {
        assertThat(from(Map.of(CheckConfig.SEED_PROPERTY, String.valueOf(Long.MIN_VALUE))).seed()).isEqualTo(Long.MIN_VALUE);
        assertThat(from(Map.of(CheckConfig.SEED_PROPERTY, String.valueOf(Long.MAX_VALUE))).seed()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void rejectsAPropertyThatIsNotANumber() {
        assertThatThrownBy(() -> from(Map.of(CheckConfig.SEED_PROPERTY, "abc")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("zazr.check.seed");
        assertThatThrownBy(() -> from(Map.of(CheckConfig.SAMPLES_PROPERTY, "1.5")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("zazr.check.samples");
    }

    @Test
    void rejectsANegativeOrTooLargeIntProperty() {
        assertThatThrownBy(() -> from(Map.of(CheckConfig.SIZE_PROPERTY, "-1")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("zazr.check.size");
        assertThatThrownBy(() -> from(Map.of(CheckConfig.MAX_DISCARDS_PROPERTY, "2147483648")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("zazr.check.maxDiscards");
        assertThat(from(Map.of(CheckConfig.SAMPLES_PROPERTY, "2147483647")).samples()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void readsTheSystemProperties() {
        // the system properties are not set by the build, so the defaults come back
        final CheckConfig config = CheckConfig.defaults();
        assertThat(config.samples()).isEqualTo(Integer.getInteger(CheckConfig.SAMPLES_PROPERTY, 200));
        assertThat(config.size()).isEqualTo(Integer.getInteger(CheckConfig.SIZE_PROPERTY, 100));
    }

    @Test
    void withersReplaceOneComponent() {
        final CheckConfig config = new CheckConfig(1, 2, 3L, 4);
        assertThat(config.withSamples(10)).isEqualTo(new CheckConfig(10, 2, 3L, 4));
        assertThat(config.withSize(20)).isEqualTo(new CheckConfig(1, 20, 3L, 4));
        assertThat(config.withSeed(30L)).isEqualTo(new CheckConfig(1, 2, 30L, 4));
        assertThat(config.withMaxDiscards(40)).isEqualTo(new CheckConfig(1, 2, 3L, 40));
        assertThat(config).isEqualTo(new CheckConfig(1, 2, 3L, 4));
    }

    @Test
    void acceptsZeroes() {
        assertThat(new CheckConfig(0, 0, 0L, 0)).isEqualTo(new CheckConfig(1, 1, 0L, 1).withSamples(0).withSize(0).withMaxDiscards(0));
    }

    @Test
    void rejectsNegativeComponents() {
        assertThatThrownBy(() -> new CheckConfig(-1, 0, 0L, 0)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("samples");
        assertThatThrownBy(() -> new CheckConfig(0, -1, 0L, 0)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("size");
        assertThatThrownBy(() -> new CheckConfig(0, 0, 0L, -1)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("maxDiscards");
        final CheckConfig config = new CheckConfig(1, 1, 1L, 1);
        assertThatThrownBy(() -> config.withSamples(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.withSize(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.withMaxDiscards(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
