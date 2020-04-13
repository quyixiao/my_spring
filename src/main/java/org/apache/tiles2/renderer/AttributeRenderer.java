/*
 * $Id: AttributeRenderer.java 736275 2009-01-21 09:58:20Z apetrelli $
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
package org.apache.tiles2.renderer;

import java.io.IOException;

import org.apache.tiles2.Attribute;
import org.apache.tiles2.context.TilesRequestContext;

/**
 * An object that can render an attribute. For each attribute, if it needs to be
 * rendered, has an associated renderer.
 *
 * @version $Rev: 736275 $ $Date: 2009-01-21 10:58:20 +0100 (mer, 21 gen 2009) $
 * @since 2.1.0
 */
public interface AttributeRenderer {

    /**
     * Renders an attribute.
     *
     * @param attribute The attribute to render.
     * @param request The Tiles request context.
     * @throws IOException If something goes wrong during rendition.
     * @throws RendererException If something goes wrong during rendition.
     * @since 2.1.2
     */
    void render(Attribute attribute, TilesRequestContext request)
            throws IOException;
}
