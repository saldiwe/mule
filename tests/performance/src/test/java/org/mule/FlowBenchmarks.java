/*
 * Copyright (c) 2011-2014 Pivotal Software, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mule;

import org.mule.runtime.config.spring.util.ProcessingStrategyUtils;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.context.MuleContextFactory;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.ProcessingStrategy;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.config.builders.DefaultsConfigurationBuilder;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.context.DefaultMuleContextBuilder;
import org.mule.runtime.core.context.DefaultMuleContextFactory;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChain;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.processor.strategy.AsynchronousProcessingStrategy;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BlockingSink;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.TopicProcessor;
import reactor.core.publisher.WorkQueueProcessor;

@Measurement(iterations = 2, time = 10)
@Warmup(iterations = 3, time = 500)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class FlowBenchmarks
{

    MuleContext muleContext;
    Flow flow;
    Subscriber<? super MuleEvent> subscriber;
    FluxProcessor<MuleEvent,MuleEvent> emitterProcessor;
    BlockingSink submissionEmitter;

    WorkQueueProcessor workQueueProcessor;


    MessageProcessor listener;
    WorkManager workManager;

    MuleEvent event;
    AtomicLong counter;

    // "asynchronous-with-buffer", "synchronous", "queued-asynchronous"
    @Param({"default","asynchronous", "lmax"})
    String processingStrategy;

    //@Param({"0", "16"})  //"64", "256", "512", "1024"})
    int bufferSize;

    //@Param({"16", "64", "256", "1024"})
    int queueSize = 64;

    //@Param({"0", "4"})
    int waitStrategy;

    /**
     * 1 = NONE
     * 2 = CPU
     * 3 = SLEEP
     */
    @Param({"2", "3"})
    int workMode;

    @Param({"0", "1", "2", "4"})//, "4", "8"})
    int workTime;

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
        listener = DefaultMessageProcessorChain.from(new MyMessageProcessor());
        if (processingStrategy.equals("asynchronous-with-buffer"))
        {
            processingStrategy = "asynchronous";
            bufferSize = 16;
        }

        ProcessingStrategy ps = ProcessingStrategyUtils.parseProcessingStrategy(processingStrategy);
        if (ps instanceof AsynchronousProcessingStrategy)
        {
            ((AsynchronousProcessingStrategy) ps).setPoolExhaustedAction(waitStrategy);
            ((AsynchronousProcessingStrategy) ps).setMaxBufferSize(bufferSize);
        }
        //if (ps instanceof QueuedAsynchronousProcessingStrategy)
        //{
        //    ((QueuedAsynchronousProcessingStrategy) ps).setMaxQueueSize(queueSize);
        //}
        flow.setProcessingStrategy(ps);
        if(processingStrategy.equals("lmax"))
        {
            flow.setProcessingStrategy(new ProcessingStrategy()
            {
                @Override
                public void configureProcessors(List<MessageProcessor> processors, org.mule.runtime.core.api.processor
                        .StageNameSource nameSource, MessageProcessorChainBuilder chainBuilder, MuleContext muleContext)

                {
                    workQueueProcessor = WorkQueueProcessor.create();
                    MessageProcessorChain chain = null;
                    try
                    {
                        chain = new DefaultMessageProcessorChainBuilder().chain(processors).build();
                    }
                    catch (MuleException e)
                    {
                        e.printStackTrace();
                    }
                    for (int i = 0; i < 16; i++)
                    {
                        final MessageProcessorChain finalChain = chain;
                        workQueueProcessor.subscribe(new Subscriber<MuleEvent>()
                        {
                            @Override
                            public void onSubscribe(Subscription s)
                            {
                                s.request(Long.MAX_VALUE);
                            }

                            @Override
                            public void onNext(MuleEvent event)
                            {
                                try
                                {
                                    finalChain.process(event);
                                }
                                catch (MuleException e)
                                {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onError(Throwable throwable)
                            {

                            }

                            @Override
                            public void onComplete()
                            {

                            }
                        });
                    }
                    chainBuilder.chain(new MessageProcessor()
                    {
                        @Override
                        public MuleEvent process(MuleEvent event) throws MuleException
                        {
                            workQueueProcessor.onNext(event);
                            return VoidMuleEvent.getInstance();
                        }
                    });
                }
            });
        }
        flow.setMessageProcessors(Collections.singletonList(listener));
        muleContext.getRegistry().registerFlowConstruct(flow);

        event = new DefaultMuleEvent(MuleMessage.builder().payload("Hello World").build(), MessageExchangePattern
                .ONE_WAY, flow);

        emitterProcessor = TopicProcessor.<MuleEvent>create().connect();
        Publisher<MuleEvent> p = Flux.from(emitterProcessor.as(flow));
        Flux.from(p).subscribe();
        Flux.from(p).subscribe();
    }

    @TearDown
    public void tearDown() throws InterruptedException, MuleException
    {
        emitterProcessor.onComplete();
        muleContext.dispose();
        if(workQueueProcessor !=null){
            workQueueProcessor.shutdown();
        }
    }

    @Benchmark
    public void defaultPS() throws MuleException
    {
        if (processingStrategy.equals("default"))
        {
            emitterProcessor.onNext(event);
        }
        else
        {
            flow.process(event);
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
            return event;
        }

        @Override
        public boolean isBlocking()
        {
            return workTime > 0;
        }
    }

    //public static void main(String args[]) throws RunnerException
    //{
    //
    //    Options opt = new OptionsBuilder()
    //            .include(".*" + FlowBenchmarks.class.getSimpleName() + ".*")
    //            .forks(1)
    //            .warmupForks(1)
    //            .warmupIterations(3)
    //            .warmupTime(TimeValue.seconds(5))
    //            .measurementIterations(2)
    //            .measurementTime(TimeValue.seconds(10))
    //            //.addProfiler( GCProfiler.class )
    //            .build();
    //    new Runner(opt).run();
    //}
}
