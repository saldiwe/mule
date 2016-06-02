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

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChain;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.tck.SensingNullReplyToHandler;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.IOException;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.Publisher;
import reactor.core.publisher.BlockingSink;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.WorkQueueProcessor;

public class RxFlowBenchmarksJunit extends AbstractMuleContextTestCase
{

    Flow flow;
    BlockingSink<MuleEvent> emitter;
    WorkQueueProcessor workQueueProcessor;
    MessageProcessor listener;
    MuleEvent event;
    MuleEvent nonBlockingEvent;
    SensingNullReplyToHandler sensingReplyToHandler;

//    String processingStrategy = "default";

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

    @Param({"0", "1", "2", "4"})//, "4", "8"})
    int workTime = 0;

    @Before
    public void setup() throws Exception
    {

        flow = new Flow("flow", muleContext);
        flow.setProcessingStrategy(new NonBlockingProcessingStrategy());
        listener = DefaultMessageProcessorChain.from(new MyMessageProcessor());
        flow.setMessageProcessors(Collections.singletonList(listener));
        muleContext.getRegistry().registerFlowConstruct(flow);

        event = new DefaultMuleEvent(MuleMessage.builder().payload("Hello World").build(), MessageExchangePattern
                .REQUEST_RESPONSE, flow);
        nonBlockingEvent = new DefaultMuleEvent(MuleMessage.builder().payload("Hello World").build(), MessageExchangePattern
                .REQUEST_RESPONSE, flow);

        DirectProcessor<MuleEvent> processor = DirectProcessor.<MuleEvent>create();
        //emitter = processor.connectEmitter();
        Publisher<MuleEvent> p = Flux.from(processor.as(flow));
        Flux.from(p).subscribe();
        muleContext.start();
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

    @Test
    public void defaultPS() throws MuleException, InterruptedException
    {
        System.out.println(Mono.from(Mono.just(event).as(flow)).block());
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
