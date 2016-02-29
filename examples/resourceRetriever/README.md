Resource Retriever
==================

This example application invokes a given URL expected to represent a FHIR search.
The resources in the returned Bundle are extracted an posted to the specified
destination FHIR server. 

This application is UNFINISHED.


__TO DO__
* Add support for command line options
  * search URL
  * HTTP Method (POST or GET)
  * desired file format (currently hard-wired to .json)
  * destination file server
* Add unit tests
* Add error checking
* Add support to add a tag to posted resource