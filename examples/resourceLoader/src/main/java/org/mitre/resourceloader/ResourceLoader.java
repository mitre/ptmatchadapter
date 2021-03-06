/**
 * Resource Loader - an application that can load FHIR Resources from a file
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
package org.mitre.resourceloader;

import org.apache.camel.spring.boot.FatJarRouter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@ImportResource("beans-config.xml")
@ComponentScan("org.mitre.ptmatchadapter")
public class ResourceLoader extends FatJarRouter {

  @Override
  public void configure() {
   // from("timer:trigger").transform().simple("ref:myBean").to("log:out");
  }

//  @Bean
//  String myBean() {
//    return "I'm Spring bean!";
//  }

}
