package org.apache.felix.coordinator.impl;

import java.util.HashMap;

public class MyHashMap<K,V> extends HashMap<K,V> {

    public MyHashMap() {
        super();
    }
    public MyHashMap(int size) {
        super(size);
    }
    public String getStackTrace() {
        String stackTrace = " ";
        for (StackTraceElement elem: Thread.currentThread().getStackTrace()) {
            stackTrace = stackTrace.concat(elem.getClassName() + "\t");
        }
        return stackTrace;
    } 

    @Override
    public V put(K key, V value) {
        System.out.println("[CTEST][SET-PARAM] " + key.toString() + getStackTrace());
        return super.put(key, value);
    }

    @Override
    public V get(Object key) {
        System.out.println("[CTEST][GET-PARAM] " + key.toString() + getStackTrace());
        return super.get(key);
    }

}
