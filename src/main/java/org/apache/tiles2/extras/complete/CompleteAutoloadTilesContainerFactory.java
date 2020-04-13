/*
 * $Id: CompleteAutoloadTilesContainerFactory.java 836356 2009-11-15 13:27:43Z apetrelli $
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

package org.apache.tiles2.extras.complete;

import ognl.OgnlException;
import ognl.OgnlRuntime;
import ognl.PropertyAccessor;
import org.apache.tiles2.TilesApplicationContext;
import org.apache.tiles2.TilesContainer;
import org.apache.tiles2.compat.definition.digester.CompatibilityDigesterDefinitionsReader;
import org.apache.tiles2.context.ChainedTilesRequestContextFactory;
import org.apache.tiles2.context.TilesRequestContext;
import org.apache.tiles2.context.TilesRequestContextFactory;
import org.apache.tiles2.context.TilesRequestContextHolder;
import org.apache.tiles2.definition.DefinitionsFactoryException;
import org.apache.tiles2.definition.DefinitionsReader;
import org.apache.tiles2.definition.pattern.DefinitionPatternMatcherFactory;
import org.apache.tiles2.definition.pattern.PatternDefinitionResolver;
import org.apache.tiles2.definition.pattern.PrefixedPatternDefinitionResolver;
import org.apache.tiles2.definition.pattern.regexp.RegexpDefinitionPatternMatcherFactory;
import org.apache.tiles2.definition.pattern.wildcard.WildcardDefinitionPatternMatcherFactory;
import org.apache.tiles2.el.ELAttributeEvaluator;
import org.apache.tiles2.el.JspExpressionFactoryFactory;
import org.apache.tiles2.el.TilesContextBeanELResolver;
import org.apache.tiles2.el.TilesContextELResolver;
import org.apache.tiles2.evaluator.AttributeEvaluatorFactory;
import org.apache.tiles2.evaluator.BasicAttributeEvaluatorFactory;
import org.apache.tiles2.factory.BasicTilesContainerFactory;
import org.apache.tiles2.factory.TilesContainerFactoryException;
import org.apache.tiles2.freemarker.context.FreeMarkerTilesRequestContextFactory;
import org.apache.tiles2.freemarker.renderer.FreeMarkerAttributeRenderer;
import org.apache.tiles2.impl.BasicTilesContainer;
import org.apache.tiles2.impl.mgmt.CachingTilesContainer;
import org.apache.tiles2.locale.LocaleResolver;
import org.apache.tiles2.mvel.MVELAttributeEvaluator;
import org.apache.tiles2.mvel.TilesContextBeanVariableResolverFactory;
import org.apache.tiles2.mvel.TilesContextVariableResolverFactory;
import org.apache.tiles2.ognl.*;
import org.apache.tiles2.renderer.AttributeRenderer;
import org.apache.tiles2.renderer.TypeDetectingAttributeRenderer;
import org.apache.tiles2.renderer.impl.BasicRendererFactory;
import org.apache.tiles2.renderer.impl.ChainedDelegateAttributeRenderer;
import org.apache.tiles2.util.URLUtil;
import org.apache.tiles2.velocity.context.VelocityTilesRequestContextFactory;
import org.apache.tiles2.velocity.renderer.VelocityAttributeRenderer;
import org.mvel2.integration.VariableResolverFactory;

import javax.el.*;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tiles container factory that:
 * <ul>
 * <li>create supporting objects for Velocity and FreeMarker;</li>
 * <li>create renderers for Velocity and FreeMarker templates;</li>
 * <li>allows using EL, MVEL and OGNL as attribute expressions;</li>
 * <li>allows using Wildcards and Regular Expressions in definition names;</li>
 * <li>loads Tiles 1.x definition files;</li>
 * <li>loads all the definition files that have the "tiles*.xml" pattern under
 * <code>/WEB-INF</code> directory (and subdirectories) and under
 * <code>META-INF</code> directories (and subdirectories) in every jar.</li>
 * </ul>
 *
 * @version $Rev: 836356 $ $Date: 2009-11-15 14:27:43 +0100 (dom, 15 nov 2009) $
 * @since 2.2.0
 */
public class CompleteAutoloadTilesContainerFactory extends BasicTilesContainerFactory {

    /**
     * The freemarker renderer name.
     */
    private static final String FREEMARKER_RENDERER_NAME = "freemarker";

    /**
     * The velocity renderer name.
     */
    private static final String VELOCITY_RENDERER_NAME = "velocity";

