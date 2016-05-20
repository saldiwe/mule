/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.REFERENCE_ATTRIBUTE;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.dsl.spring.BeanDefinitionCreator;
import org.mule.runtime.config.spring.dsl.spring.CreateBeanDefinitionRequest;
import org.mule.runtime.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.runtime.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.runtime.config.spring.parsers.specific.TransportElementBeanDefinitionPostProcessor;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

/**
 * Processor of the chain of responsibility that knows how to create the {@link org.springframework.beans.factory.config.BeanDefinition}
 * for a transformer or processor reference element.
 *
 * @since 4.0
 */
public class EndpointReferenceBeanDefinitionCreator extends BeanDefinitionCreator
{

    private TransportElementBeanDefinitionPostProcessor transportElementBeanDefinitionPostProcessor = new TransportElementBeanDefinitionPostProcessor();

    @Override
    public boolean handleRequest(CreateBeanDefinitionRequest createBeanDefinitionRequest)
    {
        //ComponentModel componentModel = createBeanDefinitionRequest.getComponentModel();
        //if (componentModel.getIdentifier().getName().equals("inbound-endpoint") && componentModel.getParameters().containsKey("ref"))
        //{
        //    componentModel.setType(InboundEndpoint.class);
        //    BeanDefinitionBuilder beanDefinitionBuilder = genericBeanDefinition(InboundEndpointFactoryBean.class);
        //    beanDefinitionBuilder.addConstructorArgValue(new RuntimeBeanReference(componentModel.getParameters().get(REFERENCE_ATTRIBUTE)));
        //    //TODO this is probably not needed and we can put handleRequest == false so it's done anyway by regular creator
        //    if (componentModel.getParameters().get("exchange-pattern") != null)
        //    {
        //        beanDefinitionBuilder.addPropertyValue("exchangePattern", componentModel.getParameters().get("exchange-pattern"));
        //    }
        //    AbstractBeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();
        //    componentModel.setBeanDefinition(beanDefinition);
        //    //transportElementBeanDefinitionPostProcessor.postProcess(componentModel, beanDefinition);
        //}
        //if (componentModel.getIdentifier().getName().equals("outbound-endpoint") && componentModel.getParameters().containsKey("ref"))
        //{
        //    componentModel.setType(OutboundEndpoint.class);
        //    BeanDefinitionBuilder beanDefinitionBuilder = genericBeanDefinition(OutboundEndpointFactoryBean.class);
        //    beanDefinitionBuilder.addConstructorArgValue(new RuntimeBeanReference(componentModel.getParameters().get(REFERENCE_ATTRIBUTE)));
        //    //TODO this is probably not needed and we can put handleRequest == false so it's done anyway by regular creator
        //    if (componentModel.getParameters().get("exchange-pattern") != null)
        //    {
        //        beanDefinitionBuilder.addPropertyValue("exchangePattern", componentModel.getParameters().get("exchange-pattern"));
        //    }
        //    AbstractBeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();
        //    componentModel.setBeanDefinition(beanDefinition);
        //    //transportElementBeanDefinitionPostProcessor.postProcess(componentModel, beanDefinition);
        //}
        return false;
    }
}
