Rundeck GCP Nodes Plugin
========================

Version: 0.1.3

This is a Resource Model Source plugin for [RunDeck][] 2.7.1+ that provides
Google Cloud Platform GCE Instances as nodes for the RunDeck server.

[RunDeck]: http://rundeck.org

The plugin is currently in BETA, if you have any issues please create an issue so that I
can resolve it quickly.


Installation
--------------
Download from the [releases page](https://github.com/Neutrollized/rundeck-gcp-nodes-plugin/releases).

Put the `rundeck-gcp-nodes-plugin-0.1.3.jar` into your `$RDECK_BASE/libext` dir.

You must also authenticate the rundeck-gcp-nodes-plugin to your google cloud platform
project.
        * Log into your Google Cloud Platform console, go to the API-Manager, then go to
                credentials
        * Then go to Create Credentials, Service account key.  Under the service account 
                drop down select New Service Account. Name the service account
                rundeck-gcp-nodes-plugin.  Make sure the key type is JSON
        * rename the JSON file to rundeck-gcp-nodes-plugin.json and place it in /etc/rundeck/


Requirements
------------
You will need/want to add the following labels to your GCP VMs if you want more accurate/meaning full values for the OS (because unfortunately, there currently isn't hat data in a standalone field/value taht describues that for your VM -- just look at the output of `gcloud compute instances describe`):
* environment (defaults to: test; example value: prod)
* osfamily (defaults to: linux; example value: windows)
* osname (defaults to: unknown; example value: rhel7)


Disclaimer
----------
My work is built off of the work done by [jameshcoppens](https://github.com/jameshcoppens/rundeck-gcp-nodes-plugin) and I've only branched it off to updated/maintain it seeing as there are typos in the Readme that has gone unaddressed and hasn't been updated for ~2+ years.  There were some functionality/features I wanted to add for my own use (and at work) so here we are... :)


What I've done so far...
------------------------
* fixed the documentation for installing the plugin
* cleaned up the code to remove any AWS references in the code as well as plugins that aren't used (as it was initially modified off of [Rundeck's EC2 nodes plugin](https://github.com/rundeck-plugins/rundeck-ec2-nodes-plugin)
* bumped up the rundeck-core to 2.7.1
* updated rundeckPluginVersion to 1.2
* updated the google-api-servies-compute plugin to the latest (v1-rev183-1.23.0)
* reduced the .jar file size from ~8MB to ~5.5MB
* changed the fields that get populated when you expand a node in Rundeck as some in the original plugin just wasn't working or was putting in wrong values


TODO
----
...lots...I have great plans for this plugin


#!!! PRs welcome as I'm still new at this (my Java sucks)!!!
