/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.builders;

import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.ConfigurationException;
import org.mule.api.config.DomainMuleContextAwareConfigurationBuilder;
import org.mule.config.ConfigResource;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.ClassUtils;

import java.util.List;

/**
 *
 */
public abstract class AbstractConfigurationBuilderFactory implements ConfigurationBuilderFactory
{

    @Override
    public ConfigurationBuilder createConfigurationBuilder(MuleContext domainContext, List<ConfigResource> configs) throws ConfigurationException
    {
        //TODO(pablo.kraan): OSGi - find a better way to do this
        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {

            String className = getClassName();
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            if (className == null || !ClassUtils.isClassOnPath(className, this.getClass()))
            {
                //TODO(pablo.kraan): OSGi - use createConfigResourcesString() as in AutoConfigurationBuilder
                throw new ConfigurationException(
                        CoreMessages.configurationBuilderNoMatching(configs.toString()));
            }

            ConfigResource[] constructorArg = new ConfigResource[configs.size()];
            System.arraycopy(configs.toArray(), 0, constructorArg, 0, configs.size());

            ConfigurationBuilder cb = instantiateConfigurationBuilder(className, constructorArg);

            if (domainContext != null && cb instanceof DomainMuleContextAwareConfigurationBuilder)
            {
                ((DomainMuleContextAwareConfigurationBuilder) cb).setDomainContext(domainContext);
            }
            else if (domainContext != null)
            {
                throw new MuleRuntimeException(CoreMessages.createStaticMessage(String.format("ConfigurationBuilder %s does not support domain context", cb.getClass().getCanonicalName())));
            }

            return cb;
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    protected abstract String getClassName();

    private ConfigurationBuilder instantiateConfigurationBuilder(String className, ConfigResource[] constructorArg) throws ConfigurationException
    {
        try
        {
            return (ConfigurationBuilder) ClassUtils.instanciateClass(className, new Object[] {constructorArg});
        }
        catch (Exception e)
        {
            throw new ConfigurationException(e);
        }
    }
}
