/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.bundlerepository.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.namespace.service.ServiceNamespace;

class NamespaceTranslator
{
    private static final Map<String, String> osgiToFelixMap = fillOSGiToFelixMap();
    private static final Map<String, String> felixToOSGiMap = fillFelixToOSGiMap();

    private static Map<String, String> fillOSGiToFelixMap()
    {
        Map<String, String> result = new HashMap<String, String>(4);
        result.put(PackageNamespace.PACKAGE_NAMESPACE, org.apache.felix.bundlerepository.Capability.PACKAGE);
        result.put(ServiceNamespace.SERVICE_NAMESPACE, org.apache.felix.bundlerepository.Capability.SERVICE);
        result.put(BundleNamespace.BUNDLE_NAMESPACE, org.apache.felix.bundlerepository.Capability.BUNDLE);
        result.put(HostNamespace.HOST_NAMESPACE, org.apache.felix.bundlerepository.Capability.FRAGMENT);
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, String> fillFelixToOSGiMap()
    {
        Map<String, String> result = new HashMap<String, String>(4);
        result.put(org.apache.felix.bundlerepository.Capability.PACKAGE, PackageNamespace.PACKAGE_NAMESPACE);
        result.put(org.apache.felix.bundlerepository.Capability.SERVICE, ServiceNamespace.SERVICE_NAMESPACE);
        result.put(org.apache.felix.bundlerepository.Capability.BUNDLE, BundleNamespace.BUNDLE_NAMESPACE);
        result.put(org.apache.felix.bundlerepository.Capability.FRAGMENT, HostNamespace.HOST_NAMESPACE);
        return Collections.unmodifiableMap(result);
    }

    public static String getStackTrace() {
        String stackTrace = " ";
        for (StackTraceElement elem: Thread.currentThread().getStackTrace()) {
            stackTrace = stackTrace.concat(elem.getClassName() + "\t");
        }
        return stackTrace;
    }

    public static String getFelixNamespace(String osgiNamespace)
    {
        String result = osgiToFelixMap.get(osgiNamespace);
        if (result == null) {
            System.out.println("[CTEST][GET-PARAM] " + osgiNamespace + getStackTrace());
            return osgiNamespace;
        } else {
            System.out.println("[CTEST][GET-PARAM] " + result + getStackTrace());
            return result;
        }
    }

    public static Collection<String> getTranslatedFelixNamespaces()
    {
        for (String key:felixToOSGiMap.keySet()) {
            System.out.println("[CTEST][GET-PARAM] " + key + getStackTrace());
        }
        return felixToOSGiMap.keySet();
    }

    public static String getOSGiNamespace(String felixNamespace)
    {
        String result = felixToOSGiMap.get(felixNamespace);
        if (result == null) {
            System.out.println("[CTEST][GET-PARAM] " + felixNamespace + getStackTrace());
            return felixNamespace;
        }
        else {
            System.out.println("[CTEST][GET-PARAM] " + result + getStackTrace());
            return result;
        }
    }

    public static Collection<String> getTranslatedOSGiNamespaces()
    {
        for (String key:osgiToFelixMap.keySet()) {
            System.out.println("[CTEST][GET-PARAM] " + key + getStackTrace());
        }
        return osgiToFelixMap.keySet();
    }
}
