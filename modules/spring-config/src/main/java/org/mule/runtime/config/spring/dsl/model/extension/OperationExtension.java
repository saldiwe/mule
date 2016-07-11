/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model.extension;

import org.mule.runtime.config.spring.dsl.model.ComponentModel;

import java.util.ArrayList;
import java.util.List;

public class OperationExtension
{
    private String name;
    private List<ParameterExtension> parameters = new ArrayList<>();
    private ComponentModel componentModel;

    public OperationExtension(String name, ComponentModel componentModel)
    {
        this.name = name;
        this.componentModel = componentModel;
    }

    public String getName()
    {
        return name;
    }

    public List<ParameterExtension> getParameters()
    {
        return parameters;
    }

    public void setParameters(List<ParameterExtension> parameters)
    {
        this.parameters = parameters;
    }

    public ComponentModel getComponentModel()
    {
        return componentModel;
    }
}
