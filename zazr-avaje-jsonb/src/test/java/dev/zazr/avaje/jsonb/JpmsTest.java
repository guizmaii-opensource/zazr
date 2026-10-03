package dev.zazr.avaje.jsonb;

import dev.zazr.collection.Vector;
import io.avaje.json.JsonAdapter;
import io.avaje.jsonb.Jsonb;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ResolvedModule;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// On the module path: the `dev.zazr.avaje.jsonb` module provides the component to avaje-jsonb, exports only its API,
/// and is found by avaje-jsonb in a module layer that no module `requires`, as in an application whose
/// `module-info.java` names only its own dependencies.
class JpmsTest {

    private static final String MODULE = "dev.zazr.avaje.jsonb";

    /// The modules of the layer: this module, `dev.zazr`, and avaje-jsonb's two modules.
    private static ModuleFinder finder() throws URISyntaxException {
        return ModuleFinder.of(
                location(ZazrJsonbComponent.class),
                location(Vector.class),
                location(Jsonb.class),
                location(JsonAdapter.class));
    }

    private static Path location(Class<?> type) throws URISyntaxException {
        return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    @Test
    void theModuleProvidesTheComponentAndExportsOnlyItsApi() throws URISyntaxException {
        ModuleDescriptor descriptor = finder().find(MODULE).orElseThrow().descriptor();
        assertThat(descriptor.provides()).singleElement().satisfies(provides -> {
            assertThat(provides.service()).isEqualTo("io.avaje.jsonb.spi.JsonbExtension");
            assertThat(provides.providers()).containsExactly(ZazrJsonbComponent.class.getName());
        });
        assertThat(descriptor.exports()).map(ModuleDescriptor.Exports::source).containsExactly("dev.zazr.avaje.jsonb");
    }

    @Test
    void avajeJsonbFindsTheComponentInALayerThatNoModuleRequiresItFrom() throws Exception {
        Configuration configuration = ModuleLayer.boot()
                .configuration()
                .resolveAndBind(finder(), ModuleFinder.of(), Set.of("io.avaje.jsonb"));
        assertThat(configuration.modules()).map(ResolvedModule::name).contains(MODULE, "dev.zazr");

        var layer = ModuleLayer.boot().defineModulesWithOneLoader(configuration, ClassLoader.getPlatformClassLoader());
        var loader = layer.findLoader("io.avaje.jsonb");
        var thread = Thread.currentThread();
        var previous = thread.getContextClassLoader();
        // avaje-jsonb loads its services with the context class loader, the application's loader in an application
        thread.setContextClassLoader(loader);
        try {
            var jsonbType = loader.loadClass("io.avaje.jsonb.Jsonb");
            var jsonb = jsonbType.getMethod("instance").invoke(null);
            var vectorType = loader.loadClass("dev.zazr.collection.Vector");
            var vector = vectorType.getMethod("of", Object[].class).invoke(null, (Object) new Object[] {1, 2});
            assertThat(jsonbType.getMethod("toJson", Object.class).invoke(jsonb, vector))
                    .isEqualTo("[1,2]");
            assertThat(vectorType.getModule().getLayer()).isSameAs(layer);
        } finally {
            thread.setContextClassLoader(previous);
        }
    }
}
