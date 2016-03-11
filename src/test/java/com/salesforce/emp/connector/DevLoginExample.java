package com.salesforce.emp.connector;

import static com.salesforce.emp.connector.LoginHelper.login;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * An example of using the EMP connector
 *
 * @author hal.hildebrand
 * @since 202
 */
public class DevLoginExample {
    public static void main(String[] argv) throws Exception {
        if (argv.length < 4 || argv.length > 5) {
            System.err.println("Usage: DevLoginExample url username password topic [replayFrom]");
            System.exit(1);
        }
        Consumer<Map<String, Object>> consumer = event -> System.out.println(String.format("Received:\n%s", event));
        BayeuxParameters params = login(new URL(argv[0]), argv[1], argv[2]);
        Konnnektor connector = new Konnnektor(params);
        if (!connector.start(60000)) {
            System.err.println(String.format("Unable to handshake to replay endpoint: %s", params.endpoint()));
            System.exit(1);
        }
        long replayFrom = Konnnektor.REPLAY_FROM_TIP;
        if (argv.length == 4) {
            replayFrom = Long.parseLong(argv[4]);
        }
        Future<TopicSubscription> future = connector.subscribe(argv[3], replayFrom, consumer);
        TopicSubscription subscription = future.get(60, TimeUnit.SECONDS);
        System.out.println(String.format("Subscribed: %s", subscription));
    }
}
