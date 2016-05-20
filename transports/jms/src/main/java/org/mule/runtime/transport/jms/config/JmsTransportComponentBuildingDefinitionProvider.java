/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.config;

import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromReferenceObject;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleReferenceParameter;
import static org.mule.runtime.config.spring.dsl.processor.TypeDefinition.fromConfigurationAttribute;
import static org.mule.runtime.config.spring.dsl.processor.TypeDefinition.fromType;
import org.mule.runtime.config.spring.TransportComponentBuildingDefinitionProvider;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.api.TypeConverter;
import org.mule.runtime.config.spring.dsl.processor.TypeDefinition;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.retry.policies.SimpleRetryPolicyTemplate;
import org.mule.runtime.core.transaction.MuleTransactionConfig;
import org.mule.runtime.transport.jms.JmsConnector;
import org.mule.runtime.transport.jms.JmsTransactionFactory;
import org.mule.runtime.transport.jms.activemq.ActiveMQJmsConnector;
import org.mule.runtime.transport.jms.activemq.ActiveMQXAJmsConnector;
import org.mule.runtime.transport.jms.filters.JmsSelectorFilter;
import org.mule.runtime.transport.jms.jndi.JndiNameResolver;
import org.mule.runtime.transport.jms.jndi.SimpleJndiNameResolver;
import org.mule.runtime.transport.jms.transformers.JMSMessageToObject;
import org.mule.runtime.transport.jms.transformers.ObjectToJMSMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.Session;

public class JmsTransportComponentBuildingDefinitionProvider extends TransportComponentBuildingDefinitionProvider
{

    public static final String QUEUE = "queue";
    public static final String TOPIC = "topic";
    public static final String NUMBER_OF_CONSUMERS_ATTRIBUTE = "numberOfConsumers";
    public static final String NUMBER_OF_CONCURRENT_TRANSACTED_RECEIVERS_ATTRIBUTE = "numberOfConcurrentTransactedReceivers";
    public static final String NUMBER_OF_CONSUMERS_PROPERTY = "numberOfConcurrentTransactedReceivers";

    @Override
    public void init(MuleContext muleContext)
    {
        super.init(muleContext);
    }

    //@Override
    //public List<ComponentBuildingDefinition> getComponentBuildingDefinitions()
    //{
    //    return null;
    //}


    @Override
    protected ComponentBuildingDefinition.Builder getOutboundEndpointBuildingDefinitionBuilder()
    {
        return super.getOutboundEndpointBuildingDefinitionBuilder()
                .withNamespace("jms")
                .withSetterParameterDefinition("selector", fromChildConfiguration(JmsSelectorFilter.class).build())
                .withIgnoredConfigurationParameter("queue")
                .withIgnoredConfigurationParameter("topic");
    }

    @Override
    protected ComponentBuildingDefinition.Builder getEndpointBuildingDefinitionBuilder()
    {
        return super.getEndpointBuildingDefinitionBuilder()
                .withNamespace("jms").withSetterParameterDefinition("selector", fromChildConfiguration(JmsSelectorFilter.class).build())
                .withIgnoredConfigurationParameter("queue")
                .withIgnoredConfigurationParameter("topic");
    }

    @Override
    protected ComponentBuildingDefinition.Builder getInboundEndpointBuildingDefinitionBuilder()
    {
        return super.getInboundEndpointBuildingDefinitionBuilder()
                .withNamespace("jms").withSetterParameterDefinition("selector", fromChildConfiguration(JmsSelectorFilter.class).build())
                .withIgnoredConfigurationParameter("queue")
                .withIgnoredConfigurationParameter("topic");
    }

