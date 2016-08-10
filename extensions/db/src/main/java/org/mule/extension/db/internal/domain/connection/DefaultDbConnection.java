/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.connection;

import org.mule.extension.db.api.exception.connection.ConnectionClosingException;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.result.resultset.ResultSetHandler;
import org.mule.extension.db.internal.result.statement.GenericStatementResultIteratorFactory;
import org.mule.extension.db.internal.result.statement.StatementResultIteratorFactory;

import com.google.common.collect.ImmutableList;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class DefaultDbConnection implements DbConnection {

  private final Connection jdbcConnection;

  public DefaultDbConnection(Connection jdbcConnection) {
    this.jdbcConnection = jdbcConnection;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StatementResultIteratorFactory getStatementResultIteratorFactory(ResultSetHandler resultSetHandler) {
    return new GenericStatementResultIteratorFactory(resultSetHandler);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<DbType> getVendorDataTypes() {
    return ImmutableList.of();
  }

  @Override
  public Connection getJdbcConnection() {
    return jdbcConnection;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void begin() throws Exception {
    if (jdbcConnection.getAutoCommit()) {
      jdbcConnection.setAutoCommit(false);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void commit() throws SQLException {
    jdbcConnection.commit();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void rollback() throws SQLException {
    jdbcConnection.rollback();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void release() {
    try {
      jdbcConnection.close();
    } catch (SQLException e) {
      throw new ConnectionClosingException(e);
    }
  }
}
