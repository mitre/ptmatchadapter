/**
 * PtMatchAdapter - a patient matching system adapter
 * Copyright (C) 2016 The MITRE Corporation.  ALl rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mitre.ptmatchadapter.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.instance.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.instance.model.StringType;
import org.hl7.fhir.instance.model.Type;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public class ParametersUtilTest {

  private static List<ParametersParameterComponent> paramList;

  /**
   * @throws java.lang.Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    paramList = new ArrayList<ParametersParameterComponent>();

    String[] names = { "type", "resourceUrl", "name", "type", "_lastUpdated" };
    String[] values = { "master", "http://fhirserver.net", "Mike", "query",
        "ge2016-02-16" };

    for (int i = 0; i < names.length; i++) {
      String name = names[i];
      String value = values[i];

      paramList.add(ParametersUtil.createParameter(name, value));
    }
  }

  @Test
  public void testFindByName() {

    for (ParametersParameterComponent p : paramList) {
      assertNotNull(p.getName(), ParametersUtil.findByName(paramList, p.getName()));
    }

    final String[] names = { "typo", "resourceUrls", "name1", "lastUpdated" };
    for (String name : names) {
      assertNull(name + " should be missing",
          ParametersUtil.findByName(paramList, name));
    }

  }

  @Test
  public void testCreateParametersParameterComponent() {
    String[] names = { "type", "resourceUrl", "name", "_lastUpdated" };
    String[] values = { "master", "http://fhirserver.net", "Mike", "ge2016-02-16" };

    for (int i = 0; i < names.length; i++) {
      String name = names[i];
      String value = values[i];

      ParametersParameterComponent expected = new ParametersParameterComponent();
      expected.setName(name);
      StringType strTypeVal = new StringType();
      strTypeVal.setValue(value);
      expected.setValue(strTypeVal);

      ParametersParameterComponent actual = ParametersUtil.createParameter(name,
          value);
      assertNotNull(actual);
      assertEquals("name", actual.getName(), expected.getName());
      assertEquals("value", actual.getValue().toString(),
          expected.getValue().toString());
    }

  }
}
