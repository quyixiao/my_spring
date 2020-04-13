/*
 * $Id: UntypedAttributeRenderer.java 821299 2009-10-03 12:15:05Z apetrelli $
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
package org.apache.tiles2.renderer.impl;

import java.io.IOException;

import org.apache.tiles2.Attribute;
import org.apache.tiles2.TilesContainer;
import org.apache.tiles2.context.TilesRequestContext;
import org.apache.tiles2.renderer.RendererException;
import org.apache.tiles2.awareness.TilesContainerAware;

/**
 * Renders an attribute that has no associated renderer.
 *
 * @version $Rev: 821299 $ $Date: 2009-10-03 14:15:05 +0200 (sab, 03 ott 2009) $
 * @since 2.1.0
 * @deprecated Use {@link ChainedDelegateAttributeRenderer}.
 */
public class UntypedAttributeRenderer extends AbstractBaseAttributeRenderer
        implements TilesContainerAware {

    /**
     * The Tiles container.
     */
    private TilesContainer container;

    /** {@inheritDoc} */
    public void setContainer(TilesContainer container) {
        this.container = container;
    }

    /** {@inheritDoc} */
    @Override
    public void write(Object value, Attribute attribute,
            TilesRequestContext request)
            throws IOException {
        if (value instanceof String) {
            String valueString = (String) value;
            Object[] requestItems = request.getRequestObjects();
            if (container.isValidDefinition(valueString, requestItems)) {
                container.render(valueString, requestItems);
            } else if (valueString.startsWith("/")) {
                request.dispatch(valueString);
            } else {
                request.getWriter().write(valueString);
            }
        } else {
            throw new RendererException(
                    "Cannot render an untyped object attribute");
        }
    }
}
