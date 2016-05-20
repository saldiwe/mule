/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static org.mule.runtime.config.spring.TransportXmlNamespaceInfoProvider.TRANSPORTS_NAMESPACE_NAME;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildListConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromReferenceObject;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleReferenceParameter;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromUndefinedSimpleAttributes;
import static org.mule.runtime.config.spring.dsl.processor.TypeDefinition.fromType;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinitionProvider;
import org.mule.runtime.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.runtime.config.spring.factories.MessageProcessorChainFactoryBean;
import org.mule.runtime.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.retry.RetryPolicy;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.runtime.core.processor.AbstractRedeliveryPolicy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TransportComponentBuildingDefinitionProvider implements ComponentBuildingDefinitionProvider
{

    private ComponentBuildingDefinition.Builder baseDefinition;

    @Override
    public void init(MuleContext muleContext)
    {
        baseDefinition = new ComponentBuildingDefinition.Builder().withNamespace(TRANSPORTS_NAMESPACE_NAME);
    }

    @Override
    public final List<ComponentBuildingDefinition> getComponentBuildingDefinitions()
    {
        LinkedList<ComponentBuildingDefinition> componentBuildingDefinitions = new LinkedList<>();
        componentBuildingDefinitions.add(getInboundEndpointBuildingDefinitionBuilder().build());
        componentBuildingDefinitions.add(getOutboundEndpointBuildingDefinitionBuilder().build());
        componentBuildingDefinitions.add(getEndpointBuildingDefinitionBuilder().build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier("response")
                                                 .withTypeDefinition(fromType(MessageProcessor.class))
                                                 .withObjectFactoryType(MessageProcessorChainFactoryBean.class)
                                                 //.withSetterParameterDefinition("messageProcessors", fromChildListConfiguration(MessageProcessor.class).build())
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier("service-overrides")
                                                 .withObjectFactoryType(ServiceOverridesObjectFactory.class)
                                                 .withTypeDefinition(fromType(Map.class))
                                                 .withSetterParameterDefinition("messageReceiver", fromSimpleParameter("messageReceiver").build())
                                                 .withSetterParameterDefinition("transactedMessageReceiver", fromSimpleParameter("transactedMessageReceiver").build())
                                                 .withSetterParameterDefinition("xaTransactedMessageReceiver", fromSimpleParameter("xaTransactedMessageReceiver").build())
                                                 .withSetterParameterDefinition("dispatcherFactory", fromSimpleParameter("dispatcherFactory").build())
                                                 .withSetterParameterDefinition("inboundTransformer", fromSimpleParameter("inboundTransformer").build())
                                                 .withSetterParameterDefinition("outboundTransformer", fromSimpleParameter("outboundTransformer").build())
                                                 .withSetterParameterDefinition("responseTransformer", fromSimpleParameter("responseTransformer").build())
                                                 .withSetterParameterDefinition("endpointBuilder", fromSimpleParameter("endpointBuilder").build())
                                                 .withSetterParameterDefinition("messageFactory", fromSimpleParameter("messageFactory").build())
                                                 .withSetterParameterDefinition("serviceFinder", fromSimpleParameter("serviceFinder").build())
                                                 .withSetterParameterDefinition("sessionHandler", fromSimpleParameter("sessionHandler").build())
                                                 .withSetterParameterDefinition("inboundExchangePatterns", fromSimpleParameter("inboundExchangePatterns").build())
                                                 .withSetterParameterDefinition("outboundExchangePatterns", fromSimpleParameter("outboundExchangePatterns").build())
                                                 .withSetterParameterDefinition("defaultExchangePattern", fromSimpleParameter("defaultExchangePattern").build())
                                                 .build());
        componentBuildingDefinitions.addAll(getTransportSpecificDefinitionParsers());
        return componentBuildingDefinitions;
    }

    protected Collection<? extends ComponentBuildingDefinition> getTransportSpecificDefinitionParsers()
    {
        return Collections.emptyList();
    }

    protected ComponentBuildingDefinition.Builder getOutboundEndpointBuildingDefinitionBuilder()
    {
        return baseDefinition.copy()
                .withIdentifier("outbound-endpoint")
                .withObjectFactoryType(OutboundEndpointFactoryBean.class)
                .withTypeDefinition(fromType(OutboundEndpoint.class))
                .withSetterParameterDefinition("connector", fromSimpleReferenceParameter("connector-ref").build())
                .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
                .withSetterParameterDefinition("encoding", fromSimpleParameter("encoding").build())
                .withSetterParameterDefinition("transactionConfig", fromChildConfiguration(TransactionConfig.class).build())
                .withSetterParameterDefinition("deleteUnacceptedMessages", fromSimpleParameter("deleteUnacceptedMessages").build())
                .withSetterParameterDefinition("initialState", fromSimpleParameter("initialState").build())
                .withSetterParameterDefinition("responseTimeout", fromSimpleParameter("responseTimeout").build())
                .withSetterParameterDefinition("retryPolicyTemplate", fromChildConfiguration(RetryPolicy.class).build())
                .withSetterParameterDefinition("exchangePattern", fromSimpleParameter("exchange-pattern").build())
                .withSetterParameterDefinition("muleContext", fromReferenceObject(MuleContext.class).build())
                .withSetterParameterDefinition("messageProcessors", fromChildListConfiguration(MessageProcessor.class).build())
                .withSetterParameterDefinition("disableTransportTransformer", fromSimpleParameter("disableTransportTransformer").build())
                .withSetterParameterDefinition("mimeType", fromSimpleParameter("mimeType").build())
                .withSetterParameterDefinition("exchangePattern", fromSimpleParameter("exchange-pattern").build())
                .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
                .withSetterParameterDefinition("serviceOverrides", fromChildConfiguration(HashMap.class).build())
                .withSetterParameterDefinition("redeliveryPolicy", fromChildConfiguration(AbstractRedeliveryPolicy.class).build());
    }

    protected ComponentBuildingDefinition.Builder getEndpointBuildingDefinitionBuilder()
    {
        return baseDefinition.copy()
                .withIdentifier("endpoint")
                .withTypeDefinition(fromType(EndpointURIEndpointBuilder.class))
                .withSetterParameterDefinition("connector", fromSimpleReferenceParameter("connector-ref").build())
                .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
                .withSetterParameterDefinition("encoding", fromSimpleParameter("encoding").build())
                .withSetterParameterDefinition("transactionConfig", fromChildConfiguration(TransactionConfig.class).build())
                .withSetterParameterDefinition("deleteUnacceptedMessages", fromSimpleParameter("deleteUnacceptedMessages").build())
                .withSetterParameterDefinition("initialState", fromSimpleParameter("initialState").build())
                .withSetterParameterDefinition("responseTimeout", fromSimpleParameter("responseTimeout").build())
                .withSetterParameterDefinition("retryPolicyTemplate", fromChildConfiguration(RetryPolicy.class).build())
                .withSetterParameterDefinition("exchangePattern", fromSimpleParameter("exchange-pattern").build())
                .withSetterParameterDefinition("muleContext", fromReferenceObject(MuleContext.class).build())
                .withSetterParameterDefinition("messageProcessors", fromChildListConfiguration(MessageProcessor.class).build())
                .withSetterParameterDefinition("disableTransportTransformer", fromSimpleParameter("disableTransportTransformer").build())
                .withSetterParameterDefinition("mimeType", fromSimpleParameter("mimeType").build())
                .withSetterParameterDefinition("redeliveryPolicy", fromChildConfiguration(AbstractRedeliveryPolicy.class).build())
                .withSetterParameterDefinition("properties", fromUndefinedSimpleAttributes().build())
                .withSetterParameterDefinition("exchangePattern", fromSimpleParameter("exchange-pattern").build())
                .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
                .asPrototype();
    }

    protected ComponentBuildingDefinition.Builder getInboundEndpointBuildingDefinitionBuilder()
    {
        return baseDefinition.copy()
                .withIdentifier("inbound-endpoint")
                .withObjectFactoryType(InboundEndpointFactoryBean.class)
                .withTypeDefinition(fromType(InboundEndpoint.class))
                .withSetterParameterDefinition("connector", fromSimpleReferenceParameter("connector-ref").build())
                .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
                .withSetterParameterDefinition("encoding", fromSimpleParameter("encoding").build())
                .withSetterParameterDefinition("transactionConfig", fromChildConfiguration(TransactionConfig.class).build())
                .withSetterParameterDefinition("deleteUnacceptedMessages", fromSimpleParameter("deleteUnacceptedMessages").build())
                .withSetterParameterDefinition("initialState", fromSimpleParameter("initialState").build())
                .withSetterParameterDefinition("responseTimeout", fromSimpleParameter("responseTimeout").build())
                .withSetterParameterDefinition("retryPolicyTemplate", fromChildConfiguration(RetryPolicy.class).build())
                .withSetterParameterDefinition("exchangePattern", fromSimpleParameter("exchange-pattern").build())
                .withSetterParameterDefinition("muleContext", fromReferenceObject(MuleContext.class).build())
                .withSetterParameterDefinition("messageProcessors", fromChildListConfiguration(MessageProcessor.class).build())
                .withSetterParameterDefinition("disableTransportTransformer", fromSimpleParameter("disableTransportTransformer").build())
                .withSetterParameterDefinition("mimeType", fromSimpleParameter("mimeType").build())
                .withSetterParameterDefinition("redeliveryPolicy", fromChildConfiguration(AbstractRedeliveryPolicy.class).build())
                .withSetterParameterDefinition("exchangePattern", fromSimpleParameter("exchange-pattern").build())
                .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
                .withSetterParameterDefinition("properties", fromUndefinedSimpleAttributes().build());
    }
}
