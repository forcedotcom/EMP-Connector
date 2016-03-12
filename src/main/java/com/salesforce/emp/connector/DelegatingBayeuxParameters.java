package com.salesforce.emp.connector;

import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.ProxyConfiguration.Proxy;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * @author hal.hildebrand
 * @since 202
 */
public class DelegatingBayeuxParameters implements BayeuxParameters {
    private final BayeuxParameters parameters;

    public DelegatingBayeuxParameters(BayeuxParameters parameters) {
        this.parameters = parameters;
    }

    @Override
    public String bearerToken() {
        return parameters.bearerToken();
    }

    @Override
    public URL endpoint() {
        return parameters.endpoint();
    }

    @Override
    public long keepAlive() {
        return parameters.keepAlive();
    }

    @Override
    public TimeUnit keepAliveUnit() {
        return parameters.keepAliveUnit();
    }

    @Override
    public Map<String, Object> longPollingOptions() {
        return parameters.longPollingOptions();
    }

    @Override
    public int maxBufferSize() {
        return parameters.maxBufferSize();
    }

    @Override
    public int maxNetworkDelay() {
        return parameters.maxNetworkDelay();
    }

    @Override
    public Collection<? extends Proxy> proxies() {
        return parameters.proxies();
    }

    @Override
    public SslContextFactory sslContextFactory() {
        return parameters.sslContextFactory();
    }

    @Override
    public String version() {
        return parameters.version();
    }

}
