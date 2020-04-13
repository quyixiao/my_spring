/*
 * $Id: ImportAttributeFMModel.java 765386 2009-04-15 21:56:54Z apetrelli $
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

import org.apache.tiles2.TilesContainer;
import org.apache.tiles2.freemarker.context.FreeMarkerUtil;
import org.apache.tiles2.template.ImportAttributeModel;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * Wraps {@link ImportAttributeModel} to be used in FreeMarker. For the list of
 * parameters, see
 * {@link ImportAttributeModel#getImportedAttributes(TilesContainer, String, String, boolean, Object...)}
 * .
 *
 * @version $Rev: 765386 $ $Date: 2009-04-15 23:56:54 +0200 (mer, 15 apr 2009) $
 * @since 2.2.0
 */
public class ImportAttributeFMModel implements TemplateDirectiveModel {

    /**
     * The template model.
     */
    private ImportAttributeModel model;

    /**
     * Constructor.
     *
     * @param model The template model.
     * @since 2.2.0
     */
    public ImportAttributeFMModel(ImportAttributeModel model) {
        this.model = model;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {
        Map<String, TemplateModel> parms = (Map<String, TemplateModel>) params;
        TilesContainer container = FreeMarkerUtil.getCurrentContainer(env);
        Map<String, Object> attributes = model.getImportedAttributes(container,
                FreeMarkerUtil.getAsString(parms.get("name")), FreeMarkerUtil
                        .getAsString(parms.get("toName")), FreeMarkerUtil
                        .getAsBoolean(parms.get("ignore"), false), env);
        String scope = FreeMarkerUtil.getAsString(parms.get("scope"));
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            FreeMarkerUtil.setAttribute(env, entry.getKey(), entry.getValue(),
                    scope);
        }
    }

}
