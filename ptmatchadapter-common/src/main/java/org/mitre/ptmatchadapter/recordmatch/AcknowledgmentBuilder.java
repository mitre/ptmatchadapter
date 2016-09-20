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

package org.mitre.ptmatchadapter.recordmatch;

import java.util.Date;
import java.util.UUID;

import org.bson.types.ObjectId;

import org.hl7.fhir.instance.model.Bundle;
import org.hl7.fhir.instance.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.instance.model.Bundle.BundleType;
import org.hl7.fhir.instance.model.MessageHeader;
import org.hl7.fhir.instance.model.MessageHeader.MessageDestinationComponent;
import org.hl7.fhir.instance.model.MessageHeader.MessageHeaderResponseComponent;
import org.hl7.fhir.instance.model.MessageHeader.MessageSourceComponent;
import org.hl7.fhir.instance.model.MessageHeader.ResponseType;
import org.mitre.ptmatchadapter.util.ResourceSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constructs a response message that constructs an acknowledgement message for
 * the given request message.
 * 
 * @author Michael Los, mel@mitre.org
 *
 */
public class AcknowledgmentBuilder {
  private static final Logger LOG = LoggerFactory
      .getLogger(AcknowledgmentBuilder.class);


  private String sourceName = "Unknown";

  private String sourceEndpointUri = "http://acme.com/record-matcher";

  public Bundle build(Bundle request) {
    if (BundleType.MESSAGE.equals(request.getType())) {
      final Bundle ackMsg = new Bundle();

      final ObjectId id = new ObjectId();
      ackMsg.setId(id.toHexString());

      ackMsg.setType(BundleType.MESSAGE);

      final BundleEntryComponent msgHdrEntry = new BundleEntryComponent();
      msgHdrEntry.setFullUrl(UUID.randomUUID().toString());
      final MessageHeader msgHdr = buildMessageHeader(request);
      msgHdrEntry.setResource(msgHdr);
      ackMsg.addEntry(msgHdrEntry);
      return ackMsg;
    } else {
      throw new IllegalArgumentException("Bundle expected to have type, message");
    }
  }

  private MessageHeader buildMessageHeader(Bundle request) {
    // Extract the Message Header from the Request
    final MessageHeader reqMsgHdr = (MessageHeader) request.getEntry().get(0)
        .getResource();

    final MessageHeader msgHdr = new MessageHeader();
    final ObjectId id = new ObjectId();
    msgHdr.setId(id.toHexString());
    msgHdr.setTimestamp(new Date());
    msgHdr.setEvent(reqMsgHdr.getEvent());

    final MessageSourceComponent src = new MessageSourceComponent();
    src.setName(sourceName);
    src.setEndpoint(sourceEndpointUri);
    msgHdr.setSource(src);

    // the source of the request is our destination
    final MessageDestinationComponent dest = new MessageDestinationComponent();
    dest.setEndpoint(reqMsgHdr.getSource().getEndpoint());
    dest.setName(reqMsgHdr.getSource().getName());
    msgHdr.addDestination(dest);

    final MessageHeaderResponseComponent resp = new MessageHeaderResponseComponent();
    // This library prefixes identifier w/ 'MessageHeader/', so strip that off
    final String idStr = reqMsgHdr.getId();
    LOG.info("Request Message Header ID: {}", idStr);
    if (idStr != null) {
      int pos = idStr.indexOf("/");
      resp.setIdentifier(idStr.substring(pos + 1));
    } else {
      resp.setIdentifier(idStr);
    }
    resp.setCode(ResponseType.OK);
    msgHdr.setResponse(resp);

    return msgHdr;
  }

  /**
   * @return the sourceEndpointUri
   */
  public final String getSourceEndpointUri() {
    return sourceEndpointUri;
  }

  /**
   * @param endpointUri
   *          the endpointUri to set
   */
  public final void setSourceEndpointUri(String endpointUri) {
    this.sourceEndpointUri = endpointUri;
  }

  /**
   * @return the sourceName
   */
  public final String getSourceName() {
    return sourceName;
  }

  /**
   * @param sourceName
   *          the sourceName to set
   */
  public final void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }
}
