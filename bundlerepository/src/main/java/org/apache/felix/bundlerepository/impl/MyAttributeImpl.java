package main.java.org.apache.felix.bundlerepository;
import java.util.jar.Attributes;

public class MyAttributeImpl extends Attributes implements MyAttribute{

    public static String getStackTrace() {
        String stackTrace = " ";
        for (StackTraceElement elem: Thread.currentThread().getStackTrace()) {
            stackTrace = stackTrace.concat(elem.getClassName() + "\t");
        }
        return stackTrace;
    } 

    @Override 
    public String putValue(String name, String value) {
        System.out.println("[CTEST][SET-PARAM] " + name + getStackTrace());
        return (String)put(new Attributes.Name(name), value);
    }
}
