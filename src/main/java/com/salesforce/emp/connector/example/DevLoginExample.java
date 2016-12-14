/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.TXT file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.emp.connector.example;

import static com.salesforce.emp.connector.LoginHelper.login;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

import com.salesforce.emp.connector.BayeuxParameters;
import com.salesforce.emp.connector.EmpConnector;
import com.salesforce.emp.connector.TopicSubscription;

/**
 * An example of using the EMP connector
 *
 * @author hal.hildebrand
 * @since 202
 */
public class DevLoginExample {
    public static void main(String[] argv) throws Throwable {
        if (argv.length < 4 || argv.length > 5) {
            System.err.println("Usage: DevLoginExample url username password topic [replayFrom]");
            System.exit(1);
        }
        Consumer<Map<String, Object>> consumer = event -> System.out.println(String.format("Received:\n%s", event));
        BayeuxParameters params = login(new URL(argv[0]), argv[1], argv[2]);
        EmpConnector connector = new EmpConnector(params);

        connector.start().get(5, TimeUnit.SECONDS);

        long replayFrom = EmpConnector.REPLAY_FROM_TIP;
        if (argv.length == 5) {
            replayFrom = Long.parseLong(argv[4]);
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
