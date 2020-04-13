/*
 * $Id: ImportAttributeDirective.java 902403 2010-01-23 13:31:17Z apetrelli $
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
import org.apache.tiles2.velocity.context.VelocityUtil;
import org.apache.tiles2.TilesContainer;
import org.apache.tiles2.template.ImportAttributeModel;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.tools.view.ViewContext;

/**
 * Wraps {@link org.apache.tiles2.template.ImportAttributeModel} to be used in Velocity. For the list of
 * parameters, see
 * {@link org.apache.tiles2.template.ImportAttributeModel#getImportedAttributes(TilesContainer,
 * String, String, boolean, Object...)}.
 *
 * @version $Rev: 902403 $ $Date: 2010-01-23 14:31:17 +0100 (sab, 23 gen 2010) $
 * @since 2.2.2
 */
public class ImportAttributeDirective extends Directive {

    /**
     * The template model.
     */
    private org.apache.tiles2.template.ImportAttributeModel model = new org.apache.tiles2.template.ImportAttributeModel();

    /**
     * Default constructor.
     *
     * @since 2.2.2
     */
    public ImportAttributeDirective() {
        // Does nothing.
    }

    /**
     * Constructor.
     *
     * @param model The used model.
     * @since 2.2.2
     */
    public ImportAttributeDirective(ImportAttributeModel model) {
        this.model = model;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "tiles_importAttribute";
    }

    /** {@inheritDoc} */
    @Override
    public int getType() {
        return LINE;
    }

    /** {@inheritDoc} */
    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node) {
        ViewContext viewContext = (ViewContext) context
                .getInternalUserContext();
        Map<String, Object> params = VelocityUtil.getParameters(context, node);
        HttpServletRequest request = viewContext.getRequest();
        HttpServletResponse response = viewContext.getResponse();
        ServletContext servletContext = viewContext.getServletContext();
        Map<String, Object> attributes = model.getImportedAttributes(
                ServletUtil
                        .getCurrentContainer(request, servletContext),
                (String) params.get("name"), (String) params
                        .get("toName"), VelocityUtil.toSimpleBoolean(
                        (Boolean) params.get("ignore"), false),
                context, request, response);
        String scope = (String) params.get("scope");
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            VelocityUtil.setAttribute(context, request,
                    servletContext, entry.getKey(), entry.getValue(),
                    scope);
        }
        return true;
    }

}
