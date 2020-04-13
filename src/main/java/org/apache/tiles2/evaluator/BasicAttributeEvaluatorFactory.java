/*
 * $Id: BasicAttributeEvaluatorFactory.java 788032 2009-06-24 14:08:32Z apetrelli $
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

package org.apache.tiles2.evaluator;

import java.util.HashMap;
import java.util.Map;

import org.apache.tiles2.Attribute;
import org.apache.tiles2.Expression;

/**
 * Basic implementation of {@link org.apache.tiles2.evaluator.AttributeEvaluatorFactory}. It supports a
 * default attribute evaluator, in case the language is not recognized.
 *
 * @version $Rev: 788032 $ $Date: 2009-06-24 16:08:32 +0200 (mer, 24 giu 2009) $
 * @since 2.2.0
 */
public class BasicAttributeEvaluatorFactory implements
        AttributeEvaluatorFactory {

    /**
     * The default evaluator to return if it is not found in the map of known
     * languages.
     */
    private org.apache.tiles2.evaluator.AttributeEvaluator defaultEvaluator;

    /**
     * Maps names of expression languages to their attribute evaluator.
     *
     * @since 2.2.0
     */
    private Map<String, org.apache.tiles2.evaluator.AttributeEvaluator> language2evaluator;

    /**
     * Constructor.
     *
     * @param defaultEvaluator The default evaluator to return if it is not
     * found in the map of known languages.
     * @since 2.2.0
     */
    public BasicAttributeEvaluatorFactory(org.apache.tiles2.evaluator.AttributeEvaluator defaultEvaluator) {
        this.defaultEvaluator = defaultEvaluator;
        language2evaluator = new HashMap<String, org.apache.tiles2.evaluator.AttributeEvaluator>();
    }

    /**
     * Registers a known expression language with its attribute evaluator.
     *
     * @param language The name of the expression language.
     * @param evaluator The associated attribute evaluator.
     * @since 2.2.0
     */
    public void registerAttributeEvaluator(String language, org.apache.tiles2.evaluator.AttributeEvaluator evaluator) {
        language2evaluator.put(language, evaluator);
    }

    /** {@inheritDoc} */
    public org.apache.tiles2.evaluator.AttributeEvaluator getAttributeEvaluator(String language) {
        org.apache.tiles2.evaluator.AttributeEvaluator retValue = language2evaluator.get(language);
        if (retValue == null) {
            retValue = defaultEvaluator;
        }
        return retValue;
    }

    /** {@inheritDoc} */
    public AttributeEvaluator getAttributeEvaluator(Attribute attribute) {
        Expression expression = attribute.getExpressionObject();
        if (expression != null) {
            return getAttributeEvaluator(expression.getLanguage());
        }
        return defaultEvaluator;
    }
}
