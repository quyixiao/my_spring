/*
 * $Id: OGNLAttributeEvaluator.java 817009 2009-09-20 11:26:26Z apetrelli $
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

package org.apache.tiles2.ognl;

import java.util.Map;

import ognl.Ognl;
import ognl.OgnlException;

import org.apache.tiles2.context.TilesRequestContext;
import org.apache.tiles2.evaluator.AbstractAttributeEvaluator;
import org.apache.tiles2.evaluator.EvaluationException;

/**
 * Evaluates attribute expressions and expressions with OGNL language.
 *
 * @version $Rev: 817009 $ $Date: 2009-09-20 13:26:26 +0200 (dom, 20 set 2009) $
 * @since 2.2.0
 */
public class OGNLAttributeEvaluator extends AbstractAttributeEvaluator {

    /** {@inheritDoc} */
    public Object evaluate(String expression, TilesRequestContext request) {
        if (expression == null) {
            throw new IllegalArgumentException("The expression parameter cannot be null");
        }
        try {
            return Ognl.getValue(expression, request);
        } catch (OgnlException e) {
            throw new EvaluationException("Cannot evaluate OGNL expression '"
                    + expression + "'", e);
        }
    }

    /** {@inheritDoc} */
    public void init(Map<String, String> initParameters) {
        // Nothing to initialize.
    }
}
