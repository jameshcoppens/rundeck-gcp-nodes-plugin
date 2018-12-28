[![Build Status](https://travis-ci.org/Neutrollized/rundeck-gcp-nodes-plugin.svg?branch=rundeck-3.0.9)](https://travis-ci.org/Neutrollized/rundeck-gcp-nodes-plugin)
[![Code Climate](https://codeclimate.com/github/Neutrollized/rundeck-gcp-nodes-plugin.png)](https://codeclimate.com/github/Neutrollized/rundeck-gcp-nodes-plugin)

# Rundeck GCP Nodes Plugin
[![GitHub release](https://img.shields.io/badge/release-v3.0.9--1-blue.svg)](https://github.com/Neutrollized/rundeck-gcp-nodes-plugin/releases)

### [CHANGELOG](https://github.com/Neutrollized/rundeck-gcp-nodes-plugin/blob/master/CHANGELOG.md)

This is a Resource Model Source plugin for [Rundeck](https://www.rundeck.org) 3.0.9+ that provides Google Cloud Platform GCE Instances as nodes for the Rundeck Server.

If you're still running Rundeck v2.x, please use one of the releases labeled v2.7.1-2 or earlier.


## Installation

Download from the [releases page](https://github.com/Neutrollized/rundeck-gcp-nodes-plugin/releases).

Put the `rundeck-gcp-nodes-plugin-3.0.9-1.jar` into your `$RDECK_BASE/libext` dir.

You must also authenticate the rundeck-gcp-nodes-plugin to your google cloud platform
project.
* Log into your Google Cloud Platform console, go to the API-Manager, then go to credentials
* Then go to Create Credentials, Service account key.  Under the service account drop down select New Service Account. Name the service account rundeck-gcp-nodes-plugin.  Make sure the key type is JSON
* IAM roles required: Project `Browser` & Compute Engine `Compute Viewer`  
* Rename the JSON file to `rundeck-gcp-nodes-plugin-PROJECTID.json` and place it in /etc/rundeck/ (replace `PROJECTID` with your GCP project id)


## Requirements

You will need to add the following labels to your GCP VMs if you want more accurate/meaning full values for the OS (because unfortunately, there currently isn't that data in a standalone field/value that describes that for your VM -- just look at the output of `gcloud compute instances describe`):
* `environment` (example value: prod)
* `osfamily` (example value: linux)
* `osname` (example value: rhel7)

*** No longer a requirement from `v2.7.1-2` onward ***


## Notes

By default, your connection string (denoted by the `User @ Host` column in your project nodes page) is `rundeck@hostname`, but if you want it to show IP instead you can set the `hostname.selector` attribute to `networkInterfaces` or `accessConfigs` for internal and external(NAT) IPs respectively

### Bugs/TODOs:

`Only Running Instances` in the resource config doesn't work, it will always report the nodes regardless of state.

When Rundeck 3.1 gets released sometime down the road, that's when I'll switch development/bugfixes of this plugin to Rundeck 3.x only while just retaining the older version in the releases section...or I might split them into separate branches instead...haven't decided yet.


### Disclaimer

My work is built off of the work done by [jameshcoppens](https://github.com/jameshcoppens/rundeck-gcp-nodes-plugin) and I've only branched it off to updated/maintain it seeing as there are typos in the README that has gone unaddressed and hasn't been updated for ~2+ years.  There were some functionality/features I wanted to add for my own use (and at work) so here we are... :)


## !!! PRs welcome as I'm still new at this (my Java sucks)!!!
