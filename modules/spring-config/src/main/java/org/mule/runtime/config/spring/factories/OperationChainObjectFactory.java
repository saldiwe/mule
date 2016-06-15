/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.factories;

import org.mule.runtime.config.spring.dsl.api.ObjectFactory;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.extension.define.OperationChain;
import org.mule.runtime.core.extension.define.OperationParameter;

import java.util.Collections;
import java.util.List;

public class OperationChainObjectFactory implements ObjectFactory<OperationChain>
{
    private String name;
    private List<MessageProcessor> messageProcessors;
    private List<OperationParameter> parameters = Collections.emptyList();

    @Override
    public OperationChain getObject() throws Exception
    {
        return new OperationChain(name, messageProcessors, parameters);
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setMessageProcessors(List<MessageProcessor> messageProcessors)
    {
        this.messageProcessors = messageProcessors;
    }

    public void setParameters(List<OperationParameter> parameters)
    {
        this.parameters = parameters;
    }
}
