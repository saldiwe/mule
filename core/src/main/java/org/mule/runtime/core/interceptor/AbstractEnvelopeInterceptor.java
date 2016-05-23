/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.interceptor;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.interceptor.Interceptor;
import org.mule.runtime.core.management.stats.ProcessingTime;
import org.mule.runtime.core.processor.AbstractRequestResponseMessageProcessor;

/**
 * <code>EnvelopeInterceptor</code> is an intercepter that will fire before and after
 * an event is received.
 */
public abstract class AbstractEnvelopeInterceptor extends AbstractRequestResponseMessageProcessor
        implements Interceptor, FlowConstructAware
{

    protected FlowConstruct flowConstruct;

    /**
     * This method is invoked before the event is processed
     */
    public abstract MuleEvent before(MuleEvent event) throws MuleException;

    /**
     * This method is invoked after the event has been processed, unless an exception was thrown
     */
    public abstract MuleEvent after(MuleEvent event) throws MuleException;

    /**
     *  This method is always invoked after the event has been processed,
     */
    public abstract MuleEvent last(MuleEvent event, ProcessingTime time, long startTime, boolean exceptionWasThrown) throws MuleException;

    @Override
    protected MuleEvent processBlocking(MuleEvent event) throws MuleException
    {
        long startTime = System.currentTimeMillis();
        ProcessingTime time = event.getProcessingTime();
        boolean exceptionWasThrown = true;
        MuleEvent resultEvent = event;
        try
        {
            resultEvent = after(processNext(before(resultEvent)));
            exceptionWasThrown = false;
        }
        finally
        {
            resultEvent = last(resultEvent, time, startTime, exceptionWasThrown);
        }
        return resultEvent;
    }

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
        if (next instanceof FlowConstructAware)
        {
            ((FlowConstructAware) next).setFlowConstruct(flowConstruct);
        }
    }

}
