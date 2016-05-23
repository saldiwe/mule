/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_ERROR_RESPONSE;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_RECEIVED;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_RESPONSE;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.transaction.MuleTransactionConfig;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.util.Exceptions;

/**
 * This phase routes the message through the flow.
 * <p>
 * To participate of this phase, {@link org.mule.runtime.core.execution.MessageProcessTemplate} must implement {@link org.mule.runtime.core.execution.FlowProcessingPhaseTemplate}
 */
public class AsyncResponseFlowProcessingPhase extends NotificationFiringProcessingPhase<AsyncResponseFlowProcessingPhaseTemplate>
{

    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public boolean supportsTemplate(MessageProcessTemplate messageProcessTemplate)
    {
        return messageProcessTemplate instanceof AsyncResponseFlowProcessingPhaseTemplate;
    }

    @Override
    public void runPhase(final AsyncResponseFlowProcessingPhaseTemplate template, final MessageProcessContext messageProcessContext, final PhaseResultNotifier phaseResultNotifier)
    {
        Work flowExecutionWork = new Work()
        {
            @Override
            public void release()
            {

            }

            @Override
            public void run()
            {
                final MessageSource messageSource = messageProcessContext.getMessageSource();
                final MessagingExceptionHandler exceptionHandler = messageProcessContext.getFlowConstruct().getExceptionListener();

                MuleEvent muleEvent = null;
                try
                {
                    muleEvent = template.getMuleEvent();
                }
                catch (MuleException e)
                {
                    phaseResultNotifier.phaseFailure(e);
                }

                if (muleEvent != null)
                {
                    fireNotification(messageSource, muleEvent, MESSAGE_RECEIVED);

                    if (!muleEvent.isAllowNonBlocking())
                    {
                        try
                        {
                            try
                            {
                                TransactionalErrorHandlingExecutionTemplate transactionTemplate =
                                        TransactionalErrorHandlingExecutionTemplate.
                                                createMainExecutionTemplate(messageProcessContext.getFlowConstruct().getMuleContext(),
                                                                            (messageProcessContext.getTransactionConfig() == null ?
                                                                             new MuleTransactionConfig() : messageProcessContext
                                                                                     .getTransactionConfig()),
                                                                            exceptionHandler);
                                final MuleEvent response = transactionTemplate.execute(() -> {
                                    MuleEvent muleEvent1 = template.getMuleEvent();
                                    return template.routeEvent(muleEvent1);
                                });
                                fireNotification(messageSource, response, MESSAGE_RESPONSE);
                                template.sendResponseToClient(response, createResponseCompletationCallback
                                        (phaseResultNotifier, exceptionHandler));
                            }
                            catch (final MessagingException e)
                            {
                                fireNotification(messageSource, e.getEvent(), MESSAGE_ERROR_RESPONSE);
                                template.sendFailureResponseToClient(e, createSendFailureResponseCompletationCallback
                                        (phaseResultNotifier));
                            }
                        }
                        catch (Exception e)
                        {
                            phaseResultNotifier.phaseFailure(e);
                        }
                    }
                    else
                    {
                        from(template.routeEventAsStream(muleEvent))
                                .doOnNext(event -> fireNotification(messageSource, event, MESSAGE_RESPONSE))
                                .onErrorResumeWith(messageProcessContext.getFlowConstruct().getErrorHandler())
                                .doOnNext(sendResponseToClient(exceptionHandler, template, phaseResultNotifier))
                                .onErrorResumeWith(handleExceptions(messageSource, template, phaseResultNotifier))
                                .doOnError(e -> phaseResultNotifier.phaseFailure((Exception) e))
                                .subscribe();
                    }
                }
            }
        };
        if (messageProcessContext.supportsAsynchronousProcessing())
        {
            try
            {
                messageProcessContext.getFlowExecutionWorkManager().scheduleWork(flowExecutionWork);
            }
            catch (WorkException e)
            {
                phaseResultNotifier.phaseFailure(e);
            }
        }
        else
        {
            flowExecutionWork.run();
        }
    }

    private Function<Throwable, Publisher<? extends MuleEvent>> handleExceptions(MessageSource messageSource, AsyncResponseFlowProcessingPhaseTemplate template, PhaseResultNotifier phaseResultNotifier)
    {
        return e -> {
            if (e instanceof MessagingException)
            {
                MessagingException messagingException = (MessagingException) e;
                MuleEvent event = messagingException.getEvent();
                fireNotification(messageSource, event, MESSAGE_ERROR_RESPONSE);
                try
                {
                    template.sendFailureResponseToClient(messagingException,
                                                         createSendFailureResponseCompletationCallback
                                                                 (phaseResultNotifier));
                    return just(event);
                }
                catch (MuleException me)
                {
                    return Mono.error(me);
                }
            }
            else
            {
                return Mono.error(e);
            }
        };
    }

    private Consumer<MuleEvent> sendResponseToClient(MessagingExceptionHandler exceptionHandler, AsyncResponseFlowProcessingPhaseTemplate template, PhaseResultNotifier phaseResultNotifier)
    {
        return event -> {
            try
            {
                template.sendResponseToClient(event, createResponseCompletationCallback
                        (phaseResultNotifier, exceptionHandler));
            }
            catch (MuleException e)
            {
                throw Exceptions.propagate(e);
            }
        };
    }

    private ResponseCompletionCallback createSendFailureResponseCompletationCallback(final PhaseResultNotifier phaseResultNotifier)
    {
        return new ResponseCompletionCallback()
        {
            @Override
            public void responseSentSuccessfully()
            {
                phaseResultNotifier.phaseSuccessfully();
            }

            @Override
            public MuleEvent responseSentWithFailure(Exception e, MuleEvent event)
            {
                phaseResultNotifier.phaseFailure(e);
                return event;
            }
        };
    }

    private ResponseCompletionCallback createResponseCompletationCallback(final PhaseResultNotifier phaseResultNotifier, final MessagingExceptionHandler exceptionListener)
    {
        return new ResponseCompletionCallback()
        {
            @Override
            public void responseSentSuccessfully()
            {
                phaseResultNotifier.phaseSuccessfully();
            }

            @Override
            public MuleEvent responseSentWithFailure(final Exception e, final MuleEvent event)
            {
                return executeCallback(() ->
                {
                    final MuleEvent exceptionStrategyResult = exceptionListener.handleException(e, event);
                    phaseResultNotifier.phaseSuccessfully();
                    return exceptionStrategyResult;
                }, phaseResultNotifier);
            }
        };
    }

    private MuleEvent executeCallback(final Callback callback, PhaseResultNotifier phaseResultNotifier)
    {
        try
        {
            return callback.execute();
        }
        catch (Exception callbackException)
        {
            phaseResultNotifier.phaseFailure(callbackException);
            throw new MuleRuntimeException(callbackException);
        }
    }

    @Override
    public int compareTo(MessageProcessPhase messageProcessPhase)
    {
        if (messageProcessPhase instanceof ValidationPhase)
        {
            return 1;
        }
        return 0;
    }

    private interface Callback
    {

        MuleEvent execute() throws Exception;

    }

}
