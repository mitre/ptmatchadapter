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

import org.hl7.fhir.instance.model.ContactPoint;
import org.hl7.fhir.instance.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.instance.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.instance.model.Period;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public class ContactPointBuilder {
  private ContactPointSystem system = null;
  private String value = null;
  private ContactPointUse use = null;
  private int rank = 0;
  private Period period = null;

  public ContactPointBuilder() {
  }

  public ContactPointBuilder phone(String value) {
    system = ContactPointSystem.PHONE;
    this.value = value;
    return this;
  }

  public ContactPointBuilder fax(String value) {
    system = ContactPointSystem.FAX;
    this.value = value;
    return this;
  }

  public ContactPointBuilder email(String value) {
    system = ContactPointSystem.EMAIL;
    this.value = value;
    return this;
  }

  public ContactPointBuilder pager(String value) {
    system = ContactPointSystem.PAGER;
    this.value = value;
    return this;
  }

  public ContactPointBuilder home() {
    use = ContactPointUse.HOME;
    return this;
  }

  public ContactPointBuilder work() {
    use = ContactPointUse.WORK;
    return this;
  }

  public ContactPointBuilder old() {
    use = ContactPointUse.OLD;
    return this;
  }

  public ContactPointBuilder mobile() {
    use = ContactPointUse.MOBILE;
    return this;
  }

  public ContactPointBuilder temp() {
    use = ContactPointUse.TEMP;
    return this;
  }

  public ContactPointBuilder rank(int val) {
    if (rank <= 0) {
      throw new IllegalArgumentException("Value must be a positive integer");
    }
    rank = val;
    return this;
  }

  public ContactPointBuilder period(Period period) {
    this.period = period;
    return this;
  }

  public ContactPoint build() {
    ContactPoint contact = new ContactPoint();

    if (system != null) {
      contact.setSystem(system);
    }
    if (value != null) {
      contact.setValue(value);
    }
    if (use != null) {
      contact.setUse(use);
    }
    if (rank > 0) {
      contact.setRank(rank);
    }
    if (period != null) {
      contact.setPeriod(period);
    }
    return contact;
  }
}
