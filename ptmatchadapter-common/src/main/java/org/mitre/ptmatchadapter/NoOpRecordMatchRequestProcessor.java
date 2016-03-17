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

import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.instance.model.Bundle;
import org.hl7.fhir.instance.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.instance.model.Bundle.BundleType;
import org.hl7.fhir.instance.model.MessageHeader;
import org.hl7.fhir.instance.model.MessageHeader.ResponseType;
import org.hl7.fhir.instance.model.Parameters;
import org.hl7.fhir.instance.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.ResourceType;
import org.mitre.ptmatchadapter.recordmatch.BasicRecordMatchResultsBuilder;
import org.mitre.ptmatchadapter.util.ParametersUtil;
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
public class NoOpRecordMatchRequestProcessor {
  private static final Logger LOG = LoggerFactory
      .getLogger(NoOpRecordMatchRequestProcessor.class);

  private ProducerTemplate producer;

  private IGenericClient fhirRestClient;

  private String producerEndpointUri;

  private static final String MASTER = "master";
  private static final String QUERY = "query";
  private static final String RESOURCE_TYPE = "resourceType";
  private static final String SEARCH_EXPR = "searchExpression";
  private static final String RESOURCE_URL = "resourceUrl";

  /**
   * Constructs invokes a single request for the search queries and then
   * generates a record-match result message that reports zero matches found.
   * The responses to the REST calls for the master and, when provided, query
   * record sets are ignored.
   *
   * @param bundle
   *          record-match request to process
   */
  public void process(Bundle bundle) {
    if (BundleType.MESSAGE.equals(bundle.getType())) {
      final List<BundleEntryComponent> bundleEntries = bundle.getEntry();
      try {
        // the first entry is supposed to be the MessageHeader
        final MessageHeader msgHdr = (MessageHeader) bundleEntries.get(0)
            .getResource();

        Resource masterQryParams = null;
        Resource queryQryParams = null;
        String masterSearchUrl = null;
        String querySearchUrl = null;

        // Find the Parameters resources that contain the search parameters
        // and use those to construct search Urls
        for (BundleEntryComponent entry : bundleEntries) {
          Resource r = entry.getResource();
          LOG.debug("Found Resource type: " + r.getResourceType().toString());
          if (ResourceType.Parameters.equals(r.getResourceType())) {
            Parameters params = (Parameters) r;
            List<ParametersParameterComponent> paramList = params.getParameter();
            ParametersParameterComponent p = ParametersUtil.findByName(paramList,
                "type");
            if (p != null) {
              String val = p.getValue().toString();
              if (val.equalsIgnoreCase(MASTER)) {
                masterQryParams = params;
                masterSearchUrl = buildSearchUrl(params);
              } else if (val.equalsIgnoreCase(QUERY)) {
                queryQryParams = params;
                querySearchUrl = buildSearchUrl(params);
              }
            }
          }
        }

        LOG.info("Params found: master: {} query: {}", (masterQryParams != null),
            (queryQryParams != null));

        if (masterSearchUrl == null) {
          LOG.warn(
              "Required Parameter for master record set is missing, bundle: {}",
              bundle.getId());
          return;
        }

        LoggingInterceptor loggingInterceptor = null;
        if (LOG.isDebugEnabled()) {
          loggingInterceptor = new LoggingInterceptor(true);
          fhirRestClient.registerInterceptor(loggingInterceptor);
        }
        try {
          // Retrieve the data associated with the search urls
          IQuery<Bundle> query = fhirRestClient.search()
              .byUrl(masterSearchUrl).returnBundle(Bundle.class);

          // Perform a search
          final Bundle masterSetResults = query.execute();

          Bundle querySetResults = null;
          if (querySearchUrl != null) {
            // Retrieve the data associated with the search urls
            query = fhirRestClient.search()
                .byUrl(querySearchUrl).returnBundle(Bundle.class);

            // Perform a search
            querySetResults = query.execute();
          }

        } catch (BaseServerResponseException e) {
          LOG.warn(String.format("Error response from server.  code: %d, %s",
              e.getStatusCode(), e.getMessage()));
        } catch (Exception e) {
          LOG.warn(String.format("Unable to retrieve messages: %s", e.getMessage()),
              e);
        } finally {
          if (loggingInterceptor != null) {
            fhirRestClient.unregisterInterceptor(loggingInterceptor);
          }
        }

        final BasicRecordMatchResultsBuilder builder = new BasicRecordMatchResultsBuilder(
            bundle,
            ResponseType.OK);
        builder.outcomeIssueDiagnostics("No Matches Found");
        final Bundle result = builder.build();

        LOG.info("### About to send no match result to endpoint");
        producer.sendBody(getProducerEndpointUri(), result);

      } catch (Exception e) {
        LOG.error("Processing bundle: {}", bundle.getId(), e);
      }
    } else {
      LOG.info("Unsupported Bundle type: {}", bundle.getType());
    }
  }

  /**
   * Constructs a search URL using the information in the Parameters resource.
   *
   * @param params
   *          Parameters resource containing a searchExpression parameter whose
   *          value is a Parameters resource containing a resourceUrl parameter
   *          and other parameters that comprise the query expression.
   * @return search url or an empty string if the url could not be formed
   */
  private String buildSearchUrl(Parameters params) {
    final StringBuilder searchUrl = new StringBuilder(200);

    final List<ParametersParameterComponent> paramList = params.getParameter();

    final ParametersParameterComponent p = ParametersUtil.findByName(paramList,
        SEARCH_EXPR);
    if (p != null) {
      final Resource r = p.getResource();
      if (ResourceType.Parameters.equals(r.getResourceType())) {
        final Parameters searchExprParams = (Parameters) r;
        final List<ParametersParameterComponent> searchExprParamList = searchExprParams
            .getParameter();

        String resourceUrl = null;
        final StringBuilder queryExpr = new StringBuilder(100);
        // all parameters except resourceUrl contribute to the query expression
        for (ParametersParameterComponent searchExprParam : searchExprParamList) {
          String name = searchExprParam.getName();
          try {
            String value = searchExprParam.getValue().toString();
            // resourceUrl is different than others
            if (RESOURCE_URL.equals(name)) {
              resourceUrl = value;
            } else {
              if (queryExpr.length() > 0) {
                queryExpr.append("&");
              }
              queryExpr.append(name);
              queryExpr.append("=");
              queryExpr.append(value);
            }
          } catch (NullPointerException e) {
            LOG.error("Null Value for search expression parameter, {}", name);
          }
        }

        if (resourceUrl == null) {
          LOG.warn("Reqeuired parameter, resourceUrl, is missing!");
        } else {
          searchUrl.append(resourceUrl);
          searchUrl.append("?");
          searchUrl.append(queryExpr);
        }
        LOG.info("search Url: {}", searchUrl.toString());
      }
    } else {
      LOG.warn("Unable to find search expression in message parameters");
    }
    return searchUrl.toString();
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
   * @param producerEndpointUri
   *          the producerEndpointUri to set
   */
  public final void setProducerEndpointUri(String producerEndpointUri) {
    this.producerEndpointUri = producerEndpointUri;
  }

  /**
   * @return the fhirRestClient
   */
  public final IGenericClient getFhirRestClient() {
    return fhirRestClient;
  }

  /**
   * @param fhirRestClient
   *          the fhirRestClient to set
   */
  public final void setFhirRestClient(IGenericClient fhirRestClient) {
    this.fhirRestClient = fhirRestClient;
  }

  /**
   * @return the producer
   */
  public final ProducerTemplate getProducer() {
    return producer;
  }

}
