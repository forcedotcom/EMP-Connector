/*
 * Copyright, 1999-2015, SALESFORCE.com All Rights Reserved
 */
package com.salesforce.emp.connector;

import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * @author hal.hildebrand
 * @since 202
 */
public interface BayeuxParameters {

    /**
     * @return the bearer token used to authenticate
     */
    String bearerToken();

    /**
     * @return the URL of the platform Streaming API endpoint
     */
    URL endpoint();

    /**
     * @return the keep alive interval duration
     */
    default long keepAlive() {
        return 60;
    }

    /**
     * @return keep alive interval time unit
     */
    default TimeUnit keepAliveUnit() {
        return TimeUnit.MINUTES;
    }

    default Map<String, Object> longPollingOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put("maxNetworkDelay", maxNetworkDelay());
        options.put("maxBufferSize", maxBufferSize());
        return options;
    }

    /**
     * @return The long polling transport maximum number of bytes of a HTTP response, which may contain many Bayeux
     *         messages
     */
    default int maxBufferSize() {
        return 1048576;
    }

    /**
     * @return The long polling transport maximum number of milliseconds to wait before considering a request to the
     *         Bayeux server failed
     */
    default int maxNetworkDelay() {
        return 15000;
    }

    /**
     * @return a list of proxies to use for outbound connections
     */
    default Collection<? extends org.eclipse.jetty.client.ProxyConfiguration.Proxy> proxies() {
        return Collections.emptyList();
    }

    /**
     * @return the SslContextFactory for establishing secure outbound connections
     */
    default SslContextFactory sslContextFactory() {
        return new SslContextFactory(true);
    }
}
