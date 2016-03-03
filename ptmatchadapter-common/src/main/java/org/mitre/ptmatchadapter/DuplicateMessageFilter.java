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

import java.util.HashSet;
import java.util.Set;

import org.hl7.fhir.instance.model.Bundle;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.ResourceType;
import org.hl7.fhir.instance.model.Bundle.BundleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public class DuplicateMessageFilter {
  private static final Logger LOG    = LoggerFactory.getLogger(DuplicateMessageFilter.class);

  private Set<String> msgIds = new HashSet<String>();
  
  public boolean isNewMessage(Resource resource) {
    boolean isNew = false;
    
    final ResourceType resType = resource.getResourceType();
    if (resType == ResourceType.Bundle) {
      Bundle bundle = (Bundle) resource;
      LOG.debug("bundle id: {}, type: {} ", bundle.getId(), bundle.getType());

      if (BundleType.MESSAGE.equals(bundle.getType())) {
        // First Entry is expected to be a message header
        final String msgId = bundle.getEntry().get(0).getResource().getId();
        if (!msgIds.contains(msgId)) {
          msgIds.add(msgId);
          isNew = true;
          LOG.info("NEW MESSAGE!   id: {} ", msgId);
        } else {
          LOG.info("REPEAT MESSAGE id: {} ", msgId);
        }
      }
    }
    return isNew;
  }
}
