/*
 * $Id: DefaultAttributeResolver.java 788032 2009-06-24 14:08:32Z apetrelli $
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

package org.apache.tiles2.template;

import org.apache.tiles2.Attribute;
import org.apache.tiles2.AttributeContext;
import org.apache.tiles2.Expression;
import org.apache.tiles2.TilesContainer;

/**
 * The default implementation of AttributeResolver.
 *
 * @version $Rev: 788032 $ $Date: 2009-06-24 16:08:32 +0200 (mer, 24 giu 2009) $
 * @since 2.2.0
 */
public class DefaultAttributeResolver implements AttributeResolver {

    /** {@inheritDoc} */
    public Attribute computeAttribute(TilesContainer container, Attribute attribute,
            String name, String role, boolean ignore,
            Object defaultValue, String defaultValueRole, String defaultValueType, Object... requestItems) {
        if (attribute == null) {
            AttributeContext evaluatingContext = container
                    .getAttributeContext(requestItems);
            attribute = evaluatingContext.getAttribute(name);
            if (attribute == null) {
                attribute = computeDefaultAttribute(defaultValue,
                        defaultValueRole, defaultValueType);
                if (attribute == null && !ignore) {
                    throw new NoSuchAttributeException("Attribute '" + name
                            + "' not found.");
                }
            }
        }
        if (attribute != null && role != null && !"".equals(role.trim())) {
            attribute = new Attribute(attribute);
            attribute.setRole(role);
        }
        return attribute;
    }

    /**
     * Computes the default attribute.
     *
     * @param defaultValue The default value of the attribute.
     * @param defaultValueRole The default role of tha attribute.
     * @param defaultValueType The default type of the attribute.
     * @return The default attribute.
     */
    private Attribute computeDefaultAttribute(Object defaultValue,
            String defaultValueRole, String defaultValueType) {
        Attribute attribute = null;
        if (defaultValue != null) {
            if (defaultValue instanceof Attribute) {
                attribute = (Attribute) defaultValue;
            } else if (defaultValue instanceof String) {
                attribute = new Attribute(defaultValue, (Expression) null,
                        defaultValueRole, defaultValueType);
            }
        }
        return attribute;
    }
}
