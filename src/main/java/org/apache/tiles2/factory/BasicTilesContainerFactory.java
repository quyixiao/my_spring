/*
 * $Id: BasicTilesContainerFactory.java 822631 2009-10-07 09:21:12Z apetrelli $
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
package org.apache.tiles2.factory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.tiles2.TilesApplicationContext;
import org.apache.tiles2.TilesContainer;
import org.apache.tiles2.awareness.TilesRequestContextFactoryAware;
import org.apache.tiles2.context.ChainedTilesRequestContextFactory;
import org.apache.tiles2.context.TilesRequestContextFactory;
import org.apache.tiles2.definition.Refreshable;
import org.apache.tiles2.locale.LocaleResolver;
import org.apache.tiles2.locale.impl.DefaultLocaleResolver;
import org.apache.tiles2.preparer.BasicPreparerFactory;
import org.apache.tiles2.preparer.PreparerFactory;
import org.apache.tiles2.reflect.ClassUtil;
import org.apache.tiles2.renderer.AttributeRenderer;
import org.apache.tiles2.renderer.RendererFactory;
import org.apache.tiles2.renderer.TypeDetectingAttributeRenderer;
import org.apache.tiles2.renderer.impl.BasicRendererFactory;
import org.apache.tiles2.renderer.impl.DefinitionAttributeRenderer;
import org.apache.tiles2.renderer.impl.StringAttributeRenderer;
import org.apache.tiles2.renderer.impl.TemplateAttributeRenderer;
import org.apache.tiles2.renderer.impl.ChainedDelegateAttributeRenderer;
import org.apache.tiles2.definition.DefinitionsFactory;
import org.apache.tiles2.definition.DefinitionsReader;
import org.apache.tiles2.definition.UnresolvingLocaleDefinitionsFactory;
import org.apache.tiles2.definition.dao.BaseLocaleUrlDefinitionDAO;
import org.apache.tiles2.definition.dao.DefinitionDAO;
import org.apache.tiles2.definition.dao.ResolvingLocaleUrlDefinitionDAO;
import org.apache.tiles2.definition.digester.DigesterDefinitionsReader;
import org.apache.tiles2.definition.pattern.BasicPatternDefinitionResolver;
import org.apache.tiles2.definition.pattern.PatternDefinitionResolver;
import org.apache.tiles2.definition.pattern.PatternDefinitionResolverAware;
import org.apache.tiles2.definition.pattern.wildcard.WildcardDefinitionPatternMatcherFactory;
import org.apache.tiles2.evaluator.AttributeEvaluatorFactory;
import org.apache.tiles2.evaluator.BasicAttributeEvaluatorFactory;
import org.apache.tiles2.evaluator.impl.DirectAttributeEvaluator;
import org.apache.tiles2.impl.BasicTilesContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory that builds a standard Tiles container using only Java code.
 *
 * @version $Rev: 822631 $ $Date: 2009-10-07 11:21:12 +0200 (mer, 07 ott 2009) $
 * @since 2.1.0
 */
public class BasicTilesContainerFactory extends AbstractTilesContainerFactory {

    /**
     * The string renderer name.
     */
    protected static final String STRING_RENDERER_NAME = "string";

    /**
     * The template renderer name.
     */
    protected static final String TEMPLATE_RENDERER_NAME = "template";

    /**
     * The definition renderer name.
     */
    protected static final String DEFINITION_RENDERER_NAME = "definition";
    /**
     * The logging object.
     */
    private final Logger log = LoggerFactory
            .getLogger(BasicTilesContainerFactory.class);

    /** {@inheritDoc} */
    @Override
    public TilesContainer createContainer(TilesApplicationContext applicationContext) {
        org.apache.tiles2.impl.BasicTilesContainer container = instantiateContainer(applicationContext);
        TilesRequestContextFactory requestContextFactory =
            createRequestContextFactory(applicationContext);
        container.setRequestContextFactory(requestContextFactory);
        container.setApplicationContext(applicationContext);
        LocaleResolver resolver = createLocaleResolver(applicationContext,
                requestContextFactory);
        container.setDefinitionsFactory(createDefinitionsFactory(applicationContext,
                requestContextFactory, resolver));
        org.apache.tiles2.evaluator.AttributeEvaluatorFactory attributeEvaluatorFactory = createAttributeEvaluatorFactory(
                applicationContext, requestContextFactory, resolver);
        container.setAttributeEvaluatorFactory(attributeEvaluatorFactory);
        container.setPreparerFactory(createPreparerFactory(applicationContext,
                requestContextFactory));
        container.setRendererFactory(createRendererFactory(applicationContext,
                requestContextFactory, container, attributeEvaluatorFactory));
        return container;
    }

