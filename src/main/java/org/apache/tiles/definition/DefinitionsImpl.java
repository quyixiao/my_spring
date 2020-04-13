/*
 * $Id: DefinitionsImpl.java 791161 2009-07-04 18:53:36Z apetrelli $
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

package org.apache.tiles.definition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.tiles.Attribute;
import org.apache.tiles.Definition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Rev: 791161 $ $Date: 2009-07-04 20:53:36 +0200 (sab, 04 lug 2009) $
 *
 * @deprecated This class is, in fact, part of the implementation of
 * {@link UrlDefinitionsFactory} and
 * {@link org.apache.tiles.definition.dao.ResolvingLocaleUrlDefinitionDAO}.
 */
@Deprecated
public class DefinitionsImpl implements Definitions {

    /**
     * Commons Logging instance.
     */
    private final Logger log = LoggerFactory.getLogger(DefinitionsImpl.class);

    /**
     * The base set of Definition objects not discriminated by locale.
     */
    private Map<String, Definition> baseDefinitions;
    /**
     * The locale-specific set of definitions objects.
     */
    private Map<Locale, Map<String, Definition>> localeSpecificDefinitions;

    /**
     * Creates a new instance of DefinitionsImpl.
     */
    public DefinitionsImpl() {
        baseDefinitions = new HashMap<String, Definition>();
        localeSpecificDefinitions =
            new HashMap<Locale, Map<String, Definition>>();
    }

    /**
     * Creates a new instance of DefinitionsImpl to be used as a wrapper.
     *
     * @param baseDefinitions The base definitions to use.
     * @param localeSpecificDefinitions Maps a locale to a map of definitions.
     */
    public DefinitionsImpl(Map<String, Definition> baseDefinitions,
            Map<Locale, Map<String, Definition>> localeSpecificDefinitions) {
        this.baseDefinitions = baseDefinitions;
        this.localeSpecificDefinitions = localeSpecificDefinitions;
    }

    /**
     * Returns a Definition object that matches the given name.
     *
     * @param name The name of the Definition to return.
     * @return the Definition matching the given name or null if none
     *         is found.
     */
    public Definition getDefinition(String name) {
        return baseDefinitions.get(name);
    }

    /**
     * Adds new Definition objects to the internal collection and
     * resolves inheritance attraibutes.
     *
     * @param defsMap The new definitions to add.
     * @throws NoSuchDefinitionException If something goes wrong during
     * addition.
     */
    public void addDefinitions(Map<String, Definition> defsMap) {
        this.baseDefinitions.putAll(defsMap);
        resolveInheritances();
    }

    /**
     * Adds new locale-specific Definition objects to the internal
     * collection and resolves inheritance attraibutes.
     *
     * @param defsMap The new definitions to add.
     * @param locale  The locale to add the definitions to.
     * @throws NoSuchDefinitionException If something goes wrong during
     * inheritance resolution.
     */
    public void addDefinitions(Map<String, Definition> defsMap,
            Locale locale) {
        localeSpecificDefinitions.put(locale, defsMap);
        resolveInheritances(locale);
    }

    /**
     * Returns a Definition object that matches the given name and locale.
     *
     * @param name   The name of the Definition to return.
     * @param locale The locale to use to resolve the definition.
     * @return the Definition matching the given name or null if none
     *         is found.
     */
    public Definition getDefinition(String name, Locale locale) {
        Definition definition = null;

        if (locale != null) {
            Map<String, Definition> localeSpecificMap =
                localeSpecificDefinitions.get(locale);
            if (localeSpecificMap != null) {
                definition = localeSpecificMap.get(name);
            }
        }

        if (definition == null) {
            definition = getDefinition(name);
        }

        return definition;
    }

    /**
     * Resolve extended instances.
     *
     * @throws NoSuchDefinitionException If a parent definition is not found.
     */
    public void resolveInheritances() {
        Set<String> alreadyResolvedDefinitions = new HashSet<String>();

        for (Definition definition : baseDefinitions.values()) {
            resolveInheritance(definition, null, alreadyResolvedDefinitions);
        }  // end loop
    }

    /**
     * Resolve locale-specific extended instances.
     *
     * @param locale The locale to use.
     * @throws NoSuchDefinitionException If a parent definition is not found.
     */
    public void resolveInheritances(Locale locale) {
        resolveInheritances();

        Map<String, Definition> map = localeSpecificDefinitions.get(locale);
        if (map != null) {
            Set<String> alreadyResolvedDefinitions = new HashSet<String>();
            for (Definition definition : map.values()) {
                resolveInheritance(definition, locale,
                        alreadyResolvedDefinitions);
            }  // end loop
        }
    }

    /**
     * Clears definitions.
     */
    public void reset() {
        this.baseDefinitions = new HashMap<String, Definition>();
        this.localeSpecificDefinitions =
            new HashMap<Locale, Map<String, Definition>>();
    }

    /**
     * Returns base definitions collection.
     *
     * @return The base (i.e. not depending on any locale) definitions map.
     */
    public Map<String, Definition> getBaseDefinitions() {
        return baseDefinitions;
    }

    /**
     * Searches for a definition specified as an attribute.
     *
     * @param attr   The attribute to use.
     * @param locale The locale to search into.
     * @return The required definition if found, otherwise it returns
     *         <code>null</code>.
     */
    protected Definition getDefinitionByAttribute(
        Attribute attr, Locale locale) {
        Definition retValue = null;

        Object attrValue = attr.getValue();
        if (attrValue instanceof String) {
            retValue = this.getDefinition((String) attr
                .getValue(), locale);
        }

        return retValue;
    }

    /**
     * Resolve locale-specific inheritance.
     * First, resolve parent's inheritance, then set template to the parent's
     * template.
     * Also copy attributes setted in parent, and not set in child
     * If instance doesn't extend anything, do nothing.
     *
     * @param definition The definition to resolve
     * @param locale The locale to use.
     * @param alreadyResolvedDefinitions The set of the definitions that have
     * been already resolved.
     * @throws NoSuchDefinitionException If an inheritance can not be solved.
     */
    protected void resolveInheritance(Definition definition, Locale locale,
            Set<String> alreadyResolvedDefinitions) {
        // Already done, or not needed ?
        if (!definition.isExtending()
                || alreadyResolvedDefinitions.contains(definition.getName())) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Resolve definition for child name='"
                + definition.getName()
                + "' extends='" + definition.getExtends() + "'.");
        }

        // Set as visited to avoid endless recurisvity.
        alreadyResolvedDefinitions.add(definition.getName());

        // Resolve parent before itself.
        Definition parent = getDefinition(definition.getExtends(),
            locale);
        if (parent == null) { // error
            String msg = "Error while resolving definition inheritance: child '"
                + definition.getName()
                + "' can't find its ancestor '"
                + definition.getExtends()
                + "'. Please check your description file.";
            log.error(msg);
            // to do : find better exception
            throw new NoSuchDefinitionException(msg);
        }

        resolveInheritance(parent, locale, alreadyResolvedDefinitions);

        definition.inherit(parent);
    }

    /**
     * Overloads a child definition with a given parent.
     * All attributes present in child are kept. All missing attributes are
     * copied from the parent.
     * Special attribute 'template','role' and 'extends' are overloaded in child
     * if not defined
     *
     * @param parent The parent definition.
     * @param child  The child that will be overloaded.
     * @deprecated Use {@link Definition#inherit(org.apache.tiles.AttributeContext)}.
     */
    @Deprecated
    protected void overload(Definition parent, Definition child) {
        child.inherit(parent);
    }
}
