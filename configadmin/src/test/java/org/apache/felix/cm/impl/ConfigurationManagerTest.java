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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Dictionary;
import java.util.HashMap;
import org.apache.felix.cm.file.MyHashMap;
import java.util.HashSet;
import java.util.Hashtable;
import org.apache.felix.cm.file.MyHashtable;
import java.util.Map;
import java.util.Set;

import org.apache.felix.cm.MockBundleContext;
import org.apache.felix.cm.MockLogService;
import org.apache.felix.cm.MockNotCachablePersistenceManager;
import org.apache.felix.cm.MockPersistenceManager;
import org.apache.felix.cm.PersistenceManager;
import org.apache.felix.cm.impl.helper.ManagedServiceFactoryTracker;
import org.apache.felix.cm.impl.persistence.CachingPersistenceManagerProxy;
import org.apache.felix.cm.impl.persistence.PersistenceManagerProxy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.SynchronousConfigurationListener;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

public class ConfigurationManagerTest
{

    private PrintStream replacedStdErr;

    private ByteArrayOutputStream output;


    @Before
    public void setUp() throws Exception
    {
        replacedStdErr = System.err;

        output = new ByteArrayOutputStream();
        System.setErr( new PrintStream( output ) );
        setLogLevel(LogService.LOG_WARNING);
    }


    @After
    public void tearDown() throws Exception
    {
        System.setErr( replacedStdErr );
    }

    @Test public void test_listConfigurations_cached() throws Exception
    {
        String pid = "testDefaultPersistenceManager";

        PersistenceManager pm =new MockPersistenceManager();
        Dictionary<String, Object> dictionary = new MyHashtable<>();
        dictionary.put( "property1", "value1" );
        dictionary.put( Constants.SERVICE_PID, pid );
        pm.store( pid, dictionary );

        ConfigurationManager configMgr = new ConfigurationManager(new CachingPersistenceManagerProxy(pm), null);

        ConfigurationImpl[] conf = configMgr.listConfigurations(new ConfigurationAdminImpl(configMgr, null), null);

        assertEquals(1, conf.length);
        assertEquals(2, conf[0].getProperties(true).size());

        dictionary = new MyHashtable<>();
        dictionary.put( "property1", "value2" );
        pid = "testDefaultPersistenceManager";
        dictionary.put( Constants.SERVICE_PID, pid );
        pm.store( pid, dictionary );

        conf = configMgr.listConfigurations(new ConfigurationAdminImpl(configMgr, null), null);
        assertEquals(1, conf.length);
        assertEquals(2, conf[0].getProperties(true).size());

        // verify that the property in the configurations cache was used
        assertEquals("value1", conf[0].getProperties(true).get("property1"));
    }

    @Test public void test_listConfigurations_notcached() throws Exception
    {
        String pid = "testDefaultPersistenceManager";
        PersistenceManager pm = new MockNotCachablePersistenceManager();
        Dictionary<String, Object> dictionary = new MyHashtable<>();
        dictionary.put( "property1", "value1" );
        dictionary.put( Constants.SERVICE_PID, pid );
        pm.store( pid, dictionary );

        ConfigurationManager configMgr = new ConfigurationManager(new PersistenceManagerProxy(pm), null);

        ConfigurationImpl[] conf = configMgr.listConfigurations(new ConfigurationAdminImpl(configMgr, null), null);

        assertEquals(1, conf.length);
        assertEquals(2, conf[0].getProperties(true).size());

        dictionary = new MyHashtable<>();
        dictionary.put("property1", "valueNotCached");
        pid = "testDefaultPersistenceManager";
        dictionary.put( Constants.SERVICE_PID, pid );
        pm.store( pid, dictionary );

        conf = configMgr.listConfigurations(new ConfigurationAdminImpl(configMgr, null), null);
        assertEquals(1, conf.length);
        assertEquals(2, conf[0].getProperties(true).size());

        // verify that the value returned was not the one from the cache
        assertEquals("valueNotCached", conf[0].getProperties(true).get("property1"));
    }

