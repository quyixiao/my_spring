/*
 * $Id: DefinitionsFactory.java 822631 2009-10-07 09:21:12Z apetrelli $
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

package org.apache.tiles2.definition;

import org.apache.tiles2.context.TilesRequestContext;
import org.apache.tiles2.Definition;
import org.apache.tiles2.locale.LocaleResolver;

import java.util.Map;

/**
 * Interface for creating a {@link Definition}s and managing their contents.
 * <p/>
 * <p>
 * DefinitionsFactory implementations are responsible for maintaining the data
 * sources of Tiles configuration data and using the data to create Definitions
 * sets. Implementations also know how to append locale-specific configuration
 * data to an existing Definitions set.
 * </p>
 *
 * @version $Rev: 822631 $ $Date: 2009-10-07 11:21:12 +0200 (mer, 07 ott 2009) $
 */
public interface DefinitionsFactory {

    /**
     * Property name that specifies the implementation of the DefinitionsReader.
     */
    String READER_IMPL_PROPERTY =
        "org.apache.tiles.definition.DefinitionsReader";

    /**
     * Property name that specifies the implementation of
     * {@link LocaleResolver}.
     */
    String LOCALE_RESOLVER_IMPL_PROPERTY =
        "org.apache.tiles.locale.LocaleResolver";

    /**
     * Constant representing the configuration parameter
     * used to define the tiles definition resources.
     *
     * @since 2.1.0
     */
    String DEFINITIONS_CONFIG = "org.apache.tiles.definition.DefinitionsFactory.DEFINITIONS_CONFIG";

    /**
     * Constant representing the configuration parameter used to define the
     * definition DAO to use.
     */
    String DEFINITION_DAO_INIT_PARAM =
        "org.apache.tiles.definition.DefinitionsFactory.DefinitionDAO";


    /**
     * Initializes the DefinitionsFactory and its subcomponents. <p/>
     * Implementations may support configuration properties to be passed in via
     * the params Map.
     *
     * @param params The Map of configuration properties.
     * @throws DefinitionsFactoryException If a Tiles exception, such as an initialization
     * error, occurs.
     * @deprecated Parameter based initialization is deprecated, please compose your
     * definitions factory using methods.
     */
    void init(Map<String, String> params);

    /**
     * Returns a Definition object that matches the given name and
     * Tiles context.
     *
     * @param name         The name of the Definition to return.
     * @param tilesContext The Tiles context to use to resolve the definition.
     * @return the Definition matching the given name or null if none
     *         is found.
     */
    Definition getDefinition(String name, TilesRequestContext tilesContext);

    /**
     * Adds a source where Definition objects are stored.
     * <p/>
     * Implementations should publish what type of source object they expect.
     * The source should contain enough information to resolve a configuration
     * source containing definitions.  The source should be a "base" source for
     * configurations.  Internationalization and Localization properties will be
     * applied by implementations to discriminate the correct data sources based
     * on locale.
     *
     * @param source The configuration source for definitions.
     * @deprecated Let the Definitions Factory load its sources by itself.
     */
    @Deprecated
    void addSource(Object source);

    /**
     * Creates and returns a {@link Definitions} set by reading
     * configuration data from the applied sources.
     *
     * @return The read definitions.
     * @deprecated Let the Definitions Factory use it.
     */
    @Deprecated
    Definitions readDefinitions();
}
