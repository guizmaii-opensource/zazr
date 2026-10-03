/**
 * Jackson support for Zazr: {@link dev.zazr.jackson.ZazrModule} teaches a Jackson 3 {@code ObjectMapper} to read and
 * write the Zazr collections, {@code Option} and the tuples.
 */
module dev.zazr.jackson {
    requires transitive dev.zazr;
    requires transitive tools.jackson.databind;
    requires static org.jspecify;

    exports dev.zazr.jackson;

    provides tools.jackson.databind.JacksonModule with
            dev.zazr.jackson.ZazrModule;
}
