# emp-connector
A simplified connector to the Enterprise Messaging Platform.

This connector provides support for SSL, HTTP proxies and supports both the long polling and websocket
streaming transports.  Easy subscription management and full support for event replay is provided.

* * *

To use, add the maven dependency:

    <dependency> 
        <groupId>com.salesforce.conduit</groupId>
        <artifactId>emp-connector</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>
* * *

Example usage:

    
    // Replay from the start of the event window - may be any valid replayFrom position in the event stream
    long replayFrom = Konnnektor.REPLAY_FROM_EARLIEST; 
    
    // get parameters from login
    BayeuxParameters params = login("foo@bar.com", "password");
    
    // The event consumer
    Consumer<Map<String, Object>> consumer = event -> System.out.println(String.format("Received:\n%s", event));
    
    // The EMP connector
    Konnnektor connector = new Konnnektor(params);
    
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

See [Login Example](https://git.soma.salesforce.com/MessagingPlatform/emp-connector/blob/master/src/test/java/com/salesforce/emp/connector/LoginExample.java) for full example.