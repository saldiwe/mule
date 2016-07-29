/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.exception.error;

import static org.mule.runtime.core.exception.error.ErrorType.GENERAL;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.exception.TemplateMessagingExceptionStrategy;

public class OnErrorStrategy extends TemplateMessagingExceptionStrategy
{
    private ErrorType errorType;
    private String transactionResolution = "COMMIT";
    private String nextExecutionAction = "CONTINUE";

    public OnErrorStrategy()
    {
        setHandleException(true);
    }

    @Override
    protected boolean acceptsEvent(MuleEvent event)
    {
        Throwable exception = event.getMessage().getExceptionPayload().getException();
        if (errorType.accept((MessagingException) exception))
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean acceptsAll()
    {
        return GENERAL.equals(errorType);
    }

    @Override
    protected void nullifyExceptionPayloadIfRequired(MuleEvent event)
    {
        event.setMessage(MuleMessage.builder(event.getMessage())
                                 .exceptionPayload(null)
                                 .build());
    }

    @Override
    protected MuleEvent beforeRouting(MessagingException exception, MuleEvent event)
    {
        if ("ROLLBACK".equals(transactionResolution))
        {
            rollback(exception);
        }
        return event;
    }

    @Override
    protected void markExceptionAsHandled(Exception exception)
    {
        if ("CONTINUE".equals(nextExecutionAction))
        {
            super.markExceptionAsHandled(exception);
        }
    }

    public void setErrorType(String errorType)
    {
        this.errorType = ErrorType.valueOf(errorType);
    }

    public void setTransactionResolution(String transactionResolution)
    {
        this.transactionResolution = transactionResolution;
    }

    public void setNextExecutionAction(String nextExecutionAction)
    {
        this.nextExecutionAction = nextExecutionAction;
    }
}
