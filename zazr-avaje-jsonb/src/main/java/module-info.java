/**
 * JSON adapters for the Zazr collections, {@code Option} and the tuples in avaje-jsonb. Adding this module is enough:
 * avaje-jsonb finds {@link dev.zazr.avaje.jsonb.ZazrJsonbComponent} as a service when it builds a {@code Jsonb}.
 */
module dev.zazr.avaje.jsonb {
    requires transitive dev.zazr;
    requires transitive io.avaje.jsonb;
    requires static org.jspecify;

    exports dev.zazr.avaje.jsonb;

    provides io.avaje.jsonb.spi.JsonbExtension with
            dev.zazr.avaje.jsonb.ZazrJsonbComponent;
}
