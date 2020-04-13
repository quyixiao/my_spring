/*
 * $Id: Tiles2Tool.java 901361 2010-01-20 20:10:27Z apetrelli $
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

import org.apache.tiles2.template.*;
import org.apache.velocity.runtime.Renderable;

/**
 * The Tiles tool to be used in Velocity templates. Most of the methods can be used in two ways:
 * <ul>
 * <li>calling methods that accept a map of parameters: executes immediately the required model;</li>
 * <li>calling methods without parameters: useful to include composition code inside a block.
 * You need to call then {@link #start(Map)}, then your code in the block, and then {@link #end()}.</li>
 * </ul>
 *
 * @version $Rev: 901361 $ $Date: 2010-01-20 21:10:27 +0100 (mer, 20 gen 2010) $
 * @since 2.2.0
 * @deprecated Use Velocity directives.
 */
@Deprecated
public class Tiles2Tool extends VelocityStyleTilesTool {

    /**
     * The key of the attribute that will be used to store the repository of "models".
     */
    private static final String TILES_VELOCITY_REPOSITORY_KEY = "org.apache.tiles.velocity.TilesVelocityRepository";

    /**
     * The current executable object to use. Set in {@link #start(Map)} and used in {@link #end()}.
     */
    private BodyExecutable currentExecutable;

    /**
     * The repository of Tiles+Velocity models.
     */
    private TilesVelocityRepository repository;

    /**
     * Executes the {@link AddAttributeVModel}.
     *
     * @param params The map of parameters.
     * @return The tool itself.
     * @since 2.2.0
     * @see AddAttributeModel
     */
    public Tiles2Tool addAttribute(Map<String, Object> params) {
        execute(getRepository().getAddAttribute(), params);
        return this;
    }

    /**
     * Prepares the {@link AddAttributeVModel} for the execution with a block
     * inside {@link #start(Map)} and {@link #end()} calls.
     *
     * @return The tool itself.
     * @since 2.2.0
     * @see AddAttributeModel
     */
    public Tiles2Tool addAttribute() {
        currentExecutable = getRepository().getAddAttribute();
        return this;
    }

    /**
     * Prepares the {@link AddListAttributeVModel} for the execution with a block
     * inside {@link #start(Map)} and {@link #end()} calls.
     *
     * @return The tool itself.
     * @since 2.2.0
     * @see AddListAttributeModel
     */
    public Tiles2Tool addListAttribute() {
        currentExecutable = getRepository().getAddListAttribute();
        return this;
    }

    /**
     * Executes the {@link DefinitionVModel}.
     *
     * @param params The map of parameters.
     * @return The tool itself.
     * @since 2.2.0
     * @see DefinitionModel
     */
    public Tiles2Tool definition(Map<String, Object> params) {
        execute(getRepository().getDefinition(), params);
        return this;
    }

    /**
     * Prepares the {@link DefinitionVModel} for the execution with a block
     * inside {@link #start(Map)} and {@link #end()} calls.
     *
     * @return The tool itself.
     * @since 2.2.0
     * @see DefinitionModel
     */
    public Tiles2Tool definition() {
        currentExecutable = getRepository().getDefinition();
        return this;
    }

    /**
     * Executes the {@link GetAsStringVModel}.
     *
     * @param params The map of parameters.
     * @return A renderable object that renders an attribute as a string.
     * @since 2.2.0
     * @see GetAsStringModel
     */
    public Renderable getAsString(Map<String, Object> params) {
        return execute(getRepository().getGetAsString(), params);
    }

    /**
     * Prepares the {@link GetAsStringVModel} for the execution with a block
     * inside {@link #start(Map)} and {@link #end()} calls.
     *
     * @return The tool itself.
     * @since 2.2.0
     * @see GetAsStringModel
     */
    public Tiles2Tool getAsString() {
        currentExecutable = getRepository().getGetAsString();
        return this;
    }

    /**
     * Executes the {@link ImportAttributeVModel}.
     *
     * @param params The map of parameters.
     * @return A renderable object that does not write anything, but imports attribute values when invoked.
     * @since 2.2.0
     * @see ImportAttributeModel
     */
    public Renderable importAttribute(Map<String, Object> params) {
        return execute(getRepository().getImportAttribute(), params);
    }

    /**
     * Executes the {@link InsertAttributeVModel}.
     *
     * @param params The map of parameters.
     * @return A renderable object that renders an attribute.
     * @since 2.2.0
     * @see InsertAttributeModel
     */
    public Renderable insertAttribute(Map<String, Object> params) {
        return execute(getRepository().getInsertAttribute(), params);
    }

