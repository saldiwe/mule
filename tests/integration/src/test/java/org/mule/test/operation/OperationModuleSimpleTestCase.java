/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.operation;

import static org.hamcrest.MatcherAssert.assertThat;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.extension.api.annotation.param.Ignore;

import org.hamcrest.core.Is;
import org.junit.Test;

public class OperationModuleSimpleTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "operation-module-simple-flow.xml";
    }

    @Test
    public void testSetPayloadHardcodedFlow() throws Exception
    {
        MuleEvent muleEvent = flowRunner("testSetPayloadHardcodedFlow").run();
        assertThat(muleEvent.getMessage().getPayload(), Is.is("hardcoded value"));
    }

    @Test
    public void testSetPayloadParamFlow() throws Exception
    {
        MuleEvent muleEvent = flowRunner("testSetPayloadParamFlow").run();
        assertThat(muleEvent.getMessage().getPayload(), Is.is("new payload"));
    }

    @Test
    public void testSetPayloadParamDefaultFlow() throws Exception
    {
        MuleEvent muleEvent = flowRunner("testSetPayloadParamDefaultFlow").run();
        assertThat(muleEvent.getMessage().getPayload(), Is.is(15));
    }

    @Test
    @Ignore //until we have muleevent/flowVars isolation this test will fail
    //TODO talk to PLG this one should not be ignored.. and this is the reason why we need a custom MP that does the chain, so that we can manipulate the MuleEvent to provide scopinggg
    public void testSetPayloadNoSideEffectFlow() throws Exception
    {
        MuleEvent muleEvent = flowRunner("testSetPayloadNoSideEffectFlow").run();
        assertThat(muleEvent.getMessage().getPayload(), Is.is("10"));
        assertThat(muleEvent.getFlowVariable("testVar"), Is.is("unchanged value"));
    }

    @Test
    public void testDoNothingFlow() throws Exception
    {
        MuleEvent muleEvent = flowRunner("testDoNothingFlow").run();
        assertThat(muleEvent.getMessage().getPayload(), Is.is("before calling"));
        assertThat(muleEvent.getFlowVariable("variableBeforeCalling"), Is.is("value of flowvar before calling"));
    }

    @Test
    public void testSetPayloadParamValueAppender() throws Exception
    {
        MuleEvent muleEvent = flowRunner("testSetPayloadParamValueAppender").run();
        assertThat(muleEvent.getMessage().getPayload(), Is.is("new payload from module"));
    }

    @Test
    public void testSetPayloadAddParamsValues() throws Exception
    {
        MuleEvent muleEvent = flowRunner("testSetPayloadAddParamsValues").run();
        assertThat(muleEvent.getMessage().getPayload(), Is.is(15));

    }

    //@Test
    //@Ignore
    //<!-- TODO WIP-OPERATIONS this won't be acceptable in the XML as the XSD will check for it before running the app -->
    //public void testWithoutParametersFlow() throws Exception
    //{
    //    try{
    //        flowRunner("testWithoutParametersFlow").run();
    //        fail("should not have reach this point");
    //    }catch (MessagingException me){
    //        assertThat(me.getCause(),instanceOf(ExpressionRuntimeException.class));
    //        assertThat(me.getCause().getMessage(), Is.is("Execution of the expression \"value\" failed.") );
    //    }
    //}
    //
    //@Test
    //@Ignore
    //<!-- TODO WIP-OPERATIONS this won't be acceptable in the XML as the XSD will check for it before running the app -->
    //public void testWithMoreThanExpectedParametersFlow() throws Exception
    //{
    //    try{
    //        flowRunner("testWithMoreThanExpectedParametersFlow").run();
    //        fail("should not have reach this point");
    //    }catch (MessagingException me){
    //        assertThat(me.getCause(),instanceOf(ExpressionRuntimeException.class));
    //        assertThat(me.getCause().getMessage(), Is.is("Execution of the expression \"value\" failed.") );
    //    }
    //}
}
