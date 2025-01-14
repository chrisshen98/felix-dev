/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.cm.impl;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import org.apache.felix.cm.file.MyHashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.felix.cm.PersistenceManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;


class DynamicBindings
{

    static final String BINDINGS_FILE_NAME = "org_apache_felix_cm_impl_DynamicBindings";

    private final PersistenceManager persistenceManager;

    private final Dictionary<String, String> bindings;

    @SuppressWarnings("unchecked")
    DynamicBindings( BundleContext bundleContext, PersistenceManager persistenceManager ) throws IOException
    {
        this.persistenceManager = persistenceManager;

        if ( persistenceManager.exists( BINDINGS_FILE_NAME ) )
        {
            this.bindings = persistenceManager.load( BINDINGS_FILE_NAME );

            // get locations of installed bundles to validate the bindings
            final Set<String> locations = new HashSet<>();
            final Bundle[] bundles = bundleContext.getBundles();
            for ( int i = 0; i < bundles.length; i++ )
            {
                locations.add( Activator.getLocation(bundles[i]) );
            }

            // collect pids whose location is not installed any more
            List<String> removedKeys = new ArrayList<>();
            for (Enumeration<String> ke = bindings.keys(); ke.hasMoreElements();)
            {
                final String pid = ke.nextElement();
                final String location = bindings.get( pid );
                if ( !locations.contains( location ) )
                {
                    removedKeys.add( pid );
                }
            }

            // if some bindings had to be removed, store the mapping again
            if ( removedKeys.size() > 0 )
            {
                // remove invalid mappings
                for (Iterator<String> rki = removedKeys.iterator(); rki.hasNext();)
                {
                    bindings.remove( rki.next() );
                }

                // store the modified map
                persistenceManager.store( BINDINGS_FILE_NAME, bindings );
            }
        }
        else
        {
            this.bindings = new MyHashtable<>();
        }

    }


    String getLocation( final String pid )
    {
        synchronized ( this )
        {
            return this.bindings.get( pid );
        }
    }


    void putLocation( final String pid, final String location ) throws IOException
    {
        synchronized ( this )
        {
            if ( location == null )
            {
                this.bindings.remove( pid );
            }
            else
            {
                this.bindings.put( pid, location );
            }

            if (this.bindings.isEmpty()) {
                this.persistenceManager.delete(BINDINGS_FILE_NAME);
            } else {
                this.persistenceManager.store(BINDINGS_FILE_NAME, bindings);
            }
        }
    }
}
