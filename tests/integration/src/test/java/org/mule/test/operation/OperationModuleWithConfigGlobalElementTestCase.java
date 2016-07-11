/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.operation;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.extension.http.api.request.validator.ResponseValidatorException;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;

import org.hamcrest.core.Is;
import org.junit.Test;

public class OperationModuleWithConfigGlobalElementTestCase extends ExtensionFunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "operation-with-config-global-elements-module-flow.xml";
    }

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {org.mule.extension.http.internal.HttpConnector.class};
    }

    @Test
    public void testHttpDoLogin() throws Exception{
        MuleEvent muleEvent = flowRunner("testHttpDoLogin").run();
        assertThat(muleEvent.getMessage().getPayload(), Is.is("success with basic-authentication for user: userLP"));
    }

    @Test
    public void testHttpDontLogin() throws Exception{
        try{
            flowRunner("testHttpDontLogin").run();
            fail("Should not haver reach here");
        }catch (MessagingException me){
            assertThat(me.getCause(),instanceOf(ResponseValidatorException.class));
            assertThat(me.getCause().getMessage(), Is.is("Response code 401 mapped as failure.") );
        }
    }

    @Test
    public void testHttpDoLoginGonnet() throws Exception{
        MuleEvent muleEvent = flowRunner("testHttpDoLoginGonnet").run();
        assertThat(muleEvent.getMessage().getPayload(), Is.is("success with basic-authentication for user: userGonnet"));
    }

}
