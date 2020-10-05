package com.salesforce.emp.connector.example;

import org.eclipse.jetty.util.ajax.JSON;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * An example of using the EMP connector which processes events asynchronously
 *
 * @author sivananda
 * @since API v37.0
 */
public class DevLoginAsynchronousEventProcessingExample extends DevLoginExample {

    // More than one thread can be used in the thread pool which leads to parallel processing of events which may be acceptable by the application
    // The main purpose of asynchronous event processing is to make sure that client is able to perform /meta/connect requests which keeps the session alive on the server side
    private final ExecutorService workerThreadPool = Executors.newFixedThreadPool(1);

    public static void main(String[] argv) throws Throwable {
        DevLoginAsynchronousEventProcessingExample devLoginAsynchronousEventProcessingExample = new DevLoginAsynchronousEventProcessingExample();
        devLoginAsynchronousEventProcessingExample.processEvents(argv);
    }

    @Override
    public Consumer<Map<String, Object>> getConsumer() {
        return event -> workerThreadPool.submit(() -> System.out.println(String.format("Received:\n%s, \nEvent processed by threadName:%s, threadId: %s", JSON.toString(event), Thread.currentThread().getName(), Thread.currentThread().getId())));
    }
}
