package org.apache.felix.cm.json.impl;

import javax.json.JsonArrayBuilder;
// import javax.json.JsonValue;
// import javax.json.Json;
import javax.json.JsonArray;

public class MyJsonArrayBuilderImpl{

    private JsonArrayBuilder parent;

    public MyJsonArrayBuilderImpl(JsonArrayBuilder toSave) {
        parent = toSave;
    }

    public String getStackTrace() {
        String stackTrace = " ";
        for (StackTraceElement elem: Thread.currentThread().getStackTrace()) {
            stackTrace = stackTrace.concat(elem.getClassName() + "\t");
        }
        return stackTrace;
    } 

    public MyJsonArrayBuilderImpl add(MyJsonObjectBuilderImpl value) {
        // System.out.println("[CTEST][SET-PARAM] " + value + getStackTrace());
        parent.add(value.getParent());
        return this;
    } 

    public JsonArray build() {
        return parent.build();
    }
}
