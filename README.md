# emp-connector example
A simplified connector example to the Enterprise Messaging Platform.

This example connector provides support for SSL, HTTP proxies and supports both the long polling and websocket
streaming transports.  Easy subscription management and full support for event replay is provided.

* * *

To use, add the maven dependency:

    <dependency> 
        <groupId>com.salesforce.conduit</groupId>
        <artifactId>emp-connector</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>
* * *

# Disclaimer
Please note that this repository is example code and is not supported by Salesforce.  This code has not been rigorously tested nor performance tested for throughput and scale.

This code is provided as an example only.  The underlying [CometD library](https://cometd.org/) is what provides the meat here, as the emp connector is a thin wrapper around this library.
___

Example usage:

    
    // Replay from the start of the event window - may be any valid replayFrom position in the event stream
    long replayFrom = EmpConnector.REPLAY_FROM_EARLIEST; 
    
    // get parameters from login
    BayeuxParameters params = login("foo@bar.com", "password");
    
    // The event consumer
    Consumer<Map<String, Object>> consumer = event -> System.out.println(String.format("Received:\n%s", event));
    
    // The EMP connector
    EmpConnector connector = new EmpConnector(params);
    
    // Wait for handshake with Streaming API
    connector.start().get(5, TimeUnit.SECONDS);
    
    // Subscribe to a topic
    // Block and wait for the subscription to succeed for 5 seconds
    TopicSubscription subscription = connector.subscribe("/topic/myTopic", replayFrom, consumer).get(5, TimeUnit.SECONDS);
    
    // Here's our subscription
    System.out.println(String.format("Subscribed: %s", subscription));
    
    // Cancel a subscription
    subscription.cancel();
    
    // Stop the connector
    connector.stop();

See [Login Example](src/main/java/com/salesforce/emp/connector/example/LoginExample.java) for full example.
