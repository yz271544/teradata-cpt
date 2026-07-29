package com.teradata.jni;

public enum CptMode {
    CHAR((byte) 0),
    DIGIT((byte) 1),
    VISIBLE_ASCII((byte) 2);

    private final byte value;

    CptMode(byte value) {
        this.value = value;
    }

    byte value() {
        return value;
    }
}
