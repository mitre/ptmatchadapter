Resource Loader
===============

This example application loads a FHIR resource from a file and posts it to a
specified FHIR server.  If the input FHIR resource is a Bundle, each resource
inside the Bundle is posted to the FHIR server.

This application is UNFINISHED.


__TO DO__
* Add support for command line options
  * location of input file(s)
  * accepted file format (currently hard-wired to .xml and .json)
  * destination file server
* Add unit tests
* Test with XML files
* Test more files (more than Patient resources; big bundles, empty bundles)
* Add error checking
* Add support to add a tag to posted resource