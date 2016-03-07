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
* Created: 9/1/11 4:27 PM
* 
*/
package com.dtolabs.rundeck.plugin.resources.gcp;


import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.core.resources.ResourceModelSource;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceFactory;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.dtolabs.rundeck.plugins.util.PropertyBuilder;

import java.io.File;
import java.util.*;

/**
 * GCPResourceModelSourceFactory is the factory that can create a {@link ResourceModelSource} based on a configuration.
 * <p/>
 * The configuration properties are: <ul> <li>endpoint: the GCP endpoint to use, or blank for the default</li>
 * <li>filter: A set of ";" separated query filters ("filter=value") for the GCP GCE API, see <a
 * href="http://docs.amazonwebservices.com/AWSEC2/latest/APIReference/ApiReference-query-DescribeInstances.html">DescribeInstances</a></li>
 * <li>mappingParams: A set of ";" separated mapping entries</li> <li>runningOnly: if "true", automatically filter the
 * instances by "instance-state-name=running"</li> <li>accessKey: API AccessKey value</li> <li>secretKey: API SecretKey
 * value</li> <li>mappingFile: Path to a java properties-formatted mapping definition file.</li> <li>refreshInterval:
 * Time in seconds used as minimum interval between calls to the AWS API.</li> <li>useDefaultMapping: if "true", base
 * all mapping definitions off the default mapping provided. </li> </ul>
 *
 * @author James Coppens <a href="mailto:jameshcoppens@gmail.com">jameshcoppens@gmail.com</a>
 */
@Plugin(name = "gcp-gce", service = "ResourceModelSource")
public class GCPResourceModelSourceFactory implements ResourceModelSourceFactory, Describable {
    public static final String PROVIDER_NAME = "gcp-gce";
    private Framework framework;

    public static final String ENDPOINT = "endpoint";
    public static final String FILTER_PARAMS = "filter";
    public static final String MAPPING_PARAMS = "mappingParams";
    public static final String RUNNING_ONLY = "runningOnly";
    //public static final String CLIENT_ID = "clientId";
    //public static final String CLIENT_SECRET = "clientSecret";
    public static final String PROJECT_ID = "projectId";
    public static final String MAPPING_FILE = "mappingFile";
    public static final String REFRESH_INTERVAL = "refreshInterval";
    public static final String USE_DEFAULT_MAPPING = "useDefaultMapping";
    /*public static final String HTTP_PROXY_HOST = "httpProxyHost";
    public static final String HTTP_PROXY_PORT = "httpProxyPort";
    public static final String HTTP_PROXY_USER = "httpProxyUser";
    public static final String HTTP_PROXY_PASS = "httpProxyPass";*/

    public GCPResourceModelSourceFactory(final Framework framework) {
        this.framework = framework;
    }

    public ResourceModelSource createResourceModelSource(final Properties properties) throws ConfigurationException {
        final GCPResourceModelSource gcpResourceModelSource = new GCPResourceModelSource(properties);
        gcpResourceModelSource.validate();
        return gcpResourceModelSource;
    }

    static Description DESC = DescriptionBuilder.builder()
            .name(PROVIDER_NAME)
            .title("GCP GCE Resources")
            .description("Produces nodes from GCP GCE")
            .property(PropertyUtil.string(PROJECT_ID, "Project ID", "Project ID", false, null))
            //.property(PropertyUtil.string(CLIENT_ID, "Client ID", "Client ID", false, null))
            /*.property(
                    PropertyUtil.string(
                            CLIENT_SECRET,
                            "Client Secret",
                            "Client Secret",
                            false,
                            null,
                            null,
                            null,
                            Collections.singletonMap("displayType", (Object) StringRenderingConstants.DisplayType.PASSWORD)
                    )
            )*/
            .property(PropertyUtil.integer(REFRESH_INTERVAL, "Refresh Interval",
                    "Minimum time in seconds between API requests to GCP (default is 30)", false, "30"))
            .property(PropertyUtil.string(FILTER_PARAMS, "Filter Params", "GCP GCE filters", false, null))
            .property(PropertyUtil.string(ENDPOINT, "Endpoint", "GCP GCE Endpoint, or blank for default", false, null))
            /*.property(PropertyUtil.string(HTTP_PROXY_HOST, "HTTP Proxy Host", "HTTP Proxy Host Name, or blank for default", false, null))
            .property(PropertyUtil.integer(HTTP_PROXY_PORT, "HTTP Proxy Port", "HTTP Proxy Port, or blank for 80", false, "80"))
            .property(PropertyUtil.string(HTTP_PROXY_USER, "HTTP Proxy User", "HTTP Proxy User Name, or blank for default", false, null))
            .property(
                    PropertyUtil.string(
                            HTTP_PROXY_PASS,
                            "HTTP Proxy Password",
                            "HTTP Proxy Password, or blank for default",
                            false,
                            null,
                            null,
                            null,
                            Collections.singletonMap("displayType", (Object) StringRenderingConstants.DisplayType.PASSWORD)
                    )
            )*/
            .property(PropertyUtil.string(MAPPING_PARAMS, "Mapping Params",
                    "Property mapping definitions. Specify multiple mappings in the form " +
                            "\"attributeName.selector=selector\" or \"attributeName.default=value\", " +
                            "separated by \";\"",
                    false, null))
            .property(PropertyUtil.string(MAPPING_FILE, "Mapping File", "Property mapping File", false, null,
                    new PropertyValidator() {
                        public boolean isValid(final String s) throws ValidationException {
                            if (!new File(s).isFile()) {
                                throw new ValidationException("File does not exist: " + s);
                            }
                            return true;
                        }
                    }))
            .property(PropertyUtil.bool(USE_DEFAULT_MAPPING, "Use Default Mapping",
                    "Start with default mapping definition. (Defaults will automatically be used if no others are " +
                            "defined.)",
                    false, "true"))
            .property(PropertyUtil.bool(RUNNING_ONLY, "Only Running Instances",
                    "Include Running state instances only. If false, all instances will be returned that match your " +
                            "filters.",
                    false, "true"))

            .build();

    public Description getDescription() {
        return DESC;
    }
}
