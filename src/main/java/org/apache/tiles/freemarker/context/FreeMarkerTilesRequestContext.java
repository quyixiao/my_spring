/*
 * $Id: FreeMarkerTilesRequestContext.java 765386 2009-04-15 21:56:54Z apetrelli $
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

package org.apache.tiles.freemarker.context;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Locale;

import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.context.TilesRequestContextWrapper;

import freemarker.core.Environment;

/**
 * The FreeMarker-specific request context.
 *
 * @version $Rev: 765386 $ $Date: 2009-04-15 23:56:54 +0200 (mer, 15 apr 2009) $
 * @since 2.2.0
 */
public class FreeMarkerTilesRequestContext extends TilesRequestContextWrapper implements TilesRequestContext {

    /**
     * The FreeMarker current environment.
     */
    private Environment env;

    /**
     * The request objects.
     */
    private transient Object[] requestObjects;

    /**
     * Constructor.
     *
     * @param enclosedRequest The request that exposes non-FreeMarker specific properties
     * @param env The FreeMarker environment.
     */
    public FreeMarkerTilesRequestContext(
            TilesRequestContext enclosedRequest, Environment env) {
        super(enclosedRequest);
        this.env = env;
    }

    /** {@inheritDoc} */
    public void dispatch(String path) throws IOException {
        include(path);
    }

    /** {@inheritDoc} */
    public Object getRequest() {
        return env;
    }

    /** {@inheritDoc} */
    public Locale getRequestLocale() {
        return env.getLocale();
    }

    /** {@inheritDoc} */
    public Object getResponse() {
        return env;
    }

    /** {@inheritDoc} */
    @Override
    public PrintWriter getPrintWriter() throws IOException {
        Writer writer = env.getOut();
        if (writer instanceof PrintWriter) {
            return (PrintWriter) writer;
        } else {
            return new PrintWriter(writer);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Writer getWriter() throws IOException {
        return env.getOut();
    }

    /** {@inheritDoc} */
    @Override
    public Object[] getRequestObjects() {
        if (requestObjects == null) {
            requestObjects = new Object[1];
            requestObjects[0] = env;
        }
        return requestObjects;
    }
}
