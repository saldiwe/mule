/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.extension.define;

import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.processor.chain.AbstractMessageProcessorChain;

import java.util.Collections;
import java.util.List;

public class OperationChain extends AbstractMessageProcessorChain
{
    private List<OperationParameter> parameters = Collections.emptyList();
    //TODO payload
    //TODO output

    public OperationChain(String name, List<MessageProcessor> processors, List<OperationParameter> parameters)
    {
        super(name, processors);
        this.parameters = parameters;
    }

    @Override
    protected MuleEvent doProcess(MuleEvent event) throws MuleException
    {
        for (int i = 0; i < processors.size(); i++)
        {
            MessageProcessor processor = processors.get(i);
            event = processor.process(event);
            if (event == null)
            {
                return null;
            }
            else if (event instanceof VoidMuleEvent)
            {
                return event;
            }
        }
        return event;
    }

    public List<OperationParameter> getParameters()
    {
        return parameters;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        super.setMuleContext(context);
        for (OperationParameter parameter : parameters)
        {
            parameter.setMuleContext(context);
        }
    }

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        for (OperationParameter parameter : parameters)
        {
            parameter.initialise();
        }
    }
}
