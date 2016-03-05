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
* EC2ResourceModelSource.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 9/1/11 4:34 PM
* 
*/
package com.dtolabs.rundeck.plugin.resources.gcp;



import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential.Builder;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceList;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;

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
 */
public class GCPResourceModelSource implements ResourceModelSource {
    static Logger logger = Logger.getLogger(GCPResourceModelSource.class);
    private String clientId;
    private String clientSecret;
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

    /** Directory to store user credentials. */
    private static final java.io.File DATA_STORE_DIR =
            new java.io.File(System.getProperty("user.home"), ".store/compute_engine_sample");

    /**
     * Global instance of the {@link DataStoreFactory}. The best practice is to make it a single
     * globally shared instance across your application.
     */
    private static FileDataStoreFactory dataStoreFactory;

    /** Global instance of the HTTP transport. */
    private static HttpTransport httpTransport;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /** OAuth 2.0 scopes */
    private static final List<String> SCOPES = Arrays.asList(ComputeScopes.COMPUTE_READONLY);

    Credential credential;

    INodeSet iNodeSet;
    static final Properties defaultMapping = new Properties();
    InstanceToNodeMapper mapper;

    static {
        final String mapping = "nodename.selector=tags/Name,instanceId\n"
                               + "hostname.selector=publicDnsName\n"
                               + "sshport.default=22\n"
                               + "sshport.selector=tags/ssh_config_Port\n"
                               + "description.default=EC2 node instance\n"
                               + "osArch.selector=architecture\n"
                               + "osFamily.selector=platform\n"
                               + "osFamily.default=unix\n"
                               + "osName.selector=platform\n"
                               + "osName.default=Linux\n"
                               + "username.selector=tags/Rundeck-User\n"
                               + "username.default=ec2-user\n"
                               + "editUrl.default=https://console.aws.amazon.com/ec2/home#Instances:search=${node.instanceId}\n"
                               + "privateIpAddress.selector=privateIpAddress\n"
                               + "privateDnsName.selector=privateDnsName\n"
                               + "tags.selector=tags/Rundeck-Tags\n"
                               + "instanceId.selector=instanceId\n"
                               + "tag.running.selector=state.name=running\n"
                               + "tag.stopped.selector=state.name=stopped\n"
                               + "tag.stopping.selector=state.name=stopping\n"
                               + "tag.shutting-down.selector=state.name=shutting-down\n"
                               + "tag.terminated.selector=state.name=terminated\n"
                               + "tag.pending.selector=state.name=pending\n"
                               + "state.selector=state.name\n"
                               + "tags.default=ec2\n";
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
        this.clientId = configuration.getProperty(GCPResourceModelSourceFactory.CLIENT_ID);
        this.clientSecret = configuration.getProperty(GCPResourceModelSourceFactory.CLIENT_SECRET);
        //this.endpoint = configuration.getProperty(EC2ResourceModelSourceFactory.ENDPOINT);
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
        if (null != clientId && null != clientSecret) {
            try {
                httpTransport = GoogleNetHttpTransport.newTrustedTransport();
                dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
                credential = authorize();
            } catch  (IOException e) {
                System.err.println(e.getMessage());
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        
        /*if (null != httpProxyHost && !"".equals(httpProxyHost)) {
            clientConfiguration.setProxyHost(httpProxyHost);
            clientConfiguration.setProxyPort(httpProxyPort);
            clientConfiguration.setProxyUsername(httpProxyUser);
            clientConfiguration.setProxyPassword(httpProxyPass);
        }*/
        
        initialize();
    }

    /** Authorizes the installed application to access user's protected data. */
    private Credential authorize() throws Exception {
        // set up authorization code flow
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientId, clientSecret, SCOPES).setDataStoreFactory(dataStoreFactory)
                .build();
        // authorize
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    private void initialize() {
        final ArrayList<String> params = new ArrayList<String>();
        if (null != filterParams) {
            Collections.addAll(params, filterParams.split(";"));
        }
        loadMapping();
        mapper = new InstanceToNodeMapper(credential, mapping);
        mapper.setFilterParams(params);
        mapper.setEndpoint(endpoint);
        mapper.setRunningStateOnly(runningOnly);
    }


    public synchronized INodeSet getNodes() throws ResourceModelSourceException {
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
        return refreshInterval < 0 || (System.currentTimeMillis() - lastRefresh > refreshInterval);
    }

    private void loadMapping() {
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
        if (null != clientId && null == clientSecret) {
            throw new ConfigurationException("clientSecret is required for use with clientID");
        }

    }
}
