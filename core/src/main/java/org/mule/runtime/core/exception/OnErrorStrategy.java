/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.exception;

import static org.mule.runtime.core.exception.ErrorType.ANY;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;

public class OnErrorStrategy extends TemplateMessagingExceptionStrategy
{
    private ErrorType errorType;

    public OnErrorStrategy()
    {
        setHandleException(true);
    }

    @Override
    protected boolean acceptsEvent(MuleEvent event)
    {
        Throwable exception = event.getMessage().getExceptionPayload().getException();
        if (errorType.accept(exception))
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean acceptsAll()
    {
        return ANY.equals(errorType);
    }

    @Override
    protected void nullifyExceptionPayloadIfRequired(MuleEvent event)
    {
        event.setMessage(MuleMessage.builder(event.getMessage())
                                 .exceptionPayload(null)
                                 .build());
    }

    @Override
    protected MuleEvent afterRouting(Exception exception, MuleEvent event)
    {
        return event;
    }

    @Override
    protected MuleEvent beforeRouting(Exception exception, MuleEvent event)
    {
        return event;
    }

    public void setErrorType(String errorType)
    {
        this.errorType = ErrorType.valueOf(errorType);
    }
}
