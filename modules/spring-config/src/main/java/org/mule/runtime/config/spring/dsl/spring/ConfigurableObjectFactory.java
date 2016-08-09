/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

import static org.mule.runtime.core.util.ClassUtils.instanciateClass;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.config.spring.dsl.api.ObjectFactory;
import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

//TODO remove from API
public class ConfigurableObjectFactory<T> extends AbstractAnnotatedObject implements ObjectFactory<T>
{
    @Inject
    private MuleContext muleContext;

    private Class<ObjectFactoryCommonConfigurator> commonConfiguratorType;
    private ConfigurableInstanceFactory factory;
    private Map<String, Object> parameters = new HashMap<>();

    @Override
    public T getObject() throws Exception
    {
        Object instance = factory.createInstance(parameters);
        if (instance instanceof AnnotatedObject)
        {
            ((AnnotatedObject) instance).setAnnotations(getAnnotations());
        }
        if (commonConfiguratorType != null)
        {
            ObjectFactoryCommonConfigurator commonConfigurator = instanciateClass(commonConfiguratorType);
            commonConfigurator.configure(instance, parameters);
        }
        if (instance instanceof MuleContextAware)
        {
            ((MuleContextAware) instance).setMuleContext(muleContext);
        }
        return (T) instance;
    }

    public void setCommonConfiguratorType(Class<ObjectFactoryCommonConfigurator> commonConfiguratorType)
    {
        this.commonConfiguratorType = commonConfiguratorType;
    }

    public void setFactory(ConfigurableInstanceFactory factory)
    {
        this.factory = factory;
    }

    public void setParameters(Map<String, Object> parameters)
    {
        this.parameters = parameters;
    }
}
