/*
 * $Id: AddAttributeFMModel.java 797765 2009-07-25 13:20:26Z apetrelli $
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

package org.apache.tiles.freemarker.template;

import java.io.IOException;
import java.util.Map;

import org.apache.tiles.ArrayStack;
import org.apache.tiles.freemarker.context.FreeMarkerUtil;
import org.apache.tiles.template.AddAttributeModel;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * Wraps {@link AddAttributeModel} to be used in FreeMarker. For the list of
 * parameters, see {@link AddAttributeModel#start(ArrayStack)} and
 * {@link AddAttributeModel#end(ArrayStack, Object, String, String, String, String)}.
 *
 * @version $Rev: 797765 $ $Date: 2009-07-25 15:20:26 +0200 (sab, 25 lug 2009) $
 * @since 2.2.0
 */
public class AddAttributeFMModel implements TemplateDirectiveModel {

    /**
     * The template model.
     */
    private AddAttributeModel model;

    /**
     * Constructor.
     *
     * @param model The template model.
     * @since 2.2.0
     */
    public AddAttributeFMModel(AddAttributeModel model) {
        this.model = model;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {
        ArrayStack<Object> composeStack = FreeMarkerUtil.getComposeStack(env);
        model.start(composeStack);
        String bodyString = FreeMarkerUtil.renderAsString(body);
        Map<String, TemplateModel> parms = (Map<String, TemplateModel>) params;
        model.end(composeStack, FreeMarkerUtil.getAsObject(parms.get("value")),
                FreeMarkerUtil.getAsString(parms.get("expression")), bodyString,
                FreeMarkerUtil.getAsString(parms.get("role")), FreeMarkerUtil.getAsString(parms.get("type")));
    }
}
