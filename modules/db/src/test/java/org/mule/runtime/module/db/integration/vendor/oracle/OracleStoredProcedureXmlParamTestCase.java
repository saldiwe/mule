/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.vendor.oracle;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.db.integration.TestRecordUtil.assertRecord;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.db.integration.TestDbConfig;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.module.db.integration.model.Alien;
import org.mule.runtime.module.db.integration.model.Record;
import org.mule.runtime.module.db.integration.model.XmlField;
import org.mule.runtime.module.db.internal.domain.type.oracle.OracleXmlType;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class OracleStoredProcedureXmlParamTestCase extends AbstractOracleXmlTypeTestCase {

  public static final String DESCRIPTION_FIELD = "description";

  public OracleStoredProcedureXmlParamTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getOracleResource();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/vendor/oracle/oracle-stored-procedure-xml-type-param-config.xml"};
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    testDatabase.createStoredProcedureGetAlienDescription(getDefaultDataSource());
    testDatabase.createStoredProcedureUpdateAlienDescription(getDefaultDataSource());
  }

  @Test
  public void returnsXmlTypeOutputParam() throws Exception {
    final MuleEvent responseEvent = flowRunner("xmlTypeOutputParam").withPayload("ET").run();

    final MuleMessage response = responseEvent.getMessage();
    assertThat(response.getPayload(), is(instanceOf(Map.class)));

    Map<String, Object> mapPayload = (Map) response.getPayload();

    assertRecord(new Record(new XmlField("description", Alien.ET.getXml())), new Record(mapPayload));
  }

  @Test
  public void acceptsXmlTypeInputParam() throws Exception {
    DataSource defaultDataSource = getDefaultDataSource();
    Connection connection = defaultDataSource.getConnection();

    try {
      Object xmlType = OracleXmlType.createXmlType(connection, Alien.ET.getXml());

      final MuleEvent responseEvent = flowRunner("xmlTypeInputParam").withPayload(TEST_MESSAGE)
          .withInboundProperty("name", "Monguito").withFlowVariable(DESCRIPTION_FIELD, xmlType).run();

      final MuleMessage response = responseEvent.getMessage();

      assertThat(response.getPayload(), is(instanceOf(Map.class)));
      Map<String, Object> mapPayload = (Map) response.getPayload();
      assertThat(mapPayload.size(), equalTo(0));

      assertUpdatedAlienDscription();
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }
}
