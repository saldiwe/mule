/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.connection;

import static java.util.concurrent.TimeUnit.SECONDS;
import org.mule.extension.db.api.param.TransactionIsolation;
import org.mule.extension.db.api.config.DbPoolingProfile;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.util.concurrent.TimeUnit;

/**
 * Maintains configuration information about how to build a {@link javax.sql.DataSource}
 */
public class DataSourceConfig {

  /**
   * URL used to connect to the database.
   */
  @Parameter
  @Optional
  private String url;

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
  private TimeUnit connectionTimeoutUnit = SECONDS;

  private String user;
  private String password;

  /**
   * The transaction isolation level to set on the driver when connecting the database.
   */
  @Parameter
  @Optional
  private TransactionIsolation transactionIsolation;

  /**
   * Indicates whether or not the created datasource has to support XA transactions. Default is false.
   */
  @Parameter
  @Optional(defaultValue = "false")
  private boolean useXaTransactions = false;

  /**
   * Provides a way to configure database connection pooling.
   */
  @Parameter
  @Optional
  private DbPoolingProfile poolingProfile;


  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getDriverClassName() {
    return driverClassName;
  }

  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  public TimeUnit getConnectionTimeoutUnit() {
    return connectionTimeoutUnit;
  }

  public String getPassword() {
    return password;
  }

  public String getUser() {
    return user;
  }

  public TransactionIsolation getTransactionIsolation() {
    return transactionIsolation;
  }

  public boolean isUseXaTransactions() {
    return useXaTransactions;
  }

  public DbPoolingProfile getPoolingProfile() {
    return poolingProfile;
  }
}
