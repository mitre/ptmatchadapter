Patient Match Adapter for FRIL
==========================

This example application demonstrates a representative processing workflow
for an adapter between the ptmatch test harness and a record matching system.

To run, set the working directory to the project top level (i.., ptmatchadapter)

## Configuring the Application
Set properties in the file src/main/resources/application.properties.

Properties of particular interest include:

* oauth2.authorization.server - full url to the OAuth 2.0 authorization server
* oauth2.authorization.authCodeEndpoint - relative path to the endpoint at which to request authorization code
* oauth2.authorization.accessTokenEndpoint - relative path to the endpoint at which to request an access token
* ptmatchadapter.oauth2.clientID - client ID assigned by an OAuth 2 authorization server when the ptmatch adapter is registered as a client
* ptmatchadapter.oauth2.clientSecret - code  provided by an OAuth 2.0 authorization server when the ptmatch adapter is registered as a client and the HEART profile is NOT used.
* ptmatchadapter.key.location - absolute path to a file containing a JSON Web Key store with a key that will be used to authenticate with the authentication provider in accordance with the HEART profile.



## Running the Application

### Using Gradle
1. Open a command line console.  
2. Change your working directory to the project's top level folder (i.e., ptmatchadapter).
3. Enter the following one line line and press the Enter key: 
   
./gradlew :examples:ptmatchadapter-fril:bootRun 

After starting, the application will open a browser window within which you can specify an OpenID Connect server with which to associate the ptmatch adapter. 


## Stopping the Application

Enter <Ctrl>-c in the console in which the application was started.

To stop the application, enter <ctrl>-C in the console windows.

## JMX Monitoring

The Patient Match Adapter for FRIL incorporates the Jolokia JMX agent library.
One can use an application like jconsole (provided with Oracle Java) or a
hawt.io JMX client to view information about the record match job processing.
 
The Jolokia endpoint for JMX clients is at http://<host>:8778/jolokia

## Configuration for [HEART](http://openid.net/wg/heart/) profile
### Generating a JSON Public/Private Key Pair
A pre-compiled version of the open source application, json-web-key-generator, is provided for convenience. https://github.com/mitreid-connect/json-web-key-generator

To generate a key pair, open a command window, change the working directory
to the ptmatchadapter-fril folder and run the following:
$ java -jar etc/json-web-key-generator-0.4-SNAPSHOT-jar-with-dependencies.jar -t RSA -s 2048 -p > etc/ptmkey.json

Make a copy of this file.

$ cp etc/ptmkey.json etc/ptmkey.pub.json

Open ptmkey.json in a text editor and delete the first line "Full key:" and all 
lines starting with and following the line with text, "Public key:". 
Save the file and exit the editor.

Open ptmkey.pub.json in a text editor and delete every line up to and including
"Public key:". 

Enter the following two lines at the very beginning of the file (i.e., before
the first character.
```
{"keys":
 [
```

Enter the following two lines at the very end of the file (i.e., after the final 
character).
```
  ]
 }
 ```
 

Save the file and exit the editor.

Open ptmkey.pub.json in a text editor.

Delete all lines up to an including Public Key.

Enter the following two lines at the very beginning of the file (i.e., before the first character.
```
{"keys":
 [
```

Enter the following two lines at the very end of the file (i.e., after the final character).
```
  ]
 }
 ```

After performing the above steps, ptkey.json has both the public and private key.  It should be considered private data.  The file, ptmkey.pub.json contains only the public key and is provided to the OAuth 2 authorization server when registering the ptmatch adapter as a client.

Note that the files containing the public and private keys can be any name 
and reside in any folder.


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
