package com.cleverpush.mapper;

public interface Mapper<K, V> {
    public K toKey(V value);

    public V toValue(K key);
}
