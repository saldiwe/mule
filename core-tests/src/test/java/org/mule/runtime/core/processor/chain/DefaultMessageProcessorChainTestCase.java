/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.processor.chain;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;
import static reactor.core.publisher.Mono.justOrEmpty;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorBuilder;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.construct.flow.DefaultFlowProcessingStrategy;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;
import org.mule.runtime.core.processor.AbstractRequestResponseMessageProcessor;
import org.mule.runtime.core.processor.ResponseMessageProcessorAdapter;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.runtime.core.routing.ChoiceRouter;
import org.mule.runtime.core.routing.ScatterGatherRouter;
import org.mule.runtime.core.routing.filters.AcceptAllFilter;
import org.mule.runtime.core.util.ObjectUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang.RandomStringUtils;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.util.Exceptions;

@RunWith(Parameterized.class)
@SmallTest
@SuppressWarnings("deprecation")
public class DefaultMessageProcessorChainTestCase extends AbstractMuleContextTestCase
{

    protected MuleContext muleContext;

    protected MessageExchangePattern exchangePattern;
    protected boolean nonBlocking;
    protected boolean synchronous;
    private volatile int scheduled = 0;
    private volatile int expectedTasks = 0;
    private Flow mockFlow;

    private ThreadPoolExecutor executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                                                 60L, TimeUnit.SECONDS,
                                                                 new SynchronousQueue<>())
    {
        @Override
        public Future<?> submit(Runnable task)
        {
            scheduled++;
            return super.submit(task);
        }
    };

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {MessageExchangePattern.REQUEST_RESPONSE, false, true},
                {MessageExchangePattern.REQUEST_RESPONSE, false, false},
                {MessageExchangePattern.REQUEST_RESPONSE, true, true},
                {MessageExchangePattern.REQUEST_RESPONSE, true, false},
                {MessageExchangePattern.ONE_WAY, false, true},
                {MessageExchangePattern.ONE_WAY, false, false},
                {MessageExchangePattern.ONE_WAY, true, true},
                {MessageExchangePattern.ONE_WAY, true, false}
        });
    }

    public DefaultMessageProcessorChainTestCase(MessageExchangePattern exchangePattern, boolean nonBlocking, boolean
            synchronous)
    {
        this.exchangePattern = exchangePattern;
        this.nonBlocking = nonBlocking;
        this.synchronous = synchronous;
    }

    @Before
    public void before() throws InitialisationException
    {
        muleContext = mock(MuleContext.class);
        MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);
        when(muleConfiguration.isContainerMode()).thenReturn(false);
        when(muleConfiguration.getId()).thenReturn(RandomStringUtils.randomNumeric(3));
        when(muleConfiguration.getShutdownTimeout()).thenReturn(1000);
        when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
        mockFlow = mock(Flow.class);
        if(nonBlocking)
        {
            NonBlockingProcessingStrategy processingStrategy = new NonBlockingProcessingStrategy(executor);
            when(mockFlow.getProcessingStrategy()).thenReturn(processingStrategy);
        }
    }

    @After
    public void after()
    {
        executor.shutdown();
    }

    @Test
    public void testMPChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(getAppendingMP("1"), getAppendingMP("2"), getAppendingMP("3"));
        assertEquals("0123", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload());
    }

    /*
     * Any MP returns null: - Processing doesn't proceed - Result of chain is Nnll
     */
    @Test
    public void testMPChainWithNullReturn() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();

        AppendingMP mp1 = getAppendingMP("1");
        AppendingMP mp2 = getAppendingMP("2");
        ReturnNullMP nullmp = new ReturnNullMP();
        AppendingMP mp3 = getAppendingMP("3");
        builder.chain(mp1, mp2, nullmp, mp3);

        MuleEvent requestEvent = getTestEventUsingFlow("0");
        assertNull(process(builder.build(), requestEvent));

        // mp1
        assertSame(requestEvent.getMessage(), mp1.event.getMessage());
        assertNotSame(mp1.event, mp1.resultEvent);
        assertEquals("01", mp1.resultEvent.getMessage().getPayload());

        // mp2
        assertSame(mp1.resultEvent.getMessage(), mp2.event.getMessage());
        assertNotSame(mp2.event, mp2.resultEvent);
        assertEquals("012", mp2.resultEvent.getMessage().getPayload());

        // nullmp
        assertSame(mp2.resultEvent.getMessage(), nullmp.event.getMessage());
        assertEquals("012", nullmp.event.getMessage().getPayload());

        // mp3
        assertNull(mp3.event);
    }

    /*
     * Any MP returns null: - Processing doesn't proceed - Result of chain is Nnll
     */
    @Test
    public void testMPChainWithVoidReturn() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();

        AppendingMP mp1 = getAppendingMP("1");
        AppendingMP mp2 = getAppendingMP("2");
        ReturnVoidMP voidmp = new ReturnVoidMP();
        AppendingMP mp3 = getAppendingMP("3");
        builder.chain(mp1, mp2, voidmp, mp3);

        MuleEvent requestEvent = getTestEventUsingFlow("0");
        assertEquals("0123", process(builder.build(), requestEvent).getMessage().getPayload());

        // mp1
        //assertSame(requestEvent, mp1.event);
        assertNotSame(mp1.event, mp1.resultEvent);

        // mp2
        //assertSame(mp1.resultEvent, mp2.event);
        assertNotSame(mp2.event, mp2.resultEvent);

        // void mp
        assertEquals(mp2.resultEvent, voidmp.event);

        // mp3
        assertNotSame(mp3.event, mp2.resultEvent);
        assertThat(mp3.event.getMessage().getPayload(), equalTo(mp2.resultEvent.getMessage().getPayload()));
        assertEquals(mp3.event.getMessage().getPayload(), "012");
    }

    @Test
    public void testMPChainWithNullReturnAtEnd() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(getAppendingMP("1"), getAppendingMP("2"), getAppendingMP("3"), new ReturnNullMP());
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));
    }

    @Test
    public void testMPChainWithVoidReturnAtEnd() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(getAppendingMP("1"), getAppendingMP("2"), getAppendingMP("3"), new ReturnVoidMP());
        assertEquals("0123", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload());
    }

    @Test
    public void testMPChainWithBuilder() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(getAppendingMP("1"));
        builder.chain((MessageProcessorBuilder) () -> getAppendingMP("2"));
        builder.chain(getAppendingMP("3"));
        assertEquals("0123", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload());
    }

    @Test
    public void testInterceptingMPChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), new AppendingInterceptingMP("2"),
                      new AppendingInterceptingMP("3"));
        assertEquals("0before1before2before3after3after2after1",
                     process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload());
    }

    @Test
    public void testInterceptingMPChainWithNullReturn() throws Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();

        AppendingInterceptingMP lastMP = new AppendingInterceptingMP("3");

        builder.chain(new AppendingInterceptingMP("1"), new AppendingInterceptingMP("2"),
                      new ReturnNullInterceptongMP(), lastMP);
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));
        assertFalse(lastMP.invoked);
    }

    @Test
    public void testInterceptingMPChainWithVoidReturn() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();

        AppendingInterceptingMP lastMP = new AppendingInterceptingMP("3");

        builder.chain(new AppendingInterceptingMP("1"), new AppendingInterceptingMP("2"),
                      new ReturnNullInterceptongMP(), lastMP);
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));
        assertFalse(lastMP.invoked);
    }

    @Test
    public void testMixedMPChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), getAppendingMP("3"),
                      new AppendingInterceptingMP("4"), getAppendingMP("5"));
        assertEquals("0before123before45after4after1", process(builder.build()
                , getTestEventUsingFlow("0"))
                .getMessage()
                .getPayload());
    }

    @Test
    // Whenever there is a IMP that returns null the final result is null
    public void testMixedMPChainWithNullReturn1() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), new ReturnNullInterceptongMP(), getAppendingMP("2"),
                      getAppendingMP("3"), new AppendingInterceptingMP("4"), getAppendingMP("5"));
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));
    }

    @Test
    // Whenever there is a IMP that returns null the final result is null
    public void testMixedMPChainWithVoidReturn1() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), new ReturnVoidMPInterceptongMP(),
                      getAppendingMP("2"), getAppendingMP("3"), new AppendingInterceptingMP("4"),
                      getAppendingMP("5"));
        assertThat(process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload(), equalTo("0before1after1"));
    }

    @Test
    // Whenever there is a IMP that returns null the final result is null
    public void testMixedMPChainWithNullReturn2() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), new ReturnNullInterceptongMP(),
                      getAppendingMP("3"), new AppendingInterceptingMP("4"), getAppendingMP("5"));
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));
    }

    @Test
    // Whenever there is a IMP that returns null the final result is null
    public void testMixedMPChainWithVoidlReturn2() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"),
                      new ReturnVoidMPInterceptongMP(), getAppendingMP("3"), new AppendingInterceptingMP("4"),
                      getAppendingMP("5"));
        assertEquals("0before12after1", process(builder.build()
                , getTestEventUsingFlow("0"))
                .getMessage()
                .getPayload());
    }

    @Test
    // A simple MP that returns null does not affect flow as long as it's not at the
    // end
    public void testMixedMPChainWithNullReturn3() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), new ReturnNullMP(), getAppendingMP("2"),
                      getAppendingMP("3"), new AppendingInterceptingMP("4"), getAppendingMP("5"));
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));
    }

    @Test
    // A simple MP that returns null does not affect flow as long as it's not at the
    // end
    public void testMixedMPChainWithVoidReturn3() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), new ReturnVoidMP(), getAppendingMP("2"),
                      getAppendingMP("3"), new AppendingInterceptingMP("4"), getAppendingMP("5"));
        assertEquals("0before123before45after4after1", process(builder.build()
                , getTestEventUsingFlow("0"))
                .getMessage()
                .getPayload());
    }

    @Test
    // A simple MP that returns null does not affect flow as long as it's not at the
    // end
    public void testMixedMPChainWithNullReturn4() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), new ReturnNullMP(),
                      getAppendingMP("3"), new AppendingInterceptingMP("4"), getAppendingMP("5"));
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));
    }

    @Test
    // A simple MP that returns null does not affect flow as long as it's not at the
    // end
    public void testMixedMPChainWithVoidReturn4() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), new ReturnVoidMP(),
                      getAppendingMP("3"), new AppendingInterceptingMP("4"), getAppendingMP("5"));
        assertEquals("0before123before45after4after1", process(builder.build(),
                                                               getTestEventUsingFlow("0"))
                .getMessage()
                .getPayload());
    }

    @Test
    // A simple MP that returns null does not affect flow as long as it's not at the
    // end
    public void testMixedMPChainWithNullReturn5() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), getAppendingMP("3"),
                      new ReturnNullMP(), new AppendingInterceptingMP("4"), getAppendingMP("5"));
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));
    }

    @Test
    // A simple MP that returns null does not affect flow as long as it's not at the
    // end
    public void testMixedMPChainWithVoidReturn5() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), getAppendingMP("3"),
                      new ReturnVoidMP(), new AppendingInterceptingMP("4"), getAppendingMP("5"));
        assertEquals("0before123before45after4after1", process(builder.build()
                , getTestEventUsingFlow("0"))
                .getMessage()
                .getPayload());
    }

    @Test
    // A simple MP at the end of a single level chain causes chain to return null
    public void testMixedMPChainWithNullReturnAtEnd() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), getAppendingMP("3"),
                      new AppendingInterceptingMP("4"), getAppendingMP("5"), new ReturnNullMP());
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));
    }

    @Test
    // A simple MP at the end of a single level chain causes chain to return null
    public void testMixedMPChainWithVoidReturnAtEnd() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), getAppendingMP("3"),
                      new AppendingInterceptingMP("4"), getAppendingMP("5"), new ReturnVoidMP());
        assertEquals("0before123before45after4after1", process(builder.build()
                , getTestEventUsingFlow("0"))
                .getMessage()
                .getPayload());
    }

    @Test
    public void testNestedMPChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(getAppendingMP("1"),
                      new DefaultMessageProcessorChainBuilder().chain(getAppendingMP("a"), getAppendingMP("b"))
                              .build(), getAppendingMP("2"));
        assertEquals("01ab2", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload());
    }

    @Test
    public void testNestedMPChainWithNullReturn() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(
                getAppendingMP("1"),
                new DefaultMessageProcessorChainBuilder().chain(getAppendingMP("a"), new ReturnNullMP(),
                                                                getAppendingMP("b")).build(), new ReturnNullMP(),
                getAppendingMP("2"));
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));
    }

    @Test
    public void testNestedMPChainWithVoidReturn() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(
                getAppendingMP("1"),
                new DefaultMessageProcessorChainBuilder().chain(getAppendingMP("a"), new ReturnVoidMP(),
                                                                getAppendingMP("b")).build(), new ReturnVoidMP(),
                getAppendingMP("2"));
        assertEquals("01ab2", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload());
    }

    @Test
    public void testNestedMPChainWithNullReturnAtEndOfNestedChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(
                getAppendingMP("1"),
                new DefaultMessageProcessorChainBuilder().chain(getAppendingMP("a"), getAppendingMP("b"),
                                                                new ReturnNullMP()).build(), getAppendingMP("2"));
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));
    }

    @Test
    public void testNestedMPChainWithVoidReturnAtEndOfNestedChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(
                getAppendingMP("1"),
                new DefaultMessageProcessorChainBuilder().chain(getAppendingMP("a"), getAppendingMP("b"),
                                                                new ReturnVoidMP()).build(), getAppendingMP("2"));
        assertEquals("01ab2", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload());
    }

    @Test
    public void testNestedMPChainWithNullReturnAtEndOfNestedChainWithNonInterceptingWrapper()
            throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        final MessageProcessor nested = new DefaultMessageProcessorChainBuilder().chain(getAppendingMP("a"),
                                                                                        getAppendingMP("b"), new
                        ReturnNullMP()).build();
        builder.chain(getAppendingMP("1"), DefaultMessageProcessorChain.from(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                return nested.process(event);
            }
        }, getAppendingMP("2")));
        assertNull("012", process(builder.build(), getTestEventUsingFlow("0")));
    }

    @Test
    public void testNestedMPChainWithVoidReturnAtEndOfNestedChainWithNonInterceptingWrapper()
            throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        final MessageProcessor nested = new DefaultMessageProcessorChainBuilder().chain(getAppendingMP("a"),
                                                                                        getAppendingMP("b"), new
                        ReturnVoidMP()).build();

        assertEquals("01ab2", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload());
    }

    @Test
    public void testNestedInterceptingMPChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(
                new AppendingInterceptingMP("1"),
                new DefaultMessageProcessorChainBuilder().chain(new AppendingInterceptingMP("a"),
                                                                new AppendingInterceptingMP("b")).build(), new
                        AppendingInterceptingMP("2"));
        assertEquals("0before1beforeabeforebafterbafterabefore2after2after1",
                     process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload());
    }

    @Test
    public void testNestedInterceptingMPChainWithNullReturn() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(
                new AppendingInterceptingMP("1"),
                new DefaultMessageProcessorChainBuilder().chain(new AppendingInterceptingMP("a"),
                                                                new ReturnNullInterceptongMP(), new
                                AppendingInterceptingMP("b")).build(),
                new AppendingInterceptingMP("2"));
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));
    }

    @Test
    public void testNestedInterceptingMPChainWithVoidReturn() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(
                new AppendingInterceptingMP("1"),
                new DefaultMessageProcessorChainBuilder().chain(new AppendingInterceptingMP("a"),
                                                                new ReturnVoidMPInterceptongMP(), new
                                AppendingInterceptingMP("b")).build(),
                new AppendingInterceptingMP("2"));
        assertThat(process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload(),
                   equalTo("0before1beforeaafterabefore2after2after1"));
    }

    @Test
    public void testNestedMixedMPChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(
                getAppendingMP("1"),
                new DefaultMessageProcessorChainBuilder().chain(new AppendingInterceptingMP("a"),
                                                                getAppendingMP("b")).build(), new
                        AppendingInterceptingMP("2"));
        assertEquals("01beforeabafterabefore2after2", process(builder.build(), getTestEventUsingFlow("0"))
                .getMessage()
                .getPayload());
    }

    @Test
    public void testInterceptingMPChainStopFlow() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), new AppendingInterceptingMP("2", true),
                      new AppendingInterceptingMP("3"));
        assertEquals("0before1after1", process(builder.build(),
                                               getTestEventUsingFlow("0"))
                .getMessage()
                .getPayload());
    }

    /**
     * Note: Stopping the flow of a nested chain causes the nested chain to return
     * early, but does not stop the flow of the parent chain.
     */
    @Test
    public void testNestedInterceptingMPChainStopFlow() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(
                new AppendingInterceptingMP("1"),
                new DefaultMessageProcessorChainBuilder().chain(new AppendingInterceptingMP("a", true),
                                                                new AppendingInterceptingMP("b")).build(), new
                        AppendingInterceptingMP("3"));
        assertEquals("0before1before3after3after1", process(builder.build(),
                                                            getTestEventUsingFlow("0"))
                .getMessage()
                .getPayload());
    }

    @Test
    public void testMPChainLifecycle() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        AppendingInterceptingMP mp1 = new AppendingInterceptingMP("1");
        AppendingInterceptingMP mp2 = new AppendingInterceptingMP("2");
        MessageProcessor chain = builder.chain(mp1, mp2).build();
        ((MuleContextAware) chain).setMuleContext(mock(MuleContext.class, Mockito.RETURNS_DEEP_STUBS));
        ((FlowConstructAware) chain).setFlowConstruct(mock(FlowConstruct.class));
        ((Lifecycle) chain).initialise();
        ((Lifecycle) chain).start();
        ((Lifecycle) chain).stop();
        ((Lifecycle) chain).dispose();
        assertLifecycle(mp1);
        assertLifecycle(mp2);
    }

    @Test
    public void testNestedMPChainLifecycle() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        DefaultMessageProcessorChainBuilder nestedBuilder = new DefaultMessageProcessorChainBuilder();
        AppendingInterceptingMP mp1 = new AppendingInterceptingMP("1");
        AppendingInterceptingMP mp2 = new AppendingInterceptingMP("2");
        AppendingInterceptingMP mpa = new AppendingInterceptingMP("a");
        AppendingInterceptingMP mpb = new AppendingInterceptingMP("b");
        MessageProcessor chain = builder.chain(mp1, nestedBuilder.chain(mpa, mpb).build(), mp2).build();
        ((MuleContextAware) chain).setMuleContext(mock(MuleContext.class, Mockito.RETURNS_DEEP_STUBS));
        ((FlowConstructAware) chain).setFlowConstruct(mock(FlowConstruct.class));
        ((Lifecycle) chain).initialise();
        ((Lifecycle) chain).start();
        ((Lifecycle) chain).stop();
        ((Lifecycle) chain).dispose();
        assertLifecycle(mp1);
        assertLifecycle(mp2);
        assertLifecycle(mpa);
        assertLifecycle(mpb);
    }

    @Test
    public void testNoneIntercepting() throws Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new TestNonIntercepting(), new TestNonIntercepting(), new TestNonIntercepting());
        MuleEvent restul = process(builder.build(), getTestEventUsingFlow(""));
        assertEquals("MessageProcessorMessageProcessorMessageProcessor", restul.getMessage().getPayload());
    }

    @Test
    public void testAllIntercepting() throws Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new TestIntercepting(), new TestIntercepting(), new TestIntercepting());
        MuleEvent restul = process(builder.build(), getTestEventUsingFlow(""));
        assertEquals("InterceptingMessageProcessorInterceptingMessageProcessorInterceptingMessageProcessor",
                     restul.getMessage().getPayload());
    }

    @Test
    public void testMix() throws Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new TestIntercepting(), new TestNonIntercepting(), new TestNonIntercepting(),
                      new TestIntercepting(), new TestNonIntercepting(), new TestNonIntercepting());
        MuleEvent restul = process(builder.build(), getTestEventUsingFlow(""));
        assertEquals(
                "InterceptingMessageProcessorMessageProcessorMessageProcessorInterceptingMessageProcessorMessageProcessorMessageProcessor",
                restul.getMessage().getPayload());
    }

    @Test
    public void testMixStaticFactoryt() throws Exception
    {
        MessageProcessorChain chain = DefaultMessageProcessorChain.from(new TestIntercepting(),
                                                                        new TestNonIntercepting(), new
                        TestNonIntercepting(), new TestIntercepting(),
                                                                        new TestNonIntercepting(), new
                                                                                TestNonIntercepting());
        MuleEvent restul = chain.process(getTestEventUsingFlow(""));
        assertEquals(
                "InterceptingMessageProcessorMessageProcessorMessageProcessorInterceptingMessageProcessorMessageProcessorMessageProcessor",
                restul.getMessage().getPayload());
    }

    @Test
    public void testMix2() throws Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new TestNonIntercepting(), new TestIntercepting(), new TestNonIntercepting(),
                      new TestNonIntercepting(), new TestNonIntercepting(), new TestIntercepting());
        MuleEvent restul = process(builder.build(), getTestEventUsingFlow(""));
        assertEquals(
                "MessageProcessorInterceptingMessageProcessorMessageProcessorMessageProcessorMessageProcessorInterceptingMessageProcessor",
                restul.getMessage().getPayload());
    }

    @Test
    public void testMix2StaticFactory() throws Exception
    {
        MessageProcessorChain chain = DefaultMessageProcessorChain.from(new TestNonIntercepting(),
                                                                        new TestIntercepting(), new
                        TestNonIntercepting(), new TestNonIntercepting(),
                                                                        new TestNonIntercepting(), new
                                                                                TestIntercepting());
        MuleEvent restul = chain.process(getTestEventUsingFlow(""));
        assertEquals(
                "MessageProcessorInterceptingMessageProcessorMessageProcessorMessageProcessorMessageProcessorInterceptingMessageProcessor",
                restul.getMessage().getPayload());
    }

    @Test
    public void testResponseProcessor() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(getAppendingMP("1"), new ResponseMessageProcessorAdapter(getAppendingMP("3")),
                      getAppendingMP("2"));
        assertThat(process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload(), equalTo
                ("0123"));
    }

    @Test
    public void testResponseProcessorInNestedChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(getAppendingMP("1"), DefaultMessageProcessorChain.from
                              (getAppendingMP("a"), new ResponseMessageProcessorAdapter(getAppendingMP("c")),
                               getAppendingMP("b")),
                      getAppendingMP("2"));
        assertThat(process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload(), equalTo
                ("01abc2"));
    }

    @Test
    public void testNestedResponseProcessor() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(getAppendingMP("1"), new ResponseMessageProcessorAdapter(DefaultMessageProcessorChain.from
                              (new ResponseMessageProcessorAdapter(getAppendingMP("4")), getAppendingMP("3"))),
                      getAppendingMP("2"));
        process(builder.build(), getTestEventUsingFlow("0"));
        assertThat(process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload(), equalTo
                ("01234"));
    }

    @Test
    public void testNestedResponseProcessorEndOfChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new ResponseMessageProcessorAdapter(DefaultMessageProcessorChain.from
                (getAppendingMP("1"))));
        process(builder.build(), getTestEventUsingFlow("0"));
        assertThat(process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload(), equalTo
                ("01"));
    }

    @Test
    @Ignore("RX")
    public void testAll() throws MuleException, Exception
    {
        ScatterGatherRouter scatterGatherRouter = new ScatterGatherRouter();
        scatterGatherRouter.addRoute(DefaultMessageProcessorChain.from(getAppendingMP("1")));
        scatterGatherRouter.addRoute(DefaultMessageProcessorChain.from(getAppendingMP("2")));
        scatterGatherRouter.addRoute(DefaultMessageProcessorChain.from(getAppendingMP("3")));
        ThreadingProfile tp = ThreadingProfile.DEFAULT_THREADING_PROFILE;
        tp.setMuleContext(muleContext);
        scatterGatherRouter.setThreadingProfile(tp);
        scatterGatherRouter.setMuleContext(muleContext);
        scatterGatherRouter.setFlowConstruct(mockFlow);
        scatterGatherRouter.initialise();
        scatterGatherRouter.start();

        MuleEvent event = getTestEventUsingFlow("0");
        MuleMessage result = process(DefaultMessageProcessorChain.from(scatterGatherRouter), new DefaultMuleEvent
                (event.getMessage(), event)).getMessage();
        assertThat(result.getPayload(), instanceOf(List.class));
        List<MuleMessage> resultMessage = (List<MuleMessage>) result.getPayload();
        assertThat(resultMessage.stream().map(MuleMessage::getPayload).collect(Collectors.toList()).toArray(), Is.is(equalTo(new String[] {"01", "02", "03"})));
    }

    @Test
    public void testChoice() throws MuleException, Exception
    {
        ChoiceRouter choiceRouter = new ChoiceRouter();
        choiceRouter.addRoute(DefaultMessageProcessorChain.from(getAppendingMP("1")), new AcceptAllFilter());
        choiceRouter.addRoute(DefaultMessageProcessorChain.from(getAppendingMP("2")), new AcceptAllFilter());
        choiceRouter.addRoute(DefaultMessageProcessorChain.from(getAppendingMP("3")), new AcceptAllFilter());

        assertThat(process(DefaultMessageProcessorChain.from(choiceRouter), getTestEventUsingFlow("0"))
                           .getMessage().getPayload(), equalTo("01"));
    }

    @Test(expected = MessagingException.class)
    public void testExceptionAfter() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(getAppendingMP("1"), new ExceptionThrowingMessageProcessor());
        process(builder.build(), getTestEventUsingFlow("0"));
    }

    @Test(expected = MessagingException.class)
    public void testExceptionBefore() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new ExceptionThrowingMessageProcessor(), getAppendingMP("1"));
        process(builder.build(), getTestEventUsingFlow("0"));
    }

    @Test(expected = MessagingException.class)
    public void testExceptionBetween() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(getAppendingMP("1"), new ExceptionThrowingMessageProcessor(),
                      getAppendingMP("2"));
        process(builder.build(), getTestEventUsingFlow("0"));
    }

    @Test(expected = MessagingException.class)
    public void testExceptionInResponse() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new ResponseMessageProcessorAdapter(new ExceptionThrowingMessageProcessor()), getAppendingMP("1"));
        process(builder.build(), getTestEventUsingFlow("0"));
    }

    private MuleEvent process(MessageProcessor messageProcessor, MuleEvent event) throws Exception
    {
        if (messageProcessor instanceof FlowConstructAware)
        {
            ((FlowConstructAware) messageProcessor).setFlowConstruct(mockFlow);
        }

        MuleEvent result;
        if (event.isAllowNonBlocking())
        {
            try
            {
                result = Mono.from(Mono.just(event).as(messageProcessor)).block();
            }
            catch (Exceptions.ReactiveException e)
            {
                throw (MuleException) Exceptions.unwrap(e);
            }
        }
        else
        {
            result = messageProcessor.process(event);
        }

        if(nonBlocking && !synchronous)
        {
            assertThat(scheduled, greaterThanOrEqualTo(expectedTasks));
        }
        return result;
    }

    private AppendingMP getAppendingMP(String append)
    {
        return new AppendingMP(append);
    }

    static class TestNonIntercepting implements MessageProcessor
    {

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return new DefaultMuleEvent(MuleMessage.builder()
                                                   .payload(event.getMessage().getPayload() + "MessageProcessor")
                                                   .build(),
                    event);
        }
    }

    static class TestIntercepting extends AbstractInterceptingMessageProcessor
    {
        public Publisher<MuleEvent> apply(Publisher<MuleEvent> publisher)
        {
            Flux<MuleEvent> result = from(publisher).flatMap(event -> just(event).map(event2 -> (MuleEvent) new DefaultMuleEvent(MuleMessage.builder()
                                                                  .payload(event.getMessage().getPayload() + "InterceptingMessageProcessor")
                                                                  .build(), event2)));
            if (next == null)
            {
                return result;
            }
            else
            {
                return result.compose(next);
            }
        }

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return processNext(new DefaultMuleEvent(MuleMessage.builder()
                                                               .payload(event.getMessage().getPayload() + "InterceptingMessageProcessor")
                                                               .build(),
                    event));
        }
    }

    private void assertLifecycle(AppendingMP mp)
    {
        assertTrue(mp.flowConstuctInjected);
        assertTrue(mp.muleContextInjected);
        assertTrue(mp.initialised);
        assertTrue(mp.started);
        assertTrue(mp.stopped);
        assertTrue(mp.disposed);
    }

    private void assertLifecycle(AppendingInterceptingMP mp)
    {
        assertTrue(mp.flowConstuctInjected);
        assertTrue(mp.muleContextInjected);
        assertTrue(mp.initialised);
        assertTrue(mp.started);
        assertTrue(mp.stopped);
        assertTrue(mp.disposed);
    }

    class AppendingMP implements MessageProcessor, Lifecycle, FlowConstructAware, MuleContextAware
    {

        String appendString;
        boolean muleContextInjected;
        boolean flowConstuctInjected;
        boolean initialised;
        boolean started;
        boolean stopped;
        boolean disposed;
        MuleEvent event;
        MuleEvent resultEvent;

        public AppendingMP(String append)
        {
            this.appendString = append;
        }

        @Override
        public MuleEvent process(final MuleEvent event) throws MuleException
        {

            if(flowConstuctInjected)
            {
                expectedTasks++;
            }
            return innerProcess(event);
        }

        private MuleEvent innerProcess(MuleEvent event)
        {
            this.event = event;
            return new DefaultMuleEvent(MuleMessage.builder().payload(event.getMessage().getPayload() + appendString).build(),
                                        event);
        }

        @Override
        public void initialise() throws InitialisationException
        {
            initialised = true;
        }

        @Override
        public void start() throws MuleException
        {
            started = true;
        }

        @Override
        public void stop() throws MuleException
        {
            stopped = true;
        }

        @Override
        public void dispose()
        {
            disposed = true;
        }

        @Override
        public String toString()
        {
            return ObjectUtils.toString(this);
        }

        @Override
        public void setMuleContext(MuleContext context)
        {
            this.muleContextInjected = true;
        }

        @Override
        public void setFlowConstruct(FlowConstruct flowConstruct)
        {
            this.flowConstuctInjected = true;
        }

        @Override
        public boolean isBlocking()
        {
            return nonBlocking;
        }

        @Override
        public Publisher<MuleEvent> apply(Publisher<MuleEvent> publisher)
        {
            return from(publisher).map(e -> {
                try
                {
                    return process(e);
                }
                catch (MuleException e1)
                {
                    throw Exceptions.propagate(e1);
                }
            });
        }
    }

    class AppendingInterceptingMP extends AbstractRequestResponseMessageProcessor implements FlowConstructAware, Lifecycle
    {

        String appendString;
        boolean muleContextInjected;
        boolean flowConstuctInjected;
        boolean initialised;
        boolean started;
        boolean stopped;
        boolean disposed;
        MuleEvent event;
        MuleEvent resultEvent;
        private boolean stopProcessing;
        boolean invoked = false;

        public AppendingInterceptingMP(String appendString)
        {
            this(appendString, false);
        }

        public AppendingInterceptingMP(String appendString, boolean stopProcessing)
        {
            this.appendString = appendString;
            this.stopProcessing = stopProcessing;
        }

        @Override
        protected MuleEvent processRequest(MuleEvent event) throws MuleException
        {
            return new DefaultMuleEvent(MuleMessage.builder()
                                                .payload(event.getMessage().getPayload() + "before" + appendString).build(), event);
        }

        @Override
        protected MuleEvent processResponse(MuleEvent response, MuleEvent request) throws MuleException
        {
            if (response == null || response instanceof VoidMuleEvent)
            {
                return response;
            }
            return new DefaultMuleEvent(MuleMessage.builder()
                                                .payload(event.getMessage().getPayload() + "after" + appendString).build(), response);
        }

        @Override
        public Publisher<MuleEvent> apply(Publisher<MuleEvent> publisher)
        {
            return super.apply(publisher);
        }

        @Override
        protected MuleEvent processBlocking(MuleEvent event) throws MuleException
        {
            if (stopProcessing)
            {
                return event;
            }
            else
            {
                return super.processBlocking(event);

            }
        }

        @Override
        public void initialise() throws InitialisationException
        {
            initialised = true;
        }

        @Override
        public void start() throws MuleException
        {
            started = true;
        }

        @Override
        public void stop() throws MuleException
        {
            stopped = true;
        }

        @Override
        public void dispose()
        {
            disposed = true;
        }

        @Override
        public String toString()
        {
            return ObjectUtils.toString(this);
        }

        @Override
        public void setMuleContext(MuleContext context)
        {
            this.muleContextInjected = true;
        }

        @Override
        public void setFlowConstruct(FlowConstruct flowConstruct)
        {
            super.setFlowConstruct(flowConstruct);
            this.flowConstuctInjected = true;
        }
    }

    static class ReturnNullMP implements MessageProcessor
    {

        MuleEvent event;

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            this.event = event;
            return null;
        }
    }

    static class ReturnNullInterceptongMP extends AbstractInterceptingMessageProcessor
    {

        public Publisher<MuleEvent> apply(Publisher<MuleEvent> publisher)
        {
            return from(publisher).flatMap(event -> justOrEmpty(null));
        }

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return null;
        }
    }

    private static class ReturnVoidMP implements MessageProcessor
    {

        MuleEvent event;

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            this.event = event;
            return VoidMuleEvent.getInstance();
        }
    }

    static class ReturnVoidMPInterceptongMP extends AbstractInterceptingMessageProcessor
    {

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return VoidMuleEvent.getInstance();
        }

        public Publisher<MuleEvent> apply(Publisher<MuleEvent> publisher)
        {
            return from(publisher).map(event -> VoidMuleEvent.getInstance());
        }
    }

    protected MuleEvent getTestEventUsingFlow(Object data) throws Exception
    {
        MuleEvent event = mock(MuleEvent.class);
        MuleMessage message = MuleMessage.builder().payload(data).build();
        when(event.getId()).thenReturn(RandomStringUtils.randomNumeric(3));
        when(event.getMessage()).thenReturn(message);
        when(event.getExchangePattern()).thenReturn(exchangePattern);
        when(event.getMuleContext()).thenReturn(muleContext);
        Pipeline mockFlow = mock(Flow.class);
        when(mockFlow.getProcessingStrategy()).thenReturn(nonBlocking ? new NonBlockingProcessingStrategy() : new
                DefaultFlowProcessingStrategy());
        when(mockFlow.getMuleContext()).thenReturn(muleContext);
        when(event.getFlowConstruct()).thenReturn(mockFlow);
        when(event.getSession()).thenReturn(mock(MuleSession.class));
        when(event.isSynchronous()).thenReturn(synchronous);
        when(event.isAllowNonBlocking()).thenReturn(!synchronous && nonBlocking);
        return event;
    }

    public static class ExceptionThrowingMessageProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            throw new IllegalStateException();
        }
    }

}
