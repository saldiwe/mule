/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.extension.define;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.runtime.core.util.AttributeEvaluator;

public class OperationParameter implements MuleContextAware, Initialisable
{
    private String parameterName;
    private AttributeEvaluator defaultValue = new AttributeEvaluator(null);

    private MuleContext muleContext;

    public String getParameterName()
    {
        return parameterName;
    }

    public void setParameterName(String parameterName)
    {
        this.parameterName = parameterName;
    }

    public void setDefaultValue(String defaultValue)
    {
        this.defaultValue = new AttributeEvaluator(defaultValue);
    }

    public TypedValue getDefaultValue(MuleEvent event)
    {
        return defaultValue.resolveTypedValue(event);
    }

    public boolean hasDefaultValue(){
        return defaultValue.getRawValue() != null;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        defaultValue.initialize(muleContext.getExpressionManager());
    }
}
