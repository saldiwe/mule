/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.listener;

import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTPS;

import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.module.http.api.listener.HttpListenerConfig;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.util.ObjectNameHelper;

public class HttpListenerConfigBuilder {

  private final DefaultHttpListenerConfig config;
  private final MuleContext muleContext;

  public HttpListenerConfigBuilder(final String name, final MuleContext muleContext) {
    this.config = new DefaultHttpListenerConfig();
    this.config.setMuleContext(muleContext);
    this.config.setName(name);
    this.muleContext = muleContext;
  }

  public HttpListenerConfigBuilder(final MuleContext muleContext) {
    this(getNextAutoGeneratedConfigName(muleContext), muleContext);
  }

  private static String getNextAutoGeneratedConfigName(MuleContext muleContext) {
    return new ObjectNameHelper(muleContext).getUniqueName("auto-generated-listener-config");
  }

  public HttpListenerConfigBuilder setPort(int port) {
    this.config.setPort(port);
    return this;
  }

  public HttpListenerConfigBuilder setHost(String host) {
    this.config.setHost(host);
    return this;
  }

  public HttpListenerConfigBuilder setTlsContextFactory(TlsContextFactory tlsContextFactory) {
    this.config.setTlsContext(tlsContextFactory);
    this.config.setProtocol(HTTPS);
    return this;
  }

  public HttpListenerConfig build() throws MuleException {
    try {
      this.muleContext.getRegistry().registerObject(this.config.getName(), this.config);
      this.config.initialise();
      this.config.start();
      return this.config;
    } catch (Exception e) {
      throw new DefaultMuleException(e);
    }
  }
}
