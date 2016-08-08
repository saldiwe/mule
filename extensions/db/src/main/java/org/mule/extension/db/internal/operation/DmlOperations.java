/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.operation;

import org.mule.extension.db.api.StatementStreamingResultSetCloser;
import org.mule.extension.db.api.param.InputParameter;
import org.mule.extension.db.api.param.QueryDefinition;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.executor.SelectExecutor;
import org.mule.extension.db.internal.domain.statement.QueryStatementFactory;
import org.mule.extension.db.internal.result.resultset.IteratorResultSetHandler;
import org.mule.extension.db.internal.result.resultset.ListResultSetHandler;
import org.mule.extension.db.internal.result.resultset.ResultSetHandler;
import org.mule.extension.db.internal.result.row.InsensitiveMapRowHandler;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.util.List;

import javax.inject.Inject;

public class DmlOperations
{

    @Inject
    private StatementStreamingResultSetCloser resultSetCloser;

    /**
     * Selects data from a database
     */
    public Object /*List<Map<String, Object>>*/ select(QueryDefinition queryDefinition,
                                                       @ParameterGroup QuerySettings settings,
                                                       @Optional List<InputParameter> inParams,
                                                       @Optional(defaultValue = "false") boolean streaming,
                                                       @Optional(defaultValue = "10") int fetchSize,
                                                       @Connection DbConnection connection,
                                                       @Optional Integer maxRows) throws Exception
    {
        QueryStatementFactory defaultStatementFactory = new QueryStatementFactory();
        if (maxRows != null)
        {
            defaultStatementFactory.setMaxRows(maxRows);
        }

        defaultStatementFactory.setFetchSize(fetchSize);
        defaultStatementFactory.setQueryTimeout(new Long(settings.getQueryTimeoutUnit().toSeconds(settings.getQueryTimeout())).intValue());

        InsensitiveMapRowHandler recordHandler = new InsensitiveMapRowHandler();
        ResultSetHandler resultSetHandler = streaming ? new IteratorResultSetHandler(recordHandler, resultSetCloser) : new ListResultSetHandler(recordHandler);

        return new SelectExecutor(defaultStatementFactory, resultSetHandler).execute(connection, null);
    }

}
