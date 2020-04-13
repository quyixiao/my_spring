/*
 * $Id: InsertDefinitionDirective.java 902403 2010-01-23 13:31:17Z apetrelli $
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

package org.apache.tiles2.velocity.template;

import java.io.Writer;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tiles2.servlet.context.ServletUtil;
import org.apache.tiles2.TilesContainer;
import org.apache.tiles2.template.InsertDefinitionModel;
import org.apache.velocity.context.InternalContextAdapter;

/**
 * Wraps {@link org.apache.tiles2.template.InsertDefinitionModel} to be used in Velocity. For the list of
 * parameters, see
 * {@link org.apache.tiles2.template.InsertDefinitionModel#start(TilesContainer, Object...)}
 * , {@link org.apache.tiles2.template.InsertDefinitionModel#end(TilesContainer,
 * String, String, String, String, String, String, Object...)} and
 * {@link org.apache.tiles2.template.InsertDefinitionModel#execute(TilesContainer,
 * String, String, String, String, String, String, Object...)}.
 *
 * @version $Rev: 902403 $ $Date: 2010-01-23 14:31:17 +0100 (sab, 23 gen 2010) $
 * @since 2.2.2
 */
public class InsertDefinitionDirective extends BlockDirective {

    /**
     * The template model.
     */
    private org.apache.tiles2.template.InsertDefinitionModel model = new org.apache.tiles2.template.InsertDefinitionModel();

    /**
     * Default constructor.
     *
     * @since 2.2.2
     */
    public InsertDefinitionDirective() {
        // Does nothing.
    }

    /**
     * Constructor.
     *
     * @param model The used model.
     * @since 2.2.2
     */
    public InsertDefinitionDirective(InsertDefinitionModel model) {
        this.model = model;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "tiles_insertDefinition";
    }

    /** {@inheritDoc} */
    @Override
    protected void end(InternalContextAdapter context, Writer writer,
            Map<String, Object> params, HttpServletRequest request,
            HttpServletResponse response, ServletContext servletContext) {
        model.end(ServletUtil.getCurrentContainer(request,
                servletContext), (String) params.get("name"), (String) params
                .get("template"), (String) params.get("templateType"),
                (String) params.get("templateExpression"), (String) params
                        .get("role"), (String) params.get("preparer"), context,
                request, response, writer);
    }

    /** {@inheritDoc} */
    @Override
    protected void start(InternalContextAdapter context, Writer writer,
            Map<String, Object> params, HttpServletRequest request,
            HttpServletResponse response, ServletContext servletContext) {
        model.start(ServletUtil.getCurrentContainer(request,
                servletContext), context, request, response);
    }

}
