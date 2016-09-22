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

import java.io.Serializable;
import java.util.Comparator;

import org.hl7.fhir.instance.model.ContactPoint;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public class ContactPointComparator
    implements Comparator<ContactPoint>, Serializable {

  /**
   * serial vesion identifier
   */
  private static final long serialVersionUID = 4635457105945591503L;

  /**
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  @Override
  public int compare(ContactPoint cp1, ContactPoint cp2) {
    final int rank1 = cp1.getRank();
    final int rank2 = cp2.getRank();
    
    int result = 0;
    
    if (rank1 == 0 && rank2 == 0) {
      result = 0;
    } else if (rank1 == 0) {
      result = -1;
    } else {
      result = cp1.getRank() - cp2.getRank();
    }
    
    return result;
  }

  
}
