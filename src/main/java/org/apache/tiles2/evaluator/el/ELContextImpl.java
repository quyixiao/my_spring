/*
 * $Id: ELContextImpl.java 816924 2009-09-19 13:45:40Z apetrelli $
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

package org.apache.tiles2.evaluator.el;

import javax.el.ELResolver;

/**
 * Implementation of ELContext.<br>
 * Copied from Apache Tomcat 6.0.16 source code.
 *
 * @since 2.1.0
 * @deprecated Use {@link org.apache.tiles2.el.ELContextImpl}.
 */
public final class ELContextImpl extends org.apache.tiles2.el.ELContextImpl {

    /**
     * Constructor.
     *
     * @param resolver The resolver to use.
     * @deprecated Use
     * {@link org.apache.tiles2.el.ELContextImpl#ELContextImpl(ELResolver)}.
     */
    public ELContextImpl(ELResolver resolver) {
        super(resolver);
    }
}
