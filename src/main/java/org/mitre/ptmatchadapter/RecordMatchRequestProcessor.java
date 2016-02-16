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

import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.instance.model.Bundle;
import org.hl7.fhir.instance.model.MessageHeader.ResponseType;
import org.mitre.ptmatchadapter.recordmatch.RecordMatchResultsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.client.IGenericClient;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
@Component
public class RecordMatchRequestProcessor {
  private static final Logger LOG = 
      LoggerFactory.getLogger(RecordMatchRequestProcessor.class);

  @Autowired
  private ProducerTemplate producer;

  @Autowired
  private IGenericClient    fhirRestClient;

  private String producerEndpointUri;

  public void process(Bundle bundle) {
    
    
    final RecordMatchResultsBuilder builder = new RecordMatchResultsBuilder(bundle, ResponseType.OK);
    builder.outcomeIssueDiagnostics("No Matches Found");
    final Bundle result = builder.build();

    LOG.info("### About to send no match result to endpoint");
    producer.sendBody(getProducerEndpointUri(), result);
  }

  /**
   * @param producer
   *          the producer to set
   */
  public final void setProducer(ProducerTemplate producer) {
    this.producer = producer;
  }


  /**
   * @return the producerEndpointUri
   */
  public final String getProducerEndpointUri() {
    return producerEndpointUri;
  }

  /**
   * @param producerEndpointUri the producerEndpointUri to set
   */
  public final void setProducerEndpointUri(String producerEndpointUri) {
    this.producerEndpointUri = producerEndpointUri;
  }

}
