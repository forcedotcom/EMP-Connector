/*
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license.
 * For full license text, see LICENSE.TXT file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.emp.connector.example;

import com.salesforce.emp.connector.BayeuxParameters;
import com.salesforce.emp.connector.EmpConnector;
import com.salesforce.emp.connector.LoginHelper;
import com.salesforce.emp.connector.TopicSubscription;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.eclipse.jetty.util.ajax.JSON;

import static org.cometd.bayeux.Channel.*;

/**
 * An example of using the EMP connector
 *
 * @author hal.hildebrand
 * @since API v37.0
 */
public class OMSRedirectExample {

    public static void main(String[] argv) throws Throwable {
        if (argv.length < 7 || argv.length > 8) {
            System.err.println("Usage: OMSRedirectExample url username password topic [replayFrom]");
            System.exit(1);
        }
        Consumer<Map<String, Object>> consumer = event -> System.out.println(String.format("Received:\n%s", JSON.toString(event)));

        BearerTokenProvider tokenProvider = new BearerTokenProvider(() -> {
            try {
                return LoginHelper.login(new URL(argv[0]), argv[1], argv[2]);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        BayeuxParameters params = tokenProvider.login();

        EmpConnector connector = new EmpConnector(params);
        String json_log = "[" + JSON.toString(event) + "]";
        OMSLoggingListener loggingListener = new OMSLoggingListener(true, true, argv[4],  argv[5], argv[6], json_log);

        connector.addListener(META_HANDSHAKE, loggingListener)
                .addListener(META_CONNECT, loggingListener)
                .addListener(META_DISCONNECT, loggingListener)
                .addListener(META_SUBSCRIBE, loggingListener)
                .addListener(META_UNSUBSCRIBE, loggingListener);

        connector.setBearerTokenProvider(tokenProvider);

        connector.start().get(5, TimeUnit.SECONDS);

        long replayFrom = EmpConnector.REPLAY_FROM_EARLIEST;
        if (argv.length == 8) {
            replayFrom = Long.parseLong(argv[7]);
        }
        TopicSubscription subscription;
        try {
            subscription = connector.subscribe(argv[3], replayFrom, consumer).get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            System.err.println(e.getCause().toString());
            System.exit(1);
            throw e.getCause();
        } catch (TimeoutException e) {
            System.err.println("Timed out subscribing");
            System.exit(1);
            throw e.getCause();
        }

        System.out.println(String.format("Subscribed: %s", subscription));
    }
}
