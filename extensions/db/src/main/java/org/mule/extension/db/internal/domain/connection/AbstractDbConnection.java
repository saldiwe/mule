/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.connection;

import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.result.resultset.ResultSetHandler;
import org.mule.extension.db.internal.result.statement.GenericStatementResultIteratorFactory;
import org.mule.extension.db.internal.result.statement.StatementResultIteratorFactory;

import com.google.common.collect.ImmutableList;

import java.sql.Connection;
import java.util.List;

/**
 * Implements connector side of {@link DbConnection}
 */
public abstract class AbstractDbConnection implements DbConnection
{

    protected final Connection delegate;

    public AbstractDbConnection(Connection delegate)
    {
        this.delegate = delegate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StatementResultIteratorFactory getStatementResultIteratorFactory(ResultSetHandler resultSetHandler)
    {
        return new GenericStatementResultIteratorFactory(resultSetHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void begin() throws Exception
    {
        if (getAutoCommit())
        {
            setAutoCommit(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DbType> getVendorDataTypes()
    {
        return ImmutableList.of();
    }
}
