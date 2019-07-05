/*
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license.
 * For full license text, see LICENSE.TXT file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.emp.connector.example;

import static com.salesforce.emp.connector.LoginHelper.login;

import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.client.Origin.Address;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.util.ajax.JSON;

import com.salesforce.emp.connector.BayeuxParameters;
import com.salesforce.emp.connector.EmpConnector;
import com.salesforce.emp.connector.LoginHelper;
import com.salesforce.emp.connector.ProxyBayeuxParameter;
import com.salesforce.emp.connector.TopicSubscription;

/**
 * An example of using the EMP connector using login credentials
 *
 * @author hal.hildebrand
 * @since API v37.0
 */
public class LoginProxyExample {
	public static void main(String[] argv) throws Exception {
		System.out.print("argv size : " + argv.length);

		if (argv.length < 5) {
			System.err.println(
					"Usage: LoginExample username password topic loginUrl proxyProtocol proxyHost proxyPort [proxyUsername] [proxyPassword]");
			System.exit(1);
		}
		long replayFrom = EmpConnector.REPLAY_FROM_EARLIEST;
		int count = 3;
		String loginUrl = argv[count++];
		String proxyProtocol = argv[count++];
		String proxyHost = argv[count++];
		int proxyPort = Integer.valueOf(argv[count++]);

		String proxyUsername = "";
		String proxyPassword = "";
		if (argv.length > 6) {
			proxyUsername = argv[count++];
			proxyPassword = argv[count++];
		}

		System.out.println(String.format(
				"username : %s, password : %s, topic : %s, loginUrl : %s, proxyProtocol : %s, proxyHost : %s,proxyUsername : %s,proxyPassword : %s ",
				argv[0], argv[1], argv[2],loginUrl, proxyProtocol, proxyHost, proxyUsername, proxyPassword));

		

		Consumer<Map<String, Object>> consumer = event -> System.out
				.println(String.format("Received:\n%s", JSON.toString(event)));

		ProxyBayeuxParameter dbp = new ProxyBayeuxParameter();
		Address a = new Address(proxyHost, proxyPort);
		HttpProxy p = new HttpProxy(a, proxyProtocol.equals("https"));
		dbp.addProxy(p);
		if (!proxyUsername.isEmpty()) {
			BasicAuthentication auth = new BasicAuthentication(
					new URI(String.format("%s://%s:%s", proxyProtocol, proxyHost, proxyPort)), "*", proxyUsername,
					proxyPassword) {
				@Override
				public boolean matches(String type, URI uri, String realm) {
					realm = "*";
					return super.matches(type, uri, realm);
				}

			};
			dbp.addAuthentication(auth);
		}
		
		BayeuxParameters params = LoginHelper.login(new URL(loginUrl), argv[0], argv[1],dbp);
		

		EmpConnector connector = new EmpConnector(params);

		connector.start().get(5, TimeUnit.SECONDS);

		TopicSubscription subscription = connector.subscribe(argv[2], replayFrom, consumer).get(5, TimeUnit.SECONDS);

		System.out.println(String.format("Subscribed: %s", subscription));
	}
}
