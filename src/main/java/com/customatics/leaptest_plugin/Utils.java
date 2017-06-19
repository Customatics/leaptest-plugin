package com.customatics.leaptest_plugin;

import com.google.gson.JsonElement;

/**
 * Created by User on 26.05.2017.
 */
public class Utils {

    public static String defaultStringIfNull(JsonElement jsonElement)
    {

        if(jsonElement != null)
            return jsonElement.getAsString();
        else
            return "";
    }

    public static String defaultStringIfNull(JsonElement jsonElement, String defaultValue)
    {

        if(jsonElement != null)
            return jsonElement.getAsString();
        else
            return defaultValue;
    }

    public static int defaultIntIfNull(JsonElement jsonElement, int defaultValue)
    {

        if(jsonElement != null)
            return jsonElement.getAsInt();
        else
            return defaultValue;
    }

    public static int defaultIntIfNull(JsonElement jsonElement)
    {

        if(jsonElement != null)
            return jsonElement.getAsInt();
        else
            return 0;
    }
}
