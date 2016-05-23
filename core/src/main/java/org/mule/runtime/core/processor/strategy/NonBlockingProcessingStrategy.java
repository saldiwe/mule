/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.StageNameSource;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Allows Mule to use non-blocking execution model where possible and free up threads when performing IO
 * operations.
 *
 * @since 3.7
 */
public class NonBlockingProcessingStrategy extends AbstractThreadingProfileProcessingStrategy
{

    private static final int DEFAULT_MAX_THREADS = 128;

    public NonBlockingProcessingStrategy()
    {
        super();
    }

    public NonBlockingProcessingStrategy(ExecutorService executorService)
    {
        super(executorService);
        maxThreads = DEFAULT_MAX_THREADS;
    }

    @Override
    public void configureProcessors(List<MessageProcessor> processors, StageNameSource
            nameSource, MessageProcessorChainBuilder chainBuilder, MuleContext muleContext)
    {
        for (MessageProcessor processor : processors)
        {
            chainBuilder.chain((MessageProcessor) processor);
        }
    }

}
