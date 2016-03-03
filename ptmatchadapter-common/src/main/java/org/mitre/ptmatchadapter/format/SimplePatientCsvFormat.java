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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.Pointer;
import org.hl7.fhir.instance.model.ContactPoint;
import org.hl7.fhir.instance.model.HumanName;
import org.hl7.fhir.instance.model.Identifier;
import org.hl7.fhir.instance.model.Patient;
import org.hl7.fhir.instance.model.StringType;
import org.hl7.fhir.instance.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.instance.model.ContactPoint.ContactPointUse;
import org.mitre.ptmatchadapter.util.ContactPointUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Formats a Patient FHIR Resource as a line of comma-separated values.
 * 
 * The field order is:
 * <ul>
 * <li>id</li>
 * <li>identifiers</li>
 * <li>name parts</li>
 * <li>gender</li>
 * <li>date of birth</li>
 * <li>mobile phone number</li>
 * <li>work phone</li>
 * <li>home phone</li>
 * <li>work email address</li>
 * <li>home email address</li>
 * </ul>
 * 
 * @author Michael Los, mel@mitre.org
 *
 */
public class SimplePatientCsvFormat {
  private static final Logger LOG = LoggerFactory
      .getLogger(SimplePatientCsvFormat.class);

  private static final int INITIAL_ROW_LENGTH = 1000;
  private static final String COMMA = ",";
  private static final String DOUBLE_QUOTE = "\"";
  private static final String UNDERSCORE = "_";

  private String[] identifierSystems = { "SSN" };

  private String[] nameUses = { "", "official", "usual" };
  private String[] nameParts = { TEXT_NAME_PART, "family", "suffix", "given" };

  public static final String TEXT_NAME_PART = "text";

  private ContactPointSystem [] telecomSystems = {ContactPointSystem.PHONE, ContactPointSystem.EMAIL};
  private ContactPointUse [] telecomUses = {ContactPointUse.WORK, ContactPointUse.HOME};

  public static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
  
  /**
   * Returns column titles as a comma-separated strings.
   * 
   * @return
   */
  public String getHeaders() {
    final StringBuilder sb = new StringBuilder(INITIAL_ROW_LENGTH);

    // resource Id
    sb.append("id");
    sb.append(COMMA);

    // Identifiers
    for (String sysName : identifierSystems) {
      sb.append(DOUBLE_QUOTE);
      sb.append("identifier_");
      sb.append(sysName);
      sb.append(DOUBLE_QUOTE);
      sb.append(COMMA);
    }
    // Name fields. Title formed as name_use_part (e.g., name_official_family)
    String sep = "";
    for (String use : nameUses) {
      for (String part : nameParts) {
        sb.append(sep);
        sb.append("name_");
        sb.append(use);
        sb.append(UNDERSCORE);
        sb.append(part);
        sep = COMMA;
      }
    }

    sb.append(COMMA);
    sb.append("gender");

    sb.append(COMMA);
    sb.append("DOB");


    sb.append(COMMA);
    sb.append("telecom_phone_mobile");

    for (ContactPointSystem system : telecomSystems) {
      for (ContactPointUse use : telecomUses) {
        sb.append(COMMA);
        sb.append("telecom_");
        sb.append(system.toString().toLowerCase());
        sb.append(UNDERSCORE);
        sb.append(use.toString().toLowerCase());
      }
    }

    return sb.toString();
  }

  /**
   * Returns supported Patient properties as a string of comma-separated values.
   * Values of String fields are enclosed by double-quotes.
   * 
   * @param patient
   *          the patient resource to serialize to a CSV string
   * @return
   */
  public String toCSV(Patient patient) {
    final StringBuilder sb = new StringBuilder(INITIAL_ROW_LENGTH);
    JXPathContext patientCtx = JXPathContext.newContext(patient);

    // resource id
    if (patient.getId() != null) {
      sb.append(patient.getId());
    }
    sb.append(COMMA);

    // identifiers of interest
    for (String sysName : identifierSystems) {
      Pointer ptr = patientCtx.getPointer("identifier[system='" + sysName + "']");
      Identifier id = (Identifier) ptr.getValue();
      if (id != null) {
        sb.append(id.getValue());
      }
      sb.append(COMMA);
    }

    // Extract Name Parts of interest
    for (String use : nameUses) {
      Pointer ptr;
      if (use.isEmpty()) {
        ptr = patientCtx.getPointer("name[not(use)]");
      } else {
        ptr = patientCtx.getPointer("name[use='" + use + "']");
      }
      HumanName name = (HumanName) ptr.getValue();
      if (name != null) {
        JXPathContext nameCtx = JXPathContext.newContext(ptr.getValue());
        for (String part : nameParts) {
          sb.append(DOUBLE_QUOTE);
          if (TEXT_NAME_PART.equals(part)) {
            Object val = nameCtx.getValue(part);
            if (val != null) {
              sb.append(val.toString());
            }
          } else {
            // other supported parts return lists of string types 
            Object namePart = nameCtx.getValue(part);
            if (namePart instanceof List<?>) {
              List <StringType> partList = (List<StringType>) namePart;
              if (partList.size() > 0) {
                sb.append(partList.get(0).getValue());
              }
            }
          }
          sb.append(DOUBLE_QUOTE);
          sb.append(COMMA);
        }
      } else {
        // add blank sections for the name parts
        for (int i = 0; i < nameParts.length; i++) {
          sb.append(COMMA);
        }
      }
    }

    // Gender
    sb.append(patient.getGender().toString());
    sb.append(COMMA);

    // Date of Birth
    Date dob = patient.getBirthDate();
    if (dob != null) {
      sb.append(dateFormat.format(dob));
    }

    sb.append(COMMA);
    sb.append(buidContactInfo(patient));
    
    return sb.toString();
  }

  /**
   * Concatenate contact point information as a string of comma-separated values.
   * 
   * @param patient
   * @return
   */
  private String buidContactInfo(Patient patient) {
    final StringBuilder sb = new StringBuilder(100);
    
    List<ContactPoint> matches = ContactPointUtil.find(
        patient.getTelecom(), ContactPointSystem.PHONE, ContactPointUse.MOBILE);

    if (matches.size() > 0) {
      sb.append(matches.get(0).getValue());
    }
    
    for (ContactPointSystem system : telecomSystems) {
      for (ContactPointUse use : telecomUses) {
        matches = ContactPointUtil.find(patient.getTelecom(), system, use);
        sb.append(COMMA);
        if (matches.size() > 0) {
          sb.append(matches.get(0).getValue());
        }
      }
    }
    
    return sb.toString();
  }

  /**
   * @param nameParts
   *          the nameParts to set
   */
  public final void setNameParts(String[] nameParts) {
    this.nameParts = new String[nameParts.length];
    for (int i = 0; i < nameParts.length; i++) {
      this.nameParts[i] = nameParts[i].toLowerCase();
    }
  }

  /**
   * @return the nameUses
   */
  public final String[] getNameUses() {
    return nameUses;
  }

  /**
   * @param nameUses
   *          the nameUses to set
   */
  public final void setNameUses(String[] nameUses) {
    this.nameUses = new String[nameUses.length];
    for (int i = 0; i < nameUses.length; i++) {
      this.nameUses[i] = nameUses[i].toLowerCase();
    }
  }

}
