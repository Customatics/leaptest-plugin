package com.customatics.leaptest;


public class MutableBoolean {
    private boolean value;

    public MutableBoolean(boolean value)
    {
        this.value = value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }
}
