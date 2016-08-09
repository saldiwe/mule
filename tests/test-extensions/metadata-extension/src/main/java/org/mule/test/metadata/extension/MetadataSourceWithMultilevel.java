/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension;

import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.AMERICA;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.ARGENTINA;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.BUENOS_AIRES;

import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.tck.message.StringAttributes;
import org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver;
import org.mule.test.metadata.extension.resolver.TestOutputNullTypeResolver;

import java.util.Map;

@MetadataScope(keysResolver = TestMultiLevelKeyResolver.class, contentResolver = TestMultiLevelKeyResolver.class,
    outputResolver = TestOutputNullTypeResolver.class)
public class MetadataSourceWithMultilevel extends Source<Map<String, Object>, StringAttributes> {

  private static final String ERROR_MESSAGE = "LocationKey field was not injected properly";

  @MetadataKeyId
  @ParameterGroup
  public LocationKey key;

  @Override
  public void start() {
    boolean injectedProperly = key != null
        && key.getCity().equals(BUENOS_AIRES)
        && key.getCountry().equals(ARGENTINA)
        && key.getContinent().equals(AMERICA);

    if (!injectedProperly) {
      throw new RuntimeException(ERROR_MESSAGE);
    }
  }

  @Override
  public void stop() {

  }
}
