/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.exception;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.transformer.TransformerException;

import java.io.IOException;
import java.util.List;

/**
 * This does not scale, but it's a nice first approach. We will probably need an interface and several implementations (and that on the xml side as well)
 */
public enum ErrorType
{
    CONNECTIVITY(IOException.class),
    TRANSFORMATION(TransformerException.class),
    SCRIPTING(),
    ROUTING(),
    ANY();

    private List<Class<? extends Throwable>> handledErrors;

    ErrorType(Class<? extends Throwable>... errors)
    {
        if (errors == null)
        {
            this.handledErrors = newArrayList();
        }
        else
        {
            this.handledErrors = asList(errors);
        }
    }

    public boolean accept(Throwable error)
    {
        return handledErrors.stream().map(a -> a.isAssignableFrom(((MessagingException) error).getCauseException().getClass())).reduce(false, (a,b) -> a | b);
    }
}
