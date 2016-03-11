package com.salesforce.emp.connector;

import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import static com.salesforce.emp.connector.LoginHelper.*;

/**
 * An example of using the EMP connector
 *
 * @author hal.hildebrand
 * @since 202
 */
public class LoginExample {
    public static void main(String[] argv) throws Exception {
        if (argv.length < 3 || argv.length > 4) {
            System.err.println("Usage: LoginExample username password topic [replayFrom]");
            System.exit(1);
        }
        long replayFrom = Konnnektor.REPLAY_FROM_EARLIEST;
        if (argv.length == 4) {
            replayFrom = Long.parseLong(argv[3]);
        }
        
        BayeuxParameters params;
        try {
            params = login(argv[0], argv[1]);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
            throw e;
        }
        
        Consumer<Map<String, Object>> consumer = event -> System.out.println(String.format("Received:\n%s", event));
        Konnnektor connector = new Konnnektor(params);
        if (!connector.start(60000)) {
            System.err.println(String.format("Unable to handshake to replay endpoint: %s", params.endpoint()));
            System.exit(1);
        }
        Future<TopicSubscription> future = connector.subscribe(argv[2], replayFrom, consumer);
        TopicSubscription subscription = future.get(60, TimeUnit.SECONDS);
        System.out.println(String.format("Subscribed: %s", subscription));
    }
}
