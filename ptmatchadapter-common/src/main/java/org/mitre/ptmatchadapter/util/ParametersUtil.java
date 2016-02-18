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

import java.util.List;

import org.hl7.fhir.instance.model.StringType;
import org.hl7.fhir.instance.model.Parameters.ParametersParameterComponent;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public abstract class ParametersUtil {

  public static ParametersParameterComponent findByName(
      List<ParametersParameterComponent> params, String name) {

    if (name != null && !name.isEmpty()) {
      for (ParametersParameterComponent p : params) {
        if (name.equals(p.getName())) {
          return p;
        }
      }
    }
    return null;
  }
  
  public static ParametersParameterComponent createParameter(String name, String value) {
    final ParametersParameterComponent  p = new ParametersParameterComponent();
    p.setName(name);
    StringType strTypeVal = new StringType();
    strTypeVal.setValue(value);
    p.setValue(strTypeVal);
    return p;
  }

}
