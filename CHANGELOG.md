# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [0.2.2] - 2018-07-07
### Changed
- bump `google-api-services-compute` plugin to `v1-rev190-1.23.0`
### Added
- Code Climate badge to README.md
### Removed
- further clean up of Java code based on report from [Code Climate](https://codeclimate.com) that I'm trying out to see where I can improve on in terms of code quality
- 3 unused methods in `InstanceToNodeMapper.java`: `getFilterParams()`, `getProjectId()` and `getMapping()`


## [0.2.1] - 2018-06-27
### Changed
- bump `google-api-services-compute` plugin to `v1-rev188-1.23.0`
- I used [BFG Repo-Cleaner](https://rtyley.github.io/bfg-repo-cleaner/) to try to clean up the repo but in doing so I messed up all the tagging, so I'm going to bump the minor version up to `v0.2.1` instead (couldn't get gradlew to tag the release at `v0.2.0` so I'm not going to fight it -- did I mention I'm sorta new at this?)
### Added
- `.travis.yml` + "build" badge to README.md


## [0.1.5] - 2018-06-18
### Changed
- bump `google-api-services-compute` plugin to `v1-rev186-1.23.0`
### Removed
- further clean up of Java code

## [0.1.4] - 2018-06-06
### Changed
- change the default tag from a static `gce` to the `labels.environment`, `labels.osname` and `projectId`
- removed a lot of the error-level logging that was in the code (presumably for debug purposes) and set most of them to info-level (except for actual errors, of course)
### Added
- fields: `projectId`

## [0.1.3] - 2018-06-03
### Added
- a lot more documentation to README.md :)
### Removed
- fields: `sshport` (as well as Java code relating to this)
- deleted more Java code which refernced AWS and its resources (endpoints in particular)

## [0.1.2] - 2018-06-02
### Changed
- bump `axion-release plugin` to `1.9.1`
- bump `sourceCompatibility` to `1.8`
- bump `rundeckPluginVersion` to `1.2`
- bump `rundeck-core` to `2.7.1`
- bump `commons-beanutils` plugin to `1.9.3`
- bump `google-api-services-compute` plugin to `v1-rev183-1.23.0`
- bump `gradleVersion` to `4.7`
- renamed `privateIpAddress` to `internalIp` to match GCP naming
### Added
- fields: `environment`, `selfLink`
### Removed
- `aws-java-sdk-ec2` plugin
- fields: `editUrl`, `osArch`, `privateDnsName`
- deleted some Java code which referenced AWS and its resources

## [0.1.1-BETA] - 2016-03-20
### Added
- original rundeck-gcp-nodes-plugin from jameshcoppens
