/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static reactor.core.publisher.Flux.from;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.security.SecurityFilter;

import org.reactivestreams.Publisher;
import reactor.core.util.Exceptions;

/**
 * Filters the flow using the specified {@link SecurityFilter}. 
 * If unauthorised the flow is stopped and therefore the
 * message is not send or dispatched by the transport. When unauthorised the request
 * message is returned as the response.
 */
public class SecurityFilterMessageProcessor extends AbstractInterceptingMessageProcessor implements Initialisable
{
    private SecurityFilter filter;

    /**
     * For IoC only
     * @deprecated Use SecurityFilterMessageProcessor(SecurityFilter filter) instead
     */
    @Deprecated
    public SecurityFilterMessageProcessor()
    {
        super();
    }

    @Override
    public void initialise() throws InitialisationException
    {
        LifecycleUtils.initialiseIfNeeded(filter, muleContext);
    }

    public SecurityFilterMessageProcessor(SecurityFilter filter)
    {
        this.filter = filter;
    }

    public SecurityFilter getFilter()
    {
        return filter;
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (filter != null)
        {
            filter.doFilter(event);
        }
        return processNext(event);
    }

    public void setFilter(SecurityFilter filter)
    {
        this.filter = filter;
    }

    @Override
    public Publisher<MuleEvent> apply(Publisher<MuleEvent> publisher)
    {
        if (filter == null)
        {
            return super.apply(publisher);
        }
        else
        {
            return from(publisher).doOnNext(event -> {
                try
                {
                    filter.doFilter(event);
                }
                catch (Exception e)
                {
                    throw Exceptions.propagate(e);
                }
            }).doOnNext(muleEvent -> {
                if (logger.isTraceEnabled())
                {
                    logger.trace("Invoking next MessageProcessor: '" + next.getClass().getName() + "' ");
                }
            }).as(next);
        }
    }
}
