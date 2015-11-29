package com.ukiuni.slite.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by tito on 2015/10/11.
 */
public class StreamMap<K, V> extends HashMap<K, V> {
    public StreamMap<K, V> p(K k, V v) {
        put(k, v);
        return this;
    }

    public String toJSON() {
        try {
            JSONObject obj = new JSONObject();
            for (K key : this.keySet()) {
                V value = this.get(key);
                if (value instanceof StreamMap) {
                    obj.put((String) key, ((StreamMap) value).toJSON());
                } else {
                    obj.put((String) key, value);
                }
            }
            return obj.toString(0);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
