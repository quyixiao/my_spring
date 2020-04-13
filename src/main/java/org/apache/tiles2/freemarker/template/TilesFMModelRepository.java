/*
 * $Id: TilesFMModelRepository.java 765386 2009-04-15 21:56:54Z apetrelli $
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

import org.apache.tiles2.template.AddAttributeModel;
import org.apache.tiles2.template.AddListAttributeModel;
import org.apache.tiles2.template.AttributeResolver;
import org.apache.tiles2.template.DefaultAttributeResolver;
import org.apache.tiles2.template.DefinitionModel;
import org.apache.tiles2.template.GetAsStringModel;
import org.apache.tiles2.template.ImportAttributeModel;
import org.apache.tiles2.template.InsertAttributeModel;
import org.apache.tiles2.template.InsertDefinitionModel;
import org.apache.tiles2.template.InsertTemplateModel;
import org.apache.tiles2.template.PutAttributeModel;
import org.apache.tiles2.template.PutListAttributeModel;

/**
 * Collects all Tiles FreeMarker directives to be used in an application.
 *
 * @version $Rev: 765386 $ $Date: 2009-04-15 23:56:54 +0200 (mer, 15 apr 2009) $
 * @since 2.2.0
 */
public class TilesFMModelRepository {

    /**
     * The "addAttribute" directive.
     */
    private AddAttributeFMModel addAttribute;

    /**
     * The "addListAttribute" directive.
     */
    private org.apache.tiles2.freemarker.template.AddListAttributeFMModel addListAttribute;

    /**
     * The "definition" directive.
     */
    private DefinitionFMModel definition;

    /**
     * The "getAsString" directive.
     */
    private GetAsStringFMModel getAsString;

    /**
     * The "importAttribute" directive.
     */
    private org.apache.tiles2.freemarker.template.ImportAttributeFMModel importAttribute;

    /**
     * The "insertAttribute" directive.
     */
    private org.apache.tiles2.freemarker.template.InsertAttributeFMModel insertAttribute;

    /**
     * The "insertDefinition" directive.
     */
    private org.apache.tiles2.freemarker.template.InsertDefinitionFMModel insertDefinition;

    /**
     * The "insertTemplate" directive.
     */
    private org.apache.tiles2.freemarker.template.InsertTemplateFMModel insertTemplate;

    /**
     * The "putAttribute" directive.
     */
    private org.apache.tiles2.freemarker.template.PutAttributeFMModel putAttribute;

    /**
     * The "putListAttribute" directive.
     */
    private org.apache.tiles2.freemarker.template.PutListAttributeFMModel putListAttribute;

    /**
     * The "setCurrentContainer" directive.
     */
    private org.apache.tiles2.freemarker.template.SetCurrentContainerFMModel setCurrentContainer;

    /**
     * Constructor.
     *
     * @since 2.2.0
     */
    public TilesFMModelRepository() {
        addAttribute = new AddAttributeFMModel(new AddAttributeModel());
        addListAttribute = new org.apache.tiles2.freemarker.template.AddListAttributeFMModel(
                new AddListAttributeModel());
        definition = new DefinitionFMModel(new DefinitionModel());
        AttributeResolver attributeResolver = new DefaultAttributeResolver();
        getAsString = new GetAsStringFMModel(new GetAsStringModel(
                attributeResolver));
        importAttribute = new org.apache.tiles2.freemarker.template.ImportAttributeFMModel(new ImportAttributeModel());
        insertAttribute = new org.apache.tiles2.freemarker.template.InsertAttributeFMModel(new InsertAttributeModel(
                attributeResolver));
        insertDefinition = new org.apache.tiles2.freemarker.template.InsertDefinitionFMModel(
                new InsertDefinitionModel());
        insertTemplate = new org.apache.tiles2.freemarker.template.InsertTemplateFMModel(new InsertTemplateModel());
        putAttribute = new org.apache.tiles2.freemarker.template.PutAttributeFMModel(new PutAttributeModel());
        putListAttribute = new org.apache.tiles2.freemarker.template.PutListAttributeFMModel(
                new PutListAttributeModel());
        setCurrentContainer = new org.apache.tiles2.freemarker.template.SetCurrentContainerFMModel();
    }
    /**
     * Returns the "addAttribute" directive.
     *
     * @return The "addAttribute" directive.
     * @since 2.2.0
     */
    public AddAttributeFMModel getAddAttribute() {
        return addAttribute;
    }

    /**
     * Returns the "addListAttribute" directive.
     *
     * @return The "addListAttribute" directive.
     * @since 2.2.0
     */
    public AddListAttributeFMModel getAddListAttribute() {
        return addListAttribute;
    }

    /**
     * Returns the "definition" directive.
     *
     * @return The "definition" directive.
     * @since 2.2.0
     */
    public DefinitionFMModel getDefinition() {
        return definition;
    }

    /**
     * Returns the "getAsString" directive.
     *
     * @return The "getAsString" directive.
     * @since 2.2.0
     */
    public GetAsStringFMModel getGetAsString() {
        return getAsString;
    }

    /**
     * Returns the "importAttribute" directive.
     *
     * @return The "importAttribute" directive.
     * @since 2.2.0
     */
    public ImportAttributeFMModel getImportAttribute() {
        return importAttribute;
    }

    /**
     * Returns the "insertAttribute" directive.
     *
     * @return The "insertAttribute" directive.
     * @since 2.2.0
     */
    public InsertAttributeFMModel getInsertAttribute() {
        return insertAttribute;
    }

    /**
     * Returns the "insertDefinition" directive.
     *
     * @return The "insertDefinition" directive.
     * @since 2.2.0
     */
    public InsertDefinitionFMModel getInsertDefinition() {
        return insertDefinition;
    }

    /**
     * Returns the "insertTemplate" directive.
     *
     * @return The "insertTemplate" directive.
     * @since 2.2.0
     */
    public InsertTemplateFMModel getInsertTemplate() {
        return insertTemplate;
    }

    /**
     * Returns the "putAttribute" directive.
     *
     * @return The "putAttribute" directive.
     * @since 2.2.0
     */
    public PutAttributeFMModel getPutAttribute() {
        return putAttribute;
    }

    /**
     * Returns the "putListAttribute" directive.
     *
     * @return The "putListAttribute" directive.
     * @since 2.2.0
     */
    public PutListAttributeFMModel getPutListAttribute() {
        return putListAttribute;
    }

    /**
     * Returns the "setCurrentContainer" directive.
     *
     * @return The "setCurrentContainer" directive.
     * @since 2.2.0
     */
    public SetCurrentContainerFMModel getSetCurrentContainer() {
        return setCurrentContainer;
    }
}
