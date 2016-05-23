/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.construct;

import static reactor.core.publisher.Flux.error;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.lifecycle.LifecycleStateEnabled;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.MessageInfoMapping;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.management.stats.FlowConstructStatistics;

import java.util.function.Function;

import org.reactivestreams.Publisher;

/**
 * A uniquely identified {@link FlowConstruct} that once implemented and configured defines a construct
 * through which messages are processed using {@link MessageSource} and {@link MessageProcessor} building
 * blocks.
 */
public interface FlowConstruct extends NamedObject, LifecycleStateEnabled
{

    /**
     * @return The exception listener that will be used to handle exceptions that may be thrown at different
     *         points during the message flow defined by this construct.
     */
    MessagingExceptionHandler getExceptionListener();

    /**
     * @return The statistics holder used by this flow construct to keep track of its activity.
     */
    FlowConstructStatistics getStatistics();

    /**
     * @return This implementation of {@link MessageInfoMapping} used to control how Important message
     *         information is pulled from the current message.
     */
    MessageInfoMapping getMessageInfoMapping();

    /**
     * @return This muleContext that this flow construct belongs to and runs in the context of.
     */
    MuleContext getMuleContext();

    default Function<Throwable, Publisher<? extends MuleEvent>> getErrorHandler()
    {
        return exception -> {
            if (exception instanceof MessagingException)
            {
                MessagingException msgException = (MessagingException) exception;
                MuleEvent result;
                if (getExceptionListener() != null)
                {
                    result = getExceptionListener().handleException(msgException, msgException.getEvent());
                    msgException.setProcessedEvent(result);
                }
                if (msgException.handled())
                {
                    return just(msgException.getEvent());
                }
                else
                {
                    return error(msgException);
                }
            }
            else
            {
                return error(exception);
            }
        };
    }

}
