package dev.zazr.jackson;

import dev.zazr.collection.Vector;
import dev.zazr.control.Option;
import org.junit.jupiter.api.Test;
import tools.jackson.core.Version;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/// Registering the module: on the builder, or found by `findAndAddModules()`.
class ZazrModuleTest {

    @Test
    void theModuleIsNamed() {
        var module = new ZazrModule();
        assertThat(module.getModuleName()).isEqualTo("ZazrModule");
        assertThat(module.version()).isEqualTo(Version.unknownVersion());
    }

    @Test
    void findAndAddModulesFindsIt() {
        var mapper = JsonMapper.builder().findAndAddModules().build();
        assertThat(mapper.registeredModules()).anyMatch(m -> m instanceof ZazrModule);
        assertThat(mapper.writeValueAsString(Vector.of(Option.some(1), Option.none())))
                .isEqualTo("[1,null]");
    }

    @Test
    void withoutTheModuleAnOptionIsWrittenAsARecord() {
        assertThat(JsonMapper.builder().build().writeValueAsString(Option.some(1)))
                .isEqualTo("{\"value\":1,\"defined\":true,\"empty\":false,\"orNull\":1}");
        assertThat(Json.write(Option.some(1))).isEqualTo("1");
    }
}
