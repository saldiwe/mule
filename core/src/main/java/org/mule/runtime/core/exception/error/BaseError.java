/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.exception.error;

import org.mule.runtime.core.api.MessagingException;

import java.util.List;

public class BaseError implements ErrorType
{
    protected List<Class<? extends Throwable>> handledErrors;

    @Override
    public boolean includes(Throwable error)
    {
        return handledErrors.stream().map(a -> a.isAssignableFrom(((MessagingException) error).getCauseException().getClass())).reduce(false, (a,b) -> a | b);
    }
}
