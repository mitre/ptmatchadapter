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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hl7.fhir.instance.model.ContactPoint;
import org.hl7.fhir.instance.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.instance.model.ContactPoint.ContactPointUse;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public abstract class ContactPointUtil {

  public static List<ContactPoint> find(List<ContactPoint> contacts, 
      ContactPointSystem sys, ContactPointUse use) {
    
    final List<ContactPoint> match = new ArrayList<ContactPoint>();
    
    for (ContactPoint cp : contacts) {
      if (cp.getSystem() != null && cp.getSystem().equals(sys)) {
        if (cp.getUse() != null && cp.getUse().equals(use)) {
          match.add(cp);
        }
      }
    }
    
    Collections.sort(match, new ContactPointComparator());
    return match;
  }
}