    /**
     * Instantiate the container, without initialization.
     *
     * @param context The Tiles application context object.
     * @return The instantiated container.
     * @since 2.1.1
     */
    protected org.apache.tiles2.impl.BasicTilesContainer instantiateContainer(
            TilesApplicationContext context) {
        return new BasicTilesContainer();
    }

    /**
     * Create a Tiles request context factory. By default it creates a
     * {@link ChainedTilesRequestContextFactory}.
     *
     * @param context The Tiles application context.
     * @return The request context factory.
     * @since 2.1.1
     */
    protected TilesRequestContextFactory createRequestContextFactory(
            TilesApplicationContext context) {
        ChainedTilesRequestContextFactory contextFactory = new ChainedTilesRequestContextFactory();
        registerChainedRequestContextFactories(contextFactory);

        return contextFactory;
    }

    /**
     * Register elements of a chained request context factory.
     *
     * @param contextFactory The request context factory to use.
     * @since 2.1.1
     */
    protected void registerChainedRequestContextFactories(
            ChainedTilesRequestContextFactory contextFactory) {
        List<TilesRequestContextFactory> factories = getTilesRequestContextFactoriesToBeChained(contextFactory);
        contextFactory.setFactories(factories);
    }

    /**
     * Returns the list of {@link TilesRequestContextFactory} instances to be
     * chained together.
     *
     * @param parent The parent factory.
     * @return The list of factories.
     * @since 2.2.0
     */
    protected List<TilesRequestContextFactory> getTilesRequestContextFactoriesToBeChained(
            ChainedTilesRequestContextFactory parent) {
        List<TilesRequestContextFactory> factories = new ArrayList<TilesRequestContextFactory>();
        registerRequestContextFactory(
                "org.apache.tiles.servlet.context.ServletTilesRequestContextFactory",
                factories, parent);
        registerRequestContextFactory(
                "org.apache.tiles.portlet.context.PortletTilesRequestContextFactory",
                factories, parent);
        registerRequestContextFactory(
                "org.apache.tiles.jsp.context.JspTilesRequestContextFactory",
                factories, parent);
        return factories;
    }

