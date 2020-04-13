/*
 * $Id: CachingLocaleUrlDefinitionDAO.java 823662 2009-10-09 18:48:03Z apetrelli $
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

package org.apache.tiles2.definition.dao;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.tiles2.Definition;
import org.apache.tiles2.definition.Refreshable;
import org.apache.tiles2.util.LocaleUtil;
import org.apache.tiles2.definition.DefinitionsFactoryException;
import org.apache.tiles2.definition.NoSuchDefinitionException;
import org.apache.tiles2.definition.pattern.BasicPatternDefinitionResolver;
import org.apache.tiles2.definition.pattern.PatternDefinitionResolver;
import org.apache.tiles2.definition.pattern.PatternDefinitionResolverAware;
import org.apache.tiles2.definition.pattern.wildcard.WildcardDefinitionPatternMatcherFactory;

/**
 * <p>
 * A definitions DAO (loading URLs and using Locale as a customization key) that
 * caches definitions that have been loaded in a raw way (i.e. with inheritance
 * that is not resolved).
 * </p>
 * <p>
 * It can check if the URLs change, but by default this feature is turned off.
 * </p>
 *
 * @version $Rev: 823662 $ $Date: 2009-10-09 20:48:03 +0200 (ven, 09 ott 2009) $
 * @since 2.1.0
 */
