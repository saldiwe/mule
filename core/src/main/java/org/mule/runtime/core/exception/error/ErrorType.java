/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.exception.error;

/**
 * This approach is more viable if we consider adding the error type to the MessagingException. Then it can be explicitly
 *  defined or implicitly resolved considering the implementors of this interface (a way to define them should be possible).
 */
public interface ErrorType
{

    /**
     * Defines whether this ErrorType includes a certain error
     *
     * @param error the error to consider
     * @return true if this type includes the error. false otherwise
     */
    boolean includes(Throwable error);
}
