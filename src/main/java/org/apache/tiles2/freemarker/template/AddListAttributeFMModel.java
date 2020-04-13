/*
 * $Id: AddListAttributeFMModel.java 797765 2009-07-25 13:20:26Z apetrelli $
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

package org.apache.tiles2.freemarker.template;

import java.io.IOException;
import java.util.Map;

import org.apache.tiles2.ArrayStack;
import org.apache.tiles2.freemarker.context.FreeMarkerUtil;
import org.apache.tiles2.template.AddListAttributeModel;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * Wraps {@link AddListAttributeModel} to be used in FreeMarker. For the list of
 * parameters, see {@link AddListAttributeModel#start(ArrayStack, String)} and
 * {@link AddListAttributeModel#end(ArrayStack)}.
 *
 * @version $Rev: 797765 $ $Date: 2009-07-25 15:20:26 +0200 (sab, 25 lug 2009) $
 * @since 2.2.0
 */
public class AddListAttributeFMModel implements TemplateDirectiveModel {

    /**
     * The template model.
     */
    private AddListAttributeModel model;

    /**
     * Constructor.
     *
     * @param model The template model.
     * @since 2.2.0
     */
    public AddListAttributeFMModel(AddListAttributeModel model) {
        this.model = model;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {
        Map<String, TemplateModel> parms = (Map<String, TemplateModel>) params;
        ArrayStack<Object> composeStack = FreeMarkerUtil.getComposeStack(env);
        model.start(composeStack, FreeMarkerUtil.getAsString(parms.get("role")));
        FreeMarkerUtil.evaluateBody(body);
        model.end(composeStack);
    }
}
