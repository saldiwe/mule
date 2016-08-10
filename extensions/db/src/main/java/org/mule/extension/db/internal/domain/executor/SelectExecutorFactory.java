/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.executor;

import org.mule.extension.db.internal.domain.statement.StatementFactory;
import org.mule.extension.db.internal.result.resultset.ResultSetHandler;

/**
 * Creates {@link QueryExecutor} instances
 */
public class SelectExecutorFactory implements QueryExecutorFactory {

  private final StatementFactory statementFactory;
  private final ResultSetHandler resultHandler;

  public SelectExecutorFactory(StatementFactory statementFactory, ResultSetHandler resultHandler) {
    this.statementFactory = statementFactory;
    this.resultHandler = resultHandler;
  }

  @Override
  public QueryExecutor create() {
    return new SelectExecutor(statementFactory, resultHandler);
  }
}
