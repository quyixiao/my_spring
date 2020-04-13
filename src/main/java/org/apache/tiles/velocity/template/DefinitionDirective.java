/*
 * $Id: DefinitionDirective.java 902403 2010-01-23 13:31:17Z apetrelli $
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

import java.io.Writer;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tiles.mgmt.MutableTilesContainer;
import org.apache.tiles.servlet.context.ServletUtil;
import org.apache.tiles.template.DefinitionModel;
import org.apache.velocity.context.InternalContextAdapter;

/**
 * Wraps {@link DefinitionModel} to be used in Velocity. For the list of
 * parameters, see
 * {@link DefinitionModel#start(java.util.Stack, String, String, String, String, String)}
 * , {@link DefinitionModel#end(MutableTilesContainer, java.util.Stack, Object...)} and
 * {@link DefinitionModel#execute(MutableTilesContainer, java.util.Stack, String, String,
 * String, String, String, Object...)}.
 *
 * @version $Rev: 902403 $ $Date: 2010-01-23 14:31:17 +0100 (sab, 23 gen 2010) $
 * @since 2.2.2
 */
public class DefinitionDirective extends BlockDirective {

    /**
     * The template model.
     */
    private DefinitionModel model = new DefinitionModel();

    /**
     * Default constructor.
     *
     * @since 2.2.2
     */
    public DefinitionDirective() {
        // Does nothing.
    }

    /**
     * Constructor.
     *
     * @param model The used model.
     * @since 2.2.2
     */
    public DefinitionDirective(DefinitionModel model) {
        this.model = model;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "tiles_definition";
    }

    /** {@inheritDoc} */
    @Override
    protected void end(InternalContextAdapter context, Writer writer,
            Map<String, Object> params, HttpServletRequest request,
            HttpServletResponse response, ServletContext servletContext) {
        model.end((MutableTilesContainer) ServletUtil.getCurrentContainer(
                request, servletContext), ServletUtil.getComposeStack(request), context, request,
                response);
    }

    /** {@inheritDoc} */
    @Override
    protected void start(InternalContextAdapter context, Writer writer,
            Map<String, Object> params, HttpServletRequest request,
            HttpServletResponse response, ServletContext servletContext) {
        model.start(ServletUtil.getComposeStack(request), (String) params.get("name"), (String) params
                .get("template"), (String) params.get("role"), (String) params
                .get("extends"), (String) params.get("preparer"));
    }

}
