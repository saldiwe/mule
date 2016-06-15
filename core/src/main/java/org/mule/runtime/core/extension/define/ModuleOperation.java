/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.extension.define;


import java.util.List;

public class ModuleOperation
{
    private String name;
    private ConfigOperation config;
    private List<OperationChain> operations;

    public List<OperationChain> getOperations()
    {
        return operations;
    }

    public void setOperations(List<OperationChain> operations)
    {
        this.operations = operations;
    }

    public ConfigOperation getConfig()
    {
        return config;
    }

    public void setConfig(ConfigOperation config)
    {
        this.config = config;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
