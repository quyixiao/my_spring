/*
 * $Id: FreeMarkerAttributeRenderer.java 821299 2009-10-03 12:15:05Z apetrelli $
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

package org.apache.tiles2.freemarker.renderer;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tiles2.Attribute;
import org.apache.tiles2.context.TilesRequestContext;
import org.apache.tiles2.freemarker.FreeMarkerTilesException;
import org.apache.tiles2.freemarker.servlet.TilesFreemarkerServlet;
import org.apache.tiles2.impl.InvalidTemplateException;
import org.apache.tiles2.renderer.impl.AbstractTypeDetectingAttributeRenderer;
import org.apache.tiles2.servlet.context.ExternalWriterHttpServletResponse;
import org.apache.tiles2.servlet.context.ServletTilesRequestContext;
import org.apache.tiles2.servlet.context.ServletUtil;
import org.apache.tiles2.util.IteratorEnumeration;

/**
 * FreeMarker renderer for rendering FreeMarker templates as Tiles attributes.
 * It is only usable under a Servlet environment, because it uses
 * {@link TilesFreemarkerServlet} internally to forward the request.<br/>
 * To initialize it correctly, call {@link #setParameter(String, String)} for all the
 * parameters that you want to set, and then call {@link #commit()}.
 *
 * @version $Rev: 821299 $ $Date: 2009-10-03 14:15:05 +0200 (sab, 03 ott 2009) $
 * @since 2.2.0
 */
public class FreeMarkerAttributeRenderer extends AbstractTypeDetectingAttributeRenderer {

    /**
     * The servlet that is used to forward the request to.
     */
    private AttributeValueFreemarkerServlet servlet;

    /**
     * The initialization parameters.
     */
    private Map<String, String> params = new HashMap<String, String>();

    /**
     * Sets a parameter for the internal servlet.
     *
     * @param key The name of the parameter.
     * @param value The value of the parameter.
     * @since 2.2.0
     */
    public void setParameter(String key, String value) {
        params.put(key, value);
    }

    /**
     * Commits the parameters and makes this renderer ready for the use.
     *
     * @since 2.2.0
     */
    public void commit() {
        servlet = new AttributeValueFreemarkerServlet();
        try {
            servlet.init(new InitParamsServletConfig());
        } catch (ServletException e) {
            throw new FreeMarkerTilesException(
                    "Cannot initialize internal servlet", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void write(Object value, Attribute attribute,
            TilesRequestContext request) throws IOException {
        if (value != null) {
            if (value instanceof String) {
                ServletTilesRequestContext servletRequest = ServletUtil.getServletRequest(request);
                HttpServletRequest httpRequest = servletRequest.getRequest();
                HttpServletResponse httpResponse = servletRequest.getResponse();
                servlet.setValue((String) value);
                try {
                    servlet.doGet(httpRequest,
                            new ExternalWriterHttpServletResponse(httpResponse,
                                    request.getPrintWriter()));
                } catch (ServletException e) {
                    throw new FreeMarkerTilesException("Exception when rendering a FreeMarker attribute", e);
                }
            } else {
                throw new InvalidTemplateException(
                        "Cannot render a template that is not a string: "
                                + value.toString());
            }
        } else {
            throw new InvalidTemplateException("Cannot render a null template");
        }
    }

    /** {@inheritDoc} */
    public boolean isRenderable(Object value, Attribute attribute,
            TilesRequestContext request) {
        if (value instanceof String) {
            String string = (String) value;
            return string.startsWith("/") && string.endsWith(".ftl");
        }
        return false;
    }

    /**
     * Extends {@link TilesFreemarkerServlet} to use the attribute value as the template name.
     *
     * @since 2.2.0
     */
    private static class AttributeValueFreemarkerServlet extends TilesFreemarkerServlet {

        /**
         * Holds the value that should be used as the template name.
         */
        private ThreadLocal<String> valueHolder = new ThreadLocal<String>();

        /**
         * Sets the value to use as the template name.
         *
         * @param value The template name.
         * @since 2.2.0
         */
        public void setValue(String value) {
            valueHolder.set(value);
        }

        /** {@inheritDoc} */
        @Override
        protected String requestUrlToTemplatePath(HttpServletRequest request) {
            return valueHolder.get();
        }
    }

    /**
     * Implements {@link ServletConfig} to initialize the internal servlet using parameters
     * set through {@link FreeMarkerAttributeRenderer#setParameter(String, String)}.
     *
     * @version $Rev: 821299 $ $Date: 2009-10-03 14:15:05 +0200 (sab, 03 ott 2009) $
     * @since 2.2.0
     */
    private class InitParamsServletConfig implements ServletConfig {

        /** {@inheritDoc} */
        public String getInitParameter(String name) {
            return params.get(name);
        }

        /** {@inheritDoc} */
        @SuppressWarnings("unchecked")
        public Enumeration getInitParameterNames() {
            return new IteratorEnumeration(params.keySet().iterator());
        }

        /** {@inheritDoc} */
        public ServletContext getServletContext() {
            return ServletUtil.getServletContext(applicationContext);
        }

        /** {@inheritDoc} */
        public String getServletName() {
            return "FreeMarker Attribute Renderer";
        }
    }
}
