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
package org.apache.felix.cm.impl.persistence;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import org.apache.felix.cm.file.MyHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.felix.cm.PersistenceManager;
import org.apache.felix.cm.impl.CaseInsensitiveDictionary;
import org.apache.felix.cm.impl.SimpleFilter;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;


/**
 * The <code>CachingPersistenceManagerProxy</code> adds a caching layer to the
 * underlying actual {@link PersistenceManager} implementation. All API calls
 * are also (or primarily) routed through a local cache of dictionaries indexed
 * by the <code>service.pid</code>.
 */
public class CachingPersistenceManagerProxy implements ExtPersistenceManager
{

    /** The actual PersistenceManager */
    private final PersistenceManager pm;

    /** Cached dictionaries */
    private final Map<String, CaseInsensitiveDictionary> cache = new MyHashMap<>();

    /** Protecting lock */
    private final ReadWriteLock globalLock = new ReentrantReadWriteLock();

    /**
     * Indicates whether the getDictionaries method has already been called
     * and the cache is complete with respect to the contents of the underlying
     * persistence manager.
     */
    private volatile boolean fullyLoaded;

    /** Factory configuration cache. */
    private final Map<String, Set<String>> factoryConfigCache = new MyHashMap<>();

    /**
     * Creates a new caching layer for the given actual {@link PersistenceManager}.
     * @param pm The actual {@link PersistenceManager}
     */
    public CachingPersistenceManagerProxy( final PersistenceManager pm )
    {
        this.pm = pm;
    }

    @Override
    public PersistenceManager getDelegatee()
    {
        return pm;
    }

    /**
     * Remove the configuration with the given PID. This implementation removes
     * the entry from the cache before calling the underlying persistence
     * manager.
     */
    @Override
    public void delete( final String pid ) throws IOException
    {
        Lock lock = globalLock.writeLock();
        try
        {
            lock.lock();
            final Dictionary props = cache.remove( pid );
            if ( props != null )
            {
                final String factoryPid = (String)props.get(ConfigurationAdmin.SERVICE_FACTORYPID);
                if ( factoryPid != null )
                {
                    final Set<String> factoryPids = this.factoryConfigCache.get(factoryPid);
                    if ( factoryPids != null )
                    {
                        factoryPids.remove(pid);
                        if ( factoryPids.isEmpty() )
                        {
                            this.factoryConfigCache.remove(factoryPid);
                        }
                    }
                }
            }
            pm.delete(pid);
        }
        finally
        {
            lock.unlock();
        }
    }


    /**
     * Checks whether a dictionary with the given pid exists. First checks for
     * the existence in the cache. If not in the cache the underlying
     * persistence manager is asked.
     */
    @Override
    public boolean exists( final String pid )
    {
        Lock lock = globalLock.readLock();
        try
        {
            lock.lock();
            return cache.containsKey( pid ) || ( !fullyLoaded && pm.exists( pid ) );
        }
        finally
        {
            lock.unlock();
        }
    }


    /**
     * Returns an <code>Enumeration</code> of <code>Dictionary</code> objects
     * representing the configurations stored in the underlying persistence
     * managers. The dictionaries returned are guaranteed to contain the
     * <code>service.pid</code> property.
     * <p>
     * Note, that each call to this method will return new dictionary objects.
     * That is modifying the contents of a dictionary returned from this method
     * has no influence on the dictionaries stored in the cache.
     */
    @Override
    public Enumeration getDictionaries() throws IOException
    {
        return Collections.enumeration(getDictionaries( null ));
    }

    private final CaseInsensitiveDictionary cache(final Dictionary props)
    {
        final String pid = (String) props.get( Constants.SERVICE_PID );
        CaseInsensitiveDictionary dict = null;
        if ( pid != null )
        {
            dict = cache.get(pid);
            if ( dict == null )
            {
                dict = new CaseInsensitiveDictionary(props);
                cache.put( pid, dict );
                final String factoryPid = (String)props.get(ConfigurationAdmin.SERVICE_FACTORYPID);
                if ( factoryPid != null )
                {
                    Set<String> factoryPids = this.factoryConfigCache.get(factoryPid);
                    if ( factoryPids == null )
                    {
                        factoryPids = new HashSet<>();
                        this.factoryConfigCache.put(factoryPid, factoryPids);
                    }
                    factoryPids.add(pid);
                }
            }
        }
        return dict;
    }

