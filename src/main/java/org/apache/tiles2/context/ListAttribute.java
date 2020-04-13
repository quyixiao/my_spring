/*
 * $Id: ListAttribute.java 673767 2008-07-03 19:10:56Z apetrelli $
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

package org.apache.tiles2.context;

import java.util.List;

/**
 * An attribute as a <code>List</code>.
 * This attribute associates a name with a list. The list can be found by the
 * property name.
 * Elements in list are retrieved using List methods.
 * This class is used to read configuration files.
 *
 * @version $Rev: 673767 $ $Date: 2008-07-03 21:10:56 +0200 (gio, 03 lug 2008) $
 * @deprecated Use {@link org.apache.tiles2.ListAttribute}.
 */
public class ListAttribute extends org.apache.tiles2.ListAttribute {

    /**
     * Constructor.
     */
    public ListAttribute() {
        super();
    }

    /**
     * Constructor.
     *
     * @param name  Name.
     * @param value List.
     * @since 2.1.0
     */
    public ListAttribute(String name, List<Object> value) {
        super(value);
        setName(name);
    }
}
