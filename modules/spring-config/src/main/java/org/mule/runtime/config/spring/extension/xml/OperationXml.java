/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.extension.xml;

import java.util.ArrayList;
import java.util.List;

public class OperationXml
{
    private String name;
    private List<PropertyXml> parameters = new ArrayList<>();

    public void setName(String name) {
        this.name = name;
    }

    public void setParameters(List<PropertyXml> parameters) {
        this.parameters = parameters;
    }

    public String getName()
    {
        return name;
    }

    public List<PropertyXml> getParameters()
    {
        return parameters;
    }
}
