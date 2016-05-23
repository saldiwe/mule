/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.enricher;

import static org.mule.runtime.core.OptimizedRequestContext.unsafeSetEvent;
import static reactor.core.publisher.Flux.just;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.expression.ExpressionManager;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.MessageProcessorContainer;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.api.processor.MessageProcessors;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.runtime.core.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.processor.chain.InterceptingChainLifecycleWrapper;
import org.mule.runtime.core.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.util.Exceptions;

/**
 * The <code>Message Enricher</code> allows the current message to be augmented using data from a seperate
 * resource.
 * <p/>
 * The Mule implementation the <i>Enrichment Resource</i> can be any Message Processor. This allows you to not
 * only use a JDBC endpoint directly but also call out to a remote service via HTTP or even reference another
 * flow or sub-flow.
 * <p/>
 * The Message Processor that implements the <i>Enrichment Resource</i> is invoked with a copy of the current
 * message along with any flow or session variables that are present. Invocation of the this message processor
 * is done in a separate context to the main flow such that any modification to the message (and it's
 * properties and attachments) or flow or session variables will not be reflected in the flow where the
 * enricher is configured.
 * <p/>
 * The <i>Enrichment Resource</i> should always return a result. If it doesn't then the Enricher will simply
 * leave the message untouched.
 * <p/>
 * The way in which the message is enriched (or modified) is by explicitly configuring mappings (source ->
 * target) between the result from the Enrichment Resource and the message using of Mule Expressions. Mule
 * Expressions are used to both select the value to be extracted from result that comes back from the
 * enrichment resource (source) and to define where this value to be inserted into the message (target). The
 * default 'source' if it's not configured is the payload of the result from the enrichment resource..
 * <p/>
 * <b>EIP Reference:</b> <a
 * href="http://eaipatterns.com/DataEnricher.html">http://eaipatterns.com/DataEnricher.html<a/>
 */
public class MessageEnricher extends AbstractMessageProcessorOwner implements MessageProcessor
{

    private List<EnrichExpressionPair> enrichExpressionPairs = new ArrayList<>();

    private MessageProcessor enrichmentProcessor;

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return enrich(enrichmentProcessor.process(copyEventForEnrichment(event)), event);
    }

    @Override
    public Publisher<MuleEvent> apply(Publisher<MuleEvent> publisher)
    {
        return Flux.from(publisher).flatMap(event -> just(event).map(request -> copyEventForEnrichment(event))
                .compose(enrichmentProcessor).map(response -> {
                    try
                    {
                        return enrich(response, event);
                    }
                    catch (MuleException e)
                    {
                        throw Exceptions.propagate(new MessagingException(event, e));
                    }
                }));
    }

    protected void enrich(MuleEvent currentEvent,
                          MuleEvent enrichmentEvent,
                          String sourceExpressionArg,
                          String targetExpressionArg,
                          ExpressionManager expressionManager)
    {
        if (StringUtils.isEmpty(sourceExpressionArg))
        {
            sourceExpressionArg = "#[payload:]";
        }

        TypedValue typedValue = expressionManager.evaluateTyped(sourceExpressionArg, enrichmentEvent);

        if (typedValue.getValue() instanceof MuleMessage)
        {
            MuleMessage muleMessage = (MuleMessage) typedValue.getValue();
            typedValue = new TypedValue(muleMessage.getPayload(), muleMessage.getDataType());
        }

        if (!StringUtils.isEmpty(targetExpressionArg))
        {
            expressionManager.enrichTyped(targetExpressionArg, currentEvent, typedValue);
        }
        else
        {
            currentEvent.setMessage(MuleMessage.builder(currentEvent.getMessage())
                                               .payload(typedValue.getValue())
                                               .mediaType(typedValue.getDataType().getMediaType())
                                               .build());
        }
    }

    public void setEnrichmentMessageProcessor(MessageProcessor enrichmentProcessor)
    {
        if (!(enrichmentProcessor instanceof MessageProcessorChain))
        {
            this.enrichmentProcessor = MessageProcessors.singletonChain(enrichmentProcessor);
        }
        else
        {
            this.enrichmentProcessor = enrichmentProcessor;
        }
    }

    /**
     * For spring
     */
    public void setMessageProcessor(MessageProcessor enrichmentProcessor)
    {
        setEnrichmentMessageProcessor(enrichmentProcessor);
    }

    public void setEnrichExpressionPairs(List<EnrichExpressionPair> enrichExpressionPairs)
    {
        this.enrichExpressionPairs = enrichExpressionPairs;
    }

    public void addEnrichExpressionPair(EnrichExpressionPair pair)
    {
        this.enrichExpressionPairs.add(pair);
    }

    private MuleEvent copyEventForEnrichment(MuleEvent event)
    {
        return unsafeSetEvent(DefaultMuleEvent.copy(event));
    }

    public static class EnrichExpressionPair
    {

        private String source;
        private String target;

        public EnrichExpressionPair()
        {
            // for spring
        }

        public EnrichExpressionPair(String target)
        {
            this.target = target;
        }

        public EnrichExpressionPair(String source, String target)
        {
            this.source = source;
            this.target = target;
        }

        public String getSource()
        {
            return source;
        }

        public void setSource(String source)
        {
            this.source = source;
        }

        public String getTarget()
        {
            return target;
        }

        public void setTarget(String target)
        {
            this.target = target;
        }
    }

    @Override
    protected List<MessageProcessor> getOwnedMessageProcessors()
    {
        return Collections.singletonList(enrichmentProcessor);
    }

    @Override
    public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement)
    {
        if (enrichmentProcessor instanceof InterceptingChainLifecycleWrapper)
        {
            super.addMessageProcessorPathElements(pathElement);
        }
        else
        {
            ((MessageProcessorContainer) enrichmentProcessor).addMessageProcessorPathElements(pathElement);
        }
    }

    protected MuleEvent enrich(final MuleEvent event, MuleEvent eventToEnrich) throws MuleException
    {
        final ExpressionManager expressionManager = eventToEnrich.getMuleContext().getExpressionManager();

        if (event != null && !VoidMuleEvent.getInstance().equals(eventToEnrich))
        {
            for (EnrichExpressionPair pair : enrichExpressionPairs)
            {
                enrich(eventToEnrich, event, pair.getSource(), pair.getTarget(), expressionManager);
            }
        }
        return unsafeSetEvent(eventToEnrich);
    }

}
