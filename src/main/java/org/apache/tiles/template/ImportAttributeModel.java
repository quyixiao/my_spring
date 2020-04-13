/*
 * $Id: ImportAttributeModel.java 795354 2009-07-18 12:32:28Z apetrelli $
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

package org.apache.tiles.template;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.tiles.Attribute;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.TilesContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * <strong>Import attribute(s) in specified context.</strong>
 * </p>
 * <p>
 * Import attribute(s) to requested scope. Attribute name and scope are
 * optional. If not specified, all attributes are imported in page scope. Once
 * imported, an attribute can be used as any other beans from jsp contexts.
 * </p>
 *
 * @version $Rev: 795354 $ $Date: 2009-07-18 14:32:28 +0200 (sab, 18 lug 2009) $
 * @since 2.2.0
 */
public class ImportAttributeModel {

    /**
     * The logging object.
     */
    private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Retuns a Map that contains the attributes to be imported. The importing
     * code must be done by the caller.
     *
     * @param container The Tiles container to use.
     * @param name The attribute to import. If null, all the attributes will be
     * imported.
     * @param toName The destination name of the attribute to import. Valid only
     * if <code>name</code> is specified.
     * @param ignore If <code>true</code> and the attribute is not found, or an
     * exception happens, the problem will be ignored.
     * @param requestItems The request objects.
     * @return A Map of the attributes to be imported: the key is the name of an
     * attribute, the value is the value of that attribute.
     * @since 2.2.0
     */
    public Map<String, Object> getImportedAttributes(TilesContainer container,
            String name, String toName, boolean ignore, Object... requestItems) {
        Map<String, Object> retValue = new HashMap<String, Object>();
        AttributeContext attributeContext = container
                .getAttributeContext(requestItems);
        // Some tags allow for unspecified attributes. This
        // implies that the tag should use all of the attributes.
        if (name != null) {
            importSingleAttribute(container, attributeContext, name, toName,
                    ignore, retValue, requestItems);
        } else {
            importAttributes(attributeContext.getCascadedAttributeNames(),
                    container, attributeContext, retValue, ignore, requestItems);
            importAttributes(attributeContext.getLocalAttributeNames(),
                    container, attributeContext, retValue, ignore, requestItems);
        }
        return retValue;
    }

    /**
     * Imports a single attribute.
     *
     * @param container The Tiles container to use.
     * @param attributeContext The context from which the attributes will be
     * got.
     * @param name The name of the attribute.
     * @param toName The name of the destination attribute. If null,
     * <code>name</code> will be used.
     * @param ignore If <code>true</code> and the attribute is not found, or an
     * exception happens, the problem will be ignored.
     * @param attributes The map of the attributes to fill.
     * @param requestItems The request objects.
     */
    private void importSingleAttribute(TilesContainer container,
            AttributeContext attributeContext, String name, String toName,
            boolean ignore, Map<String, Object> attributes,
            Object... requestItems) {
        Attribute attr = attributeContext.getAttribute(name);
        if (attr != null) {
            try {
                Object attributeValue = container.evaluate(attr,
                        requestItems);
                if (attributeValue == null) {
                    if (!ignore) {
                        throw new NoSuchAttributeException(
                                "Error importing attributes. " + "Attribute '"
                                        + name + "' has a null value ");
                    }
                } else {
                    if (toName != null) {
                        attributes.put(toName, attributeValue);
                    } else {
                        attributes.put(name, attributeValue);
                    }
                }
            } catch (RuntimeException e) {
                if (!ignore) {
                    throw e;
                } else if (log.isDebugEnabled()) {
                    log.debug("Ignoring Tiles Exception", e);
                }
            }
        } else if (!ignore) {
            throw new NoSuchAttributeException(
                    "Error importing attributes. " + "Attribute '" + name
                            + "' is null");
        }
    }

    /**
     * Imports all the attributes.
     *
     * @param names The names of the attributes to be imported.
     * @param container The Tiles container to use.
     * @param attributeContext The context from which the attributes will be
     * got.
     * @param attributes The map of the attributes to fill.
     * @param ignore If <code>true</code> and the attribute is not found, or an
     * exception happens, the problem will be ignored.
     * @param requestItems The request objects.
     */
    private void importAttributes(Collection<String> names,
            TilesContainer container, AttributeContext attributeContext,
            Map<String, Object> attributes, boolean ignore,
            Object... requestItems) {
        if (names == null || names.isEmpty()) {
            return;
        }

        for (String name : names) {
            if (name == null && !ignore) {
                throw new NoSuchAttributeException(
                        "Error importing attributes. "
                                + "Attribute with null key found.");
            } else if (name == null) {
                continue;
            }

            importSingleAttribute(container, attributeContext, name, name,
                    ignore, attributes, requestItems);
        }
    }
}
