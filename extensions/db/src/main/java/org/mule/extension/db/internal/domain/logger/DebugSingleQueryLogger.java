/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.logger;

import org.mule.extension.db.api.param.InputParameter;
import org.mule.extension.db.internal.domain.query.Query;

import org.slf4j.Logger;

/**
 * Logs a single query in debug level
 */
public class DebugSingleQueryLogger extends AbstractDebugQueryLogger implements SingleQueryLogger {

  private final Query query;

  public DebugSingleQueryLogger(Logger logger, Query query) {
    super(logger);

    this.query = query;

    builder.append("Executing query:\n").append(query.getDefinition().getSql());

    if (hasParameters()) {
      builder.append("\nParameters:");
    }
  }

  protected boolean hasParameters() {
    return query.hasInputParameters();
  }

  @Override
  public void addParameter(InputParameter param, int index) {
    builder.append("\n")
        .append(param.getName() != null ? param.getName() : index)
        .append(" = ").append(param.getValue());
  }
}