    @Test public void test_listConfigurations_notcached_handlesUpdates() throws Exception
    {
        String pid = "testDefaultPersistenceManager";
        PersistenceManager pm = new MockNotCachablePersistenceManager();
        Dictionary<String, Object> dictionary = new MyHashtable<>();
        dictionary.put( "property1", "value1" );
        dictionary.put( Constants.SERVICE_PID, pid );
        pm.store( pid, dictionary );

        ConfigurationManager configMgr = new ConfigurationManager(new PersistenceManagerProxy(pm), null) {
            @Override
            void updated(ConfigurationImpl config, boolean fireEvent) {
            }
        };

        ConfigurationImpl[] conf1 = configMgr.listConfigurations(new ConfigurationAdminImpl(configMgr, null), null);

        assertEquals(1, conf1.length);
        assertEquals(2, conf1[0].getProperties(true).size());

        // internal changecount
        long revision = conf1[0].getRevision();

        dictionary = new MyHashtable<>();
        dictionary.put("property1", "valueNotCached");
        dictionary.put( Constants.SERVICE_PID, pid );
        conf1[0].update(dictionary);

        assertEquals(revision + 1, conf1[0].getRevision());

        ConfigurationImpl[] conf2 = configMgr.listConfigurations(new ConfigurationAdminImpl(configMgr, null), null);
        assertEquals(1, conf2.length);
        assertEquals(2, conf2[0].getProperties(true).size());

        assertEquals(revision + 1, conf2[0].getRevision());

        dictionary = new MyHashtable<>();
        dictionary.put("property1", "secondUpdate");
        dictionary.put( Constants.SERVICE_PID, pid );
        conf2[0].update(dictionary);

        assertEquals(revision + 2, conf2[0].getRevision());

        ConfigurationImpl[] conf3 = configMgr.listConfigurations(new ConfigurationAdminImpl(configMgr, null), null);
        assertEquals(1, conf3.length);
        assertEquals(2, conf3[0].getProperties(true).size());

        assertEquals(revision + 2, conf3[0].getRevision());
    }

