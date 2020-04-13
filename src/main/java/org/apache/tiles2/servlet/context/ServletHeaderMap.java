/*
 * $Id: ServletHeaderMap.java 769961 2009-04-29 22:07:34Z apetrelli $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tiles2.servlet.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet2.http.HttpServletRequest;
import javax.servlet2.http.HttpServletResponse;

import org.apache.tiles2.context.MapEntry;

/**
 * <p>Private implementation of <code>Map</code> for servlet request
 * name-value.</p>
 *
 * @version $Rev: 769961 $ $Date: 2009-04-30 00:07:34 +0200 (gio, 30 apr 2009) $
 */

final class ServletHeaderMap implements Map<String, String> {


    /**
     * Constructor.
     *
     * @param request The request object to use.
     * @deprecated Use {@link #ServletHeaderMap(HttpServletRequest,HttpServletResponse)} instead
     */
    public ServletHeaderMap(HttpServletRequest request) {
        this(request, null);
    }


    /**
     * Constructor.
     *
     * @param request The request object to use.
     * @param response The response object to use.
     * @since 2.2.0
     */
    public ServletHeaderMap(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }


    /**
     * The request object to use.
     */
    private HttpServletRequest request = null;

    /**
     * The request object to use.
     */
    private HttpServletResponse response = null;


    /** {@inheritDoc} */
    public void clear() {
        throw new UnsupportedOperationException();
    }


    /** {@inheritDoc} */
    public boolean containsKey(Object key) {
        return (request.getHeader(key(key)) != null);
    }


    /** {@inheritDoc} */
    public boolean containsValue(Object value) {
        Iterator<String> values = values().iterator();
        while (values.hasNext()) {
            if (value.equals(values.next())) {
                return (true);
            }
        }
        return (false);
    }


    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public Set<Entry<String, String>> entrySet() {
        Set<Entry<String, String>> set = new HashSet<Entry<String, String>>();
        Enumeration<String> keys = request.getHeaderNames();
        String key;
        while (keys.hasMoreElements()) {
            key = keys.nextElement();
            set.add(new MapEntry<String, String>(key, request.getHeader(key),
                    false));
        }
        return (set);
    }


    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public boolean equals(Object o) {
        HttpServletRequest otherRequest = ((ServletHeaderMap) o).request;
        boolean retValue = true;
        synchronized (request) {
            for (Enumeration<String> attribs = request.getHeaderNames(); attribs
                    .hasMoreElements()
                    && retValue;) {
                String parameterName = attribs.nextElement();
                retValue = request.getHeader(parameterName).equals(
                        otherRequest.getHeader(parameterName));
            }
        }

        return retValue;
    }


    /** {@inheritDoc} */
    public String get(Object key) {
        return (request.getHeader(key(key)));
    }


    /** {@inheritDoc} */
    public int hashCode() {
        return (request.hashCode());
    }


    /** {@inheritDoc} */
    public boolean isEmpty() {
        return (size() < 1);
    }


    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public Set<String> keySet() {
        Set<String> set = new HashSet<String>();
        Enumeration<String> keys = request.getHeaderNames();
        while (keys.hasMoreElements()) {
            set.add(keys.nextElement());
        }
        return (set);
    }


    /** {@inheritDoc} */
    public String put(String key, String value) {
        response.setHeader(key, value);
        return value;
    }


    /** {@inheritDoc} */
    public void putAll(Map<? extends String, ? extends String> map) {
        for (Entry<? extends String, ? extends String> entry : map
                .entrySet()) {
            response.setHeader(entry.getKey(), entry.getValue());
        }
    }


    /** {@inheritDoc} */
    public String remove(Object key) {
        throw new UnsupportedOperationException();
    }


    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public int size() {
        int n = 0;
        Enumeration<String> keys = request.getHeaderNames();
        while (keys.hasMoreElements()) {
            keys.nextElement();
            n++;
        }
        return (n);
    }


    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public Collection<String> values() {
        List<String> list = new ArrayList<String>();
        Enumeration<String> keys = request.getHeaderNames();
        while (keys.hasMoreElements()) {
            list.add(request.getHeader(keys.nextElement()));
        }
        return (list);
    }


    /**
     * Returns the string representation of the key.
     *
     * @param key The key.
     * @return The string representation of the key.
     * @throws IllegalArgumentException If the key is <code>null</code>.
     */
    private String key(Object key) {
        if (key == null) {
            throw new IllegalArgumentException();
        } else if (key instanceof String) {
            return ((String) key);
        } else {
            return (key.toString());
        }
    }


}
