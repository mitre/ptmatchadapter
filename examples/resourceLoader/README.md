Resource Loader
===============

This example application loads one or more FHIR resources from files in a
specified directory and posts each to a specified FHIR server.  If the input FHIR resource is a Bundle, each resource inside the Bundle is posted to the FHIR server individually.

Currently, only deduplication mode support is implemented.

This application is UNFINISHED.


## Configuration

src/main/resources/application.properties
 
## Running the Application

### Using Gradle
1. Open a command line console.  
2. Change your working directory to the project's top level folder (i.e., ptmatchadapter).
3. Enter the following and press the Enter key: 
   
   gradlew :examples:resourceLoader:bootRun


## Stopping the Application

Enter <Ctrl>-c in the console in which the application was started.



## __TO DO__
* Add support for command line options
  * location of input file(s)
  * accepted file format (currently hard-wired to .xml and .json)
  * destination file server
* Add unit tests
* Test with XML files
* Test more files (more than Patient resources; big bundles, empty bundles)
* Add error checking
* Add support to add a tag to posted resource
* Automatically stop the application after all resources have been loaded.


## License

Copyright 2016 The MITRE Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
