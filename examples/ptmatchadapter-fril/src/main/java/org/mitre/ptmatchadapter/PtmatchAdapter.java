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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.spring.boot.FatJarRouter;
import org.mitre.ptmatchadapter.model.ServerAuthorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ImportResource;

import com.centerkey.utils.BareBonesBrowserLaunch;




@SpringBootApplication
@ImportResource("beans-config.xml")
public class PtmatchAdapter extends FatJarRouter {
  public static final Logger LOG = LoggerFactory.getLogger(PtmatchAdapter.class);

  // value will be retrieved from application.properties
  @Value("${ptmatchadapter.web.port}")
  private int webServerPort = 8082;

  // value will be retrieved from application.properties
  @Value("${ptmatchadapter.web.ipaddress}")
  private String webServerIpAddr = "0.0.0.0";

  @Value("${ptmatchadapter.web.enableCORS}")
  private boolean isCorsEnabled = false;
  
  @Override
  public void configure() {
    restConfiguration().component("jetty").bindingMode(RestBindingMode.json)
      .host(webServerIpAddr).port(webServerPort)
    // Note: Bug in Camel 2.16.2 (fixed after that) results in 
    // exception at startup when multiple rest() services are specified in a 
    // Spring Boot application.
    // http://stackoverflow.com/questions/33291657/how-to-have-multiple-camel-rest-dsl-definitions-with-swagger
    // https://issues.apache.org/jira/browse/CAMEL-9247
      // add swagger api-doc out of the box
      .apiContextPath("/api-doc")
      .apiProperty("api.title", "Patient Matcher FRIL Adapter API")
      .apiProperty("api.version", "0.0.1")
      //and enable CORS
      .apiProperty("cors", "true");
    
    rest("/mgr").description("Record Matching System Adapter Management rest service")

      .get("/serverAuthorization").description("Retrieve list of server authorizations")
        .enableCORS(true)
        .outType(ServerAuthorization.class)
        .to("bean:serverAuthorizationService?method=getServerAuthorizations")

      .get("/serverAuthorization/{id}").description("Find server authorization by id")
        .enableCORS(true)
        .outType(ServerAuthorization.class)
        .to("bean:serverAuthorizationService?method=getServerAuthorization(${header.id})")

      .post("/serverAuthForm")
        .bindingMode(RestBindingMode.off)
        .description("Store User Authorization for ptmatch adapter to access a server")
        .type(String.class)
        .to("bean:serverAuthorizationService?method=createFromForm")

      .post("/serverAuthorization")
        .description("Store User Authorization for ptmatch adapter to access a server")
        .enableCORS(true)
        .type(ServerAuthorization.class)
        .outType(ServerAuthorization.class)
        .to("bean:serverAuthorizationService")

      .options("/serverAuthorization")
        .to("bean:serverAuthorizationService?method=handleOptions");
        
  }

  
  public static void main(String... args) {
    // Create a one-time task that will open a url to config page in a browser
    // after the application has been given a couple of seconds to start up.
    new Timer().schedule(new TimerTask() {
      @Override
      public void run() {
        LOG.info("============= Open URL in Browser");
        // Open a Browser window for the user
        BareBonesBrowserLaunch.openURL("http://localhost:8082/index.html");
        LOG.info("============= After Open URL in Browser");
      }
    }, 10200);


    LOG.info("============= Call Fat Jar Router Main");
    // Call Fat Jar Router main last because it never returns
    FatJarRouter.main(args);
    LOG.info("============= Returned from Fat Jar Router Main");
  }

}
