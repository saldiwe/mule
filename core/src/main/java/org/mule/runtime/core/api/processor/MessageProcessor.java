/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import static reactor.core.util.Exceptions.propagate;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.util.rx.FluxNullSafeMap;

import java.util.function.Function;

import org.reactivestreams.Publisher;

/**
 * Processes {@link MuleEvent}'s.
 * <p>
 * This interface defines both a traditional blocking API as present since Mule 3.0
 * along with a new <a href="http://www.reactive-streams.org/">Reactive Streams<a/> based non-blocking API incorporated
 * in Mule 4.0. In Mule 4.0 the implementation of {@link #process(MuleEvent)} is required with the default
 * implementation of {@link #apply(Publisher)} defined in this interface to delegate to {@link #process(MuleEvent)}.
 * As such simple implementations that do no IO or routing, can continue to just implement {@link #process(MuleEvent)}
 * and require no changes and with only more complex processors such as routers and these doing IO or waiting on the
 * current thread needing to implement {@link #apply(Publisher)} to take advantage of non-blocking processing strategies.
 *
 * @since 3.0
 */
public interface MessageProcessor extends Function<Publisher<MuleEvent>, Publisher<MuleEvent>>
{

    /**
     * Invokes the MessageProcessor.
     *
     * @param event MuleEvent to be processed
     * @return optional response MuleEvent
     * @throws MuleException
     */
    MuleEvent process(MuleEvent event) throws MuleException;

    /**
     * Applies a {@link Publisher<MuleEvent>} function transforms a stream of {@link MuleEvent}'s.
     * <p>
     * The default implementation delegates to {@link #process(MuleEvent)} and will i) propagte any exception thrown ii)
     * drop events if invocation of {@link #process(MuleEvent)} returns null.
     *
     * @param publisher the event stream to transform
     * @return the transformed event stream
     */
    @Override
    default Publisher<MuleEvent> apply(Publisher<MuleEvent> publisher)
    {
        return new FluxNullSafeMap<>(publisher, event -> {
            try
            {
                return process(event);
            }
            catch (MessagingException exception)
            {
                throw propagate(new MessagingException(event, exception.getCause(), this));
            }
            catch (Throwable throwable)
            {
                throw propagate(new MessagingException(event, throwable, this));
            }
        });
    }

    /**
     * Defined if this processor may or will block either due to IO operations in the current thread or waiting using
     * {@link Thread#sleep(long)} or other mechanism for waiting on the current thread.
     * <p>
     * The default implementation is <code>false</code>, it is expected that blocking implementation override this
     * method and return <code>true</code>.  Blocking implementations may be scheduled to execute on a different thread
     * dependingon the {@link ProcessingStrategy}.
     *
     * @return true is this implementation may block
     */
    default boolean isBlocking()
    {
        return false;
    }

}
