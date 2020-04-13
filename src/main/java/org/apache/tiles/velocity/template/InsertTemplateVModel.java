/*
 * $Id: InsertTemplateVModel.java 901361 2010-01-20 20:10:27Z apetrelli $
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

package org.apache.tiles.velocity.template;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tiles.servlet.context.ServletUtil;
import org.apache.tiles.template.InsertTemplateModel;
import org.apache.tiles.velocity.context.VelocityUtil;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.Renderable;

/**
 * Wraps {@link InsertTemplateModel} to be used in Velocity. For the list of
 * parameters, see
 * {@link InsertTemplateModel#start(org.apache.tiles.TilesContainer, Object...)}
 * , {@link InsertTemplateModel#end(org.apache.tiles.TilesContainer,
 * String, String, String, String, String, Object...)} and
 * {@link InsertTemplateModel#execute(org.apache.tiles.TilesContainer,
 * String, String, String, String, String, Object...)}.
 *
 * @version $Rev: 901361 $ $Date: 2010-01-20 21:10:27 +0100 (mer, 20 gen 2010) $
 * @since 2.2.0
 * @deprecated Use Velocity directives.
 */
@Deprecated
public class InsertTemplateVModel implements Executable, BodyExecutable {

    /**
     * The template model.
     */
    private InsertTemplateModel model;

    /**
     * The Servlet context.
     */
    private ServletContext servletContext;

    /**
     * Constructor.
     *
     * @param model The template model.
     * @param servletContext The servlet context.
     * @since 2.2.0
     */
    public InsertTemplateVModel(InsertTemplateModel model,
            ServletContext servletContext) {
        this.model = model;
        this.servletContext = servletContext;
    }

    /** {@inheritDoc} */
    public Renderable execute(HttpServletRequest request,
            HttpServletResponse response, Context velocityContext,
            Map<String, Object> params) {
        return new AbstractDefaultToStringRenderable(velocityContext, params, response, request) {

            public boolean render(InternalContextAdapter context, Writer writer)
                    throws IOException {
                model.execute(ServletUtil.getCurrentContainer(request,
                        servletContext), (String) params.get("template"),
                        (String) params.get("templateType"), (String) params
                                .get("templateExpression"), (String) params.get("role"),
                        (String) params
                                .get("preparer"), velocityContext, request, response,
                        writer);
                return true;
            }
        };
    }

    /** {@inheritDoc} */
    public Renderable end(HttpServletRequest request, HttpServletResponse response,
            Context velocityContext) {
        Map<String, Object> params = VelocityUtil.getParameterStack(
                velocityContext).pop();
        return new AbstractDefaultToStringRenderable(velocityContext, params, response, request) {

            public boolean render(InternalContextAdapter context, Writer writer)
                    throws IOException {
                model.end(ServletUtil.getCurrentContainer(request,
                        servletContext), (String) params.get("template"),
                        (String) params.get("templateType"), (String) params
                                .get("templateExpression"), (String) params.get("role"),
                        (String) params
                                .get("preparer"), velocityContext, request, response,
                        writer);
                return true;
            }
        };
    }

    /** {@inheritDoc} */
    public void start(HttpServletRequest request, HttpServletResponse response,
            Context velocityContext, Map<String, Object> params) {
        VelocityUtil.getParameterStack(velocityContext).push(params);
        model.start(ServletUtil.getCurrentContainer(request, servletContext),
                velocityContext, request, response);
    }
}