    @Test public void testLogNoLogService() throws IOException
    {
        ConfigurationManager configMgr = createConfigurationManagerAndLog( null );

        setLogLevel( LogService.LOG_WARNING );
        assertNoLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertNoLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        setLogLevel( LogService.LOG_ERROR );
        assertNoLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertNoLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertNoLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        // lower than error -- no output
        setLogLevel( LogService.LOG_ERROR - 1 );
        assertNoLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertNoLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertNoLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertNoLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        // minimal log level -- no output
        setLogLevel( Integer.MIN_VALUE );
        assertNoLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertNoLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertNoLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertNoLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        setLogLevel( LogService.LOG_INFO );
        assertNoLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        setLogLevel( LogService.LOG_DEBUG );
        assertLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        // maximal log level -- all output
        setLogLevel( Integer.MAX_VALUE );
        assertLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );
    }

    // this test always expects output since when using a LogService, the log
    // level property is ignored
    @Test public void testLogWithLogService() throws IOException
    {
        LogService logService = new MockLogService();
        ConfigurationManager configMgr = createConfigurationManagerAndLog( logService );

        setLogLevel( LogService.LOG_WARNING );
        assertLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        setLogLevel( LogService.LOG_ERROR );
        assertLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        setLogLevel( LogService.LOG_ERROR - 1 );
        assertLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        setLogLevel( Integer.MIN_VALUE );
        assertLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        setLogLevel( LogService.LOG_INFO );
        assertLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        setLogLevel( LogService.LOG_DEBUG );
        assertLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        setLogLevel( Integer.MAX_VALUE );
        assertLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );
    }


    @Test public void testLogSetup() throws IOException
    {
        final MockBundleContext bundleContext = new MockBundleContext();
        createConfigurationManagerAndLog( null );

        // ensure the configuration data goes to target
        bundleContext.setProperty( "felix.cm.dir", "target/config" );

        // default value is 2
        bundleContext.setProperty( "felix.cm.loglevel", null );
        Log.logger.start( bundleContext );
        assertEquals( 2, getLogLevel( ) );
        Log.logger.stop( );

        // illegal number yields default value
        bundleContext.setProperty( "felix.cm.loglevel", "not-a-number" );
        Log.logger.start( bundleContext );
        assertEquals( 2, getLogLevel( ) );
        Log.logger.stop( );

        bundleContext.setProperty( "felix.cm.loglevel", "-100" );
        Log.logger.start( bundleContext );
        assertEquals( -100, getLogLevel( ) );
        Log.logger.stop( );

        bundleContext.setProperty( "felix.cm.loglevel", "4" );
        Log.logger.start( bundleContext );
        assertEquals( 4, getLogLevel( ) );
        Log.logger.stop( );
    }


    @Test public void testEventsStartingBundle() throws Exception
    {
        final Set<String> result = new HashSet<>();

        SynchronousConfigurationListener syncListener1 = new SynchronousConfigurationListener()
        {
            @Override
            public void configurationEvent(ConfigurationEvent event)
            {
                result.add("L1");
            }
        };
        SynchronousConfigurationListener syncListener2 = new SynchronousConfigurationListener()
        {
            @Override
            public void configurationEvent(ConfigurationEvent event)
            {
                result.add("L2");
            }
        };
        SynchronousConfigurationListener syncListener3 = new SynchronousConfigurationListener()
        {
            @Override
            public void configurationEvent(ConfigurationEvent event)
            {
                result.add("L3");
            }
        };

        ServiceReference mockRef = Mockito.mock( ServiceReference.class );
        ServiceRegistration mockReg = Mockito.mock( ServiceRegistration.class );
        Mockito.when( mockReg.getReference() ).thenReturn( mockRef );

        ConfigurationManager configMgr = new ConfigurationManager(new PersistenceManagerProxy(new MockPersistenceManager()), null);

        setServiceTrackerField( configMgr, "configurationListenerTracker" );
        ServiceReference[] refs =
                setServiceTrackerField( configMgr, "syncConfigurationListenerTracker",
                        syncListener1, syncListener2, syncListener3 );
        for ( int i=0; i < refs.length; i++)
        {
            Bundle mockBundle = Mockito.mock( Bundle.class );

            switch (i)
            {
            case 0:
                Mockito.when( mockBundle.getState() ).thenReturn( Bundle.ACTIVE );
                break;
            case 1:
                Mockito.when( mockBundle.getState() ).thenReturn( Bundle.STARTING );
                break;
            case 2:
                Mockito.when( mockBundle.getState() ).thenReturn( Bundle.STOPPING );
                break;
            }

            Mockito.when( refs[i].getBundle() ).thenReturn( mockBundle );
        }

        Field srField = configMgr.getClass().getDeclaredField( "configurationAdminRegistration" );
        srField.setAccessible( true );
        srField.set( configMgr, mockReg );
        Field utField = configMgr.getClass().getDeclaredField( "updateThread" );
        utField.setAccessible( true );
        utField.set( configMgr, new UpdateThread( null, "Test updater" ));

        Dictionary<String, Object> props = new MyHashtable<>();
        props.put( Constants.SERVICE_PID, "org.acme.testpid" );
        ConfigurationImpl config = new ConfigurationImpl( configMgr, new MockPersistenceManager(), props );
        configMgr.updated( config, true );

        assertEquals("Both listeners should have been called, both in the STARTING and ACTIVE state, but not in the STOPPING state",
                2, result.size());
    }

    @Test
    public void test_factoryConfigurationCleanup() throws Exception
    {
        MockNotCachablePersistenceManager pm = new MockNotCachablePersistenceManager();
        ConfigurationManager configMgr = new ConfigurationManager(new CachingPersistenceManagerProxy(pm), null);

        final Field bcField = configMgr.getClass().getDeclaredField("bundleContext");
        bcField.setAccessible(true);
        bcField.set(configMgr, new MockBundleContext());
        setServiceTrackerField( configMgr, "configurationListenerTracker" );
        setServiceTrackerField( configMgr, "syncConfigurationListenerTracker" );

        final Field mstField = configMgr.getClass().getDeclaredField("managedServiceFactoryTracker");
        mstField.setAccessible(true);
        mstField.set( configMgr, new ManagedServiceFactoryTracker(configMgr) {

            @Override
            public void open() {
            }
        });
        final Field utField = configMgr.getClass().getDeclaredField( "updateThread" );
        utField.setAccessible( true );
        utField.set( configMgr, new UpdateThread( null, "Test updater" ) {

            @Override
            void schedule(Runnable update) {
                update.run();
            }
        });

        final String factoryPid = "my.factory";
        final Dictionary<String, Object> props = new MyHashtable<>();
        props.put("hello", "world");

        final ConfigurationImpl c1 = configMgr.createFactoryConfiguration(factoryPid, null);
        c1.update(props);
        final ConfigurationImpl c2 = configMgr.createFactoryConfiguration(factoryPid, null);
        c2.update(props);
        final ConfigurationImpl c3 = configMgr.createFactoryConfiguration(factoryPid, null);
        c3.update(props);

        assertEquals(3, pm.getStored().size());

        c1.delete();
        assertEquals(2, pm.getStored().size());

        c2.delete();
        assertEquals(1, pm.getStored().size());

        c3.delete();
        assertEquals(0, pm.getStored().size());
    }

    @Test
    public void test_namedFactoryConfigurationCleanup() throws Exception {
        MockNotCachablePersistenceManager pm = new MockNotCachablePersistenceManager();
        ConfigurationManager configMgr = new ConfigurationManager(new CachingPersistenceManagerProxy(pm), null);
        final ConfigurationAdmin admin = new ConfigurationAdminImpl(configMgr, null);

        final Field activeField = configMgr.getClass().getDeclaredField("isActive");
        activeField.setAccessible(true);
        activeField.set(configMgr, Boolean.TRUE);

        setServiceTrackerField(configMgr, "configurationListenerTracker");
        setServiceTrackerField(configMgr, "syncConfigurationListenerTracker");

        final Field bcField = configMgr.getClass().getDeclaredField("bundleContext");
        bcField.setAccessible(true);
        bcField.set(configMgr, new MockBundleContext());
        setServiceTrackerField(configMgr, "configurationListenerTracker");
        setServiceTrackerField(configMgr, "syncConfigurationListenerTracker");

        final Field mstField = configMgr.getClass().getDeclaredField("managedServiceFactoryTracker");
        mstField.setAccessible(true);
        mstField.set(configMgr, new ManagedServiceFactoryTracker(configMgr) {

            @Override
            public void open() {
            }
        });
        final Field utField = configMgr.getClass().getDeclaredField("updateThread");
        utField.setAccessible(true);
        utField.set(configMgr, new UpdateThread(null, "Test updater") {

            @Override
            void schedule(Runnable update) {
                update.run();
            }
        });

        final String factoryPid = "my.factory";
        final Dictionary<String, Object> props = new MyHashtable<>();
        props.put("hello", "world");

        final Configuration c1 = admin.getFactoryConfiguration(factoryPid, "1", null);
        c1.update(props);
        final Configuration c2 = admin.getFactoryConfiguration(factoryPid, "2", null);
        c2.update(props);
        final Configuration c3 = admin.getFactoryConfiguration(factoryPid, "3", null);
        c3.update(props);

        assertEquals(3, pm.getStored().size());

        c1.delete();
        assertEquals(2, pm.getStored().size());

        c2.delete();
        assertEquals(1, pm.getStored().size());

        c3.delete();
        assertEquals(0, pm.getStored().size());
    }

    private void assertNoLog( ConfigurationManager configMgr, int level, String message, Throwable t )
    {
        try
        {
            Log.logger.log( level, message, t );
            assertTrue( "Expecting no log output", output.size() == 0 );
        }
        finally
        {
            // clear the output for future data
            output.reset();
        }
    }


    private void assertLog( ConfigurationManager configMgr, int level, String message, Throwable t )
    {
        try
        {
            Log.logger.log( level, message, t );
            assertTrue( "Expecting log output", output.size() > 0 );

            final String expectedLog = MockLogService.toMessageLine( level, message );
            final String actualLog = new String( output.toByteArray() );
            assertEquals( "Log Message not correct", expectedLog, actualLog );

        }
        finally
        {
            // clear the output for future data
            output.reset();
        }
    }


    private static void setLogLevel( int level )
    {
        final String fieldName = "logLevel";
        try
        {
            Field field = Log.class.getDeclaredField( fieldName );
            field.setAccessible( true );
            field.setInt( Log.logger, level );
        }
        catch ( Throwable ignore )
        {
            throw ( IllegalArgumentException ) new IllegalArgumentException( "Cannot set logLevel field value" )
                .initCause( ignore );
        }
    }


    private static int getLogLevel( )
    {
        final String fieldName = "logLevel";
        try
        {
            Field field = Log.class.getDeclaredField( fieldName );
            field.setAccessible( true );
            return field.getInt( Log.logger );
        }
        catch ( Throwable ignore )
        {
            throw ( IllegalArgumentException ) new IllegalArgumentException( "Cannot get logLevel field value" )
                .initCause( ignore );
        }
    }


    private static ServiceReference[] setServiceTrackerField( ConfigurationManager configMgr,
            String fieldName, Object ... services ) throws Exception
    {
        final Map<ServiceReference, Object> refMap = new MyHashMap<>();
        for ( Object svc : services )
        {
            ServiceReference sref = Mockito.mock( ServiceReference.class );
            Mockito.when( sref.getProperty( "objectClass" ) ).thenReturn(new String[] { "TestService" });
            refMap.put( sref, svc );
        }


        Field field = configMgr.getClass().getDeclaredField( fieldName );
        field.setAccessible( true );
        field.set( configMgr, new ServiceTracker( new MockBundleContext(), "", null )
        {
            @Override
            public ServiceReference[] getServiceReferences()
            {
                return refMap.keySet().toArray( new ServiceReference[0] );
            }

            @Override
            public Object getService(ServiceReference reference)
            {
                return refMap.get( reference );
            }
        } );

        return refMap.keySet().toArray(new ServiceReference[0]);
    }

    private static ConfigurationManager createConfigurationManagerAndLog( final LogService logService )
    throws IOException
    {
        final PersistenceManager pm = Mockito.mock(PersistenceManager.class);
        ConfigurationManager configMgr = new ConfigurationManager(new CachingPersistenceManagerProxy(pm), null);

        try
        {
            Field field = Log.class.getDeclaredField( "logTracker" );
            field.setAccessible( true );
            field.set( Log.logger, new ServiceTracker( new MockBundleContext(), "", null )
            {
                @Override
                public Object getService()
                {
                    return logService;
                }
            } );
        }
        catch ( Throwable ignore )
        {
            throw ( IllegalArgumentException ) new IllegalArgumentException( "Cannot set logTracker field value" )
                .initCause( ignore );
        }

        return configMgr;
    }
}
