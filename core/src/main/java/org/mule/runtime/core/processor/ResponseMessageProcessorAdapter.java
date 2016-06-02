/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static reactor.core.publisher.Flux.just;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChain;

import org.reactivestreams.Publisher;

public class ResponseMessageProcessorAdapter extends AbstractRequestResponseMessageProcessor implements Lifecycle,
        FlowConstructAware
{

    protected MessageProcessor responseProcessor;
    protected FlowConstruct flowConstruct;

    public ResponseMessageProcessorAdapter()
    {
        super();
    }

    public ResponseMessageProcessorAdapter(MessageProcessor responseProcessor)
    {
        super();
        this.responseProcessor = DefaultMessageProcessorChain.from(responseProcessor);
    }

    public void setProcessor(MessageProcessor processor)
    {
        this.responseProcessor = processor;
    }

    @Override
    protected Publisher<MuleEvent> processResponseAsStream(MuleEvent response, MuleEvent request) throws MuleException
    {
        if (responseProcessor == null || !isEventValid(response))
        {
            return just(response);
        }
        else
        {
            return just(response).as(responseProcessor);
        }
    }

    @Override
    protected MuleEvent processResponse(MuleEvent response) throws MuleException
    {
        if (responseProcessor == null || !isEventValid(response))
        {
            return response;
        }
        else
        {
            MuleEvent copy = new DefaultMuleEvent(response.getMessage(), response);
            MuleEvent result = responseProcessor.process(response);
            if (result == null || VoidMuleEvent.getInstance().equals(result))
            {
                // If <response> returns null then it acts as an implicit branch like in flows, the different
                // here is that what's next, it's not another message processor that follows this one in the
                // configuration file but rather the response phase of the inbound endpoint, or optionally
                // other response processing on the way back to the inbound endpoint.
                return copy;
            }
            else
            {
                return result;
            }
        }
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (responseProcessor instanceof Initialisable)
        {
            ((Initialisable) responseProcessor).initialise();
        }
    }

    @Override
    public void start() throws MuleException
    {
        if (responseProcessor instanceof Startable)
        {
            ((Startable) responseProcessor).start();
        }
    }

    @Override
    public void stop() throws MuleException
    {
        if (responseProcessor instanceof Stoppable)
        {
            ((Stoppable) responseProcessor).stop();
        }
    }

    @Override
    public void dispose()
    {
        if (responseProcessor instanceof Disposable)
        {
            ((Disposable) responseProcessor).dispose();
        }
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        super.setFlowConstruct(flowConstruct);
        if (responseProcessor instanceof FlowConstructAware)
        {
            ((FlowConstructAware) responseProcessor).setFlowConstruct(flowConstruct);
        }
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        super.setMuleContext(context);
        if (responseProcessor instanceof MuleContextAware)
        {
            ((MuleContextAware) responseProcessor).setMuleContext(muleContext);
        }
    }
}
