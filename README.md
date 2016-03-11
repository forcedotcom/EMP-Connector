# emp-connector
A simplified connector to the Enterprise Messaging Platform


Example usage:

    
    // Replay from the start of the event window
    long replayFrom = Konnnektor.REPLAY_FROM_EARLIEST; 
    
    // get parameters from login
    BayeuxParameters params = login("foo@bar.com", "password");
    
    // The event consumer
    Consumer<Map<String, Object>> consumer = event -> System.out.println(String.format("Received:\n%s", event));
    
    // The EMP connector
    Konnnektor connector = new Konnnektor(params);
    
    // Wait for handshake with Streaming API
    if (!connector.start(60000)) {
        throw new IllegalStateException(String.format("Unable to handshake to replay endpoint: %s", params.endpoint()));
    }
    
    // Subscribe to a topic
    Future<TopicSubscription> future = connector.subscribe(argv[2], replayFrom, consumer);
    
    // Block and wait for the subscription to succeed for 60 seconds
    TopicSubscription subscription = future.get(60, TimeUnit.SECONDS);
    
    // Here's our subscription
    System.out.println(String.format("Subscribed: %s", subscription));
    
    // Cancel a subscription
    subscription.cancel();
    
    // Stop the connector
    connector.stop();

See _com.salesforce.emp.connector.LoginExample_ for full example.