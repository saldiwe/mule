/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.operation;

import org.mule.extension.db.api.param.InputParameter;
import org.mule.extension.db.api.param.QueryDefinition;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DmlOperations
{

    /**
     * Selects data from a database
     */
    //TODO: MetadataResolver needed to change to Iterator<Map> in case streaming is enabled.
    public List<Map<String, Object>> select(@ParameterGroup QueryDefinition queryDefinition,
                                            @ParameterGroup QuerySettings settings,
                                            @Optional List<InputParameter> inParams,
                                            @Optional(defaultValue = "false") boolean streaming,
                                            @Optional(defaultValue = "10") int fetchSize,
                                            @Optional Integer maxRows)
    {
        return new ArrayList<>();
    }
}
