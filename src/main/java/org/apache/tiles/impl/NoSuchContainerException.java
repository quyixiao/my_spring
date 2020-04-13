/*
 * $Id: NoSuchContainerException.java 657917 2008-05-19 18:51:45Z apetrelli $
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
package org.apache.tiles.impl;

import org.apache.tiles.TilesException;

/**
 * Indicates that a keyed container has not been found.
 *
 * @version $Rev: 657917 $ $Date: 2008-05-19 20:51:45 +0200 (lun, 19 mag 2008) $
 * @since 2.1.0
 */
public class NoSuchContainerException extends TilesException {

    /**
     * Constructor.
     *
     * @since 2.1.0
     */
    public NoSuchContainerException() {
    }

    /**
     * Constructor.
     *
     * @param message The detail message.
     * @since 2.1.0
     */
    public NoSuchContainerException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param e The exception to be wrapped.
     * @since 2.1.0
     */
    public NoSuchContainerException(Exception e) {
        super(e);
    }

    /**
     * Constructor.
     *
     * @param message The detail message.
     * @param e The exception to be wrapped.
     * @since 2.1.0
     */
    public NoSuchContainerException(String message, Exception e) {
        super(message, e);
    }

}
