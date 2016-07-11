/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model.extension;

import com.google.common.base.Optional;

public class ParameterExtension
{
    private String paramName;
    private Optional<String> defaultValue;

    public ParameterExtension(String paramName, String defaultValue)
    {
        this.paramName = paramName;
        this.defaultValue = Optional.fromNullable(defaultValue);
    }

    public String getParamName()
    {
        return paramName;
    }

    public Optional<String> getDefaultValue()
    {
        return defaultValue;
    }
}
