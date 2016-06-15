/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import org.mule.runtime.core.DefaultOperationMuleEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ParamVariableMapContext extends AbstractMapContext<Object>
{

    private DefaultOperationMuleEvent event;

    public ParamVariableMapContext(DefaultOperationMuleEvent event){
            this.event = event;
    }

    @Override
    public Object doGet(String key)
    {
        return event.getParamVariable(key);
    }

    @Override
    public void doPut(String key, Object value)
    {
        event.setParamVariable(key, value);
    }

    @Override
    public void doRemove(String key)
    {
        event.removeParamVariable(key);
    }

    @Override
    public Set<String> keySet()
    {
        return event.getParamVariableNames();
    }

    @Override
    public void clear()
    {
        event.clearParamVariables();
    }

    @Override
    public String toString()
    {
        Map<String, Object> map = new HashMap<String, Object>();
        for (String key : event.getParamVariableNames())
        {
            Object value = event.getParamVariable(key);
            map.put(key, value);
        }
        return map.toString();
    }
}
