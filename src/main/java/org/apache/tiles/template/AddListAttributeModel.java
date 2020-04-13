/*
 * $Id: AddListAttributeModel.java 797765 2009-07-25 13:20:26Z apetrelli $
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

import org.apache.tiles.ArrayStack;
import org.apache.tiles.ListAttribute;

/**
 * <p>
 * <strong>Declare a list that will be pass as an attribute. </strong>
 * </p>
 * <p>
 * Declare a list that will be pass as an attribute . List elements are added
 * using the tag 'addAttribute' or 'addListAttribute'. This tag can only be used
 * inside 'insertTemplate', 'insertDefinition' or 'definition' tag.
 * </p>
 *
 * @version $Rev: 797765 $ $Date: 2009-07-25 15:20:26 +0200 (sab, 25 lug 2009) $
 * @since 2.2.0
 */
public class AddListAttributeModel {

    /**
     * Starts the operation.
     *
     * @param composeStack The composing stack.
     * @param role A comma-separated list of roles. If present, the attribute
     * will be rendered only if the current user belongs to one of the roles.
     * @since 2.2.0
     */
    public void start(ArrayStack<Object> composeStack, String role) {
        ListAttribute listAttribute = new ListAttribute();
        listAttribute.setRole(role);
        composeStack.push(listAttribute);
    }

    /**
     * Ends the operation.
     *
     * @param composeStack The composing stack.
     * @since 2.2.0
     */
    public void end(ArrayStack<Object> composeStack) {
        ListAttribute listAttribute = (ListAttribute) composeStack.pop();
        ListAttribute parent = (ListAttribute) composeStack.peek();
        parent.add(listAttribute);
    }
}
