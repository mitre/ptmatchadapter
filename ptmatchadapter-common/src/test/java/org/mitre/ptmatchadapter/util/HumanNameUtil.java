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

import org.hl7.fhir.instance.model.HumanName;
import org.hl7.fhir.instance.model.HumanName.NameUse;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public abstract class HumanNameUtil {

  public static HumanName newHumanName(
      String fullName, String[] family, String[] given) {
    return newHumanName(fullName, family, given, null);
  }

  public static HumanName newHumanName(
      String fullName, String[] family, String[] given, String[] suffix) {
    return newHumanName(fullName, family, given, suffix, null);
  }

  public static HumanName newHumanName(
      String fullName, String[] family, String[] given, String[] suffix, NameUse use) {
    final HumanName name = new HumanName();
    name.setText(fullName);

    if (family != null) {
      for (String surname : family) {
        name.addFamily(surname);
      }
    }

    if (given != null) {
      for (String str : given) {
        name.addGiven(str);
      }
    }

    if (suffix != null) {
      for (String str : suffix) {
        name.addGiven(str);
      }
    }

    if (use != null) {
      name.setUse(use);
    }
    return name;
  }

}
