/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.config;

import static java.util.concurrent.TimeUnit.SECONDS;
import org.mule.extension.db.api.TransactionIsolation;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

/**
 * Provides a way to define a JDBC configuration for any DB vendor.
 *
 * @since 4.0
 */
@Configuration(name = "generic-config")
public class GenericDbConfig
{

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
     * A {@link Map} which specifies non-standard custom data types, in which
     * the key is the ame of the data type used by the JDBC driver and the value
     * is type identifier used by the JDBC driver.
     */
    @Parameter
    @Optional
    private Map<String, String> customDataTypes;

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

    /**
     * The transaction isolation level to set on the driver when connecting the database.
     */
    @Parameter
    @Optional
    private TransactionIsolation transactionIsolation;
}