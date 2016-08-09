/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.executor;

import org.mule.extension.db.api.param.InputParameter;
import org.mule.extension.db.api.param.OutputParameter;
import org.mule.extension.db.api.param.QueryParameter;
import org.mule.extension.db.internal.domain.logger.SingleQueryLogger;
import org.mule.extension.db.internal.domain.query.Query;
import org.mule.extension.db.internal.domain.statement.StatementFactory;
import org.mule.extension.db.internal.domain.type.DbType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for query executors
 */
public abstract class AbstractExecutor
{

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractExecutor.class);

    protected final StatementFactory statementFactory;

    public AbstractExecutor(StatementFactory statementFactory)
    {
        this.statementFactory = statementFactory;
    }

    protected void doProcessParameters(PreparedStatement statement, Query query, SingleQueryLogger queryLogger) throws SQLException
    {
        final List<QueryParameter> parameters = query.getDefinition().getParameters();
        final Map<Integer, DbType> paramTypes = query.getParamTypes();

        for (int paramIndex = 1, inputParamsSize = parameters.size(); paramIndex <= inputParamsSize; paramIndex++)
        {
            QueryParameter queryParam = parameters.get(paramIndex - 1);
            if (queryParam instanceof InputParameter)
            {
                InputParameter inputParam = (InputParameter) queryParam;
                queryLogger.addParameter(inputParam, paramIndex);

                processInputParam(statement, paramIndex, inputParam.getValue(), paramTypes.get(paramIndex));
            }

            if (queryParam instanceof OutputParameter)
            {
                processOutputParam((CallableStatement) statement, paramIndex, paramTypes.get(paramIndex));
            }
        }
    }

    protected void processInputParam(PreparedStatement statement, int index, Object value, DbType type) throws SQLException
    {
        type.setParameterValue(statement, index, value);
    }

    private void processOutputParam(CallableStatement statement, int index, DbType type) throws SQLException
    {
        type.registerOutParameter(statement, index);
    }
}