    @Override
    public Collection<Dictionary> getDictionaries( final SimpleFilter filter ) throws IOException
    {
        Lock lock = globalLock.readLock();
        try
        {
            lock.lock();
            // if not fully loaded, call back to the underlying persistence
            // manager and cache all dictionaries whose service.pid is set
            if ( !fullyLoaded )
            {
                lock.unlock();
                lock = globalLock.writeLock();
                lock.lock();
                if ( !fullyLoaded )
                {
                    Enumeration fromPm = pm.getDictionaries();
                    while ( fromPm.hasMoreElements() )
                    {
                        Dictionary next = (Dictionary) fromPm.nextElement();
                        this.cache(next);
                    }
                    this.fullyLoaded = true;
                }
            }

            // Deep copy the configuration to avoid any threading issue
            final List<Dictionary> configs = new ArrayList<>();
            for (final Dictionary d : cache.values())
            {
                if ( d.get( Constants.SERVICE_PID ) != null && ( filter == null || filter.matches( d ) ) )
                {
                    configs.add( new CaseInsensitiveDictionary( d ) );
                }
            }
            return configs;
        }
        finally
        {
            lock.unlock();
        }
    }


    /**
     * Returns the dictionary for the given PID or <code>null</code> if no
     * such dictionary is stored by the underlying persistence manager. This
     * method caches the returned dictionary for future use after retrieving
     * if from the persistence manager.
     * <p>
     * Note, that each call to this method will return new dictionary instance.
     * That is modifying the contents of a dictionary returned from this method
     * has no influence on the dictionaries stored in the cache.
     */
    @Override
    public Dictionary load( final String pid ) throws IOException
    {
        Lock lock = globalLock.readLock();
        try
        {
            lock.lock();
            CaseInsensitiveDictionary loaded = cache.get( pid );
            if ( loaded == null && !fullyLoaded )
            {
                lock.unlock();
                lock = globalLock.writeLock();
                lock.lock();
                loaded = cache.get( pid );
                if ( loaded == null )
                {
                    final Dictionary props = pm.load( pid );
                    if ( props != null )
                    {
                        loaded = this.cache(props);
                    }
                }
            }
            return loaded == null ? null : new CaseInsensitiveDictionary(loaded);
        }
        finally
        {
            lock.unlock();
        }
    }


    /**
     * Stores the dictionary in the cache and in the underlying persistence
     * manager. This method first calls the underlying persistence manager
     * before updating the dictionary in the cache.
     * <p>
     * Note, that actually a copy of the dictionary is stored in the cache. That
     * is subsequent modification to the given dictionary has no influence on
     * the cached data.
     */
    @Override
    public void store( final String pid, final Dictionary properties ) throws IOException
    {
        final Lock lock = globalLock.writeLock();
        try
        {
            lock.lock();
            pm.store( pid, properties );
            this.cache.remove(pid);
            this.cache(properties);
        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    public Set<String> getFactoryConfigurationPids(final List<String> targetedFactoryPids )
    throws IOException
    {
        final Set<String> pids = new HashSet<>();
        Lock lock = globalLock.readLock();
        try
        {
            lock.lock();
            if ( !this.fullyLoaded )
            {
                lock.unlock();
                lock = globalLock.writeLock();
                lock.lock();
                if ( !this.fullyLoaded )
                {
                    final Enumeration fromPm = pm.getDictionaries();
                    while ( fromPm.hasMoreElements() )
                    {
                        Dictionary next = (Dictionary) fromPm.nextElement();
                        this.cache(next);
                    }
                    this.fullyLoaded = true;
                }
                lock.unlock();
                lock = globalLock.readLock();
                lock.lock();
            }
            for(final String targetFactoryPid : targetedFactoryPids)
            {
                final Set<String> cachedPids = this.factoryConfigCache.get(targetFactoryPid);
                if ( cachedPids != null )
                {
                    pids.addAll(cachedPids);
                }
            }
        }
        finally
        {
            lock.unlock();
        }
        return pids;
    }
}
