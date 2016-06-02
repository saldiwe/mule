///*
// * Copyright (c) 2011-2014 Pivotal Software, Inc. All Rights Reserved.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package org.mule;
//
//import org.mule.api.MuleContext;
//import org.mule.api.MuleEvent;
//import org.mule.api.MuleException;
//import org.mule.api.config.MuleProperties;
//import org.mule.api.config.ThreadingProfile;
//import org.mule.api.context.MuleContextBuilder;
//import org.mule.api.context.MuleContextFactory;
//import org.mule.api.context.WorkManager;
//import org.mule.api.context.WorkManagerSource;
//import org.mule.api.processor.MessageProcessor;
//import org.mule.api.store.QueueStore;
//import org.mule.config.DefaultMuleConfiguration;
//import org.mule.config.MutableThreadingProfile;
//import org.mule.config.QueueProfile;
//import org.mule.config.builders.DefaultsConfigurationBuilder;
//import org.mule.construct.Flow;
//import org.mule.context.DefaultMuleContextBuilder;
//import org.mule.context.DefaultMuleContextFactory;
//import org.mule.processor.AsyncInterceptingMessageProcessor;
//import org.mule.processor.SedaStageInterceptingMessageProcessor;
//
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicLong;
//
//import org.openjdk.jmh.annotations.Benchmark;
//import org.openjdk.jmh.annotations.BenchmarkMode;
//import org.openjdk.jmh.annotations.Fork;
//import org.openjdk.jmh.annotations.Measurement;
//import org.openjdk.jmh.annotations.Mode;
//import org.openjdk.jmh.annotations.OutputTimeUnit;
//import org.openjdk.jmh.annotations.Param;
//import org.openjdk.jmh.annotations.Scope;
//import org.openjdk.jmh.annotations.Setup;
//import org.openjdk.jmh.annotations.State;
//import org.openjdk.jmh.annotations.TearDown;
//import org.openjdk.jmh.annotations.Warmup;
//import org.openjdk.jmh.infra.Blackhole;
//import org.reactivestreams.Subscriber;
//import org.reactivestreams.Subscription;
//import reactor.core.publisher.WorkQueueProcessor;
//import reactor.core.util.WaitStrategy;
//
//@Measurement(iterations = 1, time = 50000)
//@Warmup(iterations = 1, time = 10)
//@Fork(1)
//@BenchmarkMode(Mode.Throughput)
//@OutputTimeUnit(TimeUnit.SECONDS)
//@State(Scope.Benchmark)
//public class AsyncBarrierBenchmarks
//{
//
//    MuleContext muleContext;
//    AsyncInterceptingMessageProcessor async;
//    SedaStageInterceptingMessageProcessor queuedAsync;
//    WorkQueueProcessor<MuleEvent> workQueueDispatcher;
//    MessageProcessor lmax;
//
//    MessageProcessor listener;
//    WorkManager workManager;
//
//    MuleEvent event;
//    AtomicLong counter;
//
//    /**
//     * 1 = NONE
//     * 2 = CPU
//     * 3 = SLEEP
//     */
//    @Param({"2", "3"})
//    int workMode;
//
//    @Param({"1", "4", "8"})
//    int workTime;
//
//    @Setup
//    public void setup() throws Exception
//    {
//
//        MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
//        MuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
//        DefaultMuleConfiguration muleConfiguration = new DefaultMuleConfiguration();
//        contextBuilder.setMuleConfiguration(muleConfiguration);
//        muleContext = muleContextFactory.createMuleContext(contextBuilder);
//        new DefaultsConfigurationBuilder().configure(muleContext);
//        muleContext.start();
//
//        Flow flow = new Flow("flow", muleContext);
//        muleContext.getRegistry().registerFlowConstruct(flow);
//
//        event = new DefaultMuleEvent(new DefaultMuleMessage("Hello World", muleContext), MessageExchangePattern
//                .ONE_WAY, flow);
//        counter = new AtomicLong(0);
//
//
//        listener = new MessageProcessor()
//        {
//            @Override
//            public MuleEvent process(MuleEvent event) throws MuleException
//            {
//                if (workTime > 0)
//                {
//                    switch (workMode)
//                    {
//                        case 2:
//                            Blackhole.consumeCPU(workTime * 1000000);
//                            break;
//                        case 3:
//                            try
//                            {
//                                Thread.sleep(workTime);
//                            }
//                            catch (InterruptedException e)
//                            {
//                                e.printStackTrace();
//                            }
//                        default:
//                    }
//                }
//                return VoidMuleEvent.getInstance();
//            }
//        };
//
//        MutableThreadingProfile tp = new MutableThreadingProfile(muleContext.getDefaultThreadingProfile());
//        tp.setPoolExhaustedAction(ThreadingProfile.WHEN_EXHAUSTED_WAIT);
//        tp.setMaxBufferSize(64);
//        tp.setMuleContext(muleContext);
//
//        workManager = tp.createWorkManager("", 30);
//        workManager.start();
//        async = new AsyncInterceptingMessageProcessor(new WorkManagerSource()
//        {
//            @Override
//            public WorkManager getWorkManager() throws MuleException
//            {
//                return workManager;
//            }
//        });
//        async.setMuleContext(muleContext);
//        async.setListener(listener);
//        async.start();
//
//
//        QueueStore queueStore = muleContext.getRegistry().lookupObject(MuleProperties
//                                                                               .QUEUE_STORE_DEFAULT_IN_MEMORY_NAME);
//        QueueProfile queueProfile = new QueueProfile(64, queueStore);
//
//        queuedAsync = new SedaStageInterceptingMessageProcessor("", "", queueProfile, 200, tp, null, muleContext);
//        queuedAsync.setMuleContext(muleContext);
//        queuedAsync.setListener(listener);
//        queuedAsync.initialise();
//        queuedAsync.start();
//
//
//        workQueueDispatcher = WorkQueueProcessor.create(
//                "workQueueDispatcher",
//                64,
//                WaitStrategy.yielding()
//        );
//        Subscriber subscriber = new Subscriber<MuleEvent>()
//        {
//            @Override
//            public void onSubscribe(Subscription s)
//            {
//                s.request(Long.MAX_VALUE);
//            }
//
//            @Override
//            public void onNext(MuleEvent event)
//            {
//                try
//                {
//                    listener.process(event);
//                }
//                catch (MuleException e)
//                {
//                    e.printStackTrace();
//                }
//            }
//
//            @Override
//            public void onError(Throwable throwable)
//            {
//
//            }
//
//            @Override
//            public void onComplete()
//            {
//
//            }
//        };
//        for (int i = 0; i < 16; i++)
//
//        {
//            workQueueDispatcher.subscribe(subscriber);
//        }
//        lmax = new AsyncInterceptingMessageProcessor()
//        {
//            @Override
//            protected void processNextAsync(MuleEvent event)
//            {
//                workQueueDispatcher.onNext(event);
//            }
//        };
//    }
//
//    @TearDown
//    public void tearDown() throws InterruptedException, MuleException
//    {
//        workManager.dispose();
//        async.stop();
//        queuedAsync.stop();
//        queuedAsync.dispose();
//        muleContext.dispose();
//        workQueueDispatcher.onComplete();
//    }
//
//    @Benchmark
//    public void async() throws MuleException
//    {
//        doTest(async);
//    }
//
//    @Benchmark
//    public void seda() throws MuleException
//    {
//        doTest(queuedAsync);
//    }
//
//    @Benchmark
//    public void direct() throws MuleException
//    {
//        listener.process(event);
//    }
//
//    @Benchmark
//    public void lmax() throws MuleException
//    {
//
//        workQueueDispatcher.onNext(event);
//    }
//
//    private void doTest(MessageProcessor processor) throws MuleException
//    {
//        processor.process(event);
//    }
//
//}
