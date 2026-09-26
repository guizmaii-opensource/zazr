/**
 * Property checks for Zazr: {@link dev.zazr.test.Gen} generates values, {@link dev.zazr.test.Check} checks a property
 * against them, and {@link dev.zazr.test.laws} states the laws a type must satisfy.
 */
module dev.zazr.test {
    requires transitive dev.zazr;

    exports dev.zazr.test;
    exports dev.zazr.test.laws;
}
