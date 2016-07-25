/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.extension.xml;

public class PropertyXml
{
    private String name;
    private String defaultValue;
    private String type;

    public void setName(String name) {
        this.name = name;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getName()
    {
        return name;
    }

    public String getDefaultValue()
    {
        return defaultValue;
    }

    public String getType()
    {
        return type;
    }

    public boolean hasDefaultValue() {
        return defaultValue != null;
    }
}
