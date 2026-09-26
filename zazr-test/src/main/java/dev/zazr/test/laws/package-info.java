/**
 * Laws: named rules that every value of a type must satisfy, checked against a subject that provides the generator
 * of the type's values and its operations.
 * <p>
 * A {@link dev.zazr.test.laws.Law} is a named value; a {@link dev.zazr.test.laws.Laws} set
 * composes laws with {@code and}. A falsified law reports its name, the counterexample and the seed that replays
 * it. The law families are {@link dev.zazr.test.laws.MapLaws},
 * {@link dev.zazr.test.laws.FlatMapLaws}, {@link dev.zazr.test.laws.ZipLaws},
 * {@link dev.zazr.test.laws.EqualityLaws}, {@link dev.zazr.test.laws.CollectionLaws},
 * {@link dev.zazr.test.laws.BuilderLaws},
 * {@link dev.zazr.test.laws.ValidationLaws} and {@link dev.zazr.test.laws.NonEmptyVectorLaws}.
 */
package dev.zazr.test.laws;
