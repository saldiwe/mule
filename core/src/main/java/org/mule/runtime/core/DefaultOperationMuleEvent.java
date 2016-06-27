/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.runtime.core.util.CopyOnWriteCaseInsensitiveMap;

import java.util.Set;

//TODO WIP-OPERATIONS remove this class
public class DefaultOperationMuleEvent extends DefaultMuleEvent
{
    private CopyOnWriteCaseInsensitiveMap<String, TypedValue> paramVariables = new CopyOnWriteCaseInsensitiveMap<>();

    public DefaultOperationMuleEvent(MuleEvent rewriteEvent)
    {
        super(rewriteEvent.getMessage(),            //MuleMessage message,
              rewriteEvent,                         //MuleEvent rewriteEvent,
              rewriteEvent.getFlowConstruct(),      //FlowConstruct flowConstruct,
              rewriteEvent.getSession(),            //MuleSession session,
              true,                                 //boolean synchronous,
              rewriteEvent.getReplyToHandler(),     //ReplyToHandler replyToHandler,
              rewriteEvent.getReplyToDestination(), //Object replyToDestination,
              false,                                //boolean shareFlowVars,
              rewriteEvent.getExchangePattern()     //MessageExchangePattern messageExchangePattern
              );
    }

    public Set<String> getParamVariableNames()
    {
        return null;//paramVariables.keySet();
    }

    public void clearParamVariables()
    {
        paramVariables.clear();
    }

    @SuppressWarnings("unchecked")
    public <T> T getParamVariable(String key)
    {
        TypedValue typedValue = paramVariables.get(key);

        return typedValue == null ? null : (T) typedValue.getValue();
    }

    public void setParamVariable(String key, Object value)
    {
//<<<<<<< 0f306de0c72c03f673974969cfd2056c7c0fdf37
//        setParamVariable(key, value, null);
//=======
//        setParamVariable(key, value, DataType.fromObject(value));
//>>>>>>> updating packages and MuleEvent contract
    }

    public void setParamVariable(String key, Object value, DataType dataType)
    {
        paramVariables.put(key, new TypedValue(value, dataType));
    }

    public void removeParamVariable(String key)
    {
        paramVariables.remove(key);
    }

}