    /**
     * Registers a {@link TilesRequestContextFactory} specifying its
     * classname.
     *
     * @param className The name of the class to instantiate.
     * @param factories The list of factories to add to.
     * @param parent The parent {@link TilesRequestContextFactory}. If null
     * it won't be considered.
     * @since 2.1.1
     */
    protected void registerRequestContextFactory(String className,
            List<TilesRequestContextFactory> factories,
            TilesRequestContextFactory parent) {
        TilesRequestContextFactory retValue = null;
        try {
            Class<? extends TilesRequestContextFactory> clazz = ClassUtil
                    .getClass(className, TilesRequestContextFactory.class);
            retValue = clazz.newInstance();
            if (parent != null
                    && retValue instanceof TilesRequestContextFactoryAware) {
                ((TilesRequestContextFactoryAware) retValue)
                        .setRequestContextFactory(parent);
            }
        } catch (ClassNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot find class '" + className + "', ignoring problem", e);
            }
        } catch (NotAvailableFeatureException e) {
            if (log.isDebugEnabled()) {
                log.debug("Not available feature exception during instantiation of class '"
                        + className + "', ignoring problem", e);
            }
        } catch (InstantiationException e) {
            throw new org.apache.tiles2.factory.TilesContainerFactoryException(
                    "Cannot instantiate '" + className + "'", e);
        } catch (IllegalAccessException e) {
            throw new org.apache.tiles2.factory.TilesContainerFactoryException(
                    "Cannot access default constructor '" + className + "'",
                    e);
        }
        if (retValue != null) {
            factories.add(retValue);
        }
    }

    /**
     * Creates the definitions factory. By default it creates a
     * {@link org.apache.tiles2.definition.UnresolvingLocaleDefinitionsFactory} with default dependencies.
     *
     * @param applicationContext The Tiles application context.
     * @param contextFactory The Tiles context factory.
     * @param resolver The locale resolver.
     *
     * @return The definitions factory.
     * @since 2.1.1
     */
    protected DefinitionsFactory createDefinitionsFactory(TilesApplicationContext applicationContext,
                                                          TilesRequestContextFactory contextFactory,
                                                          LocaleResolver resolver) {
        org.apache.tiles2.definition.UnresolvingLocaleDefinitionsFactory factory = instantiateDefinitionsFactory(
                applicationContext, contextFactory, resolver);
        factory.setApplicationContext(applicationContext);
        factory.setLocaleResolver(resolver);
        factory.setDefinitionDAO(createLocaleDefinitionDao(applicationContext,
                contextFactory, resolver));
        if (factory instanceof Refreshable) {
            ((Refreshable) factory).refresh();
        }
        return factory;
    }

    /**
     * Instantiate a new definitions factory based on Locale.
     * @param applicationContext The Tiles application context.
     * @param contextFactory The Tiles context factory.
     * @param resolver The locale resolver.
     *
     * @return The definitions factory.
     * @since 2.2.1
     */
    protected org.apache.tiles2.definition.UnresolvingLocaleDefinitionsFactory instantiateDefinitionsFactory(
            TilesApplicationContext applicationContext,
            TilesRequestContextFactory contextFactory, LocaleResolver resolver) {
        return new UnresolvingLocaleDefinitionsFactory();
    }


    /**
     * Instantiate (and does not initialize) a Locale-based definition DAO.
     * @param applicationContext The Tiles application context.
     * @param contextFactory The Tiles context factory.
     * @param resolver The locale resolver.
     *
     * @return The definition DAO.
     * @since 2.1.1
     */
    protected org.apache.tiles2.definition.dao.BaseLocaleUrlDefinitionDAO instantiateLocaleDefinitionDao(TilesApplicationContext applicationContext,
                                                                                                         TilesRequestContextFactory contextFactory,
                                                                                                         LocaleResolver resolver) {
        org.apache.tiles2.definition.dao.ResolvingLocaleUrlDefinitionDAO dao = new ResolvingLocaleUrlDefinitionDAO();
        return dao;
    }

    /**
     * Creates a Locale-based definition DAO.
     * @param applicationContext The Tiles application context.
     * @param contextFactory The Tiles context factory.
     * @param resolver The locale resolver.
     *
     * @return The definition DAO.
     * @since 2.1.1
     */
    @SuppressWarnings("unchecked")
    protected DefinitionDAO<Locale> createLocaleDefinitionDao(TilesApplicationContext applicationContext,
                                                              TilesRequestContextFactory contextFactory,
                                                              LocaleResolver resolver) {
        BaseLocaleUrlDefinitionDAO definitionDao = instantiateLocaleDefinitionDao(
                applicationContext, contextFactory, resolver);
        definitionDao.setReader(createDefinitionsReader(applicationContext, contextFactory));
        definitionDao.setSourceURLs(getSourceURLs(applicationContext, contextFactory));
        definitionDao.setApplicationContext(applicationContext);
        if (definitionDao instanceof org.apache.tiles2.definition.pattern.PatternDefinitionResolverAware) {
            ((PatternDefinitionResolverAware<Locale>) definitionDao)
                    .setPatternDefinitionResolver(createPatternDefinitionResolver(Locale.class));
        }
        return definitionDao;
    }

    /**
     * Creates the locale resolver. By default it creates a
     * {@link DefaultLocaleResolver}.
     * @param applicationContext The Tiles application context.
     * @param contextFactory The Tiles context factory.
     *
     * @return The locale resolver.
     * @since 2.1.1
     */
    protected LocaleResolver createLocaleResolver(TilesApplicationContext applicationContext,
            TilesRequestContextFactory contextFactory) {
        return new DefaultLocaleResolver();
    }

    /**
     * Creates the definitions reader. By default it creates a
     * {@link org.apache.tiles2.definition.digester.DigesterDefinitionsReader}.
     * @param applicationContext The Tiles application context.
     * @param contextFactory The Tiles context factory.
     *
     * @return The definitions reader.
     * @since 2.1.1
     */
    protected DefinitionsReader createDefinitionsReader(
            TilesApplicationContext applicationContext,
            TilesRequestContextFactory contextFactory) {
        return new DigesterDefinitionsReader();
    }

    /**
     * Returns a list containing the URLs to be parsed. By default, it returns a
     * list containing the URL point to "/WEB-INF/tiles.xml".
     * @param applicationContext The Tiles application context.
     * @param contextFactory The Tiles context factory.
     *
     * @return The source URLs.
     * @since 2.1.1
     */
    protected List<URL> getSourceURLs(TilesApplicationContext applicationContext,
            TilesRequestContextFactory contextFactory) {
        List<URL> retValue = new ArrayList<URL>(1);
        try {
            retValue.add(applicationContext.getResource("/WEB-INF/tiles.xml"));
        } catch (IOException e) {
            throw new TilesContainerFactoryException(
                    "Cannot get URL: /WEB-INF/tiles.xml", e);
        }
        return retValue;
    }

    /**
     * Creates the attribute evaluator factory to use. By default it returns a
     * {@link org.apache.tiles2.evaluator.BasicAttributeEvaluatorFactory} containing the
     * {@link org.apache.tiles2.evaluator.impl.DirectAttributeEvaluator} as the default evaluator.
     *
     * @param applicationContext The Tiles application context.
     * @param contextFactory The Tiles context factory.
     * @param resolver The locale resolver.
     *
     * @return The evaluator factory.
     * @since 2.2.0
     */
    protected org.apache.tiles2.evaluator.AttributeEvaluatorFactory createAttributeEvaluatorFactory(
            TilesApplicationContext applicationContext,
            TilesRequestContextFactory contextFactory, LocaleResolver resolver) {
        return new BasicAttributeEvaluatorFactory(new DirectAttributeEvaluator());
    }

    /**
     * Creates the preparer factory to use. By default it returns a
     * {@link BasicPreparerFactory}.
     * @param applicationContext The Tiles application context.
     * @param contextFactory The Tiles context factory.
     *
     * @return The preparer factory.
     * @since 2.1.1
     */
    protected PreparerFactory createPreparerFactory(TilesApplicationContext applicationContext,
            TilesRequestContextFactory contextFactory) {
        return new BasicPreparerFactory();
    }

    /**
     * Creates a renderer factory. By default it returns a
     * {@link BasicRendererFactory}, composed of an
     * {@link UntypedAttributeRenderer} as default, and
     * {@link StringAttributeRenderer}, {@link TemplateAttributeRenderer} and
     * {@link DefinitionAttributeRenderer}.
     * @param applicationContext The Tiles application context.
     * @param contextFactory The Tiles context factory.
     * @param container The container.
     * @param attributeEvaluatorFactory The attribute evaluator factory.
     *
     * @return The renderer factory.
     * @since 2.2.0
     */
    protected RendererFactory createRendererFactory(TilesApplicationContext applicationContext,
            TilesRequestContextFactory contextFactory,
            TilesContainer container, org.apache.tiles2.evaluator.AttributeEvaluatorFactory attributeEvaluatorFactory) {
        BasicRendererFactory retValue = new BasicRendererFactory();
        retValue.setApplicationContext(applicationContext);
        retValue.setRequestContextFactory(contextFactory);
        retValue.setContainer(container);
        retValue.setAttributeEvaluatorFactory(attributeEvaluatorFactory);
        registerAttributeRenderers(retValue, applicationContext, contextFactory,
                container, attributeEvaluatorFactory);
        retValue.setDefaultRenderer(createDefaultAttributeRenderer(retValue,
                applicationContext, contextFactory, container,
                attributeEvaluatorFactory));
        return retValue;
    }

    /**
     * Creates the default attribute renderer. By default it is an
     * {@link ChainedDelegateAttributeRenderer}.
     *
     * @param rendererFactory The renderer factory to configure.
     * @param applicationContext The Tiles application context.
     * @param contextFactory The Tiles context factory.
     * @param container The container.
     * @param attributeEvaluatorFactory The attribute evaluator factory.
     * @return The default attribute renderer.
     * @since 2.2.1
     */
    protected AttributeRenderer createDefaultAttributeRenderer(
            BasicRendererFactory rendererFactory,
            TilesApplicationContext applicationContext,
            TilesRequestContextFactory contextFactory,
            TilesContainer container,
            org.apache.tiles2.evaluator.AttributeEvaluatorFactory attributeEvaluatorFactory) {
        ChainedDelegateAttributeRenderer retValue = new ChainedDelegateAttributeRenderer();
        retValue.addAttributeRenderer((TypeDetectingAttributeRenderer) rendererFactory
                .getRenderer(DEFINITION_RENDERER_NAME));
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
     * Creates a new pattern definition resolver. By default, it instantiate a
     * {@link org.apache.tiles2.definition.pattern.BasicPatternDefinitionResolver} with
     * {@link org.apache.tiles2.definition.pattern.wildcard.WildcardDefinitionPatternMatcherFactory} to manage wildcard
     * substitution.
     *
     * @param <T> The type of the customization key.
     * @param customizationKeyClass The customization key class.
     * @return The pattern definition resolver.
     * @since 2.2.0
     */
    protected <T> PatternDefinitionResolver<T> createPatternDefinitionResolver(
            Class<T> customizationKeyClass) {
        org.apache.tiles2.definition.pattern.wildcard.WildcardDefinitionPatternMatcherFactory definitionPatternMatcherFactory =
            new WildcardDefinitionPatternMatcherFactory();
        return new BasicPatternDefinitionResolver<T>(
                definitionPatternMatcherFactory,
                definitionPatternMatcherFactory);
    }

    /**
     * Registers attribute renderers in a {@link BasicRendererFactory}. By
     * default, it registers a {@link StringAttributeRenderer}, a
     * {@link TemplateAttributeRenderer} and a
     * {@link DefinitionAttributeRenderer}.
     *
     * @param rendererFactory The renderer factory to configure.
     * @param applicationContext The Tiles application context.
     * @param contextFactory The Tiles context factory.
     * @param container The container.
     * @param attributeEvaluatorFactory The attribute evaluator factory.
     * @since 2.2.0
     */
    protected void registerAttributeRenderers(
            BasicRendererFactory rendererFactory,
            TilesApplicationContext applicationContext,
            TilesRequestContextFactory contextFactory,
            TilesContainer container,
            org.apache.tiles2.evaluator.AttributeEvaluatorFactory attributeEvaluatorFactory) {
        rendererFactory.registerRenderer(STRING_RENDERER_NAME,
                createStringAttributeRenderer(rendererFactory,
                        applicationContext, contextFactory, container,
                        attributeEvaluatorFactory));
        rendererFactory.registerRenderer(TEMPLATE_RENDERER_NAME,
                createTemplateAttributeRenderer(rendererFactory,
                        applicationContext, contextFactory, container,
                        attributeEvaluatorFactory));
        rendererFactory.registerRenderer(DEFINITION_RENDERER_NAME,
                createDefinitionAttributeRenderer(rendererFactory,
                        applicationContext, contextFactory, container,
                        attributeEvaluatorFactory));
    }

    /**
     * Creates a {@link StringAttributeRenderer}.
     *
     * @param rendererFactory The renderer factory to configure.
     * @param applicationContext The Tiles application context.
     * @param contextFactory The Tiles context factory.
     * @param container The container.
     * @param attributeEvaluatorFactory The attribute evaluator factory.
     * @return The renderer.
     * @since 2.2.1
     */
    protected AttributeRenderer createStringAttributeRenderer(
            BasicRendererFactory rendererFactory,
            TilesApplicationContext applicationContext,
            TilesRequestContextFactory contextFactory,
            TilesContainer container,
            org.apache.tiles2.evaluator.AttributeEvaluatorFactory attributeEvaluatorFactory) {
        StringAttributeRenderer stringRenderer = new StringAttributeRenderer();
        stringRenderer.setApplicationContext(applicationContext);
        stringRenderer.setRequestContextFactory(contextFactory);
        stringRenderer.setAttributeEvaluatorFactory(attributeEvaluatorFactory);
        return stringRenderer;
    }

    /**
     * Creates a {@link TemplateAttributeRenderer}.
     *
     * @param rendererFactory The renderer factory to configure.
     * @param applicationContext The Tiles application context.
     * @param contextFactory The Tiles context factory.
     * @param container The container.
     * @param attributeEvaluatorFactory The attribute evaluator factory.
     * @return The renderer.
     * @since 2.2.1
     */
    protected AttributeRenderer createTemplateAttributeRenderer(
            BasicRendererFactory rendererFactory,
            TilesApplicationContext applicationContext,
            TilesRequestContextFactory contextFactory,
            TilesContainer container,
            org.apache.tiles2.evaluator.AttributeEvaluatorFactory attributeEvaluatorFactory) {
        TemplateAttributeRenderer templateRenderer = new TemplateAttributeRenderer();
        templateRenderer.setApplicationContext(applicationContext);
        templateRenderer.setRequestContextFactory(contextFactory);
        templateRenderer.setAttributeEvaluatorFactory(attributeEvaluatorFactory);
        return templateRenderer;
    }

    /**
     * Creates a {@link DefinitionAttributeRenderer}.
     *
     * @param rendererFactory The renderer factory to configure.
     * @param applicationContext The Tiles application context.
     * @param contextFactory The Tiles context factory.
     * @param container The container.
     * @param attributeEvaluatorFactory The attribute evaluator factory.
     * @return The renderer.
     * @since 2.2.1
     */
    protected AttributeRenderer createDefinitionAttributeRenderer(
            BasicRendererFactory rendererFactory,
            TilesApplicationContext applicationContext,
            TilesRequestContextFactory contextFactory,
            TilesContainer container,
            AttributeEvaluatorFactory attributeEvaluatorFactory) {
        DefinitionAttributeRenderer definitionRenderer = new DefinitionAttributeRenderer();
        definitionRenderer.setApplicationContext(applicationContext);
        definitionRenderer.setContainer(container);
        definitionRenderer.setRequestContextFactory(contextFactory);
        definitionRenderer.setAttributeEvaluatorFactory(attributeEvaluatorFactory);
        return definitionRenderer;
    }
}
