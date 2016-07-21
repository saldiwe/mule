/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.exceptions;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.processor.MessageProcessor;

import java.io.IOException;

import org.junit.Test;

public class SimpleErrorHandlingTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/exceptions/simple-error-handling-config.xml";
    }

    @Test
    public void connectivityException() throws Exception
    {
        MuleMessage message = runFlow("ioException").getMessage();
        assertThat(message.getPayload(), is("CON"));
    }

    @Test
    public void randomException() throws Exception
    {
        MuleMessage message = runFlow("testException").getMessage();
        assertThat(message.getPayload(), is("ANY"));
    }

    @Test
    public void muleConnectivityException() throws Exception
    {
        MuleMessage message = runFlow("customException").getMessage();
        assertThat(message.getPayload(), is("CON"));
    }

    public static class ThrowExceptionMessageProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            throw new DefaultMuleException(new IOException());
        }
    }
}