    /**
     * Prepares the {@link InsertAttributeVModel} for the execution with a block
     * inside {@link #start(Map)} and {@link #end()} calls.
     *
     * @return The tool itself.
     * @since 2.2.0
     * @see InsertAttributeModel
     */
    public Tiles2Tool insertAttribute() {
        currentExecutable = getRepository().getInsertAttribute();
        return this;
    }

    /**
     * Executes the {@link InsertDefinitionVModel}.
     *
     * @param params The map of parameters.
     * @return A renderable object that renders a definition.
     * @since 2.2.0
     * @see InsertDefinitionModel
     */
    public Renderable insertDefinition(Map<String, Object> params) {
        return execute(getRepository().getInsertDefinition(), params);
    }

    /**
     * Prepares the {@link InsertDefinitionVModel} for the execution with a block
     * inside {@link #start(Map)} and {@link #end()} calls.
     *
     * @return The tool itself.
     * @since 2.2.0
     * @see InsertDefinitionModel
     */
    public Tiles2Tool insertDefinition() {
        currentExecutable = getRepository().getInsertDefinition();
        return this;
    }

    /**
     * Executes the {@link InsertTemplateVModel}.
     *
     * @param params The map of parameters.
     * @return A renderable object that renders a template.
     * @since 2.2.0
     * @see InsertTemplateModel
     */
    public Renderable insertTemplate(Map<String, Object> params) {
        return execute(getRepository().getInsertTemplate(), params);
    }

    /**
     * Prepares the {@link InsertTemplateVModel} for the execution with a block
     * inside {@link #start(Map)} and {@link #end()} calls.
     *
     * @return The tool itself.
     * @since 2.2.0
     * @see InsertTemplateModel
     */
    public Tiles2Tool insertTemplate() {
        currentExecutable = getRepository().getInsertTemplate();
        return this;
    }

    /**
     * Executes the {@link PutAttributeVModel}.
     *
     * @param params The map of parameters.
     * @return The tool itself.
     * @since 2.2.0
     * @see PutAttributeModel
     */
    public Tiles2Tool putAttribute(Map<String, Object> params) {
        execute(getRepository().getPutAttribute(), params);
        return this;
    }

    /**
     * Prepares the {@link PutAttributeVModel} for the execution with a block
     * inside {@link #start(Map)} and {@link #end()} calls.
     *
     * @return The tool itself.
     * @since 2.2.0
     * @see PutAttributeModel
     */
    public Tiles2Tool putAttribute() {
        currentExecutable = getRepository().getPutAttribute();
        return this;
    }

    /**
     * Prepares the {@link PutListAttributeVModel} for the execution with a block
     * inside {@link #start(Map)} and {@link #end()} calls.
     *
     * @return The tool itself.
     * @since 2.2.0
     * @see PutListAttributeModel
     */
    public Tiles2Tool putListAttribute() {
        currentExecutable = getRepository().getPutListAttribute();
        return this;
    }

    /**
     * Starts a "model" for the execution in a block.
     *
     * @param params The map of parameters.
     * @return The tool itself.
     * @since 2.2.0
     */
    public Tiles2Tool start(Map<String, Object> params) {
        if (currentExecutable == null) {
            throw new NullPointerException("The current model to start has not been set");
        }
        currentExecutable.start(getRequest(), getResponse(), getVelocityContext(), params);
        return this;
    }

    /**
     * Ends a "model" after the execution of a block.
     *
     * @return A renderable object. It can render actually something, or execute
     * code needed to the execution of parent models.
     * @since 2.2.0
     */
    public Renderable end() {
        if (currentExecutable == null) {
            throw new NullPointerException("The current model to start has not been set");
        }
        Renderable retValue = currentExecutable.end(getRequest(),
                getResponse(), getVelocityContext());
        currentExecutable = null;
        return retValue;
    }

    /**
     * Gets or creates the Tiles+Velocity model repository from the servlet context.
     *
     * @return The model repository.
     */
    private TilesVelocityRepository getRepository() {
        if (repository != null) {
            return repository;
        }

        repository = (TilesVelocityRepository) getServletContext()
                .getAttribute(TILES_VELOCITY_REPOSITORY_KEY);
        if (repository == null) {
            repository = new TilesVelocityRepository(getServletContext());
            getServletContext().setAttribute(TILES_VELOCITY_REPOSITORY_KEY,
                    repository);
        }
        return repository;
    }

    /**
     * Executes an "executable" model.
     *
     * @param executable The object to execute.
     * @param params The parameters map.
     * @return A renderable object. It can render actually something, or execute
     * code needed to the execution of parent models.
     */
    private Renderable execute(Executable executable, Map<String, Object> params) {
        return executable.execute(getRequest(), getResponse(), getVelocityContext(), params);
    }
}
