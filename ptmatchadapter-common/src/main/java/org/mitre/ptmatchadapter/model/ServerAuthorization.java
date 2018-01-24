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

package org.mitre.ptmatchadapter.model;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
@XmlRootElement(name = "serverAuthorization")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerAuthorization {
  @XmlElement
  private String id;
  @XmlElement
  private String title;
  @XmlElement
  private String description;
  
  @XmlElement
  private String serverUrl;
  @XmlElement
  private String accessToken;
  @XmlElement
  private String tokenType;

  @XmlElement
  private Date expiresAt;
  
  @XmlElement
  private String refreshToken;
  
  private String idToken;
  
  @XmlElement
  private String scope;
  
  /**
   * @return the id
   */
  public final String getId() {
    return id;
  }
  /**
   * @param id the id to set
   */
  public final void setId(String id) {
    this.id = id;
  }
  /**
   * @return the refreshToken
   */
  public final String getRefreshToken() {
    return refreshToken;
  }
  /**
   * @param refreshToken the refreshToken to set
   */
  public final void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }
  /**
   * @return the serverUrl
   */
  public final String getServerUrl() {
    return serverUrl;
  }
  /**
   * @param serverUrl the serverUrl to set
   */
  public final void setServerUrl(String serverUrl) {
    // Remove any trailing slash
    if (serverUrl != null && serverUrl.endsWith("/")) {
      this.serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
    } else {
      this.serverUrl = serverUrl;
    }
  }

  /**
   * @return the accessToken
   */
  public final String getAccessToken() {
    return accessToken;
  }
  /**
   * @param accessToken the accessToken to set
   */
  public final void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }
  /**
   * @return the tokenType
   */
  public final String getTokenType() {
    return tokenType;
  }
  /**
   * @param tokenType the tokenType to set
   */
  public final void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }
  /**
   * @return the expiration date-time
   */
  public final Date getExpiresAt() {
    return new Date(expiresAt.getTime());
  }
  /**
   * @param expiration the expiration to set
   */
  public final void setExpiresAt(Date expiration) {
    this.expiresAt = expiration != null ? new Date (expiration.getTime()) : null;
  }
  /**
   * @return the scope
   */
  public final String getScope() {
    return scope;
  }
  /**
   * @param scope the scope to set
   */
  public final void setScope(String scope) {
    this.scope = scope;
  }
  /**
   * @return the idToken
   */
  public final String getIdToken() {
    return idToken;
  }
  /**
   * @param idToken the idToken to set
   */
  public final void setIdToken(String idToken) {
    this.idToken = idToken;
  }
  /**
   * @return the title
   */
  public final String getTitle() {
    return title;
  }
  /**
   * @param title the title to set
   */
  public final void setTitle(String title) {
    this.title = title;
  }
  /**
   * @return the description
   */
  public final String getDescription() {
    return description;
  }
  /**
   * @param description the description to set
   */
  public final void setDescription(String description) {
    this.description = description;
  }
}
