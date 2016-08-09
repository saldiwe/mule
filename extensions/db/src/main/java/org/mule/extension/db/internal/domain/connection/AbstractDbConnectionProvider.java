/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import org.mule.extension.db.api.exception.connection.ConnectionClosingException;
import org.mule.extension.db.api.exception.connection.ConnectionCommitException;
import org.mule.extension.db.api.exception.connection.ConnectionCreationException;
import org.mule.extension.db.internal.domain.xa.XADbConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.ConfigName;

import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.sql.XAConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractDbConnectionProvider implements ConnectionProvider<DbConnection>, Initialisable, Disposable
{

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDbConnectionProvider.class);

    @ConfigName
    private String configName;

    @Inject
    private MuleContext muleContext;

    @ParameterGroup
    private ConnectionParameters connectionParameters;

    private DataSourceFactory dataSourceFactory;
    private DataSource dataSource = null;

    @Override
    public final DbConnection connect() throws ConnectionException
    {
        try
        {
            Connection jdbcConnection = dataSource.getConnection();

            if (jdbcConnection == null)
            {
                throw new ConnectionCreationException("Unable to create connection to the provided dataSource: " + dataSource);
            }

            DbConnection connection = createDbConnection(jdbcConnection);

            if (jdbcConnection instanceof XAConnection)
            {
                connection = new XADbConnection(connection, (XAConnection) jdbcConnection);
            }

            return connection;
        }
        catch (Exception e)
        {
            throw new ConnectionCreationException(e);
        }
    }

    protected abstract DbConnection createDbConnection(Connection connection) throws Exception;

    @Override
    public final void disconnect(DbConnection connection)
    {
        Connection jdbcConnection = connection.getJdbcConnection();
        try
        {
            if (jdbcConnection.isClosed())
            {
                return;
            }
        }
        catch (SQLException e)
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Error checking for closed connection while trying to disconnect", e);
            }
            return;
        }
        RuntimeException exception = null;
        try
        {
            if (!jdbcConnection.getAutoCommit())
            {
                jdbcConnection.commit();
            }
        }
        catch (SQLException e)
        {
            exception = new ConnectionCommitException(e);
        }
        finally
        {
            try
            {
                connection.release();
            }
            catch (Exception e)
            {
                if (exception == null)
                {
                    exception = new ConnectionClosingException(e);
                }
            }
        }
        if (exception != null)
        {
            throw exception;
        }
    }

    @Override
    public ConnectionValidationResult validate(DbConnection connection)
    {
        return success();
    }

    @Override
    public final void initialise() throws InitialisationException
    {
        dataSourceFactory = createDataSourceFactory();
        try
        {
            dataSource = createDataSource();
        }
        catch (SQLException e)
        {
            throw new InitialisationException(createStaticMessage("Could not create DataSource for DB config " + configName), e, this);
        }
    }

    @Override
    public final void dispose()
    {
        disposeIfNeeded(dataSourceFactory, LOGGER);
    }

    private DataSource createDataSource() throws SQLException
    {
        DataSource dataSource = connectionParameters.getDataSource();
        if (dataSource == null)
        {
            dataSource = dataSourceFactory.create(connectionParameters.getDataSourceConfig());
        }

        dataSource = dataSourceFactory.decorateDataSource(dataSource, connectionParameters.getDataSourceConfig().getPoolingProfile(), muleContext);

        return dataSource;
    }

    private DataSourceFactory createDataSourceFactory()
    {
        return new DataSourceFactory(configName, muleContext);
    }
}
