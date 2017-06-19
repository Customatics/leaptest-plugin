package com.customatics.leaptest_plugin.model;

/**
 * Created by User on 11.06.2017.
 */
public final class InvalidSchedule {

    private String name;
    private String stackTrace;

    public InvalidSchedule(String name, String stackTrace)
    {
        this.name = name;
        this.stackTrace = stackTrace;
    }

    public String getName() {
        return name;
    }

    public String getStackTrace() {
        return stackTrace;
    }
}
