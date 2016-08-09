/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.param;

import static com.google.common.collect.ImmutableList.copyOf;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Text;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@Alias("query")
public class QueryDefinition
{

    @Parameter
    @Optional
    @Text
    private String sql;

    @Parameter
    private QueryType queryType;

    @Parameter
    @Optional
    private List<QueryParameter> parameters = new LinkedList<>();

    public QueryDefinition copy() {
        QueryDefinition copy = new QueryDefinition();
        copy.sql = sql;
        copy.parameters = new LinkedList<>(parameters);
        copy.queryType = queryType;

        return copy;
    }

    public String getSql()
    {
        return sql;
    }

    public QueryType getQueryType()
    {
        return queryType;
    }

    public List<QueryParameter> getParameters()
    {
        return copyOf(parameters);
    }

    public void addParameters(Collection<QueryParameter> parameters)
    {
        if (parameters != null)
        {
            this.parameters.addAll(parameters);
        }
    }

    public void setSql(String sql)
    {
        this.sql = sql;
    }

}
