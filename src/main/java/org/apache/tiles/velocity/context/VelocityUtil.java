/*
 * $Id: VelocityUtil.java 902965 2010-01-25 20:12:46Z apetrelli $
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

package org.apache.tiles.velocity.context;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.tiles.ArrayStack;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.Renderable;
import org.apache.velocity.runtime.parser.node.ASTBlock;
import org.apache.velocity.runtime.parser.node.ASTMap;
import org.apache.velocity.runtime.parser.node.Node;

/**
 * Utilities for Velocity usage in Tiles.
 *
 * @version $Rev: 902965 $ $Date: 2010-01-25 21:12:46 +0100 (lun, 25 gen 2010) $
 * @since 2.2.0
 */
public final class VelocityUtil {

    /**
     * A renderable object that does not render anything.
     *
     * @since 2.2.0
     */
    public static final Renderable EMPTY_RENDERABLE;

    static {
        EMPTY_RENDERABLE = new Renderable() {

            @Override
            public String toString() {
                return "";
            }

            public boolean render(InternalContextAdapter context, Writer writer) {
                // Does nothing, really!
                return true;
            }
        };
    }

    /**
     * The attribute key that will be used to store the parameter map, to use across Velocity tool calls.
     *
     * @since 2.2.0
     */
    private static final String PARAMETER_MAP_STACK_KEY = "org.apache.tiles.velocity.PARAMETER_MAP_STACK";

    /**
     * Private constructor to avoid instantiation.
     */
    private VelocityUtil() {
    }

    /**
     * Null-safe conversion from Boolean to boolean.
     *
     * @param obj The Boolean object.
     * @param defaultValue This value will be returned if <code>obj</code> is null.
     * @return The boolean value of <code>obj</code> or, if null, <code>defaultValue</code>.
     * @since 2.2.0
     */
    public static boolean toSimpleBoolean(Boolean obj, boolean defaultValue) {
        return obj != null ? obj : defaultValue;
    }

    /**
     * Returns or creates the parameter stack to use. It is useful to store parameters across tool calls.
     *
     * @param context The Velocity context.
     * @return The parameter stack.
     * @since 2.2.0
     * @deprecated Use Velocity directives.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static ArrayStack<Map<String, Object>> getParameterStack(Context context) {
        ArrayStack<Map<String, Object>> stack = (ArrayStack<Map<String, Object>>) context
                .get(PARAMETER_MAP_STACK_KEY);
        if (stack == null) {
            stack = new ArrayStack<Map<String, Object>>();
            context.put(PARAMETER_MAP_STACK_KEY, stack);
        }
        return stack;
    }

    /**
     * Sets an attribute in the desired scope.
     *
     * @param velocityContext The Velocity context.
     * @param request The HTTP request.
     * @param servletContext The servlet context.
     * @param name The name of the attribute.
     * @param obj The value of the attribute.
     * @param scope The scope. It can be <code>page</code>, <code>request</code>
     * , <code>session</code>, <code>application</code>.
     * @since 2.2.0
     */
    public static void setAttribute(Context velocityContext,
            HttpServletRequest request, ServletContext servletContext,
            String name, Object obj, String scope) {
        if (scope == null) {
            scope = "page";
        }
        if ("page".equals(scope)) {
            velocityContext.put(name, obj);
        } else if ("request".equals(scope)) {
            request.setAttribute(name, obj);
        } else if ("session".equals(scope)) {
            request.getSession().setAttribute(name, obj);
        } else if ("application".equals(scope)) {
            servletContext.setAttribute(name, obj);
        }
    }

    /**
     * Evaluates the body (child node at position 1) and returns it as a string.
     *
     * @param context The Velocity context.
     * @param node The node to use.
     * @return The evaluated body.
     * @throws IOException If something goes wrong.
     * @since 2.2.2
     */
    public static String getBodyAsString(InternalContextAdapter context, Node node)
            throws IOException {
        ASTBlock block = (ASTBlock) node.jjtGetChild(1);
        StringWriter stringWriter = new StringWriter();
        block.render(context, stringWriter);
        stringWriter.close();
        String body = stringWriter.toString();
        if (body != null) {
            body = body.replaceAll("^\\s*|\\s*$", "");
            if (body.length() <= 0) {
                body = null;
            }
        }
        return body;
    }

    /**
     * Evaluates the body writing in the passed writer.
     *
     * @param context The Velocity context.
     * @param writer The writer to write into.
     * @param node The node to use.
     * @throws IOException If something goes wrong.
     * @since 2.2.2
     */
    public static void evaluateBody(InternalContextAdapter context, Writer writer,
            Node node) throws IOException {
        ASTBlock block = (ASTBlock) node.jjtGetChild(1);
        block.render(context, writer);
    }

    /**
     * Extracts the parameters from the directives, by getting the child at
     * position 0 supposing it is a map.
     *
     * @param context The Velocity context.
     * @param node The node to use.
     * @return The extracted parameters.
     * @since 2.2.2
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getParameters(InternalContextAdapter context,
            Node node) {
        ASTMap astMap = (ASTMap) node.jjtGetChild(0);
        Map<String, Object> params = (Map<String, Object>) astMap
                .value(context);
        return params;
    }
}
