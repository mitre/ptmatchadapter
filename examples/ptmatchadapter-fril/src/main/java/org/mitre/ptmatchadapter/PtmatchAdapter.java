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
  private String webServerIpAddr = "localhost";

  @Override
  public void configure() {
    restConfiguration().component("jetty")
      .host(webServerIpAddr).port(webServerPort);
    // Bug in Camel 2.16.2 (fixed since then, but not released) results in 
    // exception at startup when multiple rest() services are specified in a 
    // Spring Boot application.
    // http://stackoverflow.com/questions/33291657/how-to-have-multiple-camel-rest-dsl-definitions-with-swagger
    // https://issues.apache.org/jira/browse/CAMEL-9247
      // add swagger api-doc out of the box
//      .apiContextPath("/api-doc")
//      .apiProperty("api.title", "Patient Matcher FRIL Adapter API")
//      .apiProperty("api.version", "0.0.1")
//      //and enable CORS
//      .apiProperty("cors", "true");
    
    rest("/mgr").description("Adapter Management rest service")
      .consumes("application/json").produces("application/json")

//    .get("/{id}").description("Find user by id").outType(User.class)
//        .to("bean:userService?method=getUser(${header.id})")
//
//    .put().description("Updates or create a user").type(User.class)
//        .to("bean:userService?method=updateUser")
//
//    .get("/findAll").description("Find all users").outTypeList(User.class)


      .get("/hello").description("basic greeting").outType(String.class).produces("text/plain")
        .to("direct:hello")
      .get("/say/hello").to("direct:hello1").produces("text/plain");
    
    //rest("/hello").get().to("direct:hello");
    //rest("/say/hello1").get().to("direct:hello1");
    
    from("direct:hello").transform().simple("Hello World!");
  }

  public static void main(String... args) {

    // Create a one-time task that will open a url to config page in a browser
    // after the application has been given a couple of seconds to start up.
    new Timer().schedule(new TimerTask() {
      @Override
      public void run() {
        LOG.info("============= Open URL in Browser");
        // Open a Browser window for the user
        BareBonesBrowserLaunch.openURL("http://localhost:8082/mgr/hello");
        LOG.info("============= After Open URL in Browser");
      }
    }, 8000);

    LOG.info("============= Call Fat Jar Router Main");
    // Call Fat Jar Router main last because it never returns
    FatJarRouter.main(args);
    LOG.info("============= Returned from Fat Jar Router Main");
  }
}
