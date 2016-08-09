/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.resolver.query;

import org.mule.extension.db.api.param.InOutQueryParameter;
import org.mule.extension.db.api.param.InputParameter;
import org.mule.extension.db.api.param.OutputParameter;
import org.mule.extension.db.api.param.QueryDefinition;
import org.mule.extension.db.api.param.QueryParameter;
import org.mule.extension.db.internal.DbConnector;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.param.DefaultInOutQueryParam;
import org.mule.extension.db.internal.domain.param.DefaultInputQueryParam;
import org.mule.extension.db.internal.domain.param.DefaultOutputQueryParam;
import org.mule.extension.db.internal.domain.param.QueryParam;
import org.mule.extension.db.internal.domain.query.Query;
import org.mule.extension.db.internal.domain.query.QueryTemplate;
import org.mule.extension.db.internal.parser.QueryTemplateParser;
import org.mule.extension.db.internal.parser.SimpleQueryTemplateParser;

import java.util.List;

public class DefaultQueryResolver implements QueryResolver
{

    private final QueryTemplateParser queryTemplateParser = new SimpleQueryTemplateParser();

    @Override
    public Query resolve(QueryDefinition queryDefinition, DbConnector connector, DbConnection connection) throws QueryResolutionException
    {
        final String sql = queryDefinition.getSql();
        QueryTemplate template = new QueryTemplate(sql, queryTemplateParser.getStatementType(sql), )

    }

    private List<QueryParam> resolveParameters(QueryDefinition queryDefinition)
    {
        int index = 0;
        for (QueryParameter parameter : queryDefinition.getParameters())
        {
            if (parameter instanceof InputParameter)
            {
                new DefaultInputQueryParam(index++, null, ((InputParameter) parameter).getValue(), parameter.getName());
            }
            else if (parameter instanceof OutputParameter)
            {
                new DefaultOutputQueryParam(index++, null, parameter.getName());
            }
            else if (parameter instanceof InOutQueryParameter)
            {
                new DefaultInOutQueryParam(index++, null, parameter.getName(), ((InOutQueryParameter) parameter).getValue());
            }
        }
    }
}
