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

import org.hl7.fhir.instance.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.rest.client.IGenericClient;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public class NextPageLoader {
  private static final Logger LOG = LoggerFactory.getLogger(NextPageLoader.class);

  private IGenericClient client;

  /**
   * If the given Bundle has a non-null url for
   * 
   * @param bundle
   * @return
   */
  public Bundle loadNext(Bundle bundle) {
    Bundle nextPage = null;

    if (bundle != null && bundle.getLink(Bundle.LINK_NEXT) != null) {
      try {
        // load next page
        nextPage = client.loadPage().next(bundle).execute();
      } catch (RuntimeException e) {
        LOG.error("Loading Page for bundle: {}", bundle.getId(), e);
      }
    }

    return nextPage;
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
}
