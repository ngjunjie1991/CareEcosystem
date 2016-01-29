package com.ucsf.core.data;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Map;

/**
 * JSON interface to convert a list of object to a valid JSON object or array.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public abstract class JSON {
    public  static final Delegate DefaultDelegate = new Delegate();
    private static final String   TAG             = "ucsf:JSON";

    /**
     * Creates a JSON object using the given entries.
     * @param delegate  Delegate defining how to parse the given entries (see {@link JSON.Delegate}).
     * @param entries   Entries to put in the JSON object.
     */
    public static JSONObject create(Delegate delegate, Entry... entries) {
        JSONObject jsonObject = new JSONObject();
        for (Entry entry : entries) {
            if (entry.value != null)
                delegate.insert(entry.tag, entry.value, jsonObject);
        }
        return jsonObject;
    }

    /**
     * Creates a JSON object using the given entries. The default delegate is used to parse those
     * entries.
     * @param entries   Entries to put in the JSON object.
     */
    public static JSONObject create(Entry... entries) {
        return create(DefaultDelegate, entries);
    }

    /**
     * Creates a JSON array using the given objects.
     * @param delegate  Delegate defining how to parse the given entries (see {@link JSON.Delegate}).
     * @param objects   Entries to put in the JSON array.
     */
    public static JSONArray create(Collection<?> objects, Delegate delegate) {
        JSONArray jsonArray = new JSONArray();
        for (Object value : objects) {
            if (value != null)
                delegate.insert(value, jsonArray);
        }
        return jsonArray;
    }

    /**
     * Creates a JSON array using the given objects. The default delegate is used to parse those
     * objects.
     * @param objects   Entries to put in the JSON array.
     */
    public static JSONArray create(Collection<?> objects) {
        return create(objects, DefaultDelegate);
    }

    /**
     * Creates a JSON object using the given bundle object.
     * @param delegate  Delegate defining how to parse the given entries (see {@link JSON.Delegate}).
     * @param bundle    Map of pairs key/values to insert into the JSON object.
     */
    public static JSONObject create(Bundle bundle, Delegate delegate) {
        JSONObject jsonObject = new JSONObject();
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            if (value != null)
                delegate.insert(key, value, jsonObject);
        }
        return jsonObject;
    }

    /**
     * Creates a JSON object using the given bundle object. The default delegate is used to parse
     * the bundle entries.
     * @param bundle    Map of pairs key/values to insert into the JSON object.
     */
    public static JSONObject create(Bundle bundle) {
        return create(bundle, DefaultDelegate);
    }

    /**
     * Creates a JSON object using the given map.
     * @param delegate  Delegate defining how to parse the given entries (see {@link JSON.Delegate}).
     * @param objects   Map of pairs key/values to insert into the JSON object.
     */
    public static JSONObject create(Map<String, ?> objects, Delegate delegate) {
        final JSONObject jsonObject = new JSONObject();
        for (Map.Entry<String, ?> entry : objects.entrySet()) {
            Object value = entry.getValue();
            if (value != null)
                delegate.insert(entry.getKey(), value, jsonObject);
        }
        return jsonObject;
    }

    /**
     * Creates a JSON object using the given map. The default delegate is used to parse
     * the map entries.
     * @param objects    Map of pairs key/values to insert into the JSON object.
     */
    public static JSONObject create(Map<String, ?> objects) {
        return create(objects, DefaultDelegate);
    }

    /**
     * Class describing how to parse objects in order to insert them into a JSON object or array.
     * By default, a collection of object is converted to a JSON array and collection of pairs
     * key/value such as a map or a bundle is converted to a JSON object.
     */
    public static class Delegate {
        /**
         * Parses the given object.
         * @return Returns a JSON array if the given value is a collection of objects, a JSON object
         *         if the given value is a collection of pairs key/value, and the object itself
         *         otherwise.
         */
        public Object wrap(Object value) throws Exception {
            if (value instanceof Bundle)
                return create((Bundle) value, this);
            if (value instanceof Collection<?>)
                return create((Collection<?>) value, this);
            else if (value instanceof Map<?, ?>)
                return create((Map<String, ?>) value, this);
            return value;
        }

        /**
         * Inserts the given pair key/value in the given JSON object.
         * Calls {@link JSON.Delegate#wrap(Object)}.
         */
        final void insert(String key, @NonNull Object value, JSONObject jsonObject) {
            try {
                jsonObject.put(key, wrap(value));
            } catch (Exception e) {
                Log.e(TAG, "Failed to save value: ", e);
            }
        }

        /**
         * Inserts the given pair key/value in the given JSON array.
         * Calls {@link JSON.Delegate#wrap(Object)}.
         */
        final void insert(@NonNull Object value, JSONArray jsonArray) {
            try {
                jsonArray.put(wrap(value));
            } catch (Exception e) {
                Log.e(TAG, "Failed to save value: ", e);
            }
        }
    }
}
