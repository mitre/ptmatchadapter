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
package org.mitre.ptmatchadapter.format;

import static org.junit.Assert.*;

import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.Pointer;
import org.bson.types.ObjectId;
import org.hl7.fhir.instance.model.HumanName;
import org.hl7.fhir.instance.model.HumanName.NameUse;
import org.hl7.fhir.instance.model.Identifier;
import org.hl7.fhir.instance.model.Identifier.IdentifierUse;
import org.hl7.fhir.instance.model.Patient;
import org.hl7.fhir.instance.model.ContactPoint;
import org.hl7.fhir.instance.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.instance.model.ContactPoint.ContactPointSystemEnumFactory;
import org.hl7.fhir.instance.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.instance.model.Enumerations.AdministrativeGender;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mitre.ptmatchadapter.util.ContactPointBuilder;
import org.mitre.ptmatchadapter.util.ContactPointUtil;
import org.mitre.ptmatchadapter.util.HumanNameUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public class SimplePatientCsvFormatTest {
  private static final Logger LOG = LoggerFactory
      .getLogger(SimplePatientCsvFormatTest.class);

  private static SecureRandom rand;

  @BeforeClass
  public static void setUpBeforeClass() {
    rand = new SecureRandom();
  }

  @Test
  public void testGetHeaders() {
    final SimplePatientCsvFormat obj = new SimplePatientCsvFormat();

    obj.setNameUses(new String[] {"", "official", "usual"});
    obj.setNameParts(new String[] { "text", "family", "given" });
    assertNotNull("col titles", obj.getHeaders());

    final StringBuilder sb = new StringBuilder(200);
    sb.append("id,\"identifier_SSN\",");
    sb.append("name__text,name__family,name__given,");
    sb.append("name_official_text,name_official_family,name_official_given,");
    sb.append("name_usual_text,name_usual_family,name_usual_given,");
    sb.append("gender,DOB,telecom_phone_mobile,");
    sb.append("telecom_phone_work,telecom_phone_home,telecom_email_work,telecom_email_home");

    assertEquals("col titles", sb.toString(), obj.getHeaders());
  }

  @Test
  public void testToCsv() {
    final SimplePatientCsvFormat fmt = new SimplePatientCsvFormat();

    Patient patient = newPatient();

    
    JXPathContext patientCtx = JXPathContext.newContext(patient);
    Pointer ptr = patientCtx.getPointer("identifier[system='SSN']");
    assertNotNull(ptr);
    Identifier id = (Identifier) ptr.getValue();
    if (id != null) {
      assertNotNull("ssn", id.getValue());
    }
    Object obj1 = ptr.getNode();

    ptr = patientCtx.getPointer("name[use='official']");
    assertNotNull(ptr);
    Object obj2 = ptr.getValue();
    ptr = patientCtx.getPointer("name[not(use)]");
    assertNotNull(ptr);
    obj2 = ptr.getValue();
    
    String str = fmt.toCSV(patient);
    LOG.info("CSV: {}", str);
    assertTrue(str.length() > 0);
    // Ensure the literal, null doesn't appear
    assertTrue(!str.contains("null"));
    // Ensure there is no sign of array delimiters
    assertTrue(!str.contains("["));
    // Ensure line doesn't end with a comma
    assertTrue(!str.endsWith(","));
    assertTrue(str.contains("Smith"));
    
    List<ContactPoint> matches = ContactPointUtil.find(
        patient.getTelecom(), ContactPointSystem.PHONE, ContactPointUse.WORK);
    assertNotNull(matches);
    assertNotNull(matches.get(0));
  }

  
  public Patient newPatient() {
    final Patient p = new Patient();

    if (rand.nextBoolean()) {
      ObjectId id = new ObjectId();
      p.setId(id.toHexString());
    } else {
      p.setId(UUID.randomUUID().toString());
    }

    p.setGender(rand.nextBoolean() ? AdministrativeGender.MALE
        : AdministrativeGender.FEMALE);
    
    final String ssn =
        String.format("%03d-%02d-%04d", rand.nextInt(999)+1, rand.nextInt(99)+1, rand.nextInt(9999)+1 );
    
    p.addIdentifier(newIdentifier(IdentifierUse.OFFICIAL, "SSN", ssn));

    HumanName bsmith= 
        HumanNameUtil.newHumanName("Bill Smith", new String[] {"Smith"}, new String [] {"Bill"}, null, NameUse.USUAL);
    HumanName wsmith = 
        HumanNameUtil.newHumanName("W Smith", new String[] {"Smith"}, new String [] {"Bill"}, new String [] {"Jr"});
    p.addName(bsmith);
    p.addName(wsmith);

    long msec = (long) (rand.nextFloat() * System.currentTimeMillis());
    p.setBirthDate(new Date(msec));
    
    p.addTelecom((new ContactPointBuilder()).email("bsmith@email.net").home().build());
    p.addTelecom((new ContactPointBuilder()).email("bsmith@workemail.com").work().build());
    
    p.addTelecom((new ContactPointBuilder()).phone("248.555.0743").home().build());
    p.addTelecom((new ContactPointBuilder()).phone("248.557.1243").work().build());
    
    return p;
  }

  public Identifier newIdentifier(IdentifierUse use, String system, String value) {
    final Identifier id = new Identifier();

    id.setSystem(system);
    id.setUse(use);
    id.setValue(value);
    return id;
  }

}
