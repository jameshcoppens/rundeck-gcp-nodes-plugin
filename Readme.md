Rundeck GCP Nodes Plugin
========================

Version: 0.1.0-BETA

This is a Resource Model Source plugin for [RunDeck][] 1.5+ that provides
Google Cloud Platform GCE Instances as nodes for the RunDeck server.

[RunDeck]: http://rundeck.org

The plugin is currently in BETA, if you have any issues please create an issue so that I
can resolve it quickly.

Installation
------------

Download from the [releases page](https://github.com/jameshcoppens/rundeck-gcp-nodes-plugin/releases).

Put the `rundeck-gcp-nodes-plugin-0.1.0-BETA.jar` into your `$RDECK_BASE/libext` dir.

You must also authenticate the rundeck-gcp-nodes-plugin to your google cloud platform
project.
        * Log into your Google Cloud Platform console, go to the API-Manager, then go to
                credentials
        * Then go to Create Credentials, Service account key.  Under the service account 
                drop down select New Service Account. Name the service account
                rundeck-gcp-nodes-plugin.  Make sure the key type is JSON
        * rename the JSON file to rundeck-gcp-node-plugin.json and place it in /etc/rundeck/

I am currently running my rundeck server in a private subnet, which can not utilize user
authentication within oauth to allow for API access.  In subsuquent versions in which
users are leveraging rundeck in a public subnet that can allow for url callback, I will
account for that.
