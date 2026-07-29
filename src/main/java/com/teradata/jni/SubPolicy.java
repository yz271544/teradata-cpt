package com.teradata.jni;

public final class SubPolicy {
    int position;
    int length;

    public SubPolicy() {
    }

    public SubPolicy(int position, int length) {
        if (position < 0) {
            throw new IllegalArgumentException("position must not be negative");
        }
        if (length < 0) {
            throw new IllegalArgumentException("length must not be negative");
        }
        this.position = position;
        this.length = length;
    }

    public int getPosition() {
        return position;
    }

    public int getLength() {
        return length;
    }
}
