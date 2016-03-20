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
* InstanceToNodeMapper.java
* 
* User: James Coppens <a href="mailto:jameshcoppens@gmail.com">jameshcoppens@gmail.com</a>
* Created: March 01 2016
* 
*/
package com.dtolabs.rundeck.plugin.resources.gcp;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;

import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceList;
import com.google.api.services.compute.model.InstanceAggregatedList;
import com.google.api.services.compute.model.InstancesScopedList;
import com.google.api.services.compute.model.Tags;
import com.google.api.services.compute.model.NetworkInterface;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.common.NodeSetImpl;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.log4j.Logger;


import java.io.IOException;
//import java.net.NetworkInterface;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * InstanceToNodeMapper produces Rundeck node definitions from GCP Instances
 *
 * @author James Coppens <a href="mailto:jameshcoppens@gmail.com">jameshcoppens@gmail.com</a>
 */
class InstanceToNodeMapper {
    static final Logger logger = Logger.getLogger(InstanceToNodeMapper.class);
    final GoogleCredential credential;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private ArrayList<String> filterParams;
    private String endpoint;
    private String projectId;
    private boolean runningStateOnly = true;
    private Properties mapping;
    private Compute compute;

    /**
     * Be sure to specify the name of your application. If the application name is {@code null} or
     * blank, the application will log a warning. Suggested format is "MyCompany-ProductName/1.0".
     */
    private static final String APPLICATION_NAME = "rundeck-gcp-nodes-plugin";

    /** Global instance of the HTTP transport. */
    private static HttpTransport httpTransport;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /**
     * Create with the credentials and mapping definition
     */
    InstanceToNodeMapper(final GoogleCredential credential ,final Properties mapping) {
        logger.error("InstancetoNodeMapper Object");
        this.credential = credential;
        this.mapping = mapping;
    }

