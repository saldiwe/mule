/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.query;

import org.mule.extension.db.api.param.QueryDefinition;
import org.mule.extension.db.internal.domain.param.InputQueryParam;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents an instantiation of a {@link QueryTemplate} with parameter values
 */
public class Query
{

    private final QueryDefinition queryDefinition;
    private final StatementType statementType;

    /**
     * Creates a query from a template and a set of parameter values
     *
     * @param queryTemplate template describing the query
     * @param paramValues parameter values for the query
     */
    public Query(QueryDefinition queryDefinition, StatementType statementType)
    {
        this.queryDefinition = queryDefinition;
        this.statementType = statementType;
    }

    public QueryDefinition getQueryDefinition()
    {
        return queryDefinition;
    }

    public StatementType getStatementType()
    {
        return statementType;
    }
}
