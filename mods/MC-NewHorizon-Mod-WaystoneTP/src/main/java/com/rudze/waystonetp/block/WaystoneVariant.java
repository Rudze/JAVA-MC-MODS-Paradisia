package com.rudze.waystonetp.block;

public enum WaystoneVariant {
    A,
    B;

    public WaystoneVariant opposite() {
        return this == A ? B : A;
    }
}
