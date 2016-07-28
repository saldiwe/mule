/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.rx;

import static reactor.core.Exceptions.throwIfFatal;
import static reactor.core.Exceptions.unwrap;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.processor.MessageProcessor;

import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.FluxSource;
import reactor.core.publisher.OperatorAdapter;

public class FluxNullSafeMap extends FluxSource<MuleEvent, MuleEvent>
{

    private Function<MuleEvent, MuleEvent> mapper;
    private MessageProcessor processor;

    public FluxNullSafeMap(Publisher<MuleEvent> publisher, MessageProcessor processor, ThrowingFunction<MuleEvent, MuleEvent>
            mapper)
    {
        super(publisher);
        this.mapper = mapper;
        this.processor = processor;
    }

    @Override
    public void subscribe(Subscriber<? super MuleEvent> s)
    {
        source.subscribe(new OperatorAdapter<MuleEvent, MuleEvent>(s)
        {
            @Override
            protected void doNext(MuleEvent event)
            {
                MuleEvent result;

                try
                {
                    result = mapper.apply(event);
                }
                catch (Throwable e)
                {
                    throwIfFatal(e);
                    subscription.cancel();
                    Throwable unwrapped = unwrap(e);
                    if (unwrapped instanceof MessagingException)
                    {
                        onError(unwrapped);
                    }
                    else
                    {
                        onError(new MessagingException(event, unwrapped, processor));
                    }
                    return;
                }

                if (result != null)
                {
                    s.onNext(result);
                }
                else
                {
                    subscription.request(1);
                }
            }
        });
    }
}
