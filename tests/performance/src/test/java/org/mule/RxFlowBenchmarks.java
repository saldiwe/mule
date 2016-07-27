/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.context.MuleContextFactory;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.config.builders.DefaultsConfigurationBuilder;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.context.DefaultMuleContextBuilder;
import org.mule.runtime.core.context.DefaultMuleContextFactory;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChain;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.runtime.core.util.concurrent.Latch;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.reactivestreams.Publisher;
import reactor.core.publisher.BlockingSink;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.WorkQueueProcessor;

@Measurement(iterations = 2, time = 10)
@Warmup(iterations = 5, time = 5)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class RxFlowBenchmarks
{

    MuleContext muleContext;
    Flow flow;
    BlockingSink<MuleEvent> emitter;
    WorkQueueProcessor workQueueProcessor;
    MessageProcessor listener;
    MuleEvent event;

    @Param({"non-blocking"})//, "non-blocking-mono", "default"})
    String processingStrategy;

    //@Param({"0", "16"})  //"64", "256", "512", "1024"})
    int bufferSize;

    //@Param({"16", "64", "256", "1024"})
    int queueSize = 64;

    //@Param({"0", "4"})
    int waitStrategy = 0;

    /**
     * 1 = NONE
     * 2 = CPU
     * 3 = SLEEP
     */
    //@Param({"2", "3"})
    int workMode = 3;

    //@Param({"0", "1"})
    int workTime;

    Latch latch = new Latch();

    @Setup
    public void setup() throws Exception
    {
        MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        MuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
        DefaultMuleConfiguration muleConfiguration = new DefaultMuleConfiguration();
        contextBuilder.setMuleConfiguration(muleConfiguration);
        muleContext = muleContextFactory.createMuleContext(contextBuilder);
        new DefaultsConfigurationBuilder().configure(muleContext);
        muleContext.start();

        flow = new Flow("flow", muleContext);
        flow.setProcessingStrategy(new NonBlockingProcessingStrategy());
        listener = DefaultMessageProcessorChain.from(new MyMessageProcessor());
        flow.setMessageProcessors(Collections.singletonList(listener));
        muleContext.getRegistry().registerFlowConstruct(flow);

        event = new DefaultMuleEvent(MuleMessage.builder().payload("Hello World").build(), MessageExchangePattern
                .REQUEST_RESPONSE, flow);

        DirectProcessor<MuleEvent> processor = DirectProcessor.<MuleEvent>create();
        //emitter = processor.connectEmitter();
        emitter = BlockingSink.create(processor);
        Publisher<MuleEvent> p = Flux.from(processor.as(flow));
        Flux.from(p).subscribe();
    }

    @TearDown
    public void tearDown() throws InterruptedException, MuleException, IOException
    {
        emitter.close();
        muleContext.dispose();
        if (workQueueProcessor != null)
        {
            workQueueProcessor.shutdown();
        }
    }

    @Benchmark
    public Object rxtream() throws MuleException, InterruptedException
    {
        if (processingStrategy.equals("non-blocking"))
        {
            latch = new Latch();
            long l = emitter.submit(event);
            latch.await();
            return l;
        }
        else if (processingStrategy.equals("non-blocking-mono"))
        {
            return Mono.from(Mono.just(event).as(flow)).block();
        }
        //else if (processingStrategy.equals("non-blocking"))
        //{
        //    sensingReplyToHandler.clear();
        //    MuleEvent r = flow.process(nonBlockingEvent);
        //    //if (r instanceof NonBlockingVoidMuleEvent)
        //    //{
        //    //    sensingReplyToHandler.latch.await();
        //    //    return sensingReplyToHandler.event;
        //    //}
        //    //else
        //    //{
        //        return r;
        //    //}
        //}
        else
        {
            return flow.process(event);
        }
    }

    private class MyMessageProcessor implements MessageProcessor
    {

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            if (workTime > 0)
            {
                switch (workMode)
                {
                    case 2:
                        Blackhole.consumeCPU(workTime * 1000000);
                        break;
                    case 3:
                        try
                        {
                            Thread.sleep(workTime);
                        }
                        catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                    default:
                }
            }
            latch.countDown();
            return event;
        }

        @Override
        public boolean isBlocking()
        {
            return workTime > 0;
        }
    }

    public static void main(String args[]) throws RunnerException
    {

        Options opt = new OptionsBuilder()
                .include(".*" + RxFlowBenchmarks.class.getSimpleName() + ".*")
                .forks(1)
                .warmupForks(1)
                .warmupIterations(5)
                .warmupTime(TimeValue.seconds(5))
                .measurementIterations(1)
                .measurementTime(TimeValue.seconds(10))
                //.addProfiler( StackProfiler.class )
                .build();
        new org.openjdk.jmh.runner.Runner(opt).run();
    }
}
