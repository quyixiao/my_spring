/*
 * $Id: JspTilesRequestContext.java 736275 2009-01-21 09:58:20Z apetrelli $
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
package org.apache.tiles2.jsp.context;

import org.apache.tiles2.context.TilesRequestContext;
import org.apache.tiles2.context.TilesRequestContextWrapper;
import org.apache.tiles2.servlet.context.ServletTilesRequestContext;
import org.apache.tiles2.servlet.context.ServletUtil;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * Context implementation used for executing tiles within a
 * jsp tag library.
 *
 * @version $Rev: 736275 $ $Date: 2009-01-21 10:58:20 +0100 (mer, 21 gen 2009) $
 */
public class JspTilesRequestContext extends TilesRequestContextWrapper
    implements TilesRequestContext {

    /**
     * The current page context.
     */
    private PageContext pageContext;

    /**
     * The writer response to use.
     */
    private JspWriterResponse response;

    /**
     * The request objects, lazily initialized.
     */
    private Object[] requestObjects;

    /**
     * Constructor.
     *
     * @param enclosedRequest The request that is wrapped here.
     * @param pageContext The page context to use.
     */
    public JspTilesRequestContext(TilesRequestContext enclosedRequest,
            PageContext pageContext) {
        super(enclosedRequest);
        this.pageContext = pageContext;
    }

    /**
     * Constructor.
     *
     * @param context The servlet context to use.
     * @param pageContext The page context to use.
     * @deprecated Use
     * {@link #JspTilesRequestContext(TilesRequestContext, PageContext)}.
     */
    @Deprecated
    public JspTilesRequestContext(ServletContext context, PageContext pageContext) {
        this(new ServletTilesRequestContext(context,
                (HttpServletRequest) pageContext.getRequest(),
                (HttpServletResponse) pageContext.getResponse()), pageContext);
    }

    /**
     * Dispatches a path. In fact it "includes" it!
     *
     * @param path The path to dispatch to.
     * @throws IOException If something goes wrong during dispatching.
     * @see ServletTilesRequestContext#dispatch(String)
     */
    public void dispatch(String path) throws IOException {
        include(path);
    }

    /** {@inheritDoc} */
    public void include(String path) throws IOException {
        JspUtil.setForceInclude(pageContext, true);
        try {
            pageContext.include(path, false);
        } catch (ServletException e) {
            throw ServletUtil.wrapServletException(e, "JSPException including path '"
                    + path + "'.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public PrintWriter getPrintWriter() throws IOException {
        return new JspPrintWriterAdapter(pageContext.getOut());
    }

    /** {@inheritDoc} */
    @Override
    public Writer getWriter() throws IOException {
        return pageContext.getOut();
    }

    /** {@inheritDoc} */
    @Override
    public Object[] getRequestObjects() {
        if (requestObjects == null) {
            requestObjects = new Object[1];
            requestObjects[0] = pageContext;
        }
        return requestObjects;
    }

    /**
     * Returns the page context that originated the request.
     *
     * @return The page context.
     */
    public PageContext getPageContext() {
        return pageContext;
    }

    /**
     * Returns the response object, obtained by the JSP page context. The print
     * writer will use the object obtained by {@link PageContext#getOut()}.
     *
     * @return The response object.
     * @deprecated Use {@link #getPageContext()} or {@link #getPrintWriter()}.
     */
    @Deprecated
    public HttpServletResponse getResponse() {
        if (response == null) {
            response = new JspWriterResponse(pageContext);
        }
        return response;
    }

}
