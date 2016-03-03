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

import org.hl7.fhir.instance.model.BaseResource;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public class ResourceSerializer {
  private static final Logger LOG = LoggerFactory.getLogger(ResourceSerializer.class);
  
  private static final String XML = "xml";

  private String format = "json";

  private boolean prettyPrint = true;

  private FhirContext fhirContext;

  public ResourceSerializer(FhirContext fhirContext) {
    setFhirContext(fhirContext);
  }
  
  public String toString(BaseResource r) {
    final IParser parser = getParser();

    return parser.encodeResourceToString(r);
  }

  public Resource toResource(String str) {
    Resource result = null;
    final IParser parser = getParser();

    final IBaseResource r = parser.parseResource(str);
    if (r instanceof Resource) {
      result = (Resource) r;
    }
    return result;
  }

  private IParser getParser() {
    IParser parser;

    if (XML.equalsIgnoreCase(format)) {
      parser = fhirContext.newXmlParser().setPrettyPrint(prettyPrint);
    } else {
      // use JSON by default
      parser = fhirContext.newJsonParser().setPrettyPrint(prettyPrint);
    }
    return parser;
  }

  /**
   * @return the format
   */
  public final String getFormat() {
    return format;
  }

  /**
   * @param format
   *          the format to set
   */
  public final void setFormat(String format) {
    this.format = format;
  }

  /**
   * @return the prettyPrint
   */
  public final boolean isPrettyPrint() {
    return prettyPrint;
  }

  /**
   * @param prettyPrint
   *          the prettyPrint to set
   */
  public final void setPrettyPrint(boolean prettyPrint) {
    this.prettyPrint = prettyPrint;
  }

  /**
   * @return the fhirContext
   */
  public final FhirContext getFhirContext() {
    return fhirContext;
  }

  /**
   * @param fhirContext
   *          the fhirContext to set
   */
  public final void setFhirContext(FhirContext fhirContext) {
    this.fhirContext = fhirContext;
  }
}
