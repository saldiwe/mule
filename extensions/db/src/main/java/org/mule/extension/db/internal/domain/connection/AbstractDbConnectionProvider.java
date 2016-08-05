/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.extension.db.api.TransactionIsolation;
import org.mule.extension.db.api.config.DbPoolingProfile;
import org.mule.extension.db.api.exception.connection.ConnectionClosingException;
import org.mule.extension.db.api.exception.connection.ConnectionCommitException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractDbConnectionProvider implements ConnectionProvider<DbConnection>
{

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDbConnectionProvider.class);

    /**
     * Specifies a list of custom key-value connectionProperties for the config.
     */
    @Parameter
    @Optional
    private Map<String, String> connectionProperties;

    @Parameter
    @Optional
    private DbPoolingProfile poolingProfile;

    /**
     * The transaction isolation level to set on the driver when connecting the database.
     */
    @Parameter
    @Optional
    private TransactionIsolation transactionIsolation;

    /**
     * Reference to a JDBC DataSource object. This object is typically created using Spring.
     * When using XA transactions, an XADataSource object must be provided.
     */
    @Parameter
    @Optional
    private DataSource dataSource;

    /**
     * URL used to connect to the database.
     */
    @Parameter
    @Optional
    private String url;

    /**
     * Indicates whether or not the created datasource has to support XA transactions. Default is false.
     */
    @Parameter
    @Optional(defaultValue = "false")
    private boolean useXaTransactions = false;

    /**
     * Fully-qualified name of the database driver class.
     */
    @Parameter
    @Optional
    private String driverClassName;

    /**
     * Maximum time that the data source will wait while attempting to connect to a
     * database. A value of zero (default) specifies that the timeout is the default system timeout if there is one;
     * otherwise, it specifies that there is no timeout.
     */
    @Parameter
    @Optional(defaultValue = "0")
    private Integer connectionTimeout;


    /**
     * A {@link TimeUnit} which qualifies the {@link #connectionTimeout}
     */
    @Parameter
    @Optional(defaultValue = "SECONDS")
    private TimeUnit connectionTimeUnit = SECONDS;

    @Override
    public void disconnect(DbConnection connection)
    {
        try
        {
            if (connection.isClosed())
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
            if (!connection.getAutoCommit())
            {
                connection.commit();
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
                connection.close();
            }
            catch (SQLException e)
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
}
