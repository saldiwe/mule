/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static reactor.core.Exceptions.propagate;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.context.WorkManagerSource;
import org.mule.runtime.core.config.i18n.CoreMessages;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public class LaxAsyncInterceptingMessageProcessor extends AsyncInterceptingMessageProcessor
{

    public LaxAsyncInterceptingMessageProcessor(WorkManagerSource workManagerSource)
    {
        super(workManagerSource);
    }

    public LaxAsyncInterceptingMessageProcessor(ThreadingProfile threadingProfile,
                                                 String name,
                                                 int shutdownTimeout)
    {
        super(threadingProfile, name, shutdownTimeout);
    }

    protected boolean isProcessAsync(MuleEvent event) throws MessagingException
    {
        return doThreading && canProcessAsync(event);
    }

    @Override
    public Publisher<MuleEvent> apply(Publisher<MuleEvent> publisher)
    {
        return Flux.from(publisher).map(event -> {
            try
            {
                if (!canProcessAsync(event))
                {
                    throw propagate(new MessagingException(
                            CoreMessages.createStaticMessage(SYNCHRONOUS_NONBLOCKING_EVENT_ERROR_MESSAGE),
                            event, this));
                }
            }
            catch (MessagingException e)
            {
                throw propagate(e);
            }
            return event;
        }).as(next);
    }


}
