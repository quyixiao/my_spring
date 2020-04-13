/*
 * $Id: TilesRequestContextWrapper.java 769961 2009-04-29 22:07:34Z apetrelli $
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
package org.apache.tiles.context;

import java.util.Map;
import java.util.Locale;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

import org.apache.tiles.TilesApplicationContext;

/**
 * Delegate for ease of customization.
 *
 * @since Tiles 2.0
 * @version $Rev: 769961 $ $Date: 2009-04-30 00:07:34 +0200 (gio, 30 apr 2009) $
 */
public class TilesRequestContextWrapper implements TilesRequestContext {

    /**
     * The wrapper request context object.
     */
    private TilesRequestContext context;


    /**
     * Constructor.
     *
     * @param context The request context to wrap.
     */
    public TilesRequestContextWrapper(TilesRequestContext context) {
        this.context = context;
    }

    /**
     * Returns the wrapped Tiles request.
     *
     * @return The wrapped Tiles request.
     * @since 2.1.1
     */
    public TilesRequestContext getWrappedRequest() {
        return context;
    }

    /** {@inheritDoc} */
    public Map<String, String> getHeader() {
        return context.getHeader();
    }

    /** {@inheritDoc} */
    public Map<String, String[]> getHeaderValues() {
        return context.getHeaderValues();
    }

    /** {@inheritDoc} */
    public Map<String, Object> getRequestScope() {
        return context.getRequestScope();
    }

    /** {@inheritDoc} */
    public Map<String, Object> getSessionScope() {
        return context.getSessionScope();
    }

    /** {@inheritDoc} */
    public TilesApplicationContext getApplicationContext() {
        return context.getApplicationContext();
    }

    /** {@inheritDoc} */
    public void dispatch(String path) throws IOException {
        context.dispatch(path);
    }

    /** {@inheritDoc} */
    public void include(String path) throws IOException {
        context.include(path);
    }

    /** {@inheritDoc} */
    public OutputStream getOutputStream() throws IOException {
        return context.getOutputStream();
    }

    /** {@inheritDoc} */
    public Writer getWriter() throws IOException {
        return context.getWriter();
    }

    /** {@inheritDoc} */
    public PrintWriter getPrintWriter() throws IOException {
        return context.getPrintWriter();
    }

    /** {@inheritDoc} */
    public boolean isResponseCommitted() {
        return context.isResponseCommitted();
    }

    /** {@inheritDoc} */
    public void setContentType(String contentType) {
        context.setContentType(contentType);
    }

    /** {@inheritDoc} */
    public Map<String, String> getParam() {
        return context.getParam();
    }

    /** {@inheritDoc} */
    public Map<String, String[]> getParamValues() {
        return context.getParamValues();
    }

    /** {@inheritDoc} */
    public Locale getRequestLocale() {
        return context.getRequestLocale();
    }

    /** {@inheritDoc} */
    public boolean isUserInRole(String role) {
        return context.isUserInRole(role);
    }

    /** {@inheritDoc} */
    public Object[] getRequestObjects() {
        return context.getRequestObjects();
    }

    /** {@inheritDoc} */
    @Deprecated
    public Object getResponse() {
        return context.getResponse();
    }

    /** {@inheritDoc} */
    @Deprecated
    public Object getRequest() {
        return context.getRequest();
    }
}
