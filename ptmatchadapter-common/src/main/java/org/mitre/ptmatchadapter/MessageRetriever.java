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

import java.util.Date;
import java.text.SimpleDateFormat;

import org.hl7.fhir.instance.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public class MessageRetriever {
  private static final Logger LOG    = LoggerFactory.getLogger(MessageRetriever.class);

  private IGenericClient    client;

  private String destinationUri;
  
  /** number of milliseconds back in time for which to ask for messages. */
  private int                 period = 30000;

  private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
  
  public Bundle doSearch() {
    Bundle results = null;

    LoggingInterceptor loggingInterceptor = null; 
    if (LOG.isDebugEnabled()) {
      // Note: Logging Intercepter output is at INFO level and based on ca.uhn... tree
      loggingInterceptor = new LoggingInterceptor(true);
      client.registerInterceptor(loggingInterceptor);
    }

    final Date d = new Date(System.currentTimeMillis() - period);

    try {
      // Prepare search query for messages
      final StringBuilder sb = new StringBuilder(100);
      sb.append("Bundle?message.destination-uri=");
      sb.append(getDestinationUri());
      // the lastUpdated() convenience method uses an operator not suppored by 
      // the Intervention Engine FHIR Server so use 'gt' operator manually
//      sb.append("&_lastUpdated=gt");
 //     sb.append(df.format(d));

      final IQuery<Bundle> query = client.search()
      .byUrl(sb.toString())
      //.forResource(Bundle.class)
        //  .encodedJson() // results in _format query parameter, which is not supported by Intervention Engine FHIR Server
         //.lastUpdated(new DateRangeParam(d, null))  // 2/10/16 - IE FHIR Server doesn't support date comparison operator
//          .where((new StringClientParam(Bundle.SP_TYPE)).matches().value(BundleTypeEnum.MESSAGE.toString()))
//          .and(Patient.CAREPROVIDER.hasChainedProperty(Organization.NAME.matches().value(destinationUri)))
//          .and(Patient.CAREPROVIDER.hasChainedProperty(Organization.NAME.matches().value("Health")))
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
   * @return the desinationUri
   */
  public final String getDestinationUri() {
    return destinationUri;
  }

  /**
   * @param desinationUri the desinationUri to set
   */
  public final void setDestinationUri(String uri) {
    this.destinationUri = uri;
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
