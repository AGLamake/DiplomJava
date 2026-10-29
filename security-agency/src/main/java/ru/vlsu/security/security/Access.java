package ru.vlsu.security.security;

public enum Access {
    NONE, READ, WRITE;

    public boolean canRead() {
        return this != NONE;
    }

    public boolean canWrite() {
        return this == WRITE;
    }
}
