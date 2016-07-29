/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.exception.error;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.api.transformer.TransformerException;

import java.io.IOException;
import java.util.List;

/**
 * This will no longer be needed once the MessagingException does the error type inference at create time.
 */
public enum ErrorType
{
    CONNECTIVITY(IOException.class, ConnectionException.class),
    TRANSFORMATION(TransformerException.class),
    SCRIPTING(),
    ROUTING(RoutingException.class),
    GENERAL();

    private List<Class<? extends Throwable>> handledExceptions;

    ErrorType(Class<? extends Throwable>... errors)
    {
        if (errors == null)
        {
            this.handledExceptions = newArrayList();
        }
        else
        {
            this.handledExceptions = asList(errors);
        }
    }

    public boolean accept(MessagingException error)
    {
        return handledExceptions.stream().anyMatch(error::causedBy);
    }
}
