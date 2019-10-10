package com.salesforce.emp.connector.example;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;

import java.text.SimpleDateFormat;
import java.util.Date;

public class OMSLoggingListener implements ClientSessionChannel.MessageListener {

    private boolean logSuccess;
    private boolean logFailure;

    public OMSLoggingListener() {
        this.logSuccess = true;
        this.logFailure = true;
    }

    public OMSLoggingListener(boolean logSuccess, boolean logFailure, String customer_id, String shared_key, String log_type) {
        this.logSuccess = logSuccess;
        this.logFailure = logFailure;
    }

    @Override
    public void onMessage(ClientSessionChannel clientSessionChannel, Message message) {
        if (logSuccess && message.isSuccessful()) {
            OMSPost.main(customer_id, log_type, log_type, message, clientSessionChannel.getId(), 'Success');
        }

        if (logFailure && !message.isSuccessful()) {
            OMSPost.main(customer_id, log_type, log_type, message, clientSessionChannel.getId(), 'Failure');
        }
    }
}
