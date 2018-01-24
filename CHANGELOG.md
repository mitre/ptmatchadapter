# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/) 
and this project adheres to [Semantic Versioning 2.0.0 ](http://semver.org/).

## [Unreleased]
### Added
- Ability to authenticate with OAuth 2.0 authorization server using a 
  JSON Web Key in accordance with the [HEART](http://openid.net/wg/heart/) profile.
- /api-doc endpoint, which returns Swagger 2.0 definitions for REST
  services under /mgr.
- CHANGELOG.md

### Changed
- jar dependencies at top level
  - spring boot from 1.3.2.RELEASE to to 1.4.0.RELEASE
  - springVersion: from 4.2.4.RELEASE to 4.3.2.RELEASE 
  - camelVersion: from 2.16.2 to 2.17.3
  - hapiFhirVersion: from 1.4 to 1.5
  - slf4jVersion: from 1.7.18 to 1.7.18+
  - junitVersion: from 4.12 to 4.12+
- jar dependencies in examples/ptmatchadapter-fril
  -  'xerces:xercesImpl:2.11.0' to 'xerces:xercesImpl:2.11.+'
  -  'log4j:log4j:1.2.17' to 'log4j:log4j:1.2.17+'
- jar dependencies in ptmatchadapter-common
  -  'ch.qos.logback:logback-classic:1.1.5' to ch.qos.logback:logback-classic:1.1.5+'

## 0.0.1-SNAPSHOT
Initial version
