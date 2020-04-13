/*
 * $Id: ComposeStackUtil.java 797765 2009-07-25 13:20:26Z apetrelli $
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

import org.apache.tiles2.ArrayStack;

/**
 * Utilities to work with compose stacks.
 *
 * @version $Rev: 797765 $ $Date: 2009-07-25 15:20:26 +0200 (sab, 25 lug 2009) $
 * @since 2.2.0
 */
public final class ComposeStackUtil {

    /**
     * Private constructor to avoid instantiation.
     */
    private ComposeStackUtil() {

    }

    /**
     * Finds the first ancestor in the stack, that is assignable to the given class.
     *
     * @param composeStack The compose stack to evaluate.
     * @param clazz The class to check.
     * @return The first ancestor that is assignable to the class, or null if not found.
     * @since 2.2.0
     */
    public static Object findAncestorWithClass(ArrayStack<Object> composeStack, Class<?> clazz) {
        Object retValue = null;
        for (int i = composeStack.size() - 1; i >= 0 && retValue == null; i--) {
            Object obj = composeStack.get(i);
            if (clazz.isAssignableFrom(obj.getClass())) {
                retValue = obj;
            }
        }

        return retValue;
    }
}
