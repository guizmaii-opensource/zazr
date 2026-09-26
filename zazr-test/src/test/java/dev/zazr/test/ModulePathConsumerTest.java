package dev.zazr.test;

import java.io.File;
import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * zazr-test on the module path, as a consumer sees it: a module that {@code requires dev.zazr.test} is compiled by
 * {@code javac} and run by {@code java}, both on a module path holding the two Zazr modules only. It runs a check and
 * reads {@code dev.zazr} through the transitive dependency.
 */
class ModulePathConsumerTest {

    /// The directory or jar a class was loaded from: the compiled module of zazr-test or of zazr-core.
    private static Path location(Class<?> type) throws URISyntaxException {
        return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    /// A tool of the running JDK, such as `javac` or `java`.
    private static String tool(String name) {
        final Path java = Path.of(ProcessHandle.current().info().command().orElseThrow());
        return java.resolveSibling(name).toString();
    }

    /// Runs a command, and returns its exit code followed by its output.
    private static String run(List<String> command) throws IOException, InterruptedException {
        final Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(process.waitFor(2, TimeUnit.MINUTES)).isTrue();
        return process.exitValue() + "\n" + output;
    }

    @Test
    void theModuleIsNamedAndExportsItsApi() throws URISyntaxException {
        final ModuleDescriptor descriptor = ModuleFinder.of(location(Gen.class)).find("dev.zazr.test").orElseThrow().descriptor();
        assertThat(descriptor.isAutomatic()).isFalse();
        assertThat(descriptor.exports()).extracting(ModuleDescriptor.Exports::source)
                .containsExactlyInAnyOrder("dev.zazr.test", "dev.zazr.test.laws");
        assertThat(descriptor.requires()).anySatisfy(requires -> {
            assertThat(requires.name()).isEqualTo("dev.zazr");
            assertThat(requires.modifiers()).contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE);
        });
    }

    @Test
    void aConsumerModuleCompilesAndRunsACheck(@TempDir Path dir) throws Exception {
        final Path sources = dir.resolve("src");
        Files.createDirectories(sources.resolve("consumer"));
        Files.writeString(sources.resolve("module-info.java"), """
                module consumer {
                    requires dev.zazr.test;
                }
                """);
        Files.writeString(sources.resolve("consumer/Main.java"), """
                package consumer;

                import dev.zazr.collection.Vector;
                import dev.zazr.test.Check;
                import dev.zazr.test.CheckConfig;
                import dev.zazr.test.Gen;
                import dev.zazr.test.laws.MapLaws;

                public final class Main {
                    public static void main(String[] args) {
                        Check.check(Gen.integers(), n -> n == n);
                        System.out.println(Check.evaluate(CheckConfig.defaults().withSeed(1), Gen.vector(Gen.integers()),
                                v -> v.reverse().reverse().equals(v)));
                        System.out.println(Vector.of(1, 2) + " " + MapLaws.all());
                    }
                }
                """);
        final String modulePath = location(Gen.class) + File.pathSeparator + location(dev.zazr.Tuple.class);
        final Path classes = dir.resolve("classes");
        final List<String> javac = new ArrayList<>(List.of(tool("javac"), "--module-path", modulePath, "-d", classes.toString(),
                sources.resolve("module-info.java").toString(), sources.resolve("consumer/Main.java").toString()));
        assertThat(run(javac)).startsWith("0\n");
        final String output = run(List.of(tool("java"), "--module-path", classes + File.pathSeparator + modulePath,
                "--module", "consumer/consumer.Main"));
        assertThat(output).isEqualTo("0\nSatisfied[samples=200]\nVector(1, 2) Laws(mapIdentity, mapComposition)\n");
    }
}
