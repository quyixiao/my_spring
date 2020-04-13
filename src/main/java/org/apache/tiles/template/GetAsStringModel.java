/*
 * $Id: GetAsStringModel.java 797765 2009-07-25 13:20:26Z apetrelli $
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

import java.io.IOException;
import java.io.Writer;

import org.apache.tiles.ArrayStack;
import org.apache.tiles.Attribute;
import org.apache.tiles.TilesContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * <strong> Render the value of the specified template attribute to the current
 * Writer</strong>
 * </p>
 *
 * <p>
 * Retrieve the value of the specified template attribute property, and render
 * it to the current Writer as a String. The usual toString() conversions is
 * applied on found value.
 * </p>
 *
 * @version $Rev: 797765 $ $Date: 2009-07-25 15:20:26 +0200 (sab, 25 lug 2009) $
 * @since 2.2.0
 */
public class GetAsStringModel {

    /**
     * The logging object.
     */
    private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The attribute resolver to use.
     */
    private AttributeResolver attributeResolver;

    /**
     * Constructor.
     *
     * @param attributeResolver The attribute resolver to use.
     * @since 2.2.0
     */
    public GetAsStringModel(AttributeResolver attributeResolver) {
        this.attributeResolver = attributeResolver;
    }

    /**
     * Starts the operation.
     *
     * @param composeStack The compose stack,
     * @param container The Tiles container to use.
     * @param ignore If <code>true</code>, if an exception happens during
     * rendering, of if the attribute is null, the problem will be ignored.
     * @param preparer The preparer to invoke before rendering the attribute.
     * @param role A comma-separated list of roles. If present, the attribute
     * will be rendered only if the current user belongs to one of the roles.
     * @param defaultValue The default value of the attribute. To use only if
     * the attribute was not computed.
     * @param defaultValueRole The default comma-separated list of roles. To use
     * only if the attribute was not computed.
     * @param defaultValueType The default type of the attribute. To use only if
     * the attribute was not computed.
     * @param name The name of the attribute.
     * @param value The attribute to use immediately, if not null.
     * @param requestItems The request objects.
     * @since 2.2.0
     */
    public void start(ArrayStack<Object> composeStack, TilesContainer container,
            boolean ignore, String preparer, String role, Object defaultValue,
            String defaultValueRole, String defaultValueType, String name,
            Attribute value, Object... requestItems) {
        Attribute attribute = resolveAttribute(container, ignore, preparer,
                role, defaultValue, defaultValueRole, defaultValueType, name,
                value, requestItems);
        composeStack.push(attribute);
    }

    /**
     * Ends the operation.
     *
     * @param composeStack The compose stack,
     * @param container The Tiles container to use.
     * @param writer The writer into which the attribute will be written.
     * @param ignore If <code>true</code>, if an exception happens during
     * rendering, of if the attribute is null, the problem will be ignored.
     * @param requestItems The request objects.
     * @throws IOException If an I/O error happens during rendering.
     */
    public void end(ArrayStack<Object> composeStack, TilesContainer container,
            Writer writer, boolean ignore, Object... requestItems)
            throws IOException {
        Attribute attribute = (Attribute) composeStack.pop();
        renderAttribute(attribute, container, writer, ignore, requestItems);
    }

    /**
     * Executes the operation.
     *
     * @param container The Tiles container to use.
     * @param writer The writer into which the attribute will be written.
     * @param ignore If <code>true</code>, if an exception happens during
     * rendering, of if the attribute is null, the problem will be ignored.
     * @param preparer The preparer to invoke before rendering the attribute.
     * @param role A comma-separated list of roles. If present, the attribute
     * will be rendered only if the current user belongs to one of the roles.
     * @param defaultValue The default value of the attribute. To use only if
     * the attribute was not computed.
     * @param defaultValueRole The default comma-separated list of roles. To use
     * only if the attribute was not computed.
     * @param defaultValueType The default type of the attribute. To use only if
     * the attribute was not computed.
     * @param name The name of the attribute.
     * @param value The attribute to use immediately, if not null.
     * @param requestItems The request objects.
     * @throws IOException If an I/O error happens during rendering.
     * @since 2.2.0
     */
    public void execute(TilesContainer container, Writer writer,
            boolean ignore, String preparer, String role, Object defaultValue,
            String defaultValueRole, String defaultValueType, String name,
            Attribute value, Object... requestItems) throws IOException {
        Attribute attribute = resolveAttribute(container, ignore, preparer,
                role, defaultValue, defaultValueRole, defaultValueType, name,
                value, requestItems);
        renderAttribute(attribute, container, writer, ignore, requestItems);
    }

    /**
     * Resolves the attribute. and starts the context.
     *
     * @param container The Tiles container to use.
     * @param ignore If <code>true</code>, if an exception happens during
     * rendering, of if the attribute is null, the problem will be ignored.
     * @param preparer The preparer to invoke before rendering the attribute.
     * @param role A comma-separated list of roles. If present, the attribute
     * will be rendered only if the current user belongs to one of the roles.
     * @param defaultValue The default value of the attribute. To use only if
     * the attribute was not computed.
     * @param defaultValueRole The default comma-separated list of roles. To use
     * only if the attribute was not computed.
     * @param defaultValueType The default type of the attribute. To use only if
     * the attribute was not computed.
     * @param name The name of the attribute.
     * @param value The attribute to use immediately, if not null.
     * @param requestItems The request objects.
     * @return The resolved attribute.
     */
    private Attribute resolveAttribute(TilesContainer container,
            boolean ignore, String preparer, String role, Object defaultValue,
            String defaultValueRole, String defaultValueType, String name,
            Attribute value, Object... requestItems) {
        if (preparer != null) {
            container.prepare(preparer, requestItems);
        }
        Attribute attribute = attributeResolver.computeAttribute(container,
                value, name, role, ignore, defaultValue, defaultValueRole,
                defaultValueType, requestItems);
        container.startContext(requestItems);
        return attribute;
    }

    /**
     * Renders the attribute as a string.
     *
     * @param attribute The attribute to use, previously resolved.
     * @param container The Tiles container to use.
     * @param writer The writer into which the attribute will be written.
     * @param ignore If <code>true</code>, if an exception happens during
     * rendering, of if the attribute is null, the problem will be ignored.
     * @param requestItems The request objects.
     * @throws IOException If an I/O error happens during rendering.
     */
    private void renderAttribute(Attribute attribute, TilesContainer container,
            Writer writer, boolean ignore, Object... requestItems)
            throws IOException {
        if (attribute == null && ignore) {
            return;
        }
        try {
            writer.write(attribute.getValue().toString());
        } catch (IOException e) {
            if (!ignore) {
                throw e;
            } else if (log.isDebugEnabled()) {
                log.debug("Ignoring exception", e);
            }
        } catch (RuntimeException e) {
            if (!ignore) {
                throw e;
            } else if (log.isDebugEnabled()) {
                log.debug("Ignoring exception", e);
            }
        } finally {
            container.endContext(requestItems);
        }
    }
}
