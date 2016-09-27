/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.serializer.impl.yaml;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.apache.felix.converter.impl.Util;
import org.osgi.service.converter.ConversionException;
import org.osgi.service.converter.Converter;
import org.osgi.service.serializer.Deserializing;
import org.yaml.snakeyaml.Yaml;

public class YamlDeserializingImpl<T> implements Deserializing<T> {
    private volatile Converter converter;
    private final Type type;

    YamlDeserializingImpl(Converter c, Type cls) {
        converter = c;
        type = cls;
    }

    @Override
    public T from(InputStream in) {
        return from(in, StandardCharsets.UTF_8);
    }

    @Override
    public T from(InputStream in, Charset charset) {
        try {
            byte[] bytes = Util.readStream(in);
            String s = new String(bytes, charset);
            return from(s);
        } catch (IOException e) {
            throw new ConversionException("Error reading inputstream", e);
        }
    }

    @Override
    public T from(Readable in) {
        try (Scanner s = new Scanner(in)) {
            s.useDelimiter("\\Z");
            return from(s.next());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T from(CharSequence in) {
        Yaml yaml = new Yaml();
        Object res = yaml.load(in.toString());

        if (type instanceof Class)
            if (res.getClass().isAssignableFrom((Class<?>) type))
                return (T) res;

        return (T) converter.convert(res).to(type);
    }

    @Override
    public Deserializing<T> with(Converter c) {
        converter = c;
        return this;
    }
}
