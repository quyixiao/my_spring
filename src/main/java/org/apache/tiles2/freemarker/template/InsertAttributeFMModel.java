/*
 * $Id: InsertAttributeFMModel.java 765774 2009-04-16 21:43:00Z apetrelli $
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

import org.apache.tiles2.Attribute;
import org.apache.tiles2.TilesContainer;
import org.apache.tiles2.freemarker.context.FreeMarkerUtil;
import org.apache.tiles2.template.InsertAttributeModel;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * Wraps {@link InsertAttributeModel} to be used in FreeMarker. For the list of
 * parameters, see
 * {@link InsertAttributeModel
 * #start(java.util.Stack, TilesContainer, boolean, String, String,
 * Object, String, String, String, Attribute, Object...)}
 * and
 * {@link InsertAttributeModel
 * #end(java.util.Stack, TilesContainer, boolean, Object...)}
 * .
 *
 * @version $Rev: 765774 $ $Date: 2009-04-16 23:43:00 +0200 (gio, 16 apr 2009) $
 * @since 2.2.0
 */
public class InsertAttributeFMModel implements TemplateDirectiveModel {

    /**
     * The template model.
     */
    private InsertAttributeModel model;

    /**
     * Constructor.
     *
     * @param model The template model.
     * @since 2.2.0
     */
    public InsertAttributeFMModel(InsertAttributeModel model) {
        this.model = model;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {
        Map<String, TemplateModel> parms = (Map<String, TemplateModel>) params;
        TilesContainer container = FreeMarkerUtil.getCurrentContainer(env);
        model.start(
                FreeMarkerUtil.getComposeStack(env),
                container,
                FreeMarkerUtil.getAsBoolean(parms.get("ignore"), false),
                FreeMarkerUtil.getAsString(parms.get("preparer")),
                FreeMarkerUtil.getAsString(parms.get("role")),
                FreeMarkerUtil.getAsObject(parms.get("defaultValue")),
                FreeMarkerUtil.getAsString(parms
                        .get("defaultValueRole")), FreeMarkerUtil
                        .getAsString(parms.get("defaultValueType")),
                FreeMarkerUtil.getAsString(parms.get("name")),
                (Attribute) FreeMarkerUtil.getAsObject(parms
                        .get("value")), env);
        FreeMarkerUtil.evaluateBody(body);
        model.end(FreeMarkerUtil.getComposeStack(env), container,
                FreeMarkerUtil.getAsBoolean(parms.get("ignore"), false), env);
    }

}
