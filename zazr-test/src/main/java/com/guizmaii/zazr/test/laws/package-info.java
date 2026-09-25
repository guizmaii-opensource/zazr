/**
 * Laws: named rules that every value of a type must satisfy, checked by property against a subject that provides
 * the type's arbitrary values and operations.
 * <p>
 * A {@link com.guizmaii.zazr.test.laws.Law} is a named value; a {@link com.guizmaii.zazr.test.laws.Laws} set
 * composes laws with {@code and}. A falsified law reports its name and the counterexample. The law families are
 * {@link com.guizmaii.zazr.test.laws.MapLaws}, {@link com.guizmaii.zazr.test.laws.FlatMapLaws},
 * {@link com.guizmaii.zazr.test.laws.ZipLaws}, {@link com.guizmaii.zazr.test.laws.EqualityLaws},
 * {@link com.guizmaii.zazr.test.laws.CollectionLaws}, {@link com.guizmaii.zazr.test.laws.BuilderLaws},
 * {@link com.guizmaii.zazr.test.laws.ValidationLaws} and {@link com.guizmaii.zazr.test.laws.NonEmptyVectorLaws}.
 */
package com.guizmaii.zazr.test.laws;
