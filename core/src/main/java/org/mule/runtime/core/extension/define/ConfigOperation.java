/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.extension.define;


import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;

import java.util.Collections;
import java.util.List;

public class ConfigOperation implements MuleContextAware, Initialisable
{
    private String name;
    private List<OperationParameter> parameters = Collections.emptyList();
    private List<Object> bolsa = Collections.emptyList();


    public List<OperationParameter> getParameters()
    {
        return parameters;
    }

    public void setParameters(List<OperationParameter> parameters)
    {
        this.parameters = parameters;
    }

    public List<Object> getBolsa()
    {
        return bolsa;
    }

    public void setBolsa(List<Object> bolsa)
    {
        this.bolsa = bolsa;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        for (OperationParameter parameter : parameters)
        {
            parameter.setMuleContext(context);
        }
    }

    @Override
    public void initialise() throws InitialisationException
    {
        for (OperationParameter parameter : parameters)
        {
            parameter.initialise();
        }
    }
}
