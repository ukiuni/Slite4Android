package com.ukiuni.slite.util;

import java.util.HashMap;

/**
 * Created by tito on 2015/10/11.
 */
public class StreamMap<K, V> extends HashMap<K, V> {
    public StreamMap<K, V> p(K k, V v) {
        put(k, v);
        return this;
    }
}
