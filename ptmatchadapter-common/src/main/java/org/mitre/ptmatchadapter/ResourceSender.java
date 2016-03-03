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

import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.client.exceptions.FhirClientInappropriateForServerException;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public class ResourceSender {
  private static final Logger LOG = LoggerFactory.getLogger(ResourceSender.class);

  private IGenericClient client;

  /**
   * Submits the given Resource to a pre-configured FHIR Server using PUT.
   * The given resource must contain an id.
   * 
   * @param resource
   *          Resource containing record match results
   */
  public void update(Resource resource) {
    LoggingInterceptor loggingInterceptor = null;
    if (LOG.isDebugEnabled()) {
      loggingInterceptor = new LoggingInterceptor(true);
      client.registerInterceptor(loggingInterceptor);
    }

    MethodOutcome outcome;
    try {
      // Invoke the server update method
      outcome = client.update().resource(resource).encodedJson().execute();
    } finally {
      if (loggingInterceptor != null) {
        client.unregisterInterceptor(loggingInterceptor);
      }
    }

    // The MethodOutcome object will contain information about the
    // response from the server, including the ID of the created
    // resource, the OperationOutcome response, etc. (assuming that
    // any of these things were provided by the server! They may not
    // always be)
    IBaseOperationOutcome oo = outcome.getOperationOutcome();
    if (oo != null) {
      LOG.info("Result of Put to Server, OO: " + oo.toString());
    } else {
      LOG.info("Result of Put to Server, Operation Outcome is  null");
    }
    IBaseResource returned = outcome.getResource();
    if (!returned.isEmpty()) {
      LOG.info("Result of Put to Server, resource type: " + returned.toString());
    } else {
      LOG.info("Result of Put to Server, Resource is empty");
    }
    IIdType id = outcome.getId();
    if (id != null) {
      LOG.info("Result of Put to Server, ID: " + id.getValue());
    } else {
      LOG.info("Result of Put to Server, ID is null");
    }
  }

  public void create(Resource resource) {
    LoggingInterceptor loggingInterceptor = null;
    if (LOG.isDebugEnabled()) {
      loggingInterceptor = new LoggingInterceptor(true);
      client.registerInterceptor(loggingInterceptor);
    }

    MethodOutcome outcome;
    try {
      // Invoke the server update method
      outcome = client.create().resource(resource).encodedJson().execute();
    } catch (FhirClientConnectionException e) {
      LOG.error("Unable to connect to the FHIR Server: {}", e.getMessage());
      return;
    } catch (FhirClientInappropriateForServerException e) {
      LOG.error("Client is not compatible with the FHIR Server. {}", e.getMessage());
      return;
    } finally {
      if (loggingInterceptor != null) {
        client.unregisterInterceptor(loggingInterceptor);
      }
    }

    // The MethodOutcome object will contain information about the
    // response from the server, including the ID of the created
    // resource, the OperationOutcome response, etc. (assuming that
    // any of these things were provided by the server! They may not
    // always be)
    IBaseOperationOutcome oo = outcome.getOperationOutcome();
    if (oo != null) {
      LOG.info("Result of POST to Server, OO: " + oo.toString());
    } else {
      LOG.info("Result of POST to Server, Operation Outcome is  null");
    }
    IBaseResource returned = outcome.getResource();
    if (!returned.isEmpty()) {
      LOG.info("Result of POST to Server, resource type: " + returned.toString());
    } else {
      LOG.info("Result of POST to Server, Resource is empty");
    }
    IIdType id = outcome.getId();
    if (id != null) {
      LOG.info("Result of POST to Server, ID: " + id.getValue());
    } else {
      LOG.info("Result of POST to Server, ID is null");
    }
  }

  /**
   * @return the client
   */
  public final IGenericClient getClient() {
    return client;
  }

  /**
   * @param client
   *          the client to set
   */
  public final void setClient(IGenericClient client) {
    this.client = client;
  }

}
