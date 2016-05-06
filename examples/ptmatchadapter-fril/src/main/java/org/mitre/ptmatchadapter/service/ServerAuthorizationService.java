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
package org.mitre.ptmatchadapter.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.Headers;
import org.apache.camel.Message;
import org.apache.camel.OutHeaders;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.mitre.ptmatchadapter.service.model.AuthorizationRequestInfo;
import org.mitre.ptmatchadapter.service.model.ServerAuthorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public class ServerAuthorizationService {
  private static final Logger LOG = LoggerFactory
      .getLogger(ServerAuthorizationService.class);

  @Autowired
  private ProducerTemplate producerTemplate;

  private List<ServerAuthorization> serverAuthorizations;

  /**
   * URL to an OAuth 2.0 Authorization Server for authorization code requests.
   */
  private String authorizationServer;
  private String authorizationEndpoint;
  private String accessTokenEndpoint;

  /** Identifier by which the OAuth 2.0 server knows the ptmatch adapter. */
  private String clientId;
  private String clientSecret;

  private final SecureRandom rand = new SecureRandom();

  /** Map of variables in OAuth Session State. */
  private Map<String, Object> sessionData = new PassiveExpiringMap<String, Object>(
      ENTRY_EXPIRATION_TIME);

  /** number of milliseconds after which session information expires and is deleted. */  
  private static final long ENTRY_EXPIRATION_TIME = 300000;

  private static final String STATE_PARAM = "state";
  private static final String CODE_PARAM = "code";

  private static final String SERVER_AUTH = "serverAuthorization";

  // value will be retrieved from application.properties
  @Value("${ptmatchadapter.clientAuthRedirectPath}")
  private String clientAuthRedirectPath = "/mgr/authCodeResp";

  
  /**
   * 
   * @param reqHdrs
   * @param respHdrs
   */
  public final void handleOptions(
      @Headers Map<String, Object> reqHdrs,
      @OutHeaders Map<String, Object> respHdrs) {

    final List<String> supportedHttpMethodsList = 
        Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS");
    final String supportedHttpMethods = String.join(", ", supportedHttpMethodsList);
    
    respHdrs.put(Exchange.CONTENT_LENGTH, 0);
    respHdrs.put("Allow", supportedHttpMethods);


    // Write out message request headers
    if (LOG.isDebugEnabled()) {
      for (String key : reqHdrs.keySet()) {
        LOG.debug("handlOptions: req key: {} val: {}", key, reqHdrs.get(key));
      }
    }

    // request headers are considered case-insensitive, but camel has not normalized
    String origin = (String) reqHdrs.get("Origin");
    if (origin == null) {
      origin = (String) reqHdrs.get("origin");
    }
    LOG.debug("handleOptions: origin {}", origin);

    // Section 3.2 of RFC 7230 (https://tools.ietf.org/html/rfc7230#section-3.2)
    // says header fields are case-insensitive
    if (origin != null) {

      final String corsRequestMethod = (String) reqHdrs.get("Access-Control-Request-Method");
      LOG.info("handleOptions: corsRequestMethod {}", corsRequestMethod);

      // The W3C CORS Spec only seems to care, at a minimum, that a request
      // method header exists.  The response includes the supported methods,
      // regardless of the method in the request header (or so I read it).
      if (corsRequestMethod != null) {
        // Return a list of methods we allow
        respHdrs.put("Access-Control-Allow-Methods", supportedHttpMethods);
        respHdrs.put("Access-Control-Allow-Origin", origin);
        respHdrs.put("Access-Control-Allow-Credentials", "true");
        // Max Age - # of seconds the browser may cache this response
        respHdrs.put("Access-Control-Max-Age", 43200);
        final Object corsRequestHeaders = reqHdrs.get("Access-Control-Request-Headers");
        respHdrs.put("Access-Control-Allow-Headers", corsRequestHeaders);
      } else {
        // Origin header found, but no Request-Method, so invalid request
        respHdrs.put(Exchange.HTTP_RESPONSE_CODE, 400); // BAD REQUEST 
      }

      // Write out response headers
      if (LOG.isDebugEnabled()) {
        for (String key : respHdrs.keySet()) {
          LOG.debug("handleOptions: resp key: {} val: {}", key, respHdrs.get(key));
        }
      }

    } else {
      // A normal OPTIONS request wouldn't have any CORS headers, so treat as OK
    }
  }

    
  /**
   * Create a ServerAuthorization object.
   * 
   * @param obj
   * @param respHdrs
   * @return
   */
  public final ServerAuthorization create(ServerAuthorization serverAuth,
      @Headers Map<String, Object> reqHdrs,
      @OutHeaders Map<String, Object> respHdrs) {

    final String serverUrl = serverAuth.getServerUrl();
    if (serverUrl != null && !serverUrl.isEmpty()) {
      // Don't honor the incoming id value, if any
      serverAuth.setId(UUID.randomUUID().toString());

      final ServerAuthorization serverAuthResp = processServerAuthRequest(
          serverAuth, reqHdrs, respHdrs);

      return serverAuthResp;
    }
    return null;
  }

  /**
   * Process a request to create a Server Authorization (i.e., request to grant
   * ptmatchadapter authorization to access a particular fhir server)
   * 
   * @param serverAuth
   * @param reqHdrs
   * @param respHdrs
   * @return
   */
  private final ServerAuthorization processServerAuthRequest(
      ServerAuthorization serverAuth,
      @Headers Map<String, Object> reqHdrs,
      @OutHeaders Map<String, Object> respHdrs) {
    final String serverUrl = serverAuth.getServerUrl();
    final String accessToken = serverAuth.getAccessToken();

    // if request doesn't contain a server URL, it is an error
    if (serverUrl == null || serverUrl.isEmpty()) {
      respHdrs.put(Exchange.HTTP_RESPONSE_CODE, 400); // BAD REQUEST
      return null;
    }
    // else if the request body doesn't include an access token, redirect user
    // to an authorization server
    else if (accessToken == null || accessToken.isEmpty()) {
      // create a state identifier
      final String stateKey = newStateKey();

      respHdrs.put(STATE_PARAM, stateKey);

      final AuthorizationRequestInfo requestInfo = new AuthorizationRequestInfo();
      requestInfo.put(SERVER_AUTH, serverAuth);
      sessionData.put(stateKey, requestInfo);

      // Construct URL we will invoke on authorization server
      // GET /authorize?response_type=code&client_id=s6BhdRkqt3&state=xyz
      // &redirect_uri=https%3A%2F%2Fclient%2Eexample%2Ecom%2Fcb
      final StringBuilder authUrl = new StringBuilder(100);
      if (getAuthorizationServer() != null) {
        authUrl.append(getAuthorizationServer());
      }
      authUrl.append(getAuthorizationEndpoint());
      authUrl.append("?");

      authUrl.append("response_type=code&client_id=");
      try {
        authUrl.append(URLEncoder.encode(getClientId(), "UTF-8"));
        authUrl.append("&");
        authUrl.append(STATE_PARAM);
        authUrl.append("=");
        authUrl.append(stateKey);
        authUrl.append("&redirect_uri=");

        final HttpServletRequest req = (HttpServletRequest) reqHdrs
            .get(Exchange.HTTP_SERVLET_REQUEST);
        final String redirectUri = URLEncoder.encode(
            getClientAuthRedirectUri(req.getScheme(), req.getServerName(),
                req.getServerPort()),
            "UTF-8");
        authUrl.append(redirectUri);
        // we need to provide redirect uri with access token request, so save it
        requestInfo.put("redirectUri", redirectUri);
      } catch (UnsupportedEncodingException e) {
        // Should never happen, which is why I wrap all above once
        LOG.error("Usupported encoding used on authorization redirect", e);
      }

      respHdrs.put(Exchange.HTTP_RESPONSE_CODE, 302); // FOUND
      respHdrs.put(Exchange.CONTENT_TYPE, "text/plain");
      respHdrs.put("Location", authUrl.toString());

      return null;
    } else {
      LOG.warn("NOT IMPLEMENTED");
      return null;
    }
  }

  /**
   * Processes a form-based request to create a ServerAuthorization
   * 
   * @param body
   *          Body of the request (unused since form parameters are expected in
   *          the request header
   * @param reqHdrs
   * @param respHdrs
   * @return
   */
  public final ServerAuthorization createFromForm(@Body String body,
      @Headers Map<String, Object> reqHdrs,
      @OutHeaders Map<String, Object> respHdrs) {

    //  "application/x-www-form-urlencoded; charset=UTF-8"

    // Write out message request headers
    if (LOG.isDebugEnabled()) {
      for (String key : reqHdrs.keySet()) {
        LOG.debug("fromForm req key: {}", key);
      }
    }

    final String serverUrl = (String) reqHdrs.get("serverUrl");
    if (serverUrl != null && !serverUrl.isEmpty()) {
      final ServerAuthorization serverAuth = new ServerAuthorization();
      serverAuth.setId(UUID.randomUUID().toString());
      serverAuth.setTitle((String) reqHdrs.get("title"));
      serverAuth.setServerUrl(serverUrl);

      
      String origin = (String) reqHdrs.get("Origin");
      if (origin == null) {
        origin = (String) reqHdrs.get("origin");
      }
      LOG.debug("handleOptions: origin {}", origin);

      // Section 3.2 of RFC 7230 (https://tools.ietf.org/html/rfc7230#section-3.2)
      // says header fields are case-insensitive
      if (origin != null) {
        // Firefox on Linux wan'ts exact value of origin in response; * is being rejected
        respHdrs.put("Access-Control-Allow-Origin", origin);
        respHdrs.put("Access-Control-Allow-Credentials", "true");
      }
      
      // Redirect caller to authorization server to get an authorization code
      final ServerAuthorization serverAuthResp = processServerAuthRequest(
          serverAuth, reqHdrs, respHdrs);

      // Retrieve the state key from the query parameters
      final String stateKey = (String) respHdrs.get(STATE_PARAM);

      final AuthorizationRequestInfo requestInfo = (AuthorizationRequestInfo) sessionData
          .get(stateKey);

      // Annotate request info so we know to return html later
      requestInfo.setResponseType("html");

      return serverAuthResp;
    } else {
      // missing required parameter
      respHdrs.put(Exchange.HTTP_RESPONSE_CODE, 400); // BAD REQUEST
      respHdrs.put(Exchange.CONTENT_LENGTH, 0);
    }
    return null;
  }

  /**
   * Processes authorization code response from the OAuth 2.0 Authorization
   * Server.
   * 
   * @param body
   * @param reqHdrs
   * @param respHdrs
   * @return
   */
  public String processAuthorizationCode(@Body String body,
      @Headers Map<String, Object> reqHdrs,
      @OutHeaders Map<String, Object> respHdrs) {
    // Retrieve the state key from the query parameters
    final String stateKey = (String) reqHdrs.get(STATE_PARAM);

    if (stateKey == null) {
      final String msg = "Redirect from authorization server is missing state parameter";
      LOG.error(msg);
      throw new IllegalStateException(msg);
    }

    LOG.info("process redirect, state {}", stateKey);

    for (String key : sessionData.keySet()) {
      LOG.info("redirect session state key: {}", key);
    }

    final HttpServletRequest req = (HttpServletRequest) reqHdrs
        .get(Exchange.HTTP_SERVLET_REQUEST);
    
    final String authCode = (String) reqHdrs.get(CODE_PARAM);

    // - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Request an Access Token from the OAuth Authorization Server
    // - - - - - - - - - - - - - - - - - - - - - - - - - -
    final ServerAuthorization serverAuth = requestAccessToken(req, stateKey,
        authCode);

    if (serverAuth != null) {
      getServerAuthorizations().add(serverAuth);
      LOG.info("process AuthCodeResp, serverUrl {}", serverAuth.getServerUrl());
      LOG.info("process AuthCodeResp, # server auths {}",
          serverAuthorizations.size());
    }

    final AuthorizationRequestInfo requestInfo = (AuthorizationRequestInfo) sessionData
        .remove(stateKey);

    if (requestInfo.getResponseType().equalsIgnoreCase("html")) {
      // redirect user to page of server authorizations
      respHdrs.put(Exchange.HTTP_RESPONSE_CODE, 302); // FOUND
      respHdrs.put(Exchange.CONTENT_LENGTH, 0);
      respHdrs.put("Location", "/");
      return "";
    } else {
      respHdrs.put(Exchange.HTTP_RESPONSE_CODE, 201); // Created
      respHdrs.put(Exchange.CONTENT_TYPE, "application/json");
      return "{\"code\": \"success\"}";
    }
  }

  /**
   * Request the authorization server provide an access token in exchange for a
   * provided authorization code.
   * 
   * @return
   */
  private ServerAuthorization requestAccessToken(HttpServletRequest req,
      String stateKey, String authCode) {
    ServerAuthorization result = null;

    final AuthorizationRequestInfo requestInfo = (AuthorizationRequestInfo) sessionData
        .get(stateKey);
    if (requestInfo == null) {
      final String msg = "Unable to find original authorization request.";
      LOG.error(msg + " state: " + stateKey);
      throw new IllegalStateException(msg);
    }

    final StringBuilder sb = new StringBuilder(300);
    sb.append("grant_type=authorization_code");
    sb.append("&");
    sb.append("client_id=");
    try {
      sb.append(URLEncoder.encode(getClientId(), "UTF-8"));
      sb.append("&");
      sb.append("client_secret=");
      sb.append(URLEncoder.encode(getClientSecret(), "UTF-8"));
      sb.append("&");
      sb.append("code=");
      sb.append(URLEncoder.encode(authCode, "UTF-8"));
      sb.append("&redirect_uri=");
      sb.append((String) requestInfo.get("redirectUri"));
    } catch (UnsupportedEncodingException e) {
      // Should never happen, which is why I wrap all above once
      LOG.error("Usupported encoding used on access token request", e);
    }

    final StringBuilder reqUrl = new StringBuilder(200);
    reqUrl.append(stripScheme(getAuthorizationServer()));
    reqUrl.append(getAccessTokenEndpoint());
    LOG.trace("access token request url: {}", reqUrl.toString());

    // Make HTTP call to request access token from Authorization Server
    final Exchange exchange = producerTemplate.send("http4://" + reqUrl.toString(),
        new Processor() {
          public void process(Exchange exchange) throws Exception {
            final Message msgIn = exchange.getIn();

            msgIn.setHeader(Exchange.CONTENT_TYPE, "www-form-urlencoded");
            msgIn.setHeader(Exchange.HTTP_METHOD, "POST");

            msgIn.setHeader(Exchange.HTTP_QUERY, sb.toString());
            LOG.info("Inside Processor to that requests access token");

          }
        });

    if (exchange.isFailed()) {
      LOG.warn("access token request failed! state: {}", stateKey);
      if (exchange.getException() != null) {
        LOG.warn("Failed Access Request: {}", exchange.getException().getMessage(),
            exchange.getException());
      }
    } else {
      final Message out = exchange.getOut();

      for (String key : out.getHeaders().keySet()) {
        LOG.info("access token response msg hdr: {}  val: {}", key,
            out.getHeader(key, String.class));
      }

      final int responseCode = out.getHeader(Exchange.HTTP_RESPONSE_CODE,
          Integer.class);
      LOG.debug("response code from auth server: {}", responseCode);

      if (responseCode == 200) {
        final ServerAuthorization serverAuth = (ServerAuthorization) requestInfo
            .get(SERVER_AUTH);

        // http component is stream-based, which means body can only be read
        // once
        final String respBody = out.getBody(String.class);
        LOG.debug("Access Token Response Body {}", respBody);

        final ObjectMapper mapper = new ObjectMapper();
        try {
          // Response will be JSON; transform into java Map
          final Map<String, Object> accessResp = mapper.readValue(respBody,
              new TypeReference<Map<String, Object>>() {
              });

          int requiredPropCount = 0;

          // Extract access token, token type, etc from access token response
          for (String key : accessResp.keySet()) {
            if ("access_token".equals(key)) {
              serverAuth.setAccessToken((String) accessResp.get(key));
              requiredPropCount++;
            } else if ("token_type".equals(key)) {
              serverAuth.setTokenType((String) accessResp.get(key));
            } else if ("scope".equals(key)) {
              serverAuth.setScope((String) accessResp.get(key));
            } else if ("id_token".equals(key)) {
              serverAuth.setIdToken((String) accessResp.get(key));
            } else if ("expires_in".equals(key)) {
              Integer numSecs = (Integer) accessResp.get(key);
              serverAuth.setExpiresAt(
                  new Date(System.currentTimeMillis() + (numSecs * 1000)));
              LOG.info("Expiration: " + serverAuth.getExpiresAt().toString());
            }
          }
          if (requiredPropCount < 1) {
            LOG.warn(
                "Access Token Response didn't contain all expected properties: {}",
                respBody);
          }

        } catch (JsonGenerationException e) {
          LOG.error("Exception Generating JSON", e);
        } catch (JsonMappingException e) {
          LOG.error("Unable to convert access token response to json", e);
        } catch (IOException e) {
          LOG.error("Exception while processing access token response", e);
        }

        result = serverAuth;
      } else {
        // failure
        LOG.warn(
            "Received Error Response [{}] from Authorization Server. Error Handling NOT IMPLEMENTED",
            responseCode);
      }
    }

    return result;
  }

  /**
   * Construct and return the redirectUri using the provided scheme, host, and port
   * and configured redirect path.
   *  
   */
  private String getClientAuthRedirectUri(String scheme, String hostname,
      int port) {
    final StringBuilder redirect = new StringBuilder(200);
    redirect.append(scheme);
    redirect.append("://");
    redirect.append(hostname);
    if (port != 80 && port != 443) {
      redirect.append(":");
      redirect.append(port);
    }
    redirect.append(clientAuthRedirectPath);
    return redirect.toString();
  }

  /**
   * Strip the scheme (e.g., http://) from the given url.
   * 
   */
  private String stripScheme(String serverUrl) {
    String result = serverUrl;
    int pos = serverUrl != null ? serverUrl.indexOf("://") : -1;
    if (pos > 0) {
      result = serverUrl.substring(pos + 3);
    }
    return result;
  }

  /**
   * Generate a value that can be used as state parameter in the OAuth
   * authorization code grant exchange.
   * 
   * @return    40 hexadecimal character string
   */
  private String newStateKey() {
    // According to table at the site below, 32 Hexadecimal characters are
    // needed to achieve 128 bits of entropy. Return 40 for good measure.
    // https://en.wikipedia.org/wiki/Password_strength#Required_Bits_of_Entropy
    final StringBuilder sb = new StringBuilder(40);
    for (int i = 0; i < 10; i++) {
      // build up key value in 4 hex character chunks
      int val = rand.nextInt(65536);
      sb.append(String.format("%04X", val));
    }
    LOG.trace("new state value: {}", sb.toString());
    return sb.toString();
  }

  
  /**
   * @return the serverAuthorizations
   */
  public final List<ServerAuthorization> getServerAuthorizations() {
    return serverAuthorizations;
  }

  public final ServerAuthorization getServerAuthorization(String id) {
    // go through the list for the given id
    for (ServerAuthorization sa : serverAuthorizations) {
      try {
        if (sa.getId().equals(id)) {
          return sa;
        }
      } catch (Exception e) {
        LOG.error("Unexpected error looking for server authorization [id: {}]", 
            id, e);
      }
    }
    
    return null;
  }
  
  /**
   * @param serverAuthorizations
   *          the serverAuthorizations to set
   */
  public final void setServerAuthorizations(
      List<ServerAuthorization> serverAuthorizations) {
    this.serverAuthorizations = serverAuthorizations;
  }

  public final void setSessionData(Map<String, Object> map) {
    if (map == null) {
      sessionData = new PassiveExpiringMap<String, Object>(ENTRY_EXPIRATION_TIME);
    } else {
      sessionData = new PassiveExpiringMap<String, Object>(ENTRY_EXPIRATION_TIME,
          map);
    }
  }

  /**
   * @return the authorizationEndpoint
   */
  public final String getAuthorizationEndpoint() {
    return authorizationEndpoint;
  }

  /**
   * @param authorizationEndpoint
   *          the authorizationEndpoint to set
   */
  public final void setAuthorizationEndpoint(String authorizationEndpoint) {
    this.authorizationEndpoint = authorizationEndpoint;
    if (this.authorizationEndpoint != null
        && !this.authorizationEndpoint.startsWith("/")) {
      this.authorizationEndpoint = "/" + this.authorizationEndpoint;
    }
  }

  /**
   * @return the clientId
   */
  public final String getClientId() {
    return clientId;
  }

  /**
   * @param clientId
   *          the clientId to set
   */
  public final void setClientId(String clientId) {
    this.clientId = clientId;
  }

  /**
   * @return the sessionData
   */
  public final Map<String, Object> getSessionData() {
    return sessionData;
  }

  /**
   * @return the authorizationServer
   */
  public final String getAuthorizationServer() {
    return authorizationServer;
  }

  /**
   * @param authorizationServer
   *          the authorizationServer to set
   */
  public final void setAuthorizationServer(String authorizationServer) {
    this.authorizationServer = authorizationServer;

    // Remove any trailing slash
    if (this.authorizationServer != null
        && this.authorizationServer.endsWith("/")) {
      this.authorizationServer = this.authorizationServer.substring(0,
          this.authorizationServer.length() - 1);
    }
  }

  /**
   * @return the accessTokenEndpoint
   */
  public final String getAccessTokenEndpoint() {
    return accessTokenEndpoint;
  }

  /**
   * @param accessTokenEndpoint
   *          the accessTokenEndpoint to set
   */
  public final void setAccessTokenEndpoint(String accessTokenEndpoint) {
    this.accessTokenEndpoint = accessTokenEndpoint;
    if (this.accessTokenEndpoint != null
        && !this.accessTokenEndpoint.startsWith("/")) {
      this.accessTokenEndpoint = "/" + this.accessTokenEndpoint;
    }
  }

  /**
   * @return the clientSecret
   */
  public final String getClientSecret() {
    return clientSecret;
  }

  /**
   * @param clientSecret
   *          the clientSecret to set
   */
  public final void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

}
