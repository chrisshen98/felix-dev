package org.apache.felix.feature.impl;

import java.util.IdentityHashMap;

public class MyIdentityHashMap<K,V> extends IdentityHashMap<K,V> {

    public MyIdentityHashMap() {
        super();
    }
    public MyIdentityHashMap(int size) {
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
        // System.out.println("[CTEST][SET-PARAM] " + (String) key + getStackTrace());
        return super.put(key, value);
    }

    @Override
    public V get(Object key) {
        System.out.println("[CTEST][GET-PARAM] " + key.toString() + getStackTrace());
        // System.out.println("[CTEST][GET-PARAM] " + (String) key + getStackTrace());
        return super.get(key);
    }

}
