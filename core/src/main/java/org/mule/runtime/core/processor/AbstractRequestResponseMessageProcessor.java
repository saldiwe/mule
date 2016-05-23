/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;
import static reactor.core.tuple.Tuple.of;
import static reactor.core.util.Exceptions.propagate;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.tuple.Tuple2;

/**
 * Base implementation of a {@link org.mule.runtime.core.api.processor.MessageProcessor} that may performs processing during both the
 * request and response processing phases while supporting non-blocking execution.
 * <p/>
 *
 * In order to define the process during the request phase you should override the
 * {@link #processRequest(org.mule.runtime.core.api.MuleEvent)} method. Symmetrically, if you need to define a process to be executed
 * during the response phase, then you should override the {@link #processResponse(org.mule.runtime.core.api.MuleEvent)} method.
 * <p/>
 *
 * In some cases you'll have some code that should be always executed, even if an error occurs, for those cases you
 * should override the {@link #processFinally(org.mule.runtime.core.api.MuleEvent, org.mule.runtime.core.api.MessagingException)} method.
 *
 * @since 3.7.0
 */
public abstract class AbstractRequestResponseMessageProcessor extends AbstractInterceptingMessageProcessor
{

    @Override
    public Publisher<MuleEvent> apply(Publisher<MuleEvent> publisher)
    {
        Flux<Tuple2<MuleEvent, Publisher<MuleEvent>>> flux = from(publisher).map(event -> {
            try
            {
                return of(event, processRequestAsStream(event));
            }
            catch (MuleException e)
            {
                if (e instanceof MessagingException)
                {
                    throw propagate(e);
                }
                else
                {
                    throw propagate(new MessagingException(event, e));
                }
            }
        });
        if (next != null)
        {
            flux = flux.map(tuple -> of(tuple.getT1(), from(from(tuple.getT2()).as(next))));
        }
        return flux.flatMap(tuple -> from(tuple.getT2()).flatMap(event -> {
            try
            {
                return processResponseAsStream(event, tuple.getT1());
            }
            catch (MuleException e)
            {
                if (e instanceof MessagingException)
                {
                    throw propagate(e);
                }
                else
                {
                    throw propagate(new MessagingException(tuple.getT1(), e));
                }
            }
        }));
    }

    @Override
    public final MuleEvent process(MuleEvent event) throws MuleException
    {
        return processBlocking(event);
    }

    protected MuleEvent processBlocking(MuleEvent event) throws MuleException
    {
        MessagingException exception = null;
        try
        {
            return processResponse(processNext(processRequest(event)), event);
        }
        catch (MessagingException e)
        {
            exception = e;
            return processCatch(event, e);
        }
        finally
        {
            processFinally(event, exception);
        }
    }

    /**
     * Processes the request phase before the next message processor is invoked.
     *
     * @param event event to be processed.
     * @return result of request processing.
     * @throws MuleException exception thrown by implementations of this method whiile performing response processing
     */
    protected MuleEvent processRequest(MuleEvent event) throws MuleException
    {
        return event;
    }

    protected Publisher<MuleEvent> processRequestAsStream(MuleEvent event) throws MuleException
    {
        return just(processRequest(event));
    }

    /**
     * Processes the response phase after the next message processor and it's response phase have been invoked
     *
     * @param response response event to be processed.
     * @param request the request event
     * @return result of response processing.
     * @throws MuleException exception thrown by implementations of this method whiile performing response processing
     */
    protected MuleEvent processResponse(MuleEvent response, final MuleEvent request) throws MuleException
    {
        return processResponse(response);
    }

    protected  Publisher<MuleEvent> processResponseAsStream(MuleEvent response, final MuleEvent request) throws MuleException
    {
        return just(processResponse(response, request));
    }

    /**
     * Processes the response phase after the next message processor and it's response phase have been invoked.  This
     * method is deprecated, use {@link #processResponse(MuleEvent, MuleEvent)} instead.
     *
     * @param response response event to be processed.
     * @return result of response processing.
     * @throws MuleException exception thrown by implementations of this method whiile performing response processing
     */
    @Deprecated
    protected MuleEvent processResponse(MuleEvent response) throws MuleException
    {
        return response;
    }

    /**
     * Used to perform post processing after both request and response phases have been completed.  This method will be
     * invoked both when processing is successful as well as if an exception is thrown.
     * successful result and in the case of an exception being thrown.
     *
     * @param event     the result of request and response processing. Note that this includes the request and response
     *                  processing of the rest of the Flow following this message processor too.
     * @param exception the exception thrown during processing if any. If not exception was thrown then this parameter
     *                  is null
     */
    protected void processFinally(MuleEvent event, MessagingException exception)
    {

    }

    protected MuleEvent processCatch(MuleEvent event, MessagingException exception) throws MessagingException
    {
        throw exception;
    }

}
