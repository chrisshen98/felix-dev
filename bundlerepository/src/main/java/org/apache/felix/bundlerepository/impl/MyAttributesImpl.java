package org.apache.felix.bundlerepository.impl;

import org.apache.felix.bundlerepository.MyAttributes;
import java.util.jar.Attributes;

public class MyAttributesImpl extends Attributes implements MyAttributes{

    public String getStackTrace() {
        String stackTrace = " ";
        for (StackTraceElement elem: Thread.currentThread().getStackTrace()) {
            stackTrace = stackTrace.concat(elem.getClassName() + "\t");
        }
        return stackTrace;
    } 

    @Override 
    public String putValue(String name, String value) {
        System.out.println("[CTEST][SET-PARAM] " + name + getStackTrace());
        return super.putValue(name, value);
        // return (String)put(new Attributes.Name(name), value);
    }
}
