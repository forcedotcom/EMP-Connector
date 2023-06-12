package com.salesforce.emp.connector.example;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LoggingListener implements ClientSessionChannel.MessageListener {

    private boolean logSuccess;
    private boolean logFailure;

    private static final Logger log = LoggerFactory.getLogger(LoggingListener.class);

    public LoggingListener() {
        this.logSuccess = true;
        this.logFailure = true;
    }

    public LoggingListener(boolean logSuccess, boolean logFailure) {
        this.logSuccess = logSuccess;
        this.logFailure = logFailure;
    }

    @Override
    public void onMessage(ClientSessionChannel clientSessionChannel, Message message) {
        if (logSuccess && message.isSuccessful()) {
            log.info("Success:[ {} ], message: {} ", clientSessionChannel.getId(), message);
        }

        if (logFailure && !message.isSuccessful()) {
            log.error("Failure:[ {} ], message: {}", clientSessionChannel.getId(), message);
        }
    }

}
