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

public class OperationModuleWithConfigGlobalElementTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "operation-with-config-global-elements-module-flow.xml";
    }

    @Test
    public void testHttpDoLogin() throws Exception{
        //TODO WIP, the expect result after a login is a JSON like the following
        //{
        //   "authenticated": true,
        //   "user": "userLP"
        // }
        MuleEvent muleEvent = flowRunner("testHttpDoLogin").run();
        assertThat(muleEvent.getMessage().getPayload(), Is.is("hardcoded value from module"));
    }

}
