Resource Retriever
==================

This example application invokes a given URL expected to represent a FHIR search.
The resources in the returned Bundle are extracted and can be written to 
individual files, posted to a specified destination FHIR server, or both. 

This application is UNFINISHED.

## Configuration

application.properties
src.fhir.server.base
searchExpr
doWriteToFile
doPostToFhirServer
dest.fhir.server.base

## Running the Application

### Using Gradle Wrapper
1. Open a command line console.  
2. Change your working directory to the project's top level folder (i.e., ptmatchadapter).
3. Enter the following and press the Enter key: 
   
   gradlew :examples:resourceRetriever:bootRun


## Stopping the Application

Enter <Ctrl>-c in the console in which the application was started.


## __TO DO__
* Add support for command line options
  * search URL
  * HTTP Method (POST or GET)
  * desired file format (currently hard-wired to .json)
  * destination file server
* Add unit tests
* Add error checking
* Add support to add a tag to posted resource
* Automatically stop the application after all resources have been processed.


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
