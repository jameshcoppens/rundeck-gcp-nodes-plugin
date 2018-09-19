[![Build Status](https://travis-ci.org/Neutrollized/rundeck-gcp-nodes-plugin.svg?branch=master)](https://travis-ci.org/Neutrollized/rundeck-gcp-nodes-plugin)
[![Code Climate](https://codeclimate.com/github/Neutrollized/rundeck-gcp-nodes-plugin.png)](https://codeclimate.com/github/Neutrollized/rundeck-gcp-nodes-plugin)

# Rundeck GCP Nodes Plugin
[![GitHub release](https://img.shields.io/badge/release-v0.3.0-blue.svg)](https://github.com/Neutrollized/rundeck-gcp-nodes-plugin/releases)

### [CHANGELOG](https://github.com/Neutrollized/rundeck-gcp-nodes-plugin/blob/master/CHANGELOG.md)

This is a Resource Model Source plugin for [Rundeck][] 2.7.1+ that provides
Google Cloud Platform GCE Instances as nodes for the Rundeck server.

Confirmed to work for Rundeck 3.0.1 ...for the most part anyway.  It doesn't seem to honor `hostname.selector=networkInterfaces` if you want it to show the IP instead of hostname for `Host` in the `User @ Host` column (this is also your connection string)

UPDATE: works for Rundeck 3.0.5-20180828


[Rundeck]: http://rundeck.org


## Installation

Download from the [releases page](https://github.com/Neutrollized/rundeck-gcp-nodes-plugin/releases).

Put the `rundeck-gcp-nodes-plugin-0.3.0.jar` into your `$RDECK_BASE/libext` dir.

You must also authenticate the rundeck-gcp-nodes-plugin to your google cloud platform
project.
* Log into your Google Cloud Platform console, go to the API-Manager, then go to credentials
* Then go to Create Credentials, Service account key.  Under the service account drop down select New Service Account. Name the service account rundeck-gcp-nodes-plugin.  Make sure the key type is JSON
* IAM roles required: Project `Browser` & Compute Engine `Compute Viewer`  
* rename the JSON file to `rundeck-gcp-nodes-plugin.json` and place it in /etc/rundeck/


## Requirements

You will need to add the following labels to your GCP VMs if you want more accurate/meaning full values for the OS (because unfortunately, there currently isn't that data in a standalone field/value that describes that for your VM -- just look at the output of `gcloud compute instances describe`):
* `environment` (example value: prod)
* `osfamily` (example value: linux)
* `osname` (example value: rhel7)


## Notes

By default, your conection string (denoted by the `User @ Host` column in your project nodes page) is `rundeck@hostname`, but if you want it to show IP instead you can set the `hostname.selector` attribute to `networkInterfaces` or `accessConfigs` for internal and external(NAT) IPs respectively

### Bugs/TODOs:

My intention was for the labels (see "Requirements" above) to be optional with a default value provided, however that part doesn't seem to be working and if you don't have said labels, you will have no tags :( I'll look to fix that soon.

`Only Running Instances` in the resouce config doesn't work, it will always report the nodes regardless of state.

When Rundeck 3.1 gets released sometime down the road, that's when I'll switch development/bugfixes of this plugin to Rundeck 3.x only while just retaining the older version in the releases section...or I might split them into separate branches instead...haven't decided yet.


### Disclaimer

My work is built off of the work done by [jameshcoppens](https://github.com/jameshcoppens/rundeck-gcp-nodes-plugin) and I've only branched it off to updated/maintain it seeing as there are typos in the README that has gone unaddressed and hasn't been updated for ~2+ years.  There were some functionality/features I wanted to add for my own use (and at work) so here we are... :)


### What I've done so far...

* fixed the documentation for installing the plugin
* changed the fields that get populated when you expand a node in Rundeck as some in the original plugin just wasn't working or was putting in wrong values
* updated `rundeck-core` to `2.7.1`, `rundeckPluginVersion` to `1.2` and `google-api-servies-compute` plugins
* cleaned up the code to remove any AWS references in the code as well as plugins that aren't used (as it was initially modified off of [Rundeck's EC2 nodes plugin](https://github.com/rundeck-plugins/rundeck-ec2-nodes-plugin))
* reduced the .jar file size from ~8MB to ~5.5MB
* improved maintainability and readability


## !!! PRs welcome as I'm still new at this (my Java sucks)!!!
