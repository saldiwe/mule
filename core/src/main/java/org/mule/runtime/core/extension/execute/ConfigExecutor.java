/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.extension.execute;


import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.extension.define.ModuleOperation;
import org.mule.runtime.core.extension.define.OperationParameter;

import java.util.Collections;
import java.util.List;

public class ConfigExecutor implements Initialisable, MuleContextAware
{

    private String name;
    private ModuleOperation module;
    private List<ParameterRef> parameters = Collections.emptyList();

    private MuleContext muleContext;

    public void setName(String name)
    {
        this.name = name;
    }

    public void setParameters(List<ParameterRef> parameters)
    {
        this.parameters = parameters;
    }

    public List<ParameterRef> getParametersRef()
    {
        return parameters;
    }

    public void setModule(ModuleOperation moduleOperation)
    {
        this.module = moduleOperation;
    }

    @Override
    //TODO hack, talk to PLG, internal beans are not initialized by its own
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
        parameters.forEach(parameterEntry -> parameterEntry.setMuleContext(context));
    }

    @Override
    //TODO hack, talk to PLG, internal beans are not initialized by its own
    public void initialise() throws InitialisationException
    {
        for (ParameterRef parameter : parameters)
        {
            parameter.initialise();
        }
        //TODO hack^2, talk to PLG
        if (this.module.getConfig() != null)
        {
            this.module.getConfig().initialise();
        }
    }

    public List<OperationParameter> getParameters()
    {
        return this.module.getConfig().getParameters();
    }
}
