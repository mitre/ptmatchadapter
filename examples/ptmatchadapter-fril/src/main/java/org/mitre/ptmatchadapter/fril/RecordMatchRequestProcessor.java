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
package org.mitre.ptmatchadapter.fril;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

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

import org.mitre.ptmatchadapter.SearchResultSplitter;
import org.mitre.ptmatchadapter.format.SimplePatientCsvFormat;
import org.mitre.ptmatchadapter.fril.config.Configuration;
import org.mitre.ptmatchadapter.fril.config.Configuration.LeftDataSource.Preprocessing.Deduplication.MinusFile;
import org.mitre.ptmatchadapter.model.ServerAuthorization;
import org.mitre.ptmatchadapter.util.AuthorizationUtil;
import org.mitre.ptmatchadapter.util.ParametersUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;

import cdc.impl.Main;
import cdc.utils.RJException;

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

  /**
   * Path to the folder in which a folder for each record match run will be
   * created.
   */
  private String workDir;

  /**
   * flag that indicates whether the record match job results should be deleted.
   */
  private boolean deleteJobResults = false;

  /** Path to the record match deduplication configuration template file. */
  private String deduplicationTemplate;

  /** Path to the record match linkage configuration template file. */
  private String linkageTemplate;

  private List<ServerAuthorization> serverAuthorizations;

  private static final String MASTER = "master";
  private static final String QUERY = "query";
  private static final String RESOURCE_TYPE = "resourceType";
  private static final String SEARCH_EXPR = "searchExpression";
  private static final String RESOURCE_URL = "resourceUrl";

  /**
   * fullUrl is constructed as server base + resource type + logical id (e.g.,
   * http://serverbase/Patient/123).
   */
  public static final String FULLURL_FORMAT_SIMPLE = "simple";
  /**
   * fullUrl is constructed as server base + resource type + logical id +
   * version info (e.g., http://serverbase/Patient/123/_history/1).
   */
  public static final String FULLURL_FORMAT_VERSIONED = "versioned";

  /** format to use to construct the fullUrl returned in the results. */
  private String fullUrlFormat = FULLURL_FORMAT_SIMPLE;

  public void process(Bundle bundle) {
    Bundle response = null;
    RecordMatchResultsBuilder respBuilder;

    if (BundleType.MESSAGE.equals(bundle.getType())) {
      // Create a CSV data source for FRIL
      final File jobDir = newRunDir(workDir);

      final List<BundleEntryComponent> bundleEntries = bundle.getEntry();
      try {
        // The first entry is supposed to be the MessageHeader
        // This will force an exception if not true.
        final MessageHeader msgHdr = (MessageHeader) bundleEntries.get(0)
            .getResource();
        // This log statement keeps above from getting optimized out
        LOG.trace("msg hdr id {}", msgHdr.getId());

        Parameters masterQryParams = null;
        Parameters queryQryParams = null;
        String masterSearchUrl = null;
        String querySearchUrl = null;
        String masterServerBase = null;
        String queryServerBase = null;
        String resourceType = "Patient";
        boolean isDeduplication = true;

        // Find the Parameters resources that contain the search parameters
        // and use those to construct search Urls
        for (BundleEntryComponent entry : bundleEntries) {
          Resource r = entry.getResource();
          LOG.debug("Found Resource type: " + r.getResourceType().toString());
          if (ResourceType.Parameters.equals(r.getResourceType())) {
            Parameters params = (Parameters) r;
            List<ParametersParameterComponent> paramList = params.getParameter();
            // Now look for the parameter with name, resourceType.
            ParametersParameterComponent p = ParametersUtil.findByName(paramList,
                RESOURCE_TYPE);
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

        if (masterSearchUrl == null) {
          final String errMsg = "Required Parameter for master record set is missing, bundle: "
              + bundle.getId();
          LOG.warn(errMsg);
          // Construct and return an error result
          respBuilder = new RecordMatchResultsBuilder(bundle,
              ResponseType.FATALERROR);
          respBuilder.outcomeIssueDiagnostics(errMsg);
          response = respBuilder.build();
          getProducer().sendBody(getProducerEndpointUri(), response);
          return;
        }

        LoggingInterceptor loggingInterceptor = null;
        if (LOG.isDebugEnabled()) {
          loggingInterceptor = new LoggingInterceptor(true);
          fhirRestClient.registerInterceptor(loggingInterceptor);
        }

        int numMasterRecs = 0;
        try {
          // Retrieve the data associated with the search urls
          numMasterRecs = retrieveAndStoreData(
              masterSearchUrl, masterServerBase, jobDir, "master");

          if (querySearchUrl != null) {
            isDeduplication = false;
            retrieveAndStoreData(querySearchUrl, queryServerBase, jobDir, "query");
          }

        } catch (BaseServerResponseException e) {
          final String errMsg = String.format(
              "Error response from server.  code: %d, %s",
              e.getStatusCode(), e.getMessage());
          LOG.warn(errMsg);
          // Construct and return an error result
          respBuilder = new RecordMatchResultsBuilder(bundle,
              ResponseType.FATALERROR);
          respBuilder.outcomeIssueDiagnostics(errMsg);
          response = respBuilder.build();
          getProducer().sendBody(getProducerEndpointUri(), response);
          return;
        } catch (Exception e) {
          final String errMsg = String.format("Unable to retrieve messages: %s",
              e.getMessage());
          LOG.warn(errMsg, e);
          // Construct and return an error result
          respBuilder = new RecordMatchResultsBuilder(bundle,
              ResponseType.FATALERROR);
          respBuilder.outcomeIssueDiagnostics(errMsg);
          response = respBuilder.build();
          getProducer().sendBody(getProducerEndpointUri(), response);
          return;
        } finally {
          if (loggingInterceptor != null) {
            fhirRestClient.unregisterInterceptor(loggingInterceptor);
          }
        }

        // if no records were returned for the master record set query
        if (numMasterRecs == 0) {
          respBuilder = new RecordMatchResultsBuilder(bundle, ResponseType.OK);
          respBuilder.outcomeDetailText("No Records Found in Master Record Set");
          response = respBuilder.build();          
        } else {
          final File configFile = prepareMatchingRuleConfiguration(isDeduplication,
              jobDir);
  
          // Perform the Match Operation
          LOG.debug("About to Start FRIL w/ config {}", configFile.getAbsolutePath());
          final int numMatches = findMatches(isDeduplication, configFile);
          LOG.info("FRIL Number of Matches: {}", numMatches);
  
          if (numMatches == 0) {
            respBuilder = new RecordMatchResultsBuilder(bundle, ResponseType.OK);
            respBuilder.outcomeDetailText("No Matches Found");
            response = respBuilder.build();
  
          } else if (numMatches > 0) {
            // Find the name of the file containing duplicates from the config
            // file
            final File dupsFile = getDuplicatesFile(configFile);
            // Ensure the duplicates file exists
            if (!dupsFile.exists()) {
              final String errMsg = "Unable to find duplicates file";
              LOG.error(errMsg + " at " + dupsFile.getAbsolutePath());
              throw new FileNotFoundException(errMsg);
            }
  
            // Construct results
            respBuilder = new RecordMatchResultsBuilder(bundle, ResponseType.OK);
            respBuilder.outcomeDetailText("Deduplication Complete");
            respBuilder.duplicates(dupsFile);
            response = respBuilder.build();
  
          } else {
            final String errMsg = "Unknown Processing Error";
            LOG.error("{} bundleId: {}", errMsg, bundle.getId());
  
            // Construct an error result
            respBuilder = new RecordMatchResultsBuilder(bundle,
                ResponseType.FATALERROR);
            respBuilder.outcomeIssueDiagnostics(errMsg);
            response = respBuilder.build();
          }
        }

      } catch (Exception e) {
        final String errMsg = "Unexpected Error";
        LOG.error("Processing bundle: {}", bundle.getId(), e);

        // Construct an error result
        respBuilder = new RecordMatchResultsBuilder(bundle,
            ResponseType.FATALERROR);
        respBuilder.outcomeIssueDiagnostics(errMsg);
        try {
          response = respBuilder.build();
        } catch (IOException ioe) {
          // only so many times we can attempt to send a response; log error
          LOG.error("Unable to Send Error Response. request bundle: {}",
              bundle.getId(), ioe);
        }
      }

      if (deleteJobResults) {
        // Delete the Job Results folder and content
        deleteFolder(jobDir);
      }
    } else {
      final String errMsg = "Unsupported Bundle type: "
          + bundle.getType().toString();
      LOG.info("{} msgId: {}", errMsg, bundle.getId());

      // Construct an error result
      respBuilder = new RecordMatchResultsBuilder(bundle, ResponseType.FATALERROR);
      respBuilder.outcomeIssueDiagnostics(errMsg);
      try {
        response = respBuilder.build();
      } catch (IOException e) {
        // only so many times we can attempt to send a response; log error
        LOG.error("Unable to Send Error Response. request bundle: {}",
            bundle.getId());
      }
    }

    // Send the response back to the requester
    if (response != null) {
      getProducer().sendBody(getProducerEndpointUri(), response);
    } else {
      LOG.error("Null Response for request! bundleId: {}", bundle.getId());
    }
  }

  private void deleteFolder(File file) {
    File[] contents = file.listFiles();
    if (contents != null) {
      for (File f : contents) {
        deleteFolder(f);
      }
    }
    file.delete();
  }

  /**
   * Invokes the given search Url and writes the results to a file in the
   * specified job folder.
   * 
   * @param searchUrl
   * @param serverBase
   * @param jobDir
   * @param fileName
   * @throws IOException
   *
   */
  private int retrieveAndStoreData(String searchUrl, String serverBase,
      File jobDir, String fileName) throws IOException {

    int numRecords = 0;
    
    final String url = urlEncodeQueryParams(searchUrl);

    LOG.info("retrieveAndStoreData, serverBase: {}  searchUrl: {} encoded query: {}", 
        serverBase, searchUrl, url);
    
    final ServerAuthorization serverAuthorization = 
        AuthorizationUtil.findServerAuthorization(serverAuthorizations, serverBase);
    
    BearerTokenAuthInterceptor authInterceptor = null;
    if (serverAuthorization != null) {
      // register authorization interceptor with the client
      LOG.info("assigning bearing token interceptor, {}", serverAuthorization.getAccessToken() );
      authInterceptor = new BearerTokenAuthInterceptor(serverAuthorization.getAccessToken());
      fhirRestClient.registerInterceptor(authInterceptor);
    }
    try {
      IQuery<Bundle> query = fhirRestClient.search().byUrl(url)
          .returnBundle(Bundle.class);
  
      // Perform a search
      final Bundle searchResults = query.execute();
  
      // Split the bundle into its component resources
      final SearchResultSplitter resultSplitter = new SearchResultSplitter();
      final List<Resource> resources = resultSplitter.splitBundle(searchResults);
  
      final File dataFile = createDataSourceFile(jobDir, fileName);
      writeData(dataFile, resources, serverBase, true);
      numRecords = resources.size();
      
      // retrieve other pages of search results
      Bundle nextResults = searchResults;
      while (nextResults.getLink(Bundle.LINK_NEXT) != null) {
        nextResults = fhirRestClient.loadPage().next(nextResults).execute();
        List<Resource> nextResources = resultSplitter.splitBundle(nextResults);
        writeData(dataFile, nextResources, serverBase, false);
        numRecords += nextResources.size();
      }
    } finally {
      if (authInterceptor != null) {
        // unregister authorization interceptor with the client
        fhirRestClient.unregisterInterceptor(authInterceptor);
      }
    }
    return numRecords;
  }

    
  private String urlEncodeQueryParams(String url) {
    final StringBuilder sb = new StringBuilder((int) (url.length() * 1.2));

    final int pos = url.indexOf('?');
    if (pos > 0) {
      // split url into base and query expression
      final String queryExpr = url.substring(pos + 1);
      LOG.trace("queryExpr {}", queryExpr);

      final Pattern p = Pattern.compile(
          "\\G([A-Za-z0-9-_]+)=([A-Za-z0-9-+:#|^\\.,<>;%*\\(\\)_/\\[\\]\\{\\}\\\\ ]+)[&]?");
      final Matcher m = p.matcher(queryExpr);
      while (m.find()) {
        LOG.trace("group 1: {}   group 2: {}", m.group(1), m.group(2));
        if (sb.length() > 0) {
          sb.append("&");
        }
        sb.append(m.group(1));
        sb.append("=");
        try {
          // URL Encode the value of each query parameter
          sb.append(URLEncoder.encode(m.group(2), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          // Ignore - We know UTF-8 is supported
        }
      }

      LOG.trace("new query Expr: {}", sb.toString());

      sb.insert(0, url.substring(0, pos + 1));
    }
    return sb.toString();
  }

  /**
   * 
   * @param matchRuleConfig
   * @return number of duplicates or linked pairs found
   */
  protected int findMatches(boolean isDeduplication, File matchRuleConfig)
      throws IOException {
    int frilResult = -1;
    try {
      final Main fril = new Main(matchRuleConfig.getAbsolutePath());
      if (isDeduplication) {
        // Start FRIL in deduplication mode
        frilResult = fril.runDedupe();
      } else {
        // Find matches of query items in the master set
        frilResult = fril.runJoin();
      }
    } catch (RJException e) {
      final String msg = "Unable to load or run FRIL: " + e.getMessage();
      LOG.error(msg, e);
      throw new RuntimeException(msg);
    }
    LOG.info("FRIL RESULT: {}", frilResult);
    return frilResult;
  }

  /**
   * Prepare a matching rule configuration file in the specified job folder
   * based on whether the run is Deduplication or Linkage.
   * 
   * @param isDeduplication
   *          true when deduplicating records in a single data set.
   * 
   * @param jobDir
   * @return
   * @throws IOException
   */
  File prepareMatchingRuleConfiguration(boolean isDeduplication, File jobDir)
      throws IOException {
    final String templateFileStr = isDeduplication ? getDeduplicationTemplate()
        : getLinkageTemplate();
    if (templateFileStr == null) {
      throw new IllegalStateException("No Template File set for "
          + (isDeduplication ? "deduplication" : "linkage") + " mode");
    }

    final Template matchConfigTemplate = loadTemplate(templateFileStr);
    if (matchConfigTemplate == null) {
      final String msg = "Unable to load matching rule configuration template: "
          + templateFileStr;
      LOG.error(msg);
      throw new IllegalStateException(msg);
    }

    final File configFile = new File(jobDir, "config.xml");

    // Generate the configuration file that specifies matching rules and
    // data sources
    final Map<String, String> templateParams = new HashMap<String, String>();
    templateParams.put("jobDir", jobDir.getAbsolutePath());
    final Writer configWriter = new FileWriter(configFile);
    try {
      // Apply parameters to template and write result to file
      matchConfigTemplate.execute(templateParams, configWriter);
      configWriter.flush();
    } finally {
      try {
        configWriter.close();
      } catch (IOException e) {
        // ignore
      }
    }
    return configFile;
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
          LOG.warn(
              "Required parameter, resourceUrl, is missing from record-match request!");
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
   * Returns the server base URL found in the resourceUrl.
   * 
   */
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

  final File getDuplicatesFile(File configFile)
      throws JAXBException, FileNotFoundException {
    // Read the XML configuration file
    final JAXBContext jaxbContext = JAXBContext.newInstance(Configuration.class);

    final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    final Configuration config = (Configuration) jaxbUnmarshaller
        .unmarshal(configFile);

    try {
      // pull the name of the duplicates file from the configuration
      final MinusFile minusFile = config.getLeftDataSource().getPreprocessing()
          .getDeduplication().getMinusFile();

      return new File(minusFile.getFile());
    } catch (NullPointerException e) {
      final String errMsg = "Unable to find duplicates file";
      LOG.error(errMsg + " in " + configFile.getAbsolutePath());
      throw new RuntimeException(errMsg);
    }
  }

  /**
   * Returns the resourceUrl parameter from the Parameters resource.
   * 
   */
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
              "Required parameter, resourceUrl, is missing from record-match request!");
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
   * @param runDir
   * @param setType
   * @return
   */
  private File createDataSourceFile(File runDir, String setType) {
    final StringBuilder sb = new StringBuilder(
        (int) (runDir.getAbsolutePath().length() * 1.5));
    sb.append(runDir.getAbsolutePath());
    if (File.separatorChar != sb.charAt(sb.length() - 1)) {
      sb.append(File.separator);
    }
    if (setType != null && !setType.isEmpty()) {
      sb.append(setType);
      sb.append("-");
    }
    sb.append("data");
    sb.append(".csv");
    LOG.debug("Data Source file Name: {}", sb.toString());
    return new File(sb.toString());
  }

  private SecureRandom fileSuffixRand = new SecureRandom();

  protected File newRunDir(String workDir) {
    final Calendar cal = Calendar.getInstance();
    final StringBuilder sb = new StringBuilder();
    sb.append(workDir);
    sb.append(File.separator);
    sb.append(String.format("%02d", cal.get(Calendar.YEAR)));
    sb.append(String.format("%02d", cal.get(Calendar.MONTH) + 1));
    sb.append(String.format("%02d", cal.get(Calendar.DATE)));
    sb.append("-");
    sb.append(String.format("%02d", cal.get(Calendar.HOUR_OF_DAY)));
    sb.append(String.format("%02d", cal.get(Calendar.MINUTE)));
    sb.append(String.format("%02d", cal.get(Calendar.SECOND)));
    sb.append("-");
    sb.append(String.format("%02d", fileSuffixRand.nextInt(999)));
    final File dir = new File(sb.toString());
    if (!dir.exists()) {
      dir.mkdirs();
    }

    return dir;
  }

  private final char COMMA = ',';
  private final char SLASH = '/';
  private static final String DOUBLE_QUOTE = "\"";

  /**
   * Write the resources to the specified data file.
   * 
   * @param f
   *          data file to which to write the CSV data
   * @param resources
   *          resources to process
   * @param serverBase
   *          server based to which to preprend the resource id (to build
   *          fullUrl)
   * @param writeColumnTitles
   *          true to write a line containing column titles
   * @throws IOException
   */
  private void writeData(File f, List<Resource> resources, String serverBase,
      boolean writeColumnTitles) throws IOException {
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
      if (writeColumnTitles) {
        bw.write("fullUrl");
        bw.write(COMMA);
        bw.write(fmt.getHeaders());
        bw.newLine();
      }
      // Process each resource in the list
      for (Resource r : resources) {
        if (ResourceType.Patient.equals(r.getResourceType())) {
          // convert resource to CSV
          String csv = fmt.toCsv((Patient) r);
          if (csv != null) {
            // put fullURL to the start of the line
            bw.write(DOUBLE_QUOTE);
            bw.write(fullUrlBase);
            if (FULLURL_FORMAT_VERSIONED.equals(getFullUrlFormat())) {
              // HAPI FHIR provides <id>/_history/<version>
              bw.write(r.getId());
            } else {
              // use SIMPLE format
              bw.write(r.getIdElement().getResourceType());
              bw.write(SLASH);
              bw.write(r.getIdElement().getIdPart());
            }
            bw.write(DOUBLE_QUOTE);

            bw.write(COMMA);
            bw.write(csv);

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
   * Searches classpath and then system for a file with the specified name.
   * 
   * @param name
   *          name of template file to load
   * @return
   */
  protected Template loadTemplate(String name) throws FileNotFoundException {
    Template template = null;
    InputStream instream = this.getClass().getClassLoader()
        .getResourceAsStream(name);

    if (instream == null) {
      instream = System.class.getResourceAsStream(name);
    }
    if (instream == null) {
      instream = new FileInputStream(name);
    }

    if (instream != null) {
      final Reader r = new InputStreamReader(instream);
      try {
        template = Mustache.compiler().compile(r);
      } finally {
        try {
          r.close();
        } catch (Exception e) {
          // skip
        }
      }
    }
    return template;
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

  /**
   * @return the deduplicationTemplate
   */
  public final String getDeduplicationTemplate() {
    return deduplicationTemplate;
  }

  /**
   * @param deduplicationTemplate
   *          full path to the deduplication template file
   */
  public final void setDeduplicationTemplate(String path) {
    this.deduplicationTemplate = path;
  }

  /**
   * @return the linkageTemplate
   */
  public final String getLinkageTemplate() {
    return linkageTemplate;
  }

  /**
   * @param linkageTemplate
   *          the linkageTemplate to set
   */
  public final void setLinkageTemplate(String linkageTemplate) {
    this.linkageTemplate = linkageTemplate;
  }

  /**
   * @return the fullUrlFormat
   */
  public final String getFullUrlFormat() {
    return fullUrlFormat;
  }

  /**
   * @param fullUrlFormat
   *          the fullUrlFormat to set
   */
  public final void setFullUrlFormat(String fullUrlFormat) {
    this.fullUrlFormat = fullUrlFormat;
  }

  /**
   * @return the deleteJobResults
   */
  public final boolean isDeleteJobResults() {
    return deleteJobResults;
  }

  /**
   * @param deleteJobResults
   *          the deleteJobResults to set
   */
  public final void setDeleteJobResults(boolean deleteJobResults) {
    this.deleteJobResults = deleteJobResults;
  }

  /**
   * @return the serverAuthorizations
   */
  public final List<ServerAuthorization> getServerAuthorizations() {
    return serverAuthorizations;
  }

  /**
   * @param serverAuthorizations
   *          the serverAuthorizations to set
   */
  public final void setServerAuthorizations(
      List<ServerAuthorization> serverAuthorizations) {
    this.serverAuthorizations = serverAuthorizations;
  }

}
