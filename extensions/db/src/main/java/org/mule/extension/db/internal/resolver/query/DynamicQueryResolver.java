/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.resolver.query;

import static java.util.Collections.emptyList;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.query.Query;
import org.mule.extension.db.internal.domain.query.QueryTemplate;
import org.mule.extension.db.internal.parser.QueryTemplateParser;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.expression.ExpressionManager;

/**
 * Resolves a dynamic query evaluating expressions using a given event
 */
public class DynamicQueryResolver implements QueryResolver
{

    private final Query query;
    private final QueryTemplateParser queryTemplateParser;
    private final ExpressionManager expressionManager;

    public DynamicQueryResolver(Query query, QueryTemplateParser queryTemplateParser, ExpressionManager expressionManager)
    {
        this.query = query;
        this.queryTemplateParser = queryTemplateParser;
        this.expressionManager = expressionManager;
    }

    @Override
    public Query resolve(DbConnection connection, MuleEvent muleEvent)
    {
        try
        {
            QueryTemplate queryTemplate = query.getQueryTemplate();
            String resolvedSqlText = expressionManager.parse(queryTemplate.getSqlText(), muleEvent);
            queryTemplate = queryTemplateParser.parse(resolvedSqlText);

            return new Query(queryTemplate, emptyList());
        }
        catch (RuntimeException e)
        {
            throw new QueryResolutionException("Error parsing query", e);
        }
    }
}
