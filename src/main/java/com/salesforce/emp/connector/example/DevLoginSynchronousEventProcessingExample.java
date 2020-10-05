package com.salesforce.emp.connector.example;

import org.eclipse.jetty.util.ajax.JSON;

import java.util.Map;
import java.util.function.Consumer;

/**
 * An example of using the EMP connector which processes events synchronously
 *
 * @author sivananda
 * @since API v37.0
 */
public class DevLoginSynchronousEventProcessingExample extends  DevLoginExample {

    public static void main(String[] argv) throws Throwable {
        DevLoginSynchronousEventProcessingExample devLoginSynchronousEventProcessingExample = new DevLoginSynchronousEventProcessingExample();
        devLoginSynchronousEventProcessingExample.processEvents(argv);
    }

    @Override
    public Consumer<Map<String, Object>> getConsumer() {
        return event -> System.out.println(String.format("Received:\n%s, \nEvent processed by threadName:%s, threadId: %s", JSON.toString(event), Thread.currentThread().getName(), Thread.currentThread().getId()));
    }
}
