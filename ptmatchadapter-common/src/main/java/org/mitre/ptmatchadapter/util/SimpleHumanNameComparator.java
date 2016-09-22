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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.hl7.fhir.instance.model.HumanName;
import org.hl7.fhir.instance.model.HumanName.NameUse;
import org.hl7.fhir.instance.model.StringType;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public class SimpleHumanNameComparator
    implements Comparator<HumanName>, Serializable {

  /**
   * serial version identifier
   */
  private static final long serialVersionUID = 3552012736460293854L;

  /**
   * Compares by family, suffix, given, and then use.
   * 
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  @Override
  public int compare(HumanName name1, HumanName name2) {
    int result = 0;
    
    if (name1 == null && name2 != null) {
      result = -1;
    } else if (name1 == null && name2 == null) {
      result = 0;
    } else if (name2 == null) {
      result = 1;
    } else { // neither are null
      final StringTypeComparator stringTypeComparator = new StringTypeComparator();
      final List<StringType> family1 = name1.getFamily();
      Collections.sort(family1, stringTypeComparator);
      final List<StringType> family2 = name2.getFamily();
      Collections.sort(family2, stringTypeComparator);

      result = compare(family1, family2);
      if (result == 0) {
        final List<StringType> suffix1 = name1.getSuffix();
        Collections.sort(suffix1, stringTypeComparator);
        final List<StringType> suffix2 = name2.getSuffix();
        Collections.sort(suffix2, stringTypeComparator);

        result = compare(suffix1, suffix2);
        
        if (result == 0) {
          final List<StringType> given1 = name1.getGiven();
          Collections.sort(given1, stringTypeComparator);
          final List<StringType> given2 = name2.getGiven();
          Collections.sort(given2, stringTypeComparator);

          result = compare(given1, given2);
          
          if (result == 0) {
            NameUse use1 = name1.getUse();
            NameUse use2 = name2.getUse();
            
            if (use1 == null && use2 != null) {
              result = -1;
            } else if (use1 == null && use2 == null) {
              result = 0;
            } else if (use2 == null) {
              result = 1;
            } else { //neither are null
              result = use1.compareTo(use2);
            }
          }
        }
      }
   }
  return result;
}

  private int compare(List<StringType> list1, List<StringType> list2) {
    int result = 0;

    if (list1.size() == 0) {
      if (list2.size() == 0) {
        result = 0;
      } else {
        result = -1;
      }
    } else { // both lists have items
      // compare the first entry; assumption is that lists are already sorted
      result = list1.get(0).getValue().compareTo(list2.get(0).getValue());
    }
    return result;
  }
}
