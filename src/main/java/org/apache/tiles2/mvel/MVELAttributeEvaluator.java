/*
 * $Id: MVELAttributeEvaluator.java 817009 2009-09-20 11:26:26Z apetrelli $
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

package org.apache.tiles2.mvel;

import java.util.Map;

import org.apache.tiles2.context.TilesRequestContext;
import org.apache.tiles2.context.TilesRequestContextHolder;
import org.apache.tiles2.evaluator.AbstractAttributeEvaluator;
import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;

/**
 * Allows to use MVEL as the language to evaluate attribute values.
 *
 * @version $Rev: 817009 $ $Date: 2009-09-20 13:26:26 +0200 (dom, 20 set 2009) $
 * @since 2.2.0
 */
public class MVELAttributeEvaluator extends AbstractAttributeEvaluator {

    /**
     * Holds the Tiles request context of the current thread.
     */
    private TilesRequestContextHolder requestHolder;

    /**
     * Resolves variables when starting from the root.
     *
     * @since 2.2.0
     */
    private VariableResolverFactory variableResolverFactory;

    /**
     * Constructor.
     *
     * @param requestHolder The object that holds the Tiles request context of
     * the current thread.
     * @param variableResolverFactory The resolver factory to use.
     * @since 2.2.0
     */
    public MVELAttributeEvaluator(TilesRequestContextHolder requestHolder,
            VariableResolverFactory variableResolverFactory) {
        this.requestHolder = requestHolder;
        this.variableResolverFactory = variableResolverFactory;
    }

    /** {@inheritDoc} */
    public void init(Map<String, String> initParameters) {
        // Does nothing.
    }

    /** {@inheritDoc} */
    public Object evaluate(String expression, TilesRequestContext request) {
        if (expression == null) {
            throw new IllegalArgumentException("The expression parameter cannot be null");
        }
        requestHolder.setTilesRequestContext(request);
        return MVEL.eval(expression, variableResolverFactory);
    }
}
