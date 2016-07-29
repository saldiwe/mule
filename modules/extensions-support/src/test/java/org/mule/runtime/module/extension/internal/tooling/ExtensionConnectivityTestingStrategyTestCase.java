/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.tooling;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.tooling.api.connectivity.ConnectionResult.Status.SUCCESS;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.module.tooling.api.connectivity.ConnectionResult;
import org.mule.runtime.module.tooling.api.connectivity.MultipleConnectivityTestingObjectsFoundException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Ignore
public class ExtensionConnectivityTestingStrategyTestCase extends AbstractMuleTestCase
{

    @Rule
    public ExpectedException expectedException = none();

    private ExtensionConnectivityTestingStrategy extensionConnectivityTestingStrategy;
    private MuleContext mockMuleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS.get());
    private ConfigurationProvider mockConnectionProvider = mock(ConfigurationProvider.class, RETURNS_DEEP_STUBS.get());
    private ConnectionValidationResult mockConnectionValidationResult = mock(ConnectionValidationResult.class, RETURNS_DEEP_STUBS.get());


    @Before
    public void createTestingInstance()
    {
        extensionConnectivityTestingStrategy = new ExtensionConnectivityTestingStrategy();
        extensionConnectivityTestingStrategy.setMuleContext(mockMuleContext);
    }

    @Test
    public void multipleConnectionProvidersInConfig() throws RegistrationException
    {
        when(mockMuleContext.getRegistry().lookupObject(ConfigurationProvider.class)).thenThrow(RegistrationException.class);
        expectedException.expect(MultipleConnectivityTestingObjectsFoundException.class);
        extensionConnectivityTestingStrategy.connectionTestingObjectIsPresent();
    }

    @Test
    public void noConnectionProviderInConfig() throws RegistrationException
    {
        when(mockMuleContext.getRegistry().lookupObject(ConfigurationProvider.class)).thenReturn(null);
        assertThat(extensionConnectivityTestingStrategy.connectionTestingObjectIsPresent(), is(false));
    }

    @Test
    public void connectionProviderInConfigWithInvalidConnection() throws RegistrationException
    {
        //when(mockMuleContext.getRegistry().lookupObject(ConfigurationProvider.class)).thenReturn();
        //assertThat(extensionConnectivityTestingStrategy.connectionTestingObjectIsPresent(), is(true));
        //when(mockConnectionProvider.validate(any())).thenReturn(mockConnectionValidationResult);
        //when(mockConnectionValidationResult.isValid()).thenReturn(true);
        //ConnectionResult connectionResult = extensionConnectivityTestingStrategy.testConnectivity();
        //assertThat(connectionResult.getStatus(), Is.is(SUCCESS));;
    }

}