/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.operation;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mule.extension.db.internal.domain.transaction.TransactionalAction.JOIN_IF_POSSIBLE;
import org.mule.extension.db.internal.domain.transaction.TransactionalAction;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.util.concurrent.TimeUnit;

public class QuerySettings
{

    /**
     * Indicates how the message processor handles transactions
     */
    @Parameter
    @Optional(defaultValue = "JOIN_IF_POSSIBLE")
    private TransactionalAction transactionalAction = JOIN_IF_POSSIBLE;

    /**
     * Indicates the minimum amount of time in seconds before the JDBC driver attempts to cancel a running statement.
     * No timeout is used by default.
     */
    @Parameter
    @Optional(defaultValue = "0")
    private int queryTimeout = 0;

    /**
     * A {@link TimeUnit} which qualifies the {@link #queryTimeout}
     */
    @Parameter
    @Optional(defaultValue = "SECONDS")
    private TimeUnit queryTimeoutUnit = SECONDS;

    public TransactionalAction getTransactionalAction()
    {
        return transactionalAction;
    }

    public int getQueryTimeout()
    {
        return queryTimeout;
    }

    public TimeUnit getQueryTimeoutUnit()
    {
        return queryTimeoutUnit;
    }
}
