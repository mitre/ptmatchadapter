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

import org.hl7.fhir.instance.model.StringType;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public class StringTypeComparator
    implements Comparator<StringType>, Serializable {

  /**
   * serial version identifier
   */
  private static final long serialVersionUID = 8425679505734235922L;

  /**
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  @Override
  public int compare(StringType st1, StringType st2) {
    int result = 0;
    
    if (st1 == null && st2 != null) {
      result = -1;
    } else if (st1 == null && st2 == null) {
      result = 0;
    } else if (st2 == null) {
      result = 1;
    } else { // neither are null
      result = st1.getValue().compareTo(st2.getValue());
    }
    return result;
  }

}