    /**
     * Perform the query and return the set of instances
     *
     */
    public INodeSet performQuery() {
        final NodeSetImpl nodeSet = new NodeSetImpl();
        logger.error("Google Crendential performQuery(), this is credential " + credential);
        //if(null!=credential) {
        try {
             httpTransport = GoogleNetHttpTransport.newTrustedTransport();
             compute = new Compute.Builder(
                    httpTransport, JSON_FACTORY, null).setApplicationName(APPLICATION_NAME)
                    .setHttpRequestInitializer(credential).build();

            final Set<Instance> instances = query(compute, projectId);

            logger.error("Google Crendential query() completed");

            mapInstances(nodeSet, instances);
        } catch (IOException e) {
        System.err.println(e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
        logger.error("Google Crendential perfomquery() completed");
        //final ArrayList<Filter> filters = buildFilters();
        //final Set<Instance> instances = query(ec2, new DescribeInstancesRequest().withFilters(filters));
        return nodeSet;
    }

    /**
     * Perform the query asynchronously and return the set of instances
     *
     */
    public Future<INodeSet> performQueryAsync() {
        logger.error("PerformQueryAsync start");
        //final AmazonEC2AsyncClient ec2;
        /*if(null!=credentials){
            ec2= new AmazonEC2AsyncClient(credentials, clientConfiguration, executorService);
        } else{
            ec2 = new AmazonEC2AsyncClient(new DefaultAWSCredentialsProviderChain(), clientConfiguration, executorService);
        }
        if (null != getEndpoint()) {
            ec2.setEndpoint(getEndpoint());
        }*/
        //final ArrayList<Filter> filters = buildFilters();

        //final Future<DescribeInstancesResult> describeInstancesRequest = ec2.describeInstancesAsync(
        //    new DescribeInstancesRequest().withFilters(filters));

        return new Future<INodeSet>() {

            public boolean cancel(boolean b) {
                return true;
                //return describeInstancesRequest.cancel(b);
            }

            public boolean isCancelled() {
                return true;
                //return describeInstancesRequest.isCancelled();
            }

            public boolean isDone() {
                return true;
                //return describeInstancesRequest.isDone();
            }

            public INodeSet get() throws InterruptedException, ExecutionException {
                //DescribeInstancesResult describeInstancesResult = describeInstancesRequest.get();

                final NodeSetImpl nodeSet = new NodeSetImpl();
                //final Set<Instance> instances = examineResult(describeInstancesResult);
                final Set<Instance> instances = query(compute, projectId);

                mapInstances(nodeSet, instances);
                return nodeSet;
            }

            public INodeSet get(final long l, final TimeUnit timeUnit) throws InterruptedException, ExecutionException,
                TimeoutException {
                //DescribeInstancesResult describeInstancesResult = describeInstancesRequest.get(l, timeUnit);

                final NodeSetImpl nodeSet = new NodeSetImpl();
                final Set<Instance> instances = query(compute, projectId);
                //final Set<Instance> instances = examineResult(describeInstancesResult);

                mapInstances(nodeSet, instances);
                return nodeSet;
            }
        };
    }

    private Set<Instance> query(Compute compute, String projectId) {
        //create "running" filter

        //final List<Reservation> reservations = describeInstancesRequest.getReservations();
        final Set<Instance> instances = new HashSet<Instance>();
        try {
            logger.error("begin query function, with this projectId " + projectId);
                Compute.Instances.AggregatedList instancesAggregatedList = compute.instances().aggregatedList(projectId);
                InstanceAggregatedList list = instancesAggregatedList.execute();

                if (list.getItems() == null) {
                    logger.error("No instances found. Sign in to the Google APIs Console and create "
                            + "an instance at: code.google.com/apis/console");
                } else {
                    java.util.Map<String, InstancesScopedList> aggregated_list = list.getItems();

                    for (java.util.Map.Entry<String, InstancesScopedList> entry : aggregated_list.entrySet()) {
                        logger.error("getinstances performing");
                        if (entry.getValue().getInstances() != null) {
                            instances.addAll(entry.getValue().getInstances());
                            logger.error("Successfully pulling in node information " + entry.getValue().getInstances());
                        }
                    }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
        //for (final Reservation reservation : reservations) {
            //instances.addAll(reservation.getInstances());
        //}
        return instances;
    }

    /*private ArrayList<Filter> buildFilters() {
        final ArrayList<Filter> filters = new ArrayList<Filter>();
        if (isRunningStateOnly()) {
            final Filter filter = new Filter("instance-state-name").withValues(InstanceStateName.Running.toString());
            filters.add(filter);
        }

        if (null != getFilterParams()) {
            for (final String filterParam : getFilterParams()) {
                final String[] x = filterParam.split("=", 2);
                if (!"".equals(x[0]) && !"".equals(x[1])) {
                    filters.add(new Filter(x[0]).withValues(x[1]));
                }
            }
        }
        return filters;
    }*/

    private void mapInstances(final NodeSetImpl nodeSet, final Set<Instance> instances) {
        logger.error("mapInstances call");
        for (final Instance inst : instances) {
            final INodeEntry iNodeEntry;
            try {
                iNodeEntry = InstanceToNodeMapper.instanceToNode(inst, mapping);
                if (null != iNodeEntry) {
                    nodeSet.putNode(iNodeEntry);
                }
            } catch (GeneratorException e) {
                logger.error(e);
            }
            logger.error("Instances within the Set "+ inst);
        }
    }

    /**
     * Convert an GCP GCE Instance to a RunDeck INodeEntry based on the mapping input
     */
    @SuppressWarnings("unchecked")
    static INodeEntry instanceToNode(final Instance inst, final Properties mapping) throws GeneratorException {
        final NodeEntryImpl node = new NodeEntryImpl();
        logger.error("instancetoNode call");
        //evaluate single settings.selector=tags/* mapping
        if ("tags/*".equals(mapping.getProperty("attributes.selector"))) {
            //iterate through instance tags and generate settings
            /*for (final String tag : inst.getTags().getItems()) {
                if (null == node.getAttributes()) {
                    node.setAttribute(new HashMap<String, String>());
                }
                node.getAttribute().put(tag.getKey(), tag.getValue());
            }*/
        }
        if (null != mapping.getProperty("tags.selector")) {
            final String selector = mapping.getProperty("tags.selector");
            final String value = applySelector(inst, selector, mapping.getProperty("tags.default"), true);
            if (null != value) {
                final String[] values = value.split(",");
                final HashSet<String> tagset = new HashSet<String>();
                for (final String s : values) {
                    tagset.add(s.trim());
                }
                if (null == node.getTags()) {
                    node.setTags(tagset);
                } else {
                    final HashSet orig = new HashSet(node.getTags());
                    orig.addAll(tagset);
                    node.setTags(orig);
                }
            }
        }
        if (null == node.getTags()) {
            node.setTags(new HashSet());
        }
        final HashSet orig = new HashSet(node.getTags());
        //apply specific tag selectors
        final Pattern tagPat = Pattern.compile("^tag\\.(.+?)\\.selector$");
        //evaluate tag selectors
        for (final Object o : mapping.keySet()) {
            final String key = (String) o;
            final String selector = mapping.getProperty(key);
            //split selector by = if present
            final String[] selparts = selector.split("=");
            final Matcher m = tagPat.matcher(key);
            if (m.matches()) {
                final String tagName = m.group(1);
                if (null == node.getAttributes()) {
                    node.setAttributes(new HashMap<String, String>());
                }
                final String value = applySelector(inst, selparts[0], null);
                if (null != value) {
                    if (selparts.length > 1 && !value.equals(selparts[1])) {
                        continue;
                    }
                    //use add the tag if the value is not null
                    orig.add(tagName);
                }
            }
        }
        node.setTags(orig);

        //apply default values which do not have corresponding selector
        final Pattern attribDefPat = Pattern.compile("^([^.]+?)\\.default$");
        //evaluate selectors
        for (final Object o : mapping.keySet()) {
            final String key = (String) o;
            final String value = mapping.getProperty(key);
            final Matcher m = attribDefPat.matcher(key);
            if (m.matches() && (!mapping.containsKey(key + ".selector") || "".equals(mapping.getProperty(
                key + ".selector")))) {
                final String attrName = m.group(1);
                if (null == node.getAttributes()) {
                    node.setAttributes(new HashMap<String, String>());
                }
                if (null != value) {
                    node.getAttributes().put(attrName, value);
                }
            }
        }

        final Pattern attribPat = Pattern.compile("^([^.]+?)\\.selector$");
        //evaluate selectors
        for (final Object o : mapping.keySet()) {
            final String key = (String) o;
            final String selector = mapping.getProperty(key);
            final Matcher m = attribPat.matcher(key);
            if (m.matches()) {
                final String attrName = m.group(1);
                if(attrName.equals("tags")){
                    //already handled
                    continue;
                }
                if (null == node.getAttributes()) {
                    node.setAttributes(new HashMap<String, String>());
                }
                final String value = applySelector(inst, selector, mapping.getProperty(attrName + ".default"));
                if (null != value) {
                    //use nodename-settingname to make the setting unique to the node
                    node.getAttributes().put(attrName, value);
                }
            }
        }
        String hostSel = mapping.getProperty("hostname.selector");
        //logger.error("This is the hostSel variable " + hostSel);
        String host = applySelector(inst, hostSel, mapping.getProperty("hostname.default"));
        //logger.error("This is the host variable " + host);
        //logger.error("This is the hostname.default mapping param " + mapping.getProperty("hostname.default"));
        if (null == node.getHostname()) {
            System.err.println("Unable to determine hostname for instance: " + inst.getId());
            return null;
        }
        String name = node.getNodename();
        if (null == name || "".equals(name)) {
            name = node.getHostname();
        }
        if (null == name || "".equals(name)) {
            name = inst.getId().toString();
        }
        node.setNodename(name);

        // Set ssh port on hostname if not 22
        String sshport = node.getAttributes().get("sshport");
        if (sshport != null && !sshport.equals("") && !sshport.equals("22")) {
            node.setHostname(node.getHostname() + ":" + sshport);
        }

        return node;
    }

    /**
     * Return the result of the selector applied to the instance, otherwise return the defaultValue. The selector can be
     * a comma-separated list of selectors
     */
    public static String applySelector(final Instance inst, final String selector, final String defaultValue) throws
        GeneratorException {
        return applySelector(inst, selector, defaultValue, false);
    }

    /**
     * Return the result of the selector applied to the instance, otherwise return the defaultValue. The selector can be
     * a comma-separated list of selectors.
     * @param inst the instance
     * @param selector the selector string
     * @param defaultValue a default value to return if there is no result from the selector
     * @param tagMerge if true, allow | separator to merge multiple values
     */

    public static String applySelector(final Instance inst, final String selector, final String defaultValue,
                                       final boolean tagMerge) throws
        GeneratorException {

        if (null != selector) {
            for (final String selPart : selector.split(",")) {
                if (tagMerge) {
                    final StringBuilder sb = new StringBuilder();
                    for (final String subPart : selPart.split(Pattern.quote("|"))) {
                        final String val = applySingleSelector(inst, subPart);
                        if (null != val) {
                            if (sb.length() > 0) {
                                sb.append(",");
                            }
                            sb.append(val);
                        }
                    }
                    if (sb.length() > 0) {
                        return sb.toString();
                    }
                } else {
                    final String val = applySingleSelector(inst, selPart);
                    if (null != val) {
                        return val;
                    }
                }
            }
        }
        return defaultValue;
    }


    private static String applySingleSelector(final Instance inst, final String selector) throws
        GeneratorException {
        /*if (null != selector && !"".equals(selector) && selector.startsWith("tags/")) {
            final String tag = selector.substring("tags/".length());
            final List<Tag> tags = inst.getTags();
            for (final Tag tag1 : tags) {
                if (tag.equals(tag1.getKey())) {
                    return tag1.getValue();
                }
            }
        } else*/ if (null != selector && !"".equals(selector)) {
            try {
                logger.error("This is the selector " + selector);
                String value = null;
                if ("networkInterfaces".equals(selector)) {
                    for (NetworkInterface netint : inst.getNetworkInterfaces()) {
                         value = netint.getNetworkIP();
                    }
                }
                else {
                    value = BeanUtils.getProperty(inst, selector);
                }
                //logger.error("This is the value " + value);
                if (null != value) {
                    return value;
                }
            } catch (Exception e) {
                throw new GeneratorException(e);
            }
        }

        return null;
    }

    /**
     * Return the list of "filter=value" filters
     */
    public ArrayList<String> getFilterParams() {
        return filterParams;
    }

    /**
     * Return the endpoint
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * Return the endpoint
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Return true if runningStateOnly
     */
    public boolean isRunningStateOnly() {
        return runningStateOnly;
    }

    /**
     * If true, the an automatic "running" state filter will be applied
     */
    public void setRunningStateOnly(final boolean runningStateOnly) {
        this.runningStateOnly = runningStateOnly;
    }

    /**
     * Set the list of "filter=value" filters
     */
    public void setFilterParams(final ArrayList<String> filterParams) {
        this.filterParams = filterParams;
    }

    /**
     * Set the region endpoint to use.
     */
    public void setEndpoint(final String endpoint) {
        this.endpoint = endpoint;
    }

    public void setProjectId(final String projectId) {
        this.projectId = projectId;
    }

    public Properties getMapping() {
        return mapping;
    }

    public void setMapping(Properties mapping) {
        this.mapping = mapping;
    }

    public static class GeneratorException extends Exception {
        public GeneratorException() {
        }

        public GeneratorException(final String message) {
            super(message);
        }

        public GeneratorException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public GeneratorException(final Throwable cause) {
            super(cause);
        }
    }

}
