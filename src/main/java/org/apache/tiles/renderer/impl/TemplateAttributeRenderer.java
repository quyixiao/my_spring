/*
 * $Id: TemplateAttributeRenderer.java 821299 2009-10-03 12:15:05Z apetrelli $
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
package org.apache.tiles.renderer.impl;

import java.io.IOException;

import org.apache.tiles.Attribute;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.impl.InvalidTemplateException;

/**
 * Renders an attribute that contains a reference to a template.
 *
 * @version $Rev: 821299 $ $Date: 2009-10-03 14:15:05 +0200 (sab, 03 ott 2009) $
 * @since 2.1.0
 */
public class TemplateAttributeRenderer extends AbstractTypeDetectingAttributeRenderer {

    /** {@inheritDoc} */
    @Override
    public void write(Object value, Attribute attribute,
            TilesRequestContext request)
            throws IOException {
        if (value != null) {
            if (value instanceof String) {
                request.dispatch(value.toString());
            } else {
                throw new InvalidTemplateException(
                        "Cannot render a template that is not a string: "
                                + value.toString());
            }
        } else {
            throw new InvalidTemplateException("Cannot render a null template");
        }
    }

    /** {@inheritDoc} */
    public boolean isRenderable(Object value, Attribute attribute,
            TilesRequestContext request) {
        if (value instanceof String) {
            return ((String) value).startsWith("/");
        }
        return false;
    }
}
