/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.storedprocedure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mule.runtime.module.db.integration.TestDbConfig.getDerbyResource;
import static org.mule.runtime.module.db.integration.TestDbConfig.getOracleResource;
import static org.mule.runtime.module.db.integration.model.RegionManager.SOUTHWEST_MANAGER;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.module.db.integration.model.OracleTestDatabase;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class StoredProcedureJavaUdtTestCase extends AbstractDbIntegrationTestCase {

  public StoredProcedureJavaUdtTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    List<Object[]> params = new LinkedList<>();
    if (!getOracleResource().isEmpty()) {
      params.add(new Object[] {"integration/config/oracle-mapped-udt-db-config.xml", new OracleTestDatabase()});
    }

    if (!getDerbyResource().isEmpty()) {
      params.add(getDerbyResource().get(0));
    }

    return params;
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/storedprocedure/stored-procedure-udt-config.xml"};
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    testDatabase.createStoredProcedureGetManagerDetails(getDefaultDataSource());
  }

  @Test
  public void returnsObject() throws Exception {
    final MuleEvent responseEvent = flowRunner("returnsObject").withPayload(TEST_MESSAGE).run();
    final MuleMessage response = responseEvent.getMessage();

    assertThat(response.getPayload(), equalTo(SOUTHWEST_MANAGER.getContactDetails()));
  }
}
