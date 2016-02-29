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

import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.instance.model.Bundle;
import org.hl7.fhir.instance.model.MessageHeader;
import org.hl7.fhir.instance.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.instance.model.Bundle.BundleType;
import org.hl7.fhir.instance.model.MessageHeader.ResponseType;
import org.hl7.fhir.instance.model.Parameters;
import org.hl7.fhir.instance.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.instance.model.Patient;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.ResourceType;
import org.mitre.ptmatchadapter.format.SimplePatientCsvFormat;
import org.mitre.ptmatchadapter.recordmatch.RecordMatchResultsBuilder;
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
public class RecordMatchRequestProcessor {
  private static final Logger LOG = LoggerFactory
      .getLogger(RecordMatchRequestProcessor.class);

  private ProducerTemplate producer;

  private IGenericClient fhirRestClient;

  private String producerEndpointUri;

  /** Path to the folder for FRIL data sources, config file, & results. */
  private String workDir;

  private static final String MASTER = "master";
  private static final String QUERY = "query";
  private static final String RESOURCE_TYPE = "resourceType";
  private static final String SEARCH_EXPR = "searchExpression";
  private static final String RESOURCE_URL = "resourceUrl";

  public void process(Bundle bundle) {
    if (BundleType.MESSAGE.equals(bundle.getType())) {
      final List<BundleEntryComponent> bundleEntries = bundle.getEntry();
      try {
        // the first entry is supposed to be the MessageHeader
        final MessageHeader msgHdr = (MessageHeader) bundleEntries.get(0)
            .getResource();

        Parameters masterQryParams = null;
        Parameters queryQryParams = null;
        String masterSearchUrl = null;
        String querySearchUrl = null;
        String masterServerBase = null;
        String queryServerBase = null;
        String resourceType = "Patient";

        // Find the Parameters resources that contain the search parameters
        // and use those to construct search Urls
        for (BundleEntryComponent entry : bundleEntries) {
          Resource r = entry.getResource();
          LOG.debug("Found Resource type: " + r.getResourceType().toString());
          if (ResourceType.Parameters.equals(r.getResourceType())) {
            Parameters params = (Parameters) r;
            List<ParametersParameterComponent> paramList = params.getParameter();
            // Now look for the parameter with name, resourceType.
            ParametersParameterComponent p = 
                ParametersUtil.findByName(paramList, RESOURCE_TYPE);
            if (p != null) {
              resourceType = p.getValue().toString();
            }
            // Find parameter that distinguishes between master and query sets
            p = ParametersUtil.findByName(paramList, "type");
            if (p != null) {
              String val = p.getValue().toString();
              if (val.equalsIgnoreCase(MASTER)) {
                masterQryParams = params;
                masterSearchUrl = buildSearchUrl(params);
                masterServerBase = getServerBase(resourceType, masterQryParams);
              } else if (val.equalsIgnoreCase(QUERY)) {
                queryQryParams = params;
                querySearchUrl = buildSearchUrl(params);
                queryServerBase = getServerBase(resourceType, queryQryParams);
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
          IQuery<Bundle> query = fhirRestClient.search().byUrl(masterSearchUrl)
              .returnBundle(Bundle.class);

          // Perform a search
          final Bundle masterSetResults = query.execute();

          // Split the bundle into its component resources
          final SearchResultSplitter resultSplitter = new SearchResultSplitter();
          final List<Resource> masterResources = resultSplitter
              .splitBundle(masterSetResults);

          // Create a CSV data source for FRIL
          File runDir = newRunDir(workDir);
          String fname = createDataSourceFileName(runDir, "master");
          final File masterFile = new File(fname);
          writeData(masterFile, masterResources, masterServerBase);

          // retrieve other pages of search results
          Bundle nextResults = masterSetResults;
          while (nextResults.getLink(Bundle.LINK_NEXT) != null) {
            nextResults = fhirRestClient.loadPage().next(nextResults).execute();
            List<Resource> nextResources = resultSplitter.splitBundle(nextResults);
            writeData(masterFile, nextResources, masterServerBase);
          }

          Bundle querySetResults = null;
          if (querySearchUrl != null) {
            // Retrieve the data associated with the search urls
            query = fhirRestClient.search().byUrl(querySearchUrl)
                .returnBundle(Bundle.class);

            // Perform a search
            querySetResults = query.execute();

            // Split the bundle into its component resources
            final List<Resource> queryResources = resultSplitter
                .splitBundle(querySetResults);

            // Create a CSV data source for FRIL
            fname = createDataSourceFileName(runDir, "query");
            final File queryFile = new File(fname);
            writeData(queryFile, queryResources, queryServerBase);

            nextResults = querySetResults;
            while (nextResults.getLink(Bundle.LINK_NEXT) != null) {
              nextResults = fhirRestClient.loadPage().next(nextResults).execute();
              List<Resource> nextResources = resultSplitter.splitBundle(nextResults);
              writeData(queryFile, nextResources, queryServerBase);
            }
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

        // TODO Perform the Match Operation
        
        
        // TODO Construct results 
        final RecordMatchResultsBuilder builder = new RecordMatchResultsBuilder(
            bundle, ResponseType.OK);
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
        String resourceType = null;
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
          LOG.warn(
              "Reqeuired parameter, resourceUrl, is missing from record-match request!");
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

  private String getServerBase(String resourceType, Parameters params) {
    String serverBase = null;

    String resourceUrl = getResourceUrl(params);

    if (resourceUrl != null) {
      int pos = resourceUrl.lastIndexOf(resourceType);

      if (pos > 0) {
        // strip off the resource type from the end of the search url base
        serverBase = resourceUrl.substring(0, pos);
      }
    }
    return serverBase;
  }

  private String getResourceUrl(Parameters params) {
    String resourceUrl = null;

    final List<ParametersParameterComponent> paramList = params.getParameter();

    final ParametersParameterComponent p = ParametersUtil.findByName(paramList,
        SEARCH_EXPR);
    if (p != null) {
      final Resource r = p.getResource();
      if (ResourceType.Parameters.equals(r.getResourceType())) {
        final Parameters searchExprParams = (Parameters) r;
        final List<ParametersParameterComponent> searchExprParamList = searchExprParams
            .getParameter();

        final ParametersParameterComponent ppc = ParametersUtil
            .findByName(searchExprParamList, RESOURCE_URL);
        resourceUrl = ppc.getValue().toString();

        if (resourceUrl == null) {
          LOG.warn(
              "Reqeuired parameter, resourceUrl, is missing from record-match request!");
        }
      }
    } else {
      LOG.warn("Unable to find search expression in message parameters");
    }
    return resourceUrl;
  }

  /**
   * Construct a name with path for a data source file.
   * 
   * @param workDir
   * @param setType
   * @return
   */
  private String createDataSourceFileName(File runDir, String setType) {
    final StringBuilder sb = new StringBuilder();
    sb.append(runDir.getAbsolutePath());
    if (File.separatorChar != sb.charAt(sb.length()-1)) {
      sb.append(File.separator);
    }
    if (setType != null && !setType.isEmpty()) {
      sb.append(setType);
      sb.append("-");
    }
    sb.append("data");
    sb.append(".csv");
    LOG.info("Data Source file Name: {}", sb.toString());
    return sb.toString();
  }

  private File newRunDir(String workDir) {
    final Calendar cal = Calendar.getInstance();
    final StringBuilder sb = new StringBuilder();
    sb.append(workDir);
    sb.append(File.separator);
    sb.append(String.format("%02d", cal.get(Calendar.YEAR)));
    sb.append(String.format("%02d", cal.get(Calendar.MONTH)));
    sb.append(String.format("%02d", cal.get(Calendar.DATE)));
    sb.append("-");
    sb.append(String.format("%02d", cal.get(Calendar.HOUR)));
    sb.append(String.format("%02d", cal.get(Calendar.MINUTE)));
    sb.append(String.format("%02d", cal.get(Calendar.SECOND)));
    
    final File dir = new File(sb.toString());
    if (!dir.exists()) {
      dir.mkdirs();
    }
    
    return dir;
  }
  
  private final char COMMA = ',';
  private static final String DOUBLE_QUOTE = "\"";

  /**
   * Write the resources to the specified data files.
   * 
   * @param f
   *          data file to which to write the CSV data
   * @param resources
   *          resources to process
   * @param serverBase
   *          server based to which to preprend the resource id (to build
   *          fullUrl)
   * @throws IOException
   */
  private void writeData(File f, List<Resource> resources, String serverBase)
      throws IOException {
    // return fast if there is nothing to do
    if (resources.size() == 0) {
      return;
    }

    if (!f.exists()) {
      f.createNewFile();
    }

    String fullUrlBase = serverBase;
    if (!serverBase.endsWith("/")) {
      fullUrlBase += "/";
    }
    // Create Writer
    final BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
    final SimplePatientCsvFormat fmt = new SimplePatientCsvFormat();

    try {
      bw.write(fmt.getHeaders());
      bw.write(COMMA);
      bw.write("fullUrl");
      bw.newLine();
      // Process each resource in the list
      for (Resource r : resources) {
        if (ResourceType.Patient.equals(r.getResourceType())) {
          // convert resource to CSV
          String csv = fmt.toCSV((Patient) r);
          if (csv != null) {
            bw.write(csv);
            bw.write(COMMA);
            bw.write(DOUBLE_QUOTE);
            bw.write(fullUrlBase);
            bw.write(r.getId());
            bw.write(DOUBLE_QUOTE);
            bw.newLine();
          }
        } else {
          LOG.error("Unsupported Resource Type: {}",
              r.getResourceType().toString());
        }
      }
    } finally {
      bw.close();
    }
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

  /**
   * @return the workDir
   */
  public final String getWorkDir() {
    return workDir;
  }

  /**
   * @param workDir
   *          the workDir to set
   */
  public final void setWorkDir(String dir) {
    this.workDir = dir;
  }

}
