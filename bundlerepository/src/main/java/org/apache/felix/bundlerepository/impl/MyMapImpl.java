package org.apache.felix.bundlerepository.impl;

// import java.util.Map;
import java.util.HashMap;

public class MyMapImpl<K,V> extends HashMap<K,V> {

    public String getStackTrace() {
        String stackTrace = " ";
        for (StackTraceElement elem: Thread.currentThread().getStackTrace()) {
            stackTrace = stackTrace.concat(elem.getClassName() + "\t");
        }
        return stackTrace;
    } 

    @Override
    public V put(K key, V value) {
        System.out.println("[CTEST][SET-PARAM] " + (String) key + getStackTrace());
        return super.put(key, value);
    }
    
}
