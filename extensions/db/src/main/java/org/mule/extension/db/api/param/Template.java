/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.param;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.util.List;

@Alias("template-query-ref")
public class Template implements Statement, CallableStatement
{

    @Parameter
    @Alias("ref")
    //@RefOnly
    private Statement statement;

    @Parameter
    @Optional
    @Alias("inParams")
    private List<InputParameter> inputParameters;

    public Statement getStatement()
    {
        return statement;
    }

    public List<InputParameter> getInputParameters()
    {
        return inputParameters;
    }
}