    /**
     * {@inheritDoc}
     */
    @Override
    protected BasicTilesContainer instantiateContainer(
            TilesApplicationContext applicationContext) {
        return new CachingTilesContainer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<TilesRequestContextFactory> getTilesRequestContextFactoriesToBeChained(
            ChainedTilesRequestContextFactory parent) {
        List<TilesRequestContextFactory> factories = super.getTilesRequestContextFactoriesToBeChained(parent);
        registerRequestContextFactory(
                FreeMarkerTilesRequestContextFactory.class.getName(),
                factories, parent);
        registerRequestContextFactory(
                VelocityTilesRequestContextFactory.class.getName(),
                factories, parent);
        return factories;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerAttributeRenderers(
            BasicRendererFactory rendererFactory,
            TilesApplicationContext applicationContext,
            TilesRequestContextFactory contextFactory,
            TilesContainer container,
            AttributeEvaluatorFactory attributeEvaluatorFactory) {
        super.registerAttributeRenderers(rendererFactory, applicationContext, contextFactory,
                container, attributeEvaluatorFactory);

        FreeMarkerAttributeRenderer freemarkerRenderer = new FreeMarkerAttributeRenderer();
        freemarkerRenderer.setApplicationContext(applicationContext);
        freemarkerRenderer.setAttributeEvaluatorFactory(attributeEvaluatorFactory);
        freemarkerRenderer.setRequestContextFactory(contextFactory);
        freemarkerRenderer.setParameter("TemplatePath", "/");
        freemarkerRenderer.setParameter("NoCache", "true");
        freemarkerRenderer.setParameter("ContentType", "text/html");
        freemarkerRenderer.setParameter("template_update_delay", "0");
        freemarkerRenderer.setParameter("default_encoding", "ISO-8859-1");
        freemarkerRenderer.setParameter("number_format", "0.##########");
        freemarkerRenderer.commit();
        rendererFactory.registerRenderer(FREEMARKER_RENDERER_NAME, freemarkerRenderer);

        VelocityAttributeRenderer velocityRenderer = new VelocityAttributeRenderer();
        velocityRenderer.setApplicationContext(applicationContext);
        velocityRenderer.setAttributeEvaluatorFactory(attributeEvaluatorFactory);
        velocityRenderer.setRequestContextFactory(contextFactory);
        velocityRenderer.commit();
        rendererFactory.registerRenderer(VELOCITY_RENDERER_NAME, velocityRenderer);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected AttributeRenderer createDefaultAttributeRenderer(
            BasicRendererFactory rendererFactory,
            TilesApplicationContext applicationContext,
            TilesRequestContextFactory contextFactory,
            TilesContainer container,
            AttributeEvaluatorFactory attributeEvaluatorFactory) {
        ChainedDelegateAttributeRenderer retValue = new ChainedDelegateAttributeRenderer();
        retValue.addAttributeRenderer((TypeDetectingAttributeRenderer) rendererFactory
                .getRenderer(DEFINITION_RENDERER_NAME));
        retValue.addAttributeRenderer((TypeDetectingAttributeRenderer) rendererFactory
                .getRenderer(VELOCITY_RENDERER_NAME));
        retValue.addAttributeRenderer((TypeDetectingAttributeRenderer) rendererFactory
                .getRenderer(FREEMARKER_RENDERER_NAME));
        retValue.addAttributeRenderer((TypeDetectingAttributeRenderer) rendererFactory
                .getRenderer(TEMPLATE_RENDERER_NAME));
        retValue.addAttributeRenderer((TypeDetectingAttributeRenderer) rendererFactory
                .getRenderer(STRING_RENDERER_NAME));
        retValue.setApplicationContext(applicationContext);
        retValue.setRequestContextFactory(contextFactory);
        retValue.setAttributeEvaluatorFactory(attributeEvaluatorFactory);
        return retValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AttributeEvaluatorFactory createAttributeEvaluatorFactory(
            TilesApplicationContext applicationContext,
            TilesRequestContextFactory contextFactory, LocaleResolver resolver) {
        BasicAttributeEvaluatorFactory attributeEvaluatorFactory = new BasicAttributeEvaluatorFactory(
                createELEvaluator(applicationContext));
        attributeEvaluatorFactory.registerAttributeEvaluator("MVEL",
                createMVELEvaluator());
        attributeEvaluatorFactory.registerAttributeEvaluator("OGNL",
                createOGNLEvaluator());

        return attributeEvaluatorFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected <T> PatternDefinitionResolver<T> createPatternDefinitionResolver(
            Class<T> customizationKeyClass) {
        DefinitionPatternMatcherFactory wildcardFactory = new WildcardDefinitionPatternMatcherFactory();
        DefinitionPatternMatcherFactory regexpFactory = new RegexpDefinitionPatternMatcherFactory();
        PrefixedPatternDefinitionResolver<T> resolver = new PrefixedPatternDefinitionResolver<T>();
        resolver.registerDefinitionPatternMatcherFactory("WILDCARD", wildcardFactory);
        resolver.registerDefinitionPatternMatcherFactory("REGEXP", regexpFactory);
        return resolver;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<URL> getSourceURLs(TilesApplicationContext applicationContext,
                                      TilesRequestContextFactory contextFactory) {
        try {
            Set<URL> finalSet = new HashSet<URL>();
            Set<URL> webINFSet = applicationContext.getResources("/WEB-INF/**/tiles*.xml");
            Set<URL> metaINFSet = applicationContext.getResources("classpath*:META-INF/**/tiles*.xml");

            if (webINFSet != null) {
                finalSet.addAll(webINFSet);
            }
            if (metaINFSet != null) {
                finalSet.addAll(metaINFSet);
            }

            return URLUtil.getBaseTilesDefinitionURLs(finalSet);
        } catch (IOException e) {
            throw new DefinitionsFactoryException(
                    "Cannot load definition URLs", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DefinitionsReader createDefinitionsReader(TilesApplicationContext applicationContext,
                                                        TilesRequestContextFactory contextFactory) {
        return new CompatibilityDigesterDefinitionsReader();
    }

    /**
     * Creates the EL evaluator.
     *
     * @param applicationContext The Tiles application context.
     * @return The EL evaluator.
     */
    private org.apache.tiles2.el.ELAttributeEvaluator createELEvaluator(
            TilesApplicationContext applicationContext) {
        org.apache.tiles2.el.ELAttributeEvaluator evaluator = new ELAttributeEvaluator();
        evaluator.setApplicationContext(applicationContext);
        org.apache.tiles2.el.JspExpressionFactoryFactory efFactory = new JspExpressionFactoryFactory();
        efFactory.setApplicationContext(applicationContext);
        evaluator.setExpressionFactory(efFactory.getExpressionFactory());
        ELResolver elResolver = new CompositeELResolver() {
            {
                add(new TilesContextELResolver());
                add(new TilesContextBeanELResolver());
                add(new ArrayELResolver(false));
                add(new ListELResolver(false));
                add(new MapELResolver(false));
                add(new ResourceBundleELResolver());
                add(new BeanELResolver(false));
            }
        };
        evaluator.setResolver(elResolver);
        return evaluator;
    }

    /**
     * Creates the MVEL evaluator.
     *
     * @return The MVEL evaluator.
     */
    private MVELAttributeEvaluator createMVELEvaluator() {
        TilesRequestContextHolder requestHolder = new TilesRequestContextHolder();
        VariableResolverFactory variableResolverFactory = new TilesContextVariableResolverFactory(
                requestHolder);
        variableResolverFactory
                .setNextFactory(new TilesContextBeanVariableResolverFactory(
                        requestHolder));
        org.apache.tiles2.mvel.MVELAttributeEvaluator mvelEvaluator = new org.apache.tiles2.mvel.MVELAttributeEvaluator(requestHolder,
                variableResolverFactory);
        return mvelEvaluator;
    }

    /**
     * Creates the OGNL evaluator.
     *
     * @return The OGNL evaluator.
     */
    private OGNLAttributeEvaluator createOGNLEvaluator() {
        try {
            PropertyAccessor objectPropertyAccessor = OgnlRuntime.getPropertyAccessor(Object.class);
            PropertyAccessor mapPropertyAccessor = OgnlRuntime.getPropertyAccessor(Map.class);
            PropertyAccessor applicationContextPropertyAccessor =
                    new NestedObjectDelegatePropertyAccessor<TilesRequestContext>(
                            new TilesApplicationContextNestedObjectExtractor(),
                            objectPropertyAccessor);
            PropertyAccessor requestScopePropertyAccessor =
                    new NestedObjectDelegatePropertyAccessor<TilesRequestContext>(
                            new RequestScopeNestedObjectExtractor(), mapPropertyAccessor);
            PropertyAccessor sessionScopePropertyAccessor =
                    new NestedObjectDelegatePropertyAccessor<TilesRequestContext>(
                            new SessionScopeNestedObjectExtractor(), mapPropertyAccessor);
            PropertyAccessor applicationScopePropertyAccessor =
                    new NestedObjectDelegatePropertyAccessor<TilesRequestContext>(
                            new ApplicationScopeNestedObjectExtractor(), mapPropertyAccessor);
            PropertyAccessorDelegateFactory<TilesRequestContext> factory =
                    new TilesContextPropertyAccessorDelegateFactory(
                            objectPropertyAccessor, applicationContextPropertyAccessor,
                            requestScopePropertyAccessor, sessionScopePropertyAccessor,
                            applicationScopePropertyAccessor);
            PropertyAccessor tilesRequestAccessor = new DelegatePropertyAccessor<TilesRequestContext>(factory);
            OgnlRuntime.setPropertyAccessor(TilesRequestContext.class, tilesRequestAccessor);
            return new OGNLAttributeEvaluator();
        } catch (OgnlException e) {
            throw new TilesContainerFactoryException(
                    "Cannot initialize OGNL evaluator", e);
        }
    }
}
