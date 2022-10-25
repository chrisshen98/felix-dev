// package org.apache.felix.cm.impl;

// import java.util.Dictionary;

// public class MyDictionary<K,V> extends Dictionary<K,V> {

//     public String getStackTrace() {
//         String stackTrace = " ";
//         for (StackTraceElement elem: Thread.currentThread().getStackTrace()) {
//             stackTrace = stackTrace.concat(elem.getClassName() + "\t");
//         }
//         return stackTrace;
//     } 

//     @Override
//     public V get(K key) {
//         System.out.println("[CTEST][GET-PARAM] " + (String) key + getStackTrace());
//         return super.get(key);
//     }
    
// }
