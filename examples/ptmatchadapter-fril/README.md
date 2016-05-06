Patient Match Adapter for FRIL
==========================

This example application demonstrates a representative processing workflow
for an adapter between the ptmatch test harness and a record matching system.

To run, set the working directory to the project top level (i.., ptmatchadapter)


## Running the Application

### Using Gradle
1. Open a command line console.  
2. Change your working directory to the project's top level folder (i.e., ptmatchadapter).
3. Enter the following one line line and press the Enter key: 
   
gradlew :examples:ptmatchadapter-fril:bootRun 
   

## Stopping the Application

Enter <Ctrl>-c in the console in which the application was started.

To stop the application, enter <ctrl>-C in the console windows.

## JMX Monitoring

The Patient Match Adapter for FRIL incorporates the Jolokia JMX agent library.
One can use an application like jconsole (provided with Oracle Java) or a
hawt.io JMX client to view information about the record match job processing.
 
The Jolokia endpoint for JMX clients is at http://<host>:8778/jolokia

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


## TO DO (Wish List)
1. Use HTTP POST when performing the FHIR search to retrieve the data that 
is to be used in the record matching operation.  When using GET, the search
parameters are likely stored in the FHIR server's access log, which may be
a privacy concern.
