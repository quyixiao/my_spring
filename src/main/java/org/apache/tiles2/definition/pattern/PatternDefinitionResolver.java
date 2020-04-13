/*
 * $Id: PatternDefinitionResolver.java 823662 2009-10-09 18:48:03Z apetrelli $
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

package org.apache.tiles2.definition.pattern;

import java.util.Map;

import org.apache.tiles2.Definition;

/**
 * Resolves a definition starting from patterns stored in definition maps.
 *
 * @param <T> The type of the customization key.
 * @version $Rev: 823662 $ $Date: 2009-10-09 20:48:03 +0200 (ven, 09 ott 2009) $
 * @since 2.2.0
 */
public interface PatternDefinitionResolver<T> {

    /**
     * Stores definition patterns.
     *
     * @param localeDefsMap The map of definitions that may contain also
     * patterns.
     * @param customizationKey The customization key.
     * @return The map of the definitions not recognized as containing
     * definition patterns.
     * @since 2.2.1
     */
    Map<String, Definition> storeDefinitionPatterns(Map<String, Definition> localeDefsMap,
                                                    T customizationKey);

    /**
     * Resolves a definition searching in all patterns for the requested
     * customization key.
     *
     * @param name The name of the definition.
     * @param customizationKey The customization key.
     * @return The resolved definition.
     * @since 2.2.0
     */
    Definition resolveDefinition(String name, T customizationKey);
}
