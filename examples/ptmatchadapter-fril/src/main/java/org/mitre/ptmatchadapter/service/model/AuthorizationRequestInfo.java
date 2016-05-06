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
package org.mitre.ptmatchadapter.service.model;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public class AuthorizationRequestInfo {

  // type of response that should be created: html or json
  private String responseType = "json";
  
  private Map<String, Object> stateVars = new HashMap<String, Object>();
  
  /** number of milliseconds since Java epoch at which object was created. */ 
  private long createdOn = System.currentTimeMillis();
  

  /**
   * @return the createdOn
   */
  public final long getCreatedOn() {
    return createdOn;
  }

  /**
   * @param createdOn the createdOn to set
   */
  public final void setCreatedOn(long createdOn) {
    this.createdOn = createdOn;
  }

  /**
   * @return the responseType
   */
  public final String getResponseType() {
    return responseType;
  }

  /**
   * @param responseType the responseType to set
   */
  public final void setResponseType(String responseType) {
    this.responseType = responseType;
  }
  
  public final void put(String key, Object value) {
    stateVars.put(key, value);
  }
  
  public final Object get(String key) {
    return stateVars.get(key);
  }
}
