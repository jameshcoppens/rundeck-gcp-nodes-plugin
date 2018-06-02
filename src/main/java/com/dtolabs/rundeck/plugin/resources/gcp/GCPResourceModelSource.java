/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
* GCPResourceModelSource.java
* 
* User: James Coppens <a href="mailto:jameshcoppens@gmail.com">jameshcoppens@gmail.com</a>
* Created: 9/1/11 4:34 PM
* User: Glen Yu <a href="mailto:glen.yu@gmail.com">glen.yu@gmail.com</a>
* Modified: 2018-06-02
* 
*/
package com.dtolabs.rundeck.plugin.resources.gcp;


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential.Builder;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.compute.model.Instance;

import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.resources.ResourceModelSource;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * GCPResourceModelSource produces nodes by querying the GCP Compute Engine API to list instances.
 * <p/>
 * The RunDeck node definitions are created from the instances on a mapping system to convert properties of the amazon
 * instances to attributes defined on the nodes.
 * <p/>
 * The Compute Engine requests are performed asynchronously, so the first request to {@link #getNodes()} will return null, and
 * subsequent requests may return the data when it's available.
 *
 * @author James Coppens <a href="mailto:jameshcoppens@gmail.com">jameshcoppens@gmail.com</a>
 *
 * <p/>
 * Modified, optimized, broken features fixed
 * <p/>
 * @author Glen Yu <a href="mailto:glen.yu@gmail.com">glen.yu@gmail.com</a>
 */
public class GCPResourceModelSource implements ResourceModelSource {
    static Logger logger = Logger.getLogger(GCPResourceModelSource.class);
    private String projectId;
    long refreshInterval = 30000;
    long lastRefresh = 0;
    String filterParams;
    String endpoint;
    String httpProxyHost;
    int httpProxyPort = 80;
    String httpProxyUser;
    String httpProxyPass;
    String mappingParams;
    File mappingFile;
    boolean useDefaultMapping = true;
    boolean runningOnly = true;
    boolean queryAsync = true;
    Future<INodeSet> futureResult = null;
    final Properties mapping = new Properties();

    GoogleCredential credential;

    INodeSet iNodeSet;
    static final Properties defaultMapping = new Properties();
    InstanceToNodeMapper mapper;

    static {
        final String mapping = "nodename.selector=name,id\n"
                               + "hostname.selector=name\n"
                               + "environment.default=test\n"
                               + "environment.selector=labels.environment\n"
                               + "sshport.default=22\n"
                               + "sshport.selector=tags/ssh_config_Port\n"
                               + "description.default=GCE node instance\n"
                               + "osFamily.selector=labels.osfamily\n"
                               + "osFamily.default=unix\n"
                               + "osName.selector=labels.osname\n"
                               + "osName.default=Linux\n"
                               + "username.selector=tags/Rundeck-User\n"
                               + "username.default=rundeck\n"
                               + "privateIpAddress.selector=networkInterfaces\n"
                               + "tags.selector=tags/Rundeck-Tags\n"
                               + "instanceId.selector=instanceId\n"
                               + "selfLink.selector=selfLink\n"
                               + "tag.running.selector=status=running\n"
                               + "tag.stopped.selector=status=stopped\n"
                               + "tag.stopping.selector=status=stopping\n"
                               + "tag.shutting-down.selector=status=shutting-down\n"
                               + "tag.terminated.selector=status=terminated\n"
                               + "tag.pending.selector=status=pending\n"
                               + "state.selector=status\n"
                               + "tags.default=gce\n";
        try {

            final InputStream resourceAsStream = GCPResourceModelSource.class.getClassLoader().getResourceAsStream(
                "defaultMapping.properties");
            if (null != resourceAsStream) {
                try {
                    defaultMapping.load(resourceAsStream);
                } finally {
                    resourceAsStream.close();
                }
            }else{
                //fallback in case class loader is misbehaving
                final StringReader stringReader = new StringReader(mapping);
                try {
                    defaultMapping.load(stringReader);
                } finally {
                    stringReader.close();
                }
            }

        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    public GCPResourceModelSource(final Properties configuration) {
        logger.error("GCPResourceModelSource Constructor");
        //this.clientId = configuration.getProperty(GCPResourceModelSourceFactory.CLIENT_ID);
        //this.clientSecret = configuration.getProperty(GCPResourceModelSourceFactory.CLIENT_SECRET);
        //this.httpProxyHost = configuration.getProperty(GCPResourceModelSourceFactory.HTTP_PROXY_HOST);
        this.projectId = configuration.getProperty(GCPResourceModelSourceFactory.PROJECT_ID);
        int proxyPort = 80;
        
        //final String proxyPortStr = configuration.getProperty(GCPResourceModelSourceFactory.HTTP_PROXY_PORT);
        /*if (null != proxyPortStr && !"".equals(proxyPortStr)) {
            try {
                proxyPort = Integer.parseInt(proxyPortStr);
            } catch (NumberFormatException e) {
                logger.warn(GCPResourceModelSourceFactory.HTTP_PROXY_PORT + " value is not valid: " + proxyPortStr);
            }
        }
        this.httpProxyPort = proxyPort;
        this.httpProxyUser = configuration.getProperty(GCPResourceModelSourceFactory.HTTP_PROXY_USER);
        this.httpProxyPass = configuration.getProperty(GCPResourceModelSourceFactory.HTTP_PROXY_PASS);
        */
        this.filterParams = configuration.getProperty(GCPResourceModelSourceFactory.FILTER_PARAMS);
        this.mappingParams = configuration.getProperty(GCPResourceModelSourceFactory.MAPPING_PARAMS);
        final String mappingFilePath = configuration.getProperty(GCPResourceModelSourceFactory.MAPPING_FILE);
        if (null != mappingFilePath) {
            mappingFile = new File(mappingFilePath);
        }
        int refreshSecs = 30;
        final String refreshStr = configuration.getProperty(GCPResourceModelSourceFactory.REFRESH_INTERVAL);
        if (null != refreshStr && !"".equals(refreshStr)) {
            try {
                refreshSecs = Integer.parseInt(refreshStr);
            } catch (NumberFormatException e) {
                logger.warn(GCPResourceModelSourceFactory.REFRESH_INTERVAL + " value is not valid: " + refreshStr);
            }
        }
        refreshInterval = refreshSecs * 1000;
        if (configuration.containsKey(GCPResourceModelSourceFactory.USE_DEFAULT_MAPPING)) {
            useDefaultMapping = Boolean.parseBoolean(configuration.getProperty(
                GCPResourceModelSourceFactory.USE_DEFAULT_MAPPING));
        }
        if (configuration.containsKey(GCPResourceModelSourceFactory.RUNNING_ONLY)) {
            runningOnly = Boolean.parseBoolean(configuration.getProperty(
                GCPResourceModelSourceFactory.RUNNING_ONLY));
        }
        //if (null != clientId && null != clientSecret) {
            try {
                credential = GoogleCredential.fromStream(new FileInputStream("/etc/rundeck/rundeck-gcp-nodes-plugin.json"))
                        .createScoped(Collections.singleton(ComputeScopes.COMPUTE_READONLY));
                logger.error("Google Crendential created successfully");
            } catch  (FileNotFoundException e) {
                logger.error("Google Crendential failed creation");
                System.err.println(e.getMessage());
            } catch (Throwable t) {
                t.printStackTrace();
            }
        //}

        /*if (null != httpProxyHost && !"".equals(httpProxyHost)) {
            clientConfiguration.setProxyHost(httpProxyHost);
            clientConfiguration.setProxyPort(httpProxyPort);
            clientConfiguration.setProxyUsername(httpProxyUser);
            clientConfiguration.setProxyPassword(httpProxyPass);
        }*/
        
        initialize();
    }

    private void initialize() {
        logger.error("initialize call");
        final ArrayList<String> params = new ArrayList<String>();
        if (null != filterParams) {
            Collections.addAll(params, filterParams.split(";"));
        }
        loadMapping();
        mapper = new InstanceToNodeMapper(credential, mapping);
        mapper.setProjectId(projectId);
        mapper.setFilterParams(params);
        mapper.setEndpoint(endpoint);
        mapper.setRunningStateOnly(runningOnly);
    }


    public synchronized INodeSet getNodes() throws ResourceModelSourceException {
        logger.error("getNodes call");
        checkFuture();
        if (!needsRefresh()) {
            if (null != iNodeSet) {
                logger.info("Returning " + iNodeSet.getNodeNames().size() + " cached nodes from GCP");
            }
            return iNodeSet;
        }
        if (lastRefresh > 0 && queryAsync && null == futureResult) {
            futureResult = mapper.performQueryAsync();
            lastRefresh = System.currentTimeMillis();
        } else if (!queryAsync || lastRefresh < 1) {
            //always perform synchronous query the first time
            iNodeSet = mapper.performQuery();
            lastRefresh = System.currentTimeMillis();
        }
        if (null != iNodeSet) {
            logger.info("Read " + iNodeSet.getNodeNames().size() + " nodes from GCP");
        }
        return iNodeSet;
    }

    /**
     * if any future results are pending, check if they are done and retrieve the results
     */
    private void checkFuture() {
        logger.error("checkFuture call");
        if (null != futureResult && futureResult.isDone()) {
            try {
                iNodeSet = futureResult.get();
            } catch (InterruptedException e) {
                logger.debug(e);
            } catch (ExecutionException e) {
                logger.warn("Error performing query: " + e.getMessage(), e);
            }
            futureResult = null;
        }
    }

    /**
     * Returns true if the last refresh time was longer ago than the refresh interval
     */
    private boolean needsRefresh() {
        logger.error("needsRefresh call");
        return refreshInterval < 0 || (System.currentTimeMillis() - lastRefresh > refreshInterval);
    }

    private void loadMapping() {
        logger.error("loadMapping call");
        if (useDefaultMapping) {
            mapping.putAll(defaultMapping);
        }
        if (null != mappingFile) {
            try {
                final FileInputStream fileInputStream = new FileInputStream(mappingFile);
                try {
                    mapping.load(fileInputStream);
                } finally {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                logger.warn(e);
            }
        }
        if (null != mappingParams) {
            for (final String s : mappingParams.split(";")) {
                if (s.contains("=")) {
                    final String[] split = s.split("=", 2);
                    if (2 == split.length) {
                        mapping.put(split[0], split[1]);
                    }
                }
            }
        }
        if (mapping.size() < 1) {
            mapping.putAll(defaultMapping);
        }
    }

    public void validate() throws ConfigurationException {
        logger.error("validate call");
        /*if (null != clientId && null == clientSecret) {
            throw new ConfigurationException("clientSecret is required for use with clientID");
        }*/

    }
}
