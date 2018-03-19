package com.salesforce.emp.connector.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;

import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.function.Consumer;

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
    public void onMessage(ClientSessionChannel clientSessionChannel, Message message) {
        if (logSuccess && message.isSuccessful()) {
            System.out.println(">>>>");
            printPrefix();
            System.out.println("Success:[" + clientSessionChannel.getId() + "]");
            printJson(message);
            System.out.println("<<<<");
        }

        if (logFailure && !message.isSuccessful()) {
            System.out.println(">>>>");
            printPrefix();
            System.out.println("Failure:[" + clientSessionChannel.getId() + "]");
            printJson(message);
            System.out.println("<<<<");
        }
    }

    private void printPrefix() {
        System.out.print("[" + timeNow() + "] ");
    }

    private void printJson(Message message) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        StringWriter sw = new StringWriter();
        try {
            mapper.writeValue(sw, message);
            System.out.println(String.format("Received:\n%s", sw.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String timeNow() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date now = new Date();
        return dateFormat.format(now);
    }
}
