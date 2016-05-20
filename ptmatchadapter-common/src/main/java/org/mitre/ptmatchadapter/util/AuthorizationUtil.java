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
package org.mitre.ptmatchadapter.util;

import java.util.List;

import org.mitre.ptmatchadapter.model.ServerAuthorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public abstract class AuthorizationUtil {

  private static final Logger LOG = LoggerFactory
      .getLogger(AuthorizationUtil.class);

  public static final ServerAuthorization findServerAuthorization(
      List<ServerAuthorization> serverAuthorizations, String serverBase) {
    
    String slashlessBaseUrl;
    // Remove any trailing slash
    if (serverBase != null && serverBase.endsWith("/")) {
      slashlessBaseUrl = serverBase.substring(0, serverBase.length() - 1);
    } else {
      slashlessBaseUrl = serverBase;
    }
    
    if (serverAuthorizations != null && slashlessBaseUrl != null) {
      for (ServerAuthorization sa : serverAuthorizations) {
        LOG.info("findServerAuth serverUrl: {}  {}", sa.getServerUrl(), slashlessBaseUrl);
        try {
          if (sa.getServerUrl().equals(slashlessBaseUrl)) {
            return sa;
          }
        } catch (NullPointerException e) {
          // should never happen
          LOG.warn("NULL Server URL found for server authorization: {}",
              sa.getTitle());
        }
      }
    }
    return null;
  }

}
