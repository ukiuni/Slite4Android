package com.ukiuni.slite.util;

/**
 * Created by tito on 2015/10/11.
 */
public class SS {
    public static <K, V> StreamMap<K, V> map(K k, V v) {
        StreamMap map = new StreamMap<K, V>();
        map.p(k, v);
        return map;
    }

}
