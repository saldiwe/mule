/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db;

import org.mule.extension.db.api.DbConnector;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;

import org.junit.Test;

public class DummySchemaTestCase extends ExtensionFunctionalTestCase
{

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[]{DbConnector.class};
    }

    @Override
    protected String getConfigFile()
    {
        return "integration/select/default-database-config-query-config.xml";
    }

    @Test
    public void select() {

    }

}
