package com.guizmaii.zazr.test.laws;

/**
 * An element whose hash code takes four values only, so that hash sets and maps hold collision nodes.
 *
 * @param value the element's identity
 */
record Collider(int value) implements Comparable<Collider> {

    @Override
    public int hashCode() {
        return value & 3;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Collider that && that.value == value;
    }

    @Override
    public int compareTo(Collider that) {
        return Integer.compare(value, that.value);
    }

    @Override
    public String toString() {
        return "Collider(" + value + ")";
    }
}