public class CachingLocaleUrlDefinitionDAO extends BaseLocaleUrlDefinitionDAO
        implements Refreshable, PatternDefinitionResolverAware<Locale> {

    /**
     * Initialization parameter to set whether we want to refresh URLs when they
     * change.
     *
     * @since 2.1.0
     */
    public static final String CHECK_REFRESH_INIT_PARAMETER =
        "org.apache.tiles.definition.dao.LocaleUrlDefinitionDAO.CHECK_REFRESH";

    /**
     * The locale-specific set of definitions objects.
     *
     * @since 2.1.0
     */
    protected Map<Locale, Map<String, Definition>> locale2definitionMap;

    /**
     * Flag that, when <code>true</code>, enables automatic checking of URLs
     * changing.
     *
     * @since 2.1.0
     */
    protected boolean checkRefresh = false;

    /**
     * Resolves definitions using patterns.
     *
     * @since 2.2.0
     */
    protected org.apache.tiles2.definition.pattern.PatternDefinitionResolver<Locale> definitionResolver;

    /**
     * Constructor.
     *
     * @since 2.1.0
     */
    public CachingLocaleUrlDefinitionDAO() {
        locale2definitionMap = new HashMap<Locale, Map<String, Definition>>();
    }

    /** {@inheritDoc} */
    @Override
    public void init(Map<String, String> params) {
        super.init(params);

        String param = params.get(CHECK_REFRESH_INIT_PARAMETER);
        checkRefresh = "true".equals(param);
        org.apache.tiles2.definition.pattern.wildcard.WildcardDefinitionPatternMatcherFactory definitionPatternMatcherFactory =
            new WildcardDefinitionPatternMatcherFactory();
        definitionResolver = new BasicPatternDefinitionResolver<Locale>(
                definitionPatternMatcherFactory,
                definitionPatternMatcherFactory);
    }

    /** {@inheritDoc} */
    public void setPatternDefinitionResolver(
            PatternDefinitionResolver<Locale> definitionResolver) {
        this.definitionResolver = definitionResolver;
    }

    /** {@inheritDoc} */
    public Definition getDefinition(String name, Locale customizationKey) {
        Definition retValue = null;
        if (customizationKey == null) {
            customizationKey = LocaleUtil.NULL_LOCALE;
        }
        Map<String, Definition> definitions = getDefinitions(customizationKey);
        if (definitions != null) {
            retValue = definitions.get(name);

            if (retValue == null) {
                retValue = getDefinitionFromResolver(name, customizationKey);

                if (retValue != null) {
                    try {
                        synchronized (definitions) {
                            definitions.put(name, retValue);
                        }
                    } catch (NoSuchDefinitionException ex) {
                        throw new IllegalStateException(
                                "Unable to resolve wildcard mapping", ex);
                    }
                }
            }
        }

        return retValue;
    }

    /** {@inheritDoc} */
    public Map<String, Definition> getDefinitions(Locale customizationKey) {
        if (customizationKey == null) {
            customizationKey = LocaleUtil.NULL_LOCALE;
        }
        Map<String, Definition> retValue = locale2definitionMap
                .get(customizationKey);
        if (retValue == null || (checkRefresh && refreshRequired())) {
            retValue = checkAndloadDefinitions(customizationKey);
        }
        return retValue;
    }

    /** {@inheritDoc} */
    public synchronized void refresh() {
        if (refreshRequired()) {
            locale2definitionMap.clear();
        }
    }

    /**
     * Sets the flag to check URL refresh. If not called, the default is
     * <code>false</code>.
     *
     * @param checkRefresh When <code>true</code>, enables automatic checking
     * of URLs changing.
     * @since 2.1.0
     */
    public void setCheckRefresh(boolean checkRefresh) {
        this.checkRefresh = checkRefresh;
    }

    /**
     * Returns a definition from the definition resolver.
     *
     * @param name The name of the definition.
     * @param customizationKey The customization key to use.
     * @return The resolved definition.
     */
    protected Definition getDefinitionFromResolver(String name,
            Locale customizationKey) {
        return definitionResolver.resolveDefinition(name,
                customizationKey);
    }

    /**
     * Checks if URLs have changed. If yes, it clears the cache. Then continues
     * loading definitions.
     *
     * @param customizationKey The locale to use when loading URLs.
     * @return The loaded definitions.
     * @since 2.1.0
     */
    protected synchronized Map<String, Definition> checkAndloadDefinitions(
            Locale customizationKey) {
        if (checkRefresh && refreshRequired()) {
            locale2definitionMap.clear();
        }
        loadDefinitions(customizationKey);
        return locale2definitionMap.get(customizationKey);
    }

    /**
     * Tries to load definitions if necessary.
     *
     * @param customizationKey The locale to use when loading URLs.
     * @return The loaded definitions.
     * @since 2.1.0
     */
    protected Map<String, Definition> loadDefinitions(Locale customizationKey) {
        Map<String, Definition> localeDefsMap = locale2definitionMap
                .get(customizationKey);
        if (localeDefsMap != null) {
            return localeDefsMap;
        }

        return loadDefinitionsFromURLs(customizationKey);
    }

    /**
     * Loads definitions from the URLs.
     *
     * @param customizationKey The locale to use when loading URLs.
     * @return The loaded definitions.
     * @since 2.1.0
     */
    protected Map<String, Definition> loadDefinitionsFromURLs(Locale customizationKey) {
        Map<String, Definition> localeDefsMap;

        String postfix = LocaleUtil.calculatePostfix(customizationKey);
        Locale parentLocale = LocaleUtil.getParentLocale(customizationKey);
        localeDefsMap = new LinkedHashMap<String, Definition>();
        if (parentLocale != null) {
            Map<String, Definition> parentDefs = loadParentDefinitions(parentLocale);
            if (parentDefs != null) {
                localeDefsMap.putAll(parentDefs);
            }
        }
        // For each source, the URL must be loaded.
        for (URL url : sourceURLs) {
            String path = url.toExternalForm();

            String newPath = LocaleUtil.concatPostfix(path, postfix);
            try {
                URL newUrl = new URL(newPath);
                Map<String, Definition> defsMap = loadDefinitionsFromURL(newUrl);
                if (defsMap != null) {
                    localeDefsMap.putAll(defsMap);
                }
            } catch (MalformedURLException e) {
                throw new DefinitionsFactoryException("Error parsing URL "
                        + newPath, e);
            }
        }
        Map<String, Definition> defsMap = definitionResolver
                .storeDefinitionPatterns(copyDefinitionMap(localeDefsMap),
                        customizationKey);
        locale2definitionMap.put(customizationKey, defsMap);
        return localeDefsMap;
    }

    /**
     * Loads the raw definitions from the URLs associated with a locale.
     *
     * @param customizationKey The locale to use when loading URLs.
     * @return The loaded definitions.
     * @since 2.1.3
     * @deprecated Use {@link #loadDefinitionsFromURLs(Locale)}.
     */
    protected Map<String, Definition> loadRawDefinitionsFromURLs(
            Locale customizationKey) {
        return loadDefinitionsFromURLs(customizationKey);
    }

    /**
     * Loads parent definitions, i.e. definitions mapped to a parent locale.
     *
     * @param parentLocale The locale to use when loading URLs.
     * @return The loaded parent definitions.
     * @since 2.1.0
     */
    protected Map<String, Definition> loadParentDefinitions(Locale parentLocale) {
        return loadDefinitions(parentLocale);
    }

    /**
     * Operations to be done after definitions are loaded.
     *
     * @param localeDefsMap The loaded definitions.
     * @param customizationKey The locale to use when loading URLs.
     * @since 2.1.0
     * @deprecated Never used.
     */
    protected void postDefinitionLoadOperations(
            Map<String, Definition> localeDefsMap, Locale customizationKey) {

        definitionResolver.storeDefinitionPatterns(localeDefsMap, customizationKey);
    }

    /**
     * Copies the definition map to be passed to a higher level of customization
     * key.
     *
     * @param localeDefsMap The map of definition to be copied.
     * @return The copy of the definition map. This particular implementation
     * return the <code>localeDefsMap</code> itself.
     * @since 2.1.4
     */
    protected Map<String, Definition> copyDefinitionMap(
            Map<String, Definition> localeDefsMap) {
        return localeDefsMap;
    }
}
