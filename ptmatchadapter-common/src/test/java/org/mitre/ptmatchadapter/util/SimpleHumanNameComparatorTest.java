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

import org.hl7.fhir.instance.model.HumanName;
import org.hl7.fhir.instance.model.HumanName.NameUse;
import org.junit.Test;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public class SimpleHumanNameComparatorTest {

  @Test
  public void testCompare1() {
    HumanName name1 = 
        HumanNameUtil.newHumanName("Alan Airhead", new String[] {"Airhead"}, new String [] {"Alan"});
    HumanName name2 = 
        HumanNameUtil.newHumanName("Zach the Zebra", new String[] {"Zebra"}, new String [] {"Zachary"});
    
    SimpleHumanNameComparator shc = new SimpleHumanNameComparator();
    assertTrue(shc.compare(name1, name2) < 0);
    assertEquals(0, shc.compare(name1, name1));
    assertEquals(0, shc.compare(name2, name2));
    assertTrue(shc.compare(name2, name1) > 0);
  }

  @Test
  public void testCompareUse() {
    HumanName name1 = 
        HumanNameUtil.newHumanName("Daffy Duck", new String[] {"Duck"}, new String [] {"Daffy"}, null, NameUse.OFFICIAL);
    HumanName name2 = 
        HumanNameUtil.newHumanName("Zach the Zebra", new String[] {"Zebra"}, new String [] {"Zachary"}, null, NameUse.USUAL);
    HumanName name3 = 
        HumanNameUtil.newHumanName("Zach the Zebra", new String[] {"Zebra"}, new String [] {"Zachary"}, null, NameUse.OFFICIAL);
    HumanName name4 = 
        HumanNameUtil.newHumanName("Zach the Zebra", new String[] {"Zebra"}, new String [] {"Zachary"});
    
    SimpleHumanNameComparator shc = new SimpleHumanNameComparator();
    assertTrue(shc.compare(name1, name2) < 0);
    assertEquals(0, shc.compare(name1, name1));
    assertEquals(0, shc.compare(name2, name2));
    assertTrue(shc.compare(name2, name1) > 0);
    assertTrue(String.valueOf(shc.compare(name2, name3)), shc.compare(name2, name3) < 0);
    assertTrue(String.valueOf(shc.compare(name3, name4)), shc.compare(name3, name4) > 0);
  }

  @Test
  public void testCompareGiven() {
    HumanName name1 = 
        HumanNameUtil.newHumanName("Daffy Duck", new String[] {"Duck"}, new String [] {"Daffy"}, null, NameUse.OFFICIAL);
    HumanName name2 = 
        HumanNameUtil.newHumanName(null, new String[] {"Duck"}, new String [] {"Louie"}, null, NameUse.OFFICIAL);
    HumanName name3 = 
        HumanNameUtil.newHumanName(null, new String[] {"Duck"}, new String [] {"Huey"}, null, NameUse.OFFICIAL);

    SimpleHumanNameComparator shc = new SimpleHumanNameComparator();
    assertTrue(shc.compare(name1, name2) < 0);
    assertTrue(shc.compare(name2, name1) > 0);
    assertTrue(shc.compare(name2, name3) > 0);
  }

  
}
