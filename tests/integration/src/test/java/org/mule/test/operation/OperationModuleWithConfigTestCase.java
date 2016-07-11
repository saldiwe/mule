/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.operation;

import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleEvent;

import org.hamcrest.core.Is;
import org.junit.Test;

public class OperationModuleWithConfigTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "operation-with-config-module-flow.xml";
    }

    @Test
    public void testSetPayloadHardcodedFromModuleFlow() throws Exception{
        MuleEvent muleEvent = flowRunner("testSetPayloadHardcodedFromModuleFlow").run();
        assertThat(muleEvent.getMessage().getPayload(), Is.is("hardcoded value from module"));
    }

    @Test
    public void testSetPayloadParamFromModuleFlow() throws Exception{
        MuleEvent muleEvent = flowRunner("testSetPayloadParamFromModuleFlow").run();
        assertThat(muleEvent.getMessage().getPayload(), Is.is("new payload from module"));
    }

    @Test
    public void testSetPayloadConfigParamFlow() throws Exception{
        MuleEvent muleEvent = flowRunner("testSetPayloadConfigParamFlow").run();
        assertThat(muleEvent.getMessage().getPayload(), Is.is("some config-value-parameter"));
    }

    @Test
    public void testSetPayloadConfigDefaultParamFlow() throws Exception{
        MuleEvent muleEvent = flowRunner("testSetPayloadConfigDefaultParamFlow").run();
        assertThat(muleEvent.getMessage().getPayload(), Is.is("some default-config-value-parameter"));
    }

    @Test
    public void testSetPayloadAddParamAndPropertyValues() throws Exception{
        MuleEvent muleEvent = flowRunner("testSetPayloadAddParamAndPropertyValues").run();
        assertThat(muleEvent.getMessage().getPayload(), Is.is("a parameter value some config-value-parameter"));
    }
}
