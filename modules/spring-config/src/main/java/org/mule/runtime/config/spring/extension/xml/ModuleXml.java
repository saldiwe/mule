/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.extension.xml;

import java.util.ArrayList;
import java.util.List;

public class ModuleXml
{

    private String name;
    private List<PropertyXml> properties;
    private List<OperationXml> operations = new ArrayList<>();

    public void setName(String name) {
        this.name = name;
    }

    public void setOperations(List<OperationXml> operations) {
        this.operations = operations;
    }

    public void setProperties(List<PropertyXml> properties)
    {
        this.properties = properties;
    }

    public String getName()
    {
        return name;
    }

    public List<PropertyXml> getProperties()
    {
        return properties;
    }

    public List<OperationXml> getOperations()
    {
        return operations;
    }

    public boolean hasConfig()
    {
        return ! properties.isEmpty();
    }
}
