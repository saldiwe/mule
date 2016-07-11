/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.result.statement;

/**
 * Thrown to indicate an error during auto generated keys processing
 */
public class AutoGeneratedKeysProcessingException extends RuntimeException
{

    public AutoGeneratedKeysProcessingException(Throwable cause)
    {
        super(cause);
    }
}
