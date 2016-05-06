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

package org.mitre.ptmatchadapter.gui;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public class AdapterConfiguration implements Processor {
  private static final Logger LOG = LoggerFactory.getLogger(AdapterConfiguration.class);
  
  public void process(Exchange exchange) throws Exception {
    exchange.getOut().setBody(
        "received [" + exchange.getIn().getBody()
            + "] as an order id = "
            + exchange.getIn().getHeader("id"));
    final Map<String, Object> headers = exchange.getIn().getHeaders();
    for (String key : headers.keySet()) {
      LOG.info("key: {}  value: {}", key, headers.get(key));
    }
  }
}
