package org.apache.felix.cm.json.impl;

import javax.json.JsonObject;
// import javax.json.Json;
import javax.json.JsonObjectBuilder;

public class MyJsonObjectBuilderImpl {
    
    private JsonObjectBuilder parent;

    public MyJsonObjectBuilderImpl(JsonObjectBuilder toSave) {
        parent = toSave;
    }

    public String getStackTrace() {
        String stackTrace = " ";
        for (StackTraceElement elem: Thread.currentThread().getStackTrace()) {
            stackTrace = stackTrace.concat(elem.getClassName() + "\t");
        }
        return stackTrace;
    } 

    public MyJsonObjectBuilderImpl add(String name, String value) {
        System.out.println("[CTEST][SET-PARAM] " + name + getStackTrace());
        parent.add(name, value);
        return this;
    } 

    public MyJsonObjectBuilderImpl add(String name, int value) {
        System.out.println("[CTEST][SET-PARAM] " + name + getStackTrace());
        parent.add(name, value);
        return this;
    } 

    public JsonObjectBuilder getParent() {
        return parent;
    }

    public JsonObject build() {
        return parent.build();
    }

}
