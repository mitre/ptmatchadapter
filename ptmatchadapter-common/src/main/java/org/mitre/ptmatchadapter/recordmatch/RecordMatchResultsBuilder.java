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
import java.util.List;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.hl7.fhir.instance.model.Bundle;
import org.hl7.fhir.instance.model.OperationOutcome;
import org.hl7.fhir.instance.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.instance.model.Bundle.BundleType;
import org.hl7.fhir.instance.model.MessageHeader.MessageDestinationComponent;
import org.hl7.fhir.instance.model.MessageHeader.MessageHeaderResponseComponent;
import org.hl7.fhir.instance.model.MessageHeader.MessageSourceComponent;
import org.hl7.fhir.instance.model.MessageHeader.ResponseType;
import org.hl7.fhir.instance.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.instance.model.MessageHeader;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public class RecordMatchResultsBuilder {

  private Bundle requestMsg;
  private OperationOutcome.IssueSeverity outcomeSeverity = OperationOutcome.IssueSeverity.INFORMATION;
  private OperationOutcome.IssueType outcomeCode  = OperationOutcome.IssueType.INFORMATIONAL;
  private String outcomeDiagnostics;
  private String sourceName;
  private String sourceEndpoint;
  private ResponseType responseCode;

  public RecordMatchResultsBuilder(Bundle requestMsg, ResponseType respCode) {
    if (requestMsg == null) {
      throw new IllegalArgumentException("Null Request Message Found");
    } else if (BundleType.MESSAGE.equals(requestMsg.getType())
        && ((MessageHeader) requestMsg.getEntry().get(0).getResource()) != null) {
      this.requestMsg = requestMsg;
    } else {
      throw new IllegalArgumentException("Invalid Request Message Found");
    }
    if (respCode == null) {
      throw new IllegalArgumentException("Null Response Code Found");
    }
    this.responseCode = respCode;
  }

  public Bundle build() {
    // check parameters are valid; exception will be thrown, if invalid found
    checkParameters();

    final Bundle resultMsg = new Bundle();

    ObjectId id = new ObjectId();
    resultMsg.setId(id.toHexString());

    resultMsg.setType(BundleType.MESSAGE);

    final BundleEntryComponent msgHdrEntry = new BundleEntryComponent();
    msgHdrEntry.setFullUrl(UUID.randomUUID().toString());
    final MessageHeader msgHdr = buildMessageHeader(requestMsg);
    msgHdrEntry.setResource(msgHdr);
    resultMsg.addEntry(msgHdrEntry);

    final BundleEntryComponent opOutcomeEntry = new BundleEntryComponent();
    msgHdrEntry.setFullUrl(UUID.randomUUID().toString());
    final OperationOutcome opOutcome = buildOperationOutcome();
    opOutcomeEntry.setResource(opOutcome);
    resultMsg.addEntry(opOutcomeEntry);
    
    
    return resultMsg;
  }

  private void checkParameters() {
  }

  private MessageHeader buildMessageHeader(Bundle request) {
    // Extract the Message Header from the Request
    final MessageHeader reqMsgHdr = (MessageHeader) request.getEntry().get(0).getResource();

    final MessageHeader msgHdr = new MessageHeader();
    ObjectId id = new ObjectId();
    msgHdr.setId(id.toHexString());
    msgHdr.setTimestamp(new Date());
    msgHdr.setEvent(reqMsgHdr.getEvent());

    final MessageSourceComponent src = buildSource(reqMsgHdr);
    msgHdr.setSource(src);

    // the source of the request is our destination
    final MessageDestinationComponent dest = new MessageDestinationComponent();
    dest.setEndpoint(reqMsgHdr.getSource().getEndpoint());
    dest.setName(reqMsgHdr.getSource().getName());
    msgHdr.addDestination(dest);

    final MessageHeaderResponseComponent resp = new MessageHeaderResponseComponent();
    // This library prefixes identifier w/ 'MessageHeader/', so strip that off
    final String idStr = reqMsgHdr.getId();
    int pos = idStr.indexOf("/");
    resp.setIdentifier(idStr.substring(pos + 1));
    resp.setCode(responseCode);
    msgHdr.setResponse(resp);

    return msgHdr;
  }

  private MessageSourceComponent buildSource(MessageHeader reqMsgHdr) {
    final MessageSourceComponent src = new MessageSourceComponent();
    String name = sourceName;
    String endpoint = sourceEndpoint;

    if (sourceEndpoint == null) {
      // if only one destination in request message; assume it is us
      final List<MessageDestinationComponent> dests = reqMsgHdr.getDestination();
      if (dests.size() == 1) {
        name = dests.get(0).getName();
        endpoint = dests.get(0).getEndpoint();
      }
    }

    if (name != null && !name.isEmpty()) {
      src.setName(name);
    }
    if (endpoint == null || endpoint.isEmpty()) {
      throw new IllegalStateException("Cannot Determine Source Endpoint for Response Message");
    }
    src.setEndpoint(endpoint);
    return src;
  }
  
  private OperationOutcome buildOperationOutcome() {
    final OperationOutcome opOutcome = new OperationOutcome();

    ObjectId id = new ObjectId();
    opOutcome.setId(id.toHexString());
    
    if (outcomeSeverity != null && outcomeCode != null) {
      final OperationOutcomeIssueComponent  issue = new OperationOutcomeIssueComponent();
      issue.setSeverity(outcomeSeverity);
      issue.setCode(outcomeCode);
      opOutcome.addIssue(issue);
    }
    return opOutcome;
  }

  public RecordMatchResultsBuilder outcomeIssueSeverity(OperationOutcome.IssueSeverity severity) {
    outcomeSeverity = severity;
    return this;
  }

  public RecordMatchResultsBuilder outcomeIssueCode(OperationOutcome.IssueType code) {
    outcomeCode = code;
    return this;
  }

  public RecordMatchResultsBuilder outcomeIssueDiagnostics(String diagnostics) {
    outcomeDiagnostics = diagnostics;
    return this;
  }
}
