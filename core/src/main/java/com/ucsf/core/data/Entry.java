package com.ucsf.core.data;

import android.os.Bundle;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Simple key/value object
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class Entry {
    private static final String TAG = "ucsf:Entry";

    public final String tag;    /**< Entry key. */
    public final Object value;  /**< Entry value. */

    public Entry(String tag, Serializable value) {
        this.tag   = tag;
        this.value = value;
    }

    public Entry(String tag, Bundle value) {
        this.tag   = tag;
        this.value = value;
    }

    /**
     * Creates an entry with the given tag and value. If the flag toByteArray is set to true,
     * the value is first converted to a byte array. It is used for value types which cannot be
     * cast to/from string, in order to store them into a database.
     */
    public Entry(String tag, Serializable value, boolean toByteArray) {
        this.tag = tag;
        if (toByteArray) {
            Serializable v;
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(value);
                v = bos.toByteArray();
            } catch (Exception e) {
                Log.e(TAG, "Failed to write serializable object: ", e);
                v = new byte[0];
            }
            this.value = v;
        } else
            this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || !(o instanceof Entry))
            return false;

        Entry entry = (Entry) o;
        return !(tag != null ? !tag.equals(entry.tag) : entry.tag != null) &&
                !(value != null ? !value.equals(entry.value) : entry.value != null);

    }

    @Override
    public int hashCode() {
        return (tag != null ? 1013 * tag.hashCode() : 0) +
                (value != null ? 1009 * value.hashCode() : 0);
    }
}