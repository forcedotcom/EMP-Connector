package com.salesforce.emp.connector.example;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LoggingListener implements ClientSessionChannel.MessageListener {

    private boolean logSuccess;
    private boolean logFailure;

    public LoggingListener() {
        this.logSuccess = true;
        this.logFailure = true;
    }

    public LoggingListener(boolean logSuccess, boolean logFailure) {
        this.logSuccess = logSuccess;
        this.logFailure = logFailure;
    }

    @Override
    public void onMessage(ClientSessionChannel clientSessionChannel, Message message, String customer_id, String shared_key, String log_type) {
        if (logSuccess && message.isSuccessful()) {
            OMSPost(customer_id, log_type, log_type, message, clientSessionChannel.getId(), 'Success')
        }

        if (logFailure && !message.isSuccessful()) {
            OMSPost(customer_id, log_type, log_type, message, clientSessionChannel.getId(), 'Failure')
        }
    }

    private void printPrefix() {
        System.out.print("[" + timeNow() + "] ");
    }

    private String timeNow() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date now = new Date();
        return dateFormat.format(now);
    }
}
