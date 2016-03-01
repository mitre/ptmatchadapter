Resource Loader
===============

This example application loads one or more FHIR resources from files in a
specified directory and posts each to a specified FHIR server.  If the input FHIR resource is a Bundle, each resource inside the Bundle is posted to the FHIR server individually.

This application is UNFINISHED.

## Configuration

application.properties
 
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