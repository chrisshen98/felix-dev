// package org.apache.felix.cm.file;

// import java.util.HashSet;

// public class MyHashSet<E> extends HashSet<E> {

//     public String getStackTrace() {
//         String stackTrace = " ";
//         for (StackTraceElement elem: Thread.currentThread().getStackTrace()) {
//             stackTrace = stackTrace.concat(elem.getClassName() + "\t");
//         }
//         return stackTrace;
//     } 

//     @Override
//     public boolean add(E e) {
//         System.out.println("[CTEST][SET-PARAM] " + key.toString() + getStackTrace());
//         return super.put(key, value);
//     }

//     @Override
//     public V get(Object key) {
//         System.out.println("[CTEST][GET-PARAM] " + key.toString() + getStackTrace());
//         return super.get(key);
//     }
// }
