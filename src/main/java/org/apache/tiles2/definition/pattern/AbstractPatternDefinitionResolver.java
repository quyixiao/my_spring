/*
 * $Id: AbstractPatternDefinitionResolver.java 823662 2009-10-09 18:48:03Z apetrelli $
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tiles2.Definition;

/**
 * A pattern definition resolver that stores {@link org.apache.tiles2.definition.pattern.DefinitionPatternMatcher}
 * separated by customization key. <br>
 * Implementations should provide a way to translate a definition to a
 * {@link org.apache.tiles2.definition.pattern.DefinitionPatternMatcher}.
 *
 * @param <T> The type of the customization key.
 * @version $Rev: 823662 $ $Date: 2009-10-09 20:48:03 +0200 (ven, 09 ott 2009) $
 * @since 2.2.0
 */
public abstract class AbstractPatternDefinitionResolver<T> implements
        PatternDefinitionResolver<T> {

    /**
     * Stores patterns depending on the locale they refer to.
     */
    private Map<T, List<org.apache.tiles2.definition.pattern.DefinitionPatternMatcher>> localePatternPaths =
        new HashMap<T, List<org.apache.tiles2.definition.pattern.DefinitionPatternMatcher>>();

    /** {@inheritDoc} */
    public Definition resolveDefinition(String name, T customizationKey) {
        Definition retValue = null;
        if (localePatternPaths.containsKey(customizationKey)) {
            retValue = searchAndResolveDefinition(localePatternPaths
                    .get(customizationKey), name);
        }
        return retValue;
    }

    /** {@inheritDoc} */
    public Map<String, Definition> storeDefinitionPatterns(Map<String, Definition> localeDefsMap,
            T customizationKey) {
        List<org.apache.tiles2.definition.pattern.DefinitionPatternMatcher> lpaths = localePatternPaths
                .get(customizationKey);
        if (lpaths == null) {
            lpaths = new ArrayList<org.apache.tiles2.definition.pattern.DefinitionPatternMatcher>();
            localePatternPaths.put(customizationKey, lpaths);
        }

        return addDefinitionsAsPatternMatchers(lpaths, localeDefsMap);
    }

    /**
     * Adds definitions, filtering and adding them to the list of definition
     * pattern matchers. Only a subset of definitions will be transformed into
     * definition pattern matchers.
     *
     * @param matchers The list containing the currently stored definition pattern
     * matchers.
     * @param defsMap The definition map to parse.
     * @return The map of the definitions not recognized as containing
     * definition patterns.
     * @since 2.2.1
     */
    protected abstract Map<String, Definition> addDefinitionsAsPatternMatchers(
            List<org.apache.tiles2.definition.pattern.DefinitionPatternMatcher> matchers,
            Map<String, Definition> defsMap);

    /**
     * Try to resolve a definition by iterating all pattern matchers.
     *
     * @param paths The list containing the currently stored paths.
     * @param name The name of the definition to resolve.
     * @return A definition, if found, or <code>null</code> if not.
     */
    private Definition searchAndResolveDefinition(
            List<org.apache.tiles2.definition.pattern.DefinitionPatternMatcher> paths, String name) {
        Definition d = null;

        for (DefinitionPatternMatcher wm : paths) {
            d = wm.createDefinition(name);
            if (d != null) {
                break;
            }
        }

        return d;
    }
}
