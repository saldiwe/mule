/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.chain;

import static org.mule.runtime.core.execution.ExceptionToMessagingExceptionExecutionInterceptor.putContext;
import static org.mule.runtime.core.execution.MessageProcessorNotificationExecutionInterceptor.fireNotification;
import org.mule.runtime.core.OptimizedRequestContext;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.ProcessingStrategy;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transport.LegacyOutboundEndpoint;
import org.mule.runtime.core.context.notification.MessageProcessorNotification;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.runtime.core.execution.MessageProcessorExecutionTemplate;
import org.mule.runtime.core.routing.MessageFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public class DefaultMessageProcessorChain extends AbstractMessageProcessorChain
{
    protected MessageProcessorExecutionTemplate messageProcessorExecutionTemplate = MessageProcessorExecutionTemplate.createExecutionTemplate();
    protected ServerNotificationManager notificationManager;

    protected DefaultMessageProcessorChain(List<MessageProcessor> processors)
    {
        super(null, processors);
    }

    protected DefaultMessageProcessorChain(MessageProcessor... processors)
    {
        super(null, new ArrayList(Arrays.asList(processors)));
    }

    protected DefaultMessageProcessorChain(String name, List<MessageProcessor> processors)
    {
        super(name, processors);
    }

    protected DefaultMessageProcessorChain(String name, MessageProcessor... processors)
    {
        super(name, Arrays.asList(processors));
    }

    public static MessageProcessorChain from(MessageProcessor messageProcessor)
    {
        return new DefaultMessageProcessorChain(messageProcessor);
    }

    public static MessageProcessorChain from(MessageProcessor... messageProcessors) throws MuleException
    {
        return new DefaultMessageProcessorChainBuilder().chain(messageProcessors).build();
    }

    public static MessageProcessorChain from(List<MessageProcessor> messageProcessors) throws MuleException
    {
        return new DefaultMessageProcessorChainBuilder().chain(messageProcessors).build();
    }

    @Override
    protected MuleEvent doProcess(MuleEvent event) throws MuleException
    {
        return new ProcessorExecutorFactory().createProcessorExecutor(event, processors, messageProcessorExecutionTemplate, true).execute();
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        super.setMuleContext(context);
        notificationManager = context.getNotificationManager();
    }


    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return doProcess(event);
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        super.setFlowConstruct(flowConstruct);
    }


    @Override
    public Publisher<MuleEvent> apply(Publisher<MuleEvent> publisher)
    {
        Flux<MuleEvent> stream = Flux.from(publisher);
        for (MessageProcessor processor : processors)
        {
            if (flowConstruct instanceof Pipeline)
            {
                ProcessingStrategy processingStrategy = ((Pipeline) flowConstruct).getProcessingStrategy();
                stream = stream.compose(processingStrategy.onProcessor(processor, invokeProcessor(processor)));
            }
            else
            {
                stream = stream.compose(invokeProcessor(processor));
            }
        }
        return stream;
    }

    private Function<Publisher<MuleEvent>, Publisher<MuleEvent>> invokeProcessor(MessageProcessor processor)
    {
        if (!(processor instanceof Transformer || processor instanceof MessageFilter || processor instanceof Component
              || (processor instanceof LegacyOutboundEndpoint && !((LegacyOutboundEndpoint) processor)
                .mayReturnVoidEvent())))
        {
            return publisher -> Flux.from(publisher)
                    .doOnNext(preNotification(processor))
                    .concatMap(event ->
                               {
                                   OptimizedRequestContext.unsafeSetEvent(event);
                                   return Flux.just(event)
                                           .compose(processor)
                                           .mapError(wrapException(processor, event))
                                           .map(result -> VoidMuleEvent.getInstance().equals(result) ?
                                                          OptimizedRequestContext.unsafeSetEvent(event) : result)
                                           .doOnNext(postNotification(processor))
                                           .doOnError(MessagingException.class, errorNotification(processor));
                               });
        }
        else
        {
            return publisher -> Flux.from(publisher)
                    .doOnNext(preNotification(processor))
                    .doOnNext(event -> OptimizedRequestContext.unsafeSetEvent(event))
                    .compose(processor)
                    .mapError(wrapException(processor, null))
                    .doOnNext(postNotification(processor))
                    .doOnError(MessagingException.class, errorNotification(processor));
        }
    }

    private Consumer<MuleEvent> preNotification(MessageProcessor processor)
    {
        return event -> {
            if (event.isNotificationsEnabled())
            {
                fireNotification(event.getMuleContext().getNotificationManager(), event.getFlowConstruct(), event, processor, null,
                                 MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE);
            }
        };
    }
    private Consumer<MuleEvent> postNotification(MessageProcessor processor)
    {
        return event -> {
            if (event.isNotificationsEnabled()){
                fireNotification(event.getMuleContext().getNotificationManager(), event.getFlowConstruct(), event, processor, null,
                                 MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE);

            }};
    }

    private Consumer<MessagingException> errorNotification(MessageProcessor processor)
    {
        return exception -> {
            if (exception.getEvent().isNotificationsEnabled())
            {
                fireNotification(exception.getEvent().getMuleContext().getNotificationManager(), exception.getEvent().getFlowConstruct(), exception.getEvent(),
                                 processor, exception, MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE);
            }
        };
    }

    private Function<Throwable, MessagingException> wrapException(MessageProcessor processor, MuleEvent event)
    {
        return throwable -> {
            if (throwable instanceof MessagingException)
            {
                MessagingException msgException = (MessagingException) throwable;
                return putContext(msgException, processor, msgException.getEvent());
            }
            else
            {
                return putContext(new MessagingException(event, throwable, processor), processor, event);
            }
        };
    }
}
