/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.rx;

import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.FluxSource;
import reactor.core.subscriber.SubscriberBarrier;
import reactor.core.util.Exceptions;

public class FluxNullSafeMap<T, R> extends FluxSource<T, R>
{

    private Function<T, R> mapper;

    public FluxNullSafeMap(Publisher<T> publisher, Function<T, R> mapper)
    {
        super(publisher);
        this.mapper = mapper;
    }

    @Override
    public void subscribe(Subscriber<? super R> s)
    {
        source.subscribe(new SubscriberBarrier<T, R>(s)
        {
            @Override
            public void doNext(T event)
            {
                R result;

                try
                {
                    result = mapper.apply(event);
                }
                catch (Throwable e)
                {
                    Exceptions.throwIfFatal(e);
                    subscription.cancel();
                    onError(Exceptions.unwrap(e));
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
