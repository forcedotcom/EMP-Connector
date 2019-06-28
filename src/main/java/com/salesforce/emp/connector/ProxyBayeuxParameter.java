package com.salesforce.emp.connector;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.ProxyConfiguration.Proxy;
import org.eclipse.jetty.client.api.Authentication;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import com.salesforce.emp.connector.BayeuxParameters;

public class ProxyBayeuxParameter implements BayeuxParameters{
	private final BayeuxParameters parameters;
    
    private List<Proxy> proxies = new ArrayList<>();
    private List<Authentication> auths = new ArrayList<>();
    
    public void addProxy( Proxy proxy ){
    	proxies.add(proxy);
    }
    
    public void addAuthentication( Authentication auth ){
    	auths.add(auth);	    	
    }
    
   
    
    public ProxyBayeuxParameter(BayeuxParameters parameters) {
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
    public List<Proxy> proxies() {
        return proxies;
    }

    
    @Override
	public Collection<Authentication> authentications() {
		
		return auths;
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
