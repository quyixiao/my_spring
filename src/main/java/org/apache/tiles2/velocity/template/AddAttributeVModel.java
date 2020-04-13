/*
 * $Id: AddAttributeVModel.java 901361 2010-01-20 20:10:27Z apetrelli $
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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tiles2.servlet.context.ServletUtil;
import org.apache.tiles2.velocity.context.VelocityUtil;
import org.apache.tiles2.template.AddAttributeModel;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.Renderable;

/**
 * Wraps {@link org.apache.tiles2.template.AddAttributeModel} to be used in Velocity. For the list of
 * parameters, see {@link org.apache.tiles2.template.AddAttributeModel#start(java.util.Stack)},
 * {@link org.apache.tiles2.template.AddAttributeModel#end(java.util.Stack, Object, String, String, String, String)}
 * and {@link org.apache.tiles2.template.AddAttributeModel#execute(java.util.Stack, Object, String, String, String, String)}
 *
 * @version $Rev: 901361 $ $Date: 2010-01-20 21:10:27 +0100 (mer, 20 gen 2010) $
 * @since 2.2.0
 * @deprecated Use Velocity directives.
 */
@Deprecated
public class AddAttributeVModel implements Executable, BodyExecutable {

    /**
     * The template model.
     */
    private org.apache.tiles2.template.AddAttributeModel model;

    /**
     * Constructor.
     *
     * @param model The template model.
     * @since 2.2.0
     */
    public AddAttributeVModel(AddAttributeModel model) {
        this.model = model;
    }

    /** {@inheritDoc} */
    public Renderable execute(HttpServletRequest request,
            HttpServletResponse response, Context velocityContext,
            Map<String, Object> params) {
        model.execute(ServletUtil.getComposeStack(request),
                params.get("value"), (String) params.get("expression"), null,
                (String) params.get("role"), (String) params.get("type"));
        return VelocityUtil.EMPTY_RENDERABLE;
    }

    /** {@inheritDoc} */
    public Renderable end(HttpServletRequest request, HttpServletResponse response,
            Context velocityContext) {
        Map<String, Object> params = VelocityUtil.getParameterStack(
                velocityContext).pop();
        model.end(ServletUtil.getComposeStack(request), params.get("value"),
                (String) params.get("expression"), null, (String) params
                        .get("role"), (String) params.get("type"));
        return VelocityUtil.EMPTY_RENDERABLE;
    }

    /** {@inheritDoc} */
    public void start(HttpServletRequest request, HttpServletResponse response,
            Context velocityContext, Map<String, Object> params) {
        VelocityUtil.getParameterStack(velocityContext).push(params);
        model.start(ServletUtil.getComposeStack(request));
    }
}
