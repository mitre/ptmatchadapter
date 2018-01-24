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

import org.hl7.fhir.instance.model.Bundle;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.ResourceType;
import org.hl7.fhir.instance.model.Bundle.BundleType;
import org.hl7.fhir.instance.model.Coding;
import org.hl7.fhir.instance.model.MessageHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public class RecordMatchRequestPassFilter {
  private static final Logger LOG = LoggerFactory.getLogger(RecordMatchRequestPassFilter.class);

  private String recordMatchEventCode = "record-match";
  private String recordMatchEventSpace = "http://github.com/mitre/ptmatch/fhir/message-events";

  /**
   * 
   * @param resource
   * @return true when the given resource is a MessageHeader with the event-code
   *         that corresponds to a record-match request.
   */
  public boolean isRecordMatchRequest(Resource resource) {
    boolean isRecordMatch = false;

    final ResourceType resType = resource.getResourceType();
    if (resType == ResourceType.Bundle) {
      final Bundle bundle = (Bundle) resource;
      LOG.debug("bundle id: {}, type: {} ", bundle.getId(), bundle.getType());

      if (BundleType.MESSAGE.equals(bundle.getType())) {
        try {
          final MessageHeader msgHdr = (MessageHeader) bundle.getEntry().get(0).getResource();
          // Verify this message is not a response
          if (msgHdr.getResponse().isEmpty()) {
            final Coding evtCoding = msgHdr.getEvent();
            /// if event code and name space match expected values
            if (recordMatchEventCode.equals(evtCoding.getCode())
                && recordMatchEventSpace.equals(evtCoding.getSystem())) {
              isRecordMatch = true;
              LOG.debug("PASS Record-Match Request");
            } else {
              LOG.warn("Unsupported Msg Type: event: {}, space: {}", evtCoding.getCode(), evtCoding.getSystem());
            }
          } else {
            LOG.trace("Msg Hdr Response is not empty {}", msgHdr.getResponse());
            LOG.trace("Msg Hdr Response identifier: {}", msgHdr.getResponse().getIdentifier());
          }
        } catch (Exception e) {
          LOG.error(String.format("Unexpected resource type: %s, bundle id: %s",
              bundle.getEntry().get(0).getResource().getResourceType(), bundle.getId()));
          return false;
        }
      }
    }
    return isRecordMatch;
  }
}
