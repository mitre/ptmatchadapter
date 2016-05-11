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

import java.util.List;

import org.hl7.fhir.instance.model.Bundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.mitre.ptmatchadapter.model.ServerAuthorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public class ResultSender {
  private static final Logger LOG = LoggerFactory.getLogger(ResultSender.class);

  private IGenericClient client;

  private List<ServerAuthorization> serverAuthorizations;

  /**
   * Submits the given Bundle to a pre-configured FHIR Server using PUT.
   * 
   * @param bundle
   *          Bundle containing record match results
   */
  public void sendMessage(Bundle bundle) {
    final ServerAuthorization serverAuthorization =
        findServerAuthorization(client.getServerBase());

    BearerTokenAuthInterceptor authInterceptor = null;
    if (serverAuthorization != null) {
      // register authorization interceptor with the client
      LOG.info("assigning bearing token interceptor, {}", serverAuthorization.getAccessToken() );
      authInterceptor = new BearerTokenAuthInterceptor(serverAuthorization.getAccessToken());
      client.registerInterceptor(authInterceptor);
    }

    LoggingInterceptor loggingInterceptor = null;
    if (LOG.isDebugEnabled()) {
      loggingInterceptor = new LoggingInterceptor(true);
      client.registerInterceptor(loggingInterceptor);
    }

    MethodOutcome outcome;
    try {
      // Invoke the server update method
      outcome = client.update().resource(bundle).encodedJson().execute();
    } finally {
      if (authInterceptor != null) {
        // unregister authorization interceptor with the client
        client.unregisterInterceptor(authInterceptor);
      }
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

  private ServerAuthorization findServerAuthorization(String serverBase) {
    if (serverAuthorizations != null) {
      for (ServerAuthorization sa : serverAuthorizations) {
        LOG.info("findServerAuth serverUrl: {}  {}", sa.getServerUrl(), serverBase);
        try {
          if (sa.getServerUrl().equals(serverBase)) {
            return sa;
          }
        } catch (NullPointerException e) {
          // should never happen
          LOG.warn("NULL Server URL found for server authorization: {}", sa.getTitle());
        }
      }
    }
    return null;
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

  /**
   * @return the serverAuthorizations
   */
  public final List<ServerAuthorization> getServerAuthorizations() {
    return serverAuthorizations;
  }


  /**
   * @param serverAuthorizations the serverAuthorizations to set
   */
  public final void setServerAuthorizations(
      List<ServerAuthorization> serverAuthorizations) {
    this.serverAuthorizations = serverAuthorizations;
  }

}
