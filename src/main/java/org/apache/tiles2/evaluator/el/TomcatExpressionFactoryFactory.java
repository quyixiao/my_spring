/*
 * $Id: TomcatExpressionFactoryFactory.java 816924 2009-09-19 13:45:40Z apetrelli $
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

import javax.el.ExpressionFactory;

import org.apache.el.ExpressionFactoryImpl;
import org.apache.tiles2.el.ExpressionFactoryFactory;

/**
 * Creates an expression factory using Tomcat's Jasper engine.
 *
 * @version $Rev: 816924 $ $Date: 2009-09-19 15:45:40 +0200 (sab, 19 set 2009) $
 * @since 2.1.0
 * @deprecated Upgrade to Servlet 2.5 and JSP 2.1 and use
 * <code>JspExpressionFactoryFactory</code>.
 */
public class TomcatExpressionFactoryFactory implements ExpressionFactoryFactory {

    /** {@inheritDoc} */
    public ExpressionFactory getExpressionFactory() {
        return new ExpressionFactoryImpl();
    }
}
