/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.exception.error;

import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.exception.AbstractMessagingExceptionStrategy;
import org.mule.runtime.core.message.DefaultExceptionPayload;

import java.util.List;

public class ErrorHandler extends AbstractMessagingExceptionStrategy
{
    private List<OnErrorStrategy> errorStrategies;

    @Override
    protected void doInitialise(MuleContext context) throws InitialisationException
    {
        super.doInitialise(context);
        for (OnErrorStrategy errorStrategy : errorStrategies)
        {
            errorStrategy.initialise();
        }
    }

    @Override
    public MuleEvent handleException(MessagingException ex, MuleEvent event)
    {
        event.setMessage(MuleMessage.builder(event.getMessage())
                                 .exceptionPayload(new DefaultExceptionPayload(ex))
                                 .build());
        for (OnErrorStrategy onError : errorStrategies)
        {
            if (onError.accept(event) || onError.acceptsAll())
            {
                return onError.handleException(ex, event);
            }
        }
        return getMuleContext().getDefaultExceptionStrategy().handleException(ex, event);
    }

    public void setErrorStrategies(List<OnErrorStrategy> errorStrategies)
    {
        this.errorStrategies = errorStrategies;
    }
}
