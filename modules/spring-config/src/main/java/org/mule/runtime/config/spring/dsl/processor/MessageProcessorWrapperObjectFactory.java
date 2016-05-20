/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.processor;

import org.mule.runtime.config.spring.dsl.api.ObjectFactory;
import org.mule.runtime.core.api.processor.MessageProcessor;

public class MessageProcessorWrapperObjectFactory implements ObjectFactory<MessageProcessor>
{
    private MessageProcessor messageProcessor;

    @Override
    public MessageProcessor getObject() throws Exception
    {
        return messageProcessor;
    }

    public void setMessageProcessor(MessageProcessor messageProcessor)
    {
        this.messageProcessor = messageProcessor;
    }
}
