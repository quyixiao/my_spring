/*
 * $Id: EnhancedTilesApplicationContext.java 791161 2009-07-04 18:53:36Z apetrelli $
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
package org.apache.tiles2.context.enhanced;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.tiles2.TilesApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ApplicationContext decorator used to provide
 * enhancements to the standard context.
 *
 * Specifically, it provides:
 * <ul>
 *   <li>Ability to load resources from the classpath</li>
 *   <li>Ability to retrieve multiple resources of the same name</li>
 * </ul>
 *
 * Future features will include:
 * <ul>
 *   <li>Ability to utilize wildcards in resource pathcs</li>
 * </ul>
 *
 * @since Tiles 2.0
 * @version $Rev: 791161 $ $Date: 2009-07-04 20:53:36 +0200 (sab, 04 lug 2009) $
 */
public class EnhancedTilesApplicationContext implements TilesApplicationContext {

    /**
     * The logging object.
     */
    private final Logger log =
        LoggerFactory.getLogger(EnhancedTilesApplicationContext.class);

    /**
     * The root context to be wrapped.
     */
    private TilesApplicationContext rootContext;

    /**
     * Constructor.
     *
     * @param rootContext The root context to use.
     */
    public EnhancedTilesApplicationContext(TilesApplicationContext rootContext) {
        this.rootContext = rootContext;
    }

    /** {@inheritDoc} */
    public Object getContext() {
        return rootContext.getContext();
    }

    /**
     * Returns the root context.
     *
     * @return The root context.
     */
    public TilesApplicationContext getRootContext() {
        return rootContext;
    }

    /**
     * Sets the root context.
     *
     * @param rootContext The root context.
     */
    public void setRootContext(TilesApplicationContext rootContext) {
        this.rootContext = rootContext;
    }

    /** {@inheritDoc} */
    public Map<String, Object> getApplicationScope() {
        return rootContext.getApplicationScope();
    }

    /** {@inheritDoc} */
    public Map<String, String> getInitParams() {
        return rootContext.getInitParams();
    }

    /** {@inheritDoc} */
    public URL getResource(String path) throws IOException {
        URL rootUrl = rootContext.getResource(path);
        if (rootUrl == null) {
            Set<URL> resources = getResources(path);
            resources.remove(null);
            if (resources.size() > 0) {
                rootUrl = resources.toArray(new URL[resources.size()])[0];
            }
        }
        return rootUrl;
    }

    /** {@inheritDoc} */
    public Set<URL> getResources(String path) throws IOException {
        Set<URL> resources = new HashSet<URL>();
        resources.addAll(rootContext.getResources(path));
        resources.addAll(getClasspathResources(path));
        return resources;
    }

    /**
     * Searches for resources in the classpath, given a path.
     *
     * @param path The path to search into.
     * @return The set of found URLs.
     * @throws IOException If something goes wrong during search.
     */
    public Set<URL> getClasspathResources(String path) throws IOException {
        Set<URL> resources = new HashSet<URL>();
        resources.addAll(searchResources(getClass().getClassLoader(), path));

        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if (contextLoader != null) {
            resources.addAll(searchResources(contextLoader, path));
        }

        if (resources.size() == 0 && path.startsWith("/")) {
            return getClasspathResources(path.substring(1));
        }

        return resources;
    }

    /**
     * Searches for resources in the classpath, given a path, using a class
     * loader.
     *
     * @param loader The class loader to use.
     * @param path The path to search into.
     * @return The set of found URLs.
     */
    protected Set<URL> searchResources(ClassLoader loader, String path) {
        Set<URL> resources = new HashSet<URL>();
        try {
            Enumeration<URL> e = loader.getResources(path);
            while (e.hasMoreElements()) {
                resources.add(e.nextElement());
            }
        } catch (IOException e) {
            log.warn("Unable to retrieved resources from classloader: "
                    + loader, e);
        }
        return resources;
    }
}
