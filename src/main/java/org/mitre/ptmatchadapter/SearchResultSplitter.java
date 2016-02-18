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
package org.mitre.ptmatchadapter;

import java.util.LinkedList;
import java.util.List;

import org.hl7.fhir.instance.model.Bundle;
import org.hl7.fhir.instance.model.Bundle.BundleEntryComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hl7.fhir.instance.model.Resource;
import org.springframework.stereotype.Component;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
@Component
public class SearchResultSplitter {
  private static final Logger LOG = LoggerFactory
      .getLogger(SearchResultSplitter.class);

  /**
   * Breaks apart the given bundle into a list of Resources.
   * 
   * @param bundle
   *          the payload of the incoming message
   * @return a list containing each resource in the Bundle
   */
  public List<Resource> splitBundle(Bundle bundle) {
    final List<Resource> resources = new LinkedList<Resource>();
    
    if (bundle != null) {
      final List<BundleEntryComponent> entries = bundle.getEntry();

      for (BundleEntryComponent entry : entries) {
        Resource r = entry.getResource();
        if (r != null) {
          LOG.debug("processing resource: " + r.getId());
          resources.add(r);
        } else {
          LOG.warn("Entry does not have a resource, fullUrl: {}",
              entry.getFullUrl());
        }
      }
    }
    
    return resources;
  }
}
