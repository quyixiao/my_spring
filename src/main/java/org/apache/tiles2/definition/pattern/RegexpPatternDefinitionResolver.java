/*
 * $Id: RegexpPatternDefinitionResolver.java 823662 2009-10-09 18:48:03Z apetrelli $
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tiles2.Definition;

/**
 * Matches definition patterns through the use of regular expressions. To allow the use of regular expression,
 * remember to set the definition name with a tilde (~) as the first character.
 *
 * @param <T> The customization key class.
 * @version $Rev: 823662 $ $Date: 2009-10-09 20:48:03 +0200 (ven, 09 ott 2009) $
 * @since 2.2.0
 */
public class RegexpPatternDefinitionResolver<T> implements
        PatternDefinitionResolver<T> {

    /**
     * Maps a customization key to a pattern mapping list.
     */
    private Map<T, List<PatternMapping>> key2patternMappingList = new HashMap<T, List<PatternMapping>>();

    /** {@inheritDoc} */
    public Definition resolveDefinition(String name, T customizationKey) {
        Definition retValue = null;
        List<PatternMapping> mappings = key2patternMappingList.get(customizationKey);
        if (mappings != null) {
            for (PatternMapping mapping : mappings) {
                Matcher matcher = mapping.pattern.matcher(name);
                if (matcher.matches()) {
                    int groupCount = matcher.groupCount() + 1;
                    Object[] vars = new Object[groupCount];
                    for (int i = 0; i < groupCount; i++) {
                        vars[i] = matcher.group(i);
                    }
                    retValue = org.apache.tiles2.definition.pattern.PatternUtil.replacePlaceholders(mapping.definition, name, vars);
                    break;
                }
            }
        }
        return retValue;
    }

    /** {@inheritDoc} */
    public Map<String, Definition> storeDefinitionPatterns(Map<String, Definition> localeDefsMap,
            T customizationKey) {
        List<PatternMapping> patternMappingList = key2patternMappingList.get(customizationKey);
        if (patternMappingList == null) {
            patternMappingList = new ArrayList<PatternMapping>();
            key2patternMappingList.put(customizationKey, patternMappingList);
        }
        return addRegexpMappings(localeDefsMap, patternMappingList);
    }

    /**
     * Adds the regular expression mappings.
     *
     * @param localeDefsMap The map containing the definitions.
     * @param patternMappingList The list of pattern mapping.
     * @return The map of the definitions not recognized as containing
     * definition patterns.
     */
    private Map<String, Definition> addRegexpMappings(Map<String, Definition> localeDefsMap,
            List<PatternMapping> patternMappingList) {
        Set<String> excludedKeys = new LinkedHashSet<String>();
        for (Map.Entry<String, Definition> entry : localeDefsMap.entrySet()) {
            String name = entry.getKey();
            if (name.startsWith("~")) {
                patternMappingList.add(new PatternMapping(name.substring(1),
                        new Definition(entry.getValue())));
            } else {
                excludedKeys.add(name);
            }
        }
        return PatternUtil.createExtractedMap(localeDefsMap, excludedKeys);
    }

    /**
     * Maps a pattern to a definition.
     *
     * @version $Rev: 823662 $ $Date: 2009-10-09 20:48:03 +0200 (ven, 09 ott 2009) $
     * @since 2.2.0
     */
    private static final class PatternMapping {

        /**
         * The pattern.
         */
        private Pattern pattern;

        /**
         * The definition.
         */
        private Definition definition;

        /**
         * Constructor.
         *
         * @param regexp The regular expression for the pattern.
         * @param definition The definition.
         * @since 2.2.0
         */
        private PatternMapping(String regexp, Definition definition) {
            pattern = Pattern.compile(regexp);
            this.definition = definition;
        }
    }
}
