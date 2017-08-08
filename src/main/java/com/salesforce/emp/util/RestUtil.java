package com.salesforce.emp.util;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpResponse;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;

import com.salesforce.emp.connector.BayeuxParameters;

/**
 * Utility class to call REST resources on the Salesforce platform.
 * 
 * @author lmcalpin
 * @since 212
 */
public class RestUtil {
    private final BayeuxParameters parameters;
    
    public RestUtil(BayeuxParameters bayeuxParameters) {
        this.parameters = bayeuxParameters;
    }
    
    public String get(String serviceUrl) throws Exception {
        HttpClient client = new HttpClient(parameters.sslContextFactory());
        client.getProxyConfiguration().getProxies().addAll(parameters.proxies());
        client.start();

        // schema retrieval requests must be GETs
        Request get = client.newRequest(serviceUrl);
        get.header("Authorization", "Bearer " + parameters.bearerToken());
        ContentResponse response = get.send();
        return response.getContentAsString();
    }
}
