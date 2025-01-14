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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import org.apache.felix.cm.file.MyHashtable;

import org.apache.felix.cm.MockPersistenceManager;
import org.apache.felix.cm.PersistenceManager;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;


public class ConfigurationAdapterTest
{

    private static final String SCALAR = "scalar";
    private static final String STRING_VALUE = "String Value";
    private static final String STRING_VALUE2 = "Another String Value";

    private static final String ARRAY = "array";
    private final String[] ARRAY_VALUE;

    private static final String COLLECTION = "collection";
    private final Collection<String> COLLECTION_VALUE;

    private static final String TEST_PID = "test.pid";
    private static final String TEST_LOCATION = "test:location";

    private final PersistenceManager pm = new MockPersistenceManager();

    {
        ARRAY_VALUE = new String[]
            { STRING_VALUE };
        COLLECTION_VALUE = new ArrayList<>();
        COLLECTION_VALUE.add( STRING_VALUE );
    }


    private Configuration getConfiguration() throws IOException
    {
        final ConfigurationManager configMgr = Mockito.mock(ConfigurationManager.class);
        Mockito.when(configMgr.isActive()).thenReturn(true);

        ConfigurationImpl cimpl = new ConfigurationImpl( configMgr, pm, TEST_PID, null, TEST_LOCATION );
        return new ConfigurationAdapter( null, cimpl );
    }


    @Test public void testScalar() throws IOException
    {
        Configuration cimpl = getConfiguration();
        Dictionary<String, Object> props = cimpl.getProperties();
        assertNull( "Configuration is fresh", props );

        props = new MyHashtable<>();
        props.put( SCALAR, STRING_VALUE );
        cimpl.update( props );

        Dictionary<String, Object> newProps = cimpl.getProperties();
        assertNotNull( "Configuration is not fresh", newProps );
        assertEquals( "Expect 2 elements", 2, newProps.size() );
        assertEquals( "Service.pid must match", TEST_PID, newProps.get( Constants.SERVICE_PID ) );
        assertEquals( "Scalar value must match", STRING_VALUE, newProps.get( SCALAR ) );
    }


    @Test public void testArray() throws IOException
    {
        Configuration cimpl = getConfiguration();

        Dictionary<String, Object> props = cimpl.getProperties();
        assertNull( "Configuration is fresh", props );

        props = new MyHashtable<>();
        props.put( ARRAY, ARRAY_VALUE );
        cimpl.update( props );

        Dictionary<String, Object> newProps = cimpl.getProperties();
        assertNotNull( "Configuration is not fresh", newProps );
        assertEquals( "Expect 2 elements", 2, newProps.size() );
        assertEquals( "Service.pid must match", TEST_PID, newProps.get( Constants.SERVICE_PID ) );

        Object testProp = newProps.get( ARRAY );
        assertNotNull( testProp );
        assertTrue( testProp.getClass().isArray() );
        assertEquals( 1, Array.getLength( testProp ) );
        assertEquals( STRING_VALUE, Array.get( testProp, 0 ) );

        // modify the array property
        Array.set( testProp, 0, STRING_VALUE2 );

        // the array element change must not be reflected in the configuration
        Dictionary<String, Object> newProps2 = cimpl.getProperties();
        Object testProp2 = newProps2.get( ARRAY );
        assertNotNull( testProp2 );
        assertTrue( testProp2.getClass().isArray() );
        assertEquals( 1, Array.getLength( testProp2 ) );
        assertEquals( STRING_VALUE, Array.get( testProp2, 0 ) );
    }


    @SuppressWarnings("unchecked")
    @Test public void testCollection() throws IOException
    {
        Configuration cimpl = getConfiguration();

        Dictionary<String, Object> props = cimpl.getProperties();
        assertNull( "Configuration is fresh", props );

        props = new MyHashtable<>();
        props.put( COLLECTION, COLLECTION_VALUE );
        cimpl.update( props );

        Dictionary<String, Object> newProps = cimpl.getProperties();
        assertNotNull( "Configuration is not fresh", newProps );
        assertEquals( "Expect 2 elements", 2, newProps.size() );
        assertEquals( "Service.pid must match", TEST_PID, newProps.get( Constants.SERVICE_PID ) );

        Object testProp = newProps.get( COLLECTION );
        assertNotNull( testProp );
        assertTrue( testProp instanceof Collection );
        Collection<String> coll = ( Collection<String> ) testProp;
        assertEquals( 1, coll.size() );
        assertEquals( STRING_VALUE, coll.iterator().next() );

        // modify the array property
        coll.clear();
        coll.add( STRING_VALUE2 );

        // the array element change must not be reflected in the configuration
        Dictionary<String, Object> newProps2 = cimpl.getProperties();
        Object testProp2 = newProps2.get( COLLECTION );
        assertNotNull( testProp2 );
        assertTrue( testProp2 instanceof Collection );
        Collection<String> coll2 = ( Collection<String> ) testProp2;
        assertEquals( 1, coll2.size() );
        assertEquals( STRING_VALUE, coll2.iterator().next() );
    }
}
