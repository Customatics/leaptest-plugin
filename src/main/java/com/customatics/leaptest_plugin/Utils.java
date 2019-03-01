package com.customatics.leaptest_plugin;

import com.google.gson.JsonElement;

import java.util.Map;
import java.util.UUID;

/**
 * Created by User on 26.05.2017.
 */
public class Utils {

    public static String defaultStringIfNull(JsonElement jsonElement)
    {

        if(jsonElement != null)
        {
            try
            {
                return jsonElement.getAsString();
            }
            catch (Exception e)
            {
                return "";
            }
        }
        else
            return "";
    }

    public static String defaultStringIfNull(JsonElement jsonElement, String defaultValue)
    {

        if(jsonElement != null)
        {
            try
            {
                return jsonElement.getAsString();
            }
            catch (Exception e)
            {
                return defaultValue;
            }
        }
        else
            return defaultValue;
    }

    public static UUID defaultUuidIfNull(JsonElement jsonElement, UUID defaultValue)
    {

        if(jsonElement != null)
        {
            try
            {
                return UUID.fromString(jsonElement.getAsString());
            } catch (Exception e)
            {
                return defaultValue;
            }
        }
        else
            return defaultValue;
    }

    public static int defaultIntIfNull(JsonElement jsonElement, int defaultValue)
    {

        if(jsonElement != null)
        {
            try
            {
                return jsonElement.getAsInt();
            }
            catch (Exception e)
            {
                return defaultValue;
            }
        }
        else
            return defaultValue;
    }

    public static long defaultLongIfNull(JsonElement jsonElement, long defaultValue)
    {

        if(jsonElement != null)
        {
            try
            {
                return jsonElement.getAsLong();
            }
            catch (Exception e)
            {
                return defaultValue;
            }
        }
        else
            return defaultValue;
    }

    public static String defaultElapsedIfNull(JsonElement rawElapsed)
    {
        if(rawElapsed != null)
        {
            try
            {
                return rawElapsed.getAsString();
            }
            catch (Exception e)
            {
                return "00:00:00.0000000";
            }
        }

        else
            return "00:00:00.0000000";
    }

    public static double defaultDoubleIfNull(JsonElement jsonElement, double defaultValue)
    {

        if(jsonElement != null)
        {
            try
            {
                return jsonElement.getAsDouble();
            }
            catch (Exception e)
            {
                return defaultValue;
            }
        }
        else
            return defaultValue;
    }

    public static boolean defaultBooleanIfNull(JsonElement rawBoolean, boolean defaultValue)
    {
        if(rawBoolean != null)
        {
            try
            {
                return rawBoolean.getAsBoolean();
            }
            catch (Exception e)
            {
                return defaultValue;
            }
        }
        else
            return defaultValue;
    }

    public static  <TKey,TValue> boolean tryAddToMap(Map<TKey,TValue> map, TKey key, TValue value)
    {
        if(map.get(key) != null)
            return false;
        else
        {
            map.put(key,value);
            return true;
        }
    }

    public static boolean isBlank(String str)
    {
        if(str != null && str.trim().isEmpty() == false)
            return false;
        return true;
    }
}
