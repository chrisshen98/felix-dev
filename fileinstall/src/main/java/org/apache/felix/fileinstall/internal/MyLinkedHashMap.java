package org.apache.felix.fileinstall.internal;

import java.util.LinkedHashMap;

public class MyLinkedHashMap<K,V> extends LinkedHashMap<K,V> {

    public MyLinkedHashMap() {
        super();
    }
    public MyLinkedHashMap(int size) {
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
