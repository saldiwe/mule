/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.extension.execute;


import org.mule.runtime.core.DefaultOperationMuleEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.extension.define.ModuleOperation;
import org.mule.runtime.core.extension.define.OperationChain;
import org.mule.runtime.core.extension.define.OperationParameter;

import java.util.Collections;
import java.util.List;

public class OperationExecutor implements MessageProcessor, Initialisable, MuleContextAware
{

    private ModuleOperation module;
    private OperationChain operationChain;
    private String operationName;
    private ConfigExecutor configExecutor;

    private List<ParameterRef> parameters = Collections.emptyList();
    private MuleContext muleContext;

    public OperationExecutor(String operationName)
    {
        this.operationName = operationName;
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MuleEvent defaultOperationMuleEvent = createOperationMuleEvent(event);
        MuleEvent processResult = getOperationChain().process(defaultOperationMuleEvent);
        MuleMessage messageResult = processResult.getMessage();
        return event;
    }

    private DefaultOperationMuleEvent createOperationMuleEvent(MuleEvent event)
    {
        DefaultOperationMuleEvent defaultOperationMuleEvent = new DefaultOperationMuleEvent(event);
        if (configExecutor != null)
        {
            loadParameters(defaultOperationMuleEvent, configExecutor.getParameters(), configExecutor.getParametersRef(), event);
        }
        loadParameters(defaultOperationMuleEvent, getOperationChain().getParameters(), this.parameters, event);

        return defaultOperationMuleEvent;
    }

    private void loadParameters(DefaultOperationMuleEvent defaultOperationMuleEvent, List<OperationParameter> parameters, List<ParameterRef> parametersRef, MuleEvent event)
    {
        //take the default values from the <parameter> if they exist
        //parameters.stream()
        //        .filter(parameter -> parameter.hasDefaultValue())
        //        .forEach(operationParameter -> {
        //            TypedValue defaultTypedValue = operationParameter.getDefaultValue(event);
        //            defaultOperationMuleEvent.setParamVariable(operationParameter.getParameterName(), defaultTypedValue.getValue(), defaultTypedValue.getDataType());
        //        });
        ////take the value from the <parameter-ref>, if already defined by the default, overrides them (yeah, not clear for now)
        //parametersRef.forEach(parameterRef -> {
        //    TypedValue typedValue = parameterRef.getValue(event);
        //    defaultOperationMuleEvent.setParamVariable(parameterRef.getParameterName(), typedValue.getValue(), typedValue.getDataType());
        //});
    }

    public void setParameters(List<ParameterRef> parameters)
    {
        this.parameters = parameters;
    }

    public void setModule(ModuleOperation moduleOperation)
    {
        this.module = moduleOperation;
    }

    public void setConfigExecutor(ConfigExecutor configExecutor)
    {
        this.configExecutor = configExecutor;
    }

    @Override
    //TODO hack, talk to PLG, internal beans are not initialized by its own
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
        getOperationChain().setMuleContext(context);
        parameters.forEach(parameterEntry -> parameterEntry.setMuleContext(context));
    }

    @Override
    //TODO hack, talk to PLG, internal beans are not initialized by its own
    public void initialise() throws InitialisationException
    {
        getOperationChain().initialise();
        for (ParameterRef parameter : parameters)
        {
            parameter.initialise();
        }
    }

    /**
     * Looks for the operation chain either inside the module if defined, or in the registry if not.
     * To make this prettier, we would need to map unbounded operations to a generic "mule" module, and everything will be better
     *
     * @return an operation, or throws exception if not found.
     */
    private OperationChain getOperationChain()
    {
        if (operationChain == null)
        {
            if (module != null)
            {
                this.operationChain = module.getOperations().stream()
                        .filter(op -> op.getName().equals(this.operationName))
                        .findFirst().orElse(null);
            }
            else
            {
                this.operationChain = muleContext.getRegistry().lookupObject(operationName);
            }
            if (operationChain == null)
            {
                throw new RuntimeException(String.format("The operation [%s] was not found", operationName)
                                           + (module != null ? String.format(" in the module [%s]", module.getName()) : ""));
            }
        }
        return operationChain;
    }
}
