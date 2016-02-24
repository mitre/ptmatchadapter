/**
 * Resource Retriever - an application that retrieves FHIR Resources
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.method.SearchStyleEnum;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public class ResourceRetriever {
  private static final Logger LOG    = LoggerFactory.getLogger(ResourceRetriever.class);

  private IGenericClient    client;

  
  public Bundle doSearch(String queryExpr) {
    Bundle results = null;

    LoggingInterceptor loggingInterceptor = null; 
    if (LOG.isDebugEnabled()) {
      loggingInterceptor = new LoggingInterceptor(true);
      client.registerInterceptor(loggingInterceptor);
    }

    LOG.info("query expr: {}", queryExpr);
    try {
      // Prepare search query for messages
      final StringBuilder sb = new StringBuilder(100);
      sb.append(queryExpr);

      final IQuery<Bundle> query = client.search()
          .byUrl(sb.toString())
          .usingStyle(SearchStyleEnum.POST)
          .returnBundle(Bundle.class);

      // Perform a search
      results = query.execute();

    } catch (BaseServerResponseException e) {
      LOG.warn(String.format("Error response from server.  code: %d, %s", e.getStatusCode(), e.getMessage()));
    } catch (Exception e) {
      LOG.warn(String.format("Unable to retrieve messages: %s", e.getMessage()), e);
    } finally {
      if (loggingInterceptor != null) {
        client.unregisterInterceptor(loggingInterceptor);
      }
    }

    return results;
  }


  /**
   * @return the client
   */
  public final IGenericClient getClient() {
    return client;
  }

  /**
   * @param client the client to set
   */
  public final void setClient(IGenericClient client) {
    this.client = client;
  }
}
