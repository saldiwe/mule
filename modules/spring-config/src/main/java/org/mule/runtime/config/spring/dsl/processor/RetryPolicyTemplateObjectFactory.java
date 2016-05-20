/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.processor;

import org.mule.runtime.config.spring.dsl.api.ObjectFactory;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.retry.async.AsynchronousRetryTemplate;
import org.mule.runtime.core.retry.policies.SimpleRetryPolicyTemplate;

public class RetryPolicyTemplateObjectFactory implements ObjectFactory<RetryPolicyTemplate>
{

    private boolean blocking;
    private Integer count = SimpleRetryPolicyTemplate.DEFAULT_RETRY_COUNT;;
    private Integer frequency = SimpleRetryPolicyTemplate.DEFAULT_FREQUENCY;

    @Override
    public RetryPolicyTemplate getObject() throws Exception
    {
        RetryPolicyTemplate retryPolicyTemplate = new SimpleRetryPolicyTemplate(count, frequency);
        if (!blocking)
        {
            retryPolicyTemplate = new AsynchronousRetryTemplate(retryPolicyTemplate);
        }
        return retryPolicyTemplate;
    }

    public void setBlocking(boolean blocking)
    {
        this.blocking = blocking;
    }

    public void setCount(Integer count)
    {
        this.count = count;
    }

    public void setFrequency(Integer frequency)
    {
        this.frequency = frequency;
    }
}