    @Override
    protected Collection<? extends ComponentBuildingDefinition> getTransportSpecificDefinitionParsers()
    {
        List<ComponentBuildingDefinition> componentBuildingDefinitions = new ArrayList<>();
        ComponentBuildingDefinition.Builder baseJmsConnector = new ComponentBuildingDefinition.Builder()
                .withNamespace("jms")
                .withIdentifier("connector")
                .withTypeDefinition(fromType(JmsConnector.class))
                .withConstructorParameterDefinition(fromReferenceObject(MuleContext.class).build())
                .withSetterParameterDefinition("acknowledgementMode", fromSimpleParameter("acknowledgementMode", new TypeConverter<String, Integer>()
                {
                    @Override
                    public Integer convert(String ackMode)
                    {
                        switch (ackMode)
                        {
                            case "AUTO_ACKNOWLEDGE":
                                return Session.AUTO_ACKNOWLEDGE;
                            case "CLIENT_ACKNOWLEDGE":
                                return Session.CLIENT_ACKNOWLEDGE;
                            case "DUPS_OK_ACKNOWLEDGE":
                                return Session.DUPS_OK_ACKNOWLEDGE;
                            default:
                                throw new MuleRuntimeException(CoreMessages.createStaticMessage("Wrong acknowledgement mode configuration: " + ackMode));
                        }
                    }
                }).build())
                .withSetterParameterDefinition("clientId", fromSimpleParameter("clientId").build())
                .withSetterParameterDefinition("durable", fromSimpleParameter("durable").build())
                .withSetterParameterDefinition("noLocal", fromSimpleParameter("noLocal").build())
                .withSetterParameterDefinition("persistentDelivery", fromSimpleParameter("persistentDelivery").build())
                .withSetterParameterDefinition("cacheJmsSessions", fromSimpleParameter("cacheJmsSessions").build())
                .withSetterParameterDefinition("eagerConsumer", fromSimpleParameter("eagerConsumer").build())
                .withSetterParameterDefinition("username", fromSimpleParameter("username").build())
                .withSetterParameterDefinition("password", fromSimpleParameter("password").build())
                .withSetterParameterDefinition("jndiDestinations", fromSimpleParameter("jndiDestinations").build())
                .withSetterParameterDefinition("jndiInitialFactory", fromSimpleParameter("jndiInitialFactory").build())
                .withSetterParameterDefinition("jndiProviderUrl", fromSimpleParameter("jndiProviderUrl").build())
                .withSetterParameterDefinition("connectionFactoryJndiName", fromSimpleParameter("connectionFactoryJndiName").build())
                .withSetterParameterDefinition("jndiProviderProperties", fromSimpleReferenceParameter("jndiProviderProperties-ref").build())
                .withSetterParameterDefinition("forceJndiDestinations", fromSimpleParameter("forceJndiDestinations").build())
                .withSetterParameterDefinition("specification", fromSimpleParameter("specification").build())
                .withSetterParameterDefinition("disableTemporaryReplyToDestinations", fromSimpleParameter("disableTemporaryReplyToDestinations").build())
                .withSetterParameterDefinition("returnOriginalMessageAsReply", fromSimpleParameter("returnOriginalMessageAsReply").build())
                .withSetterParameterDefinition("embeddedMode", fromSimpleParameter("embeddedMode").build())
                .withSetterParameterDefinition("honorQosHeaders", fromSimpleParameter("honorQosHeaders").build())
                .withSetterParameterDefinition("sameRMOverrideValue", fromSimpleParameter("sameRMOverrideValue").build())
                .withSetterParameterDefinition("maxRedelivery", fromSimpleParameter("maxRedelivery").build())
                .withSetterParameterDefinition("redeliveryHandlerFactory", fromSimpleReferenceParameter("redeliveryHandlerFactory-ref").build())
                .withSetterParameterDefinition("connectionFactory", fromSimpleReferenceParameter("connectionFactory-ref").build())
                .withSetterParameterDefinition(NUMBER_OF_CONSUMERS_ATTRIBUTE, fromSimpleParameter(NUMBER_OF_CONSUMERS_ATTRIBUTE).build())
                .withSetterParameterDefinition(NUMBER_OF_CONCURRENT_TRANSACTED_RECEIVERS_ATTRIBUTE, fromSimpleParameter(NUMBER_OF_CONCURRENT_TRANSACTED_RECEIVERS_ATTRIBUTE).build())
                .withSetterParameterDefinition("serviceOverrides", fromChildConfiguration(Map.class).build())
                .withSetterParameterDefinition("retryPolicyTemplate", fromChildConfiguration(RetryPolicyTemplate.class).build())
                .withSetterParameterDefinition("jndiNameResolver", fromChildConfiguration(JndiNameResolver.class).build());
        componentBuildingDefinitions.add(baseJmsConnector.copy().build());
        ComponentBuildingDefinition.Builder baseActiveMqConnector = baseJmsConnector.copy()
                .withNamespace("jms")
                .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
                .withSetterParameterDefinition("brokerURL", fromSimpleParameter("brokerURL").build());
        componentBuildingDefinitions.add(baseActiveMqConnector.copy()
                                                 .withIdentifier("activemq-connector")
                                                 .withTypeDefinition(fromType(ActiveMQJmsConnector.class))
                                                 .build());
        componentBuildingDefinitions.add(baseActiveMqConnector.copy()
                                                 .withIdentifier("activemq-xa-connector")
                                                 .withTypeDefinition(fromType(ActiveMQXAJmsConnector.class))
                                                 .build());

        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withNamespace("jms")
                                                 .withIdentifier("default-jndi-name-resolver")
                                                 .withTypeDefinition(fromType(SimpleJndiNameResolver.class))
                                                 .withSetterParameterDefinition("jndiInitialFactory", fromSimpleParameter("jndiInitialFactory").build())
                                                 .withSetterParameterDefinition("jndiProviderUrl", fromSimpleParameter("jndiProviderUrl").build())
                                                 .withSetterParameterDefinition("jndiProviderProperties", fromSimpleReferenceParameter("jndiProviderProperties-ref").build())
                                                 .build());


        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withNamespace("jms")
                                                 .withIdentifier("custom-jndi-name-resolver")
                                                 .withTypeDefinition(fromConfigurationAttribute("class"))
                                                 .build());

        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withNamespace("jms")
                                                 .withIdentifier("selector")
                                                 .withTypeDefinition(fromType(JmsSelectorFilter.class))
                                                 .withSetterParameterDefinition("expression", fromSimpleParameter("expression").build()).build());

        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withNamespace("jms")
                                                 .withIdentifier("transaction")
                                                 .withTypeDefinition(fromType(MuleTransactionConfig.class))
                                                 .withSetterParameterDefinition("factory", fromFixedValue(new JmsTransactionFactory()).build())
                                                 .withSetterParameterDefinition("action", fromSimpleParameter("action", new TypeConverter<String, Byte>()
                                                 {
                                                     @Override
                                                     public Byte convert(String action)
                                                     {
                                                         switch (action)
                                                         {
                                                             case "ALWAYS_BEGIN": return TransactionConfig.ACTION_ALWAYS_BEGIN;
                                                             case "ALWAYS_JOIN": return TransactionConfig.ACTION_ALWAYS_JOIN;
                                                             case "JOIN_IF_POSSIBLE": return TransactionConfig.ACTION_JOIN_IF_POSSIBLE;
                                                             case "NONE": return TransactionConfig.ACTION_NONE;
                                                             case "BEGIN_OR_JOIN": return TransactionConfig.ACTION_BEGIN_OR_JOIN;
                                                             case "INDIFFERENT": return TransactionConfig.ACTION_INDIFFERENT;
                                                             case "NEVER": return TransactionConfig.ACTION_NEVER;
                                                             case "NOT_SUPPORTED": return TransactionConfig.ACTION_NOT_SUPPORTED;
                                                             default: throw new MuleRuntimeException(CoreMessages.createStaticMessage("Wrong transaction action configuration parameter: " + action));
                                                         }
                                                     }
                                                 }).build())
                                                 .withSetterParameterDefinition("expression", fromSimpleParameter("expression").build()).build());

        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withNamespace("jms")
                                                 .withIdentifier("jmsmessage-to-object-transformer")
                                                 .withTypeDefinition(fromType(JMSMessageToObject.class))
                                                 .build());
        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withNamespace("jms")
                                                 .withIdentifier("object-to-jmsmessage-transformer")
                                                 .withTypeDefinition(fromType(ObjectToJMSMessage.class))
                                                 .build());

        return componentBuildingDefinitions;
    }
}
