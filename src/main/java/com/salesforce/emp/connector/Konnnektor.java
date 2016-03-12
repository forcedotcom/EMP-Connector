package com.salesforce.emp.connector;

import java.net.ConnectException;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.LongPollingTransport;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hal.hildebrand
 * @since 202
 */
public class Konnnektor {
    private static final String ERROR = "error";

    private class SubscriptionImpl implements TopicSubscription {
        private final String topic;

        private SubscriptionImpl(String topic) {
            this.topic = topic;
        }

        /*
         * (non-Javadoc)
         * @see com.salesforce.emp.connector.Subscription#cancel()
         */
        @Override
        public void cancel() {
            replay.remove(topic);
            if (running.get() && client != null) {
                client.getChannel(topic).unsubscribe();
            }
        }

        /*
         * (non-Javadoc)
         * @see com.salesforce.emp.connector.Subscription#getReplay()
         */
        @Override
        public long getReplayFrom() {
            return replay.getOrDefault(topic, REPLAY_FROM_EARLIEST);
        }

        /*
         * (non-Javadoc)
         * @see com.salesforce.emp.connector.Subscription#getTopic()
         */
        @Override
        public String getTopic() {
            return topic;
        }

        @Override
        public String toString() {
            return String.format("Subscription [%s:%s]", getTopic(), getReplayFrom());
        }
    }

    public static long REPLAY_FROM_EARLIEST = -2L;
    public static long REPLAY_FROM_TIP = -1L;

    private static String AUTHORIZATION = "Authorization";
    private static final Logger log = LoggerFactory.getLogger(Konnnektor.class);

    private volatile BayeuxClient client;
    private final HttpClient httpClient;
    private volatile ScheduledFuture<?> keepAlive;
    private final BayeuxParameters parameters;
    private final ConcurrentMap<String, Long> replay = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean();
    private final ScheduledExecutorService scheduler;

    public Konnnektor(BayeuxParameters parameters) {
        this(parameters, Executors.newSingleThreadScheduledExecutor());
    }

    public Konnnektor(BayeuxParameters parameters, ScheduledExecutorService scheduler) {
        this.parameters = parameters;
        httpClient = new HttpClient(parameters.sslContextFactory());
        httpClient.getProxyConfiguration().getProxies().addAll(parameters.proxies());
        this.scheduler = scheduler;
    }

    /**
     * Start the connector
     * 
     * @param handshakeTimeout
     *            - milliseconds to wait until handshake has been completed
     * @return true if connection was established, false otherwise
     */
    public Future<Boolean> start() {
        if (running.compareAndSet(false, true)) { return connect(); }
        CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();
        future.complete(true);
        return future;
    }

    /**
     * Stop the connector
     */
    public void stop() {
        if (!running.compareAndSet(true, false)) { return; }
        if (keepAlive != null) {
            keepAlive.cancel(true);
            keepAlive = null;
        }
        if (client != null) {
            client.disconnect();
            client = null;
        }
        if (httpClient != null) {
            try {
                httpClient.stop();
            } catch (Exception e) {
                log.error("Unable to stop HTTP transport[{}]", parameters.endpoint(), e);
            }
        }
    }

    /**
     * Subscribe to a topic, receiving events after the replayFrom position
     * 
     * @param topic
     *            - the topic to subscribe to
     * @param replayFrom
     *            - the replayFrom position in the event stream
     * @param consumer
     *            - the consumer of the events
     * @return a Future returning the Subscription - on completion returns a Subscription or throws a CannotSubscribe
     *         exception
     */
    public Future<TopicSubscription> subscribe(String topic, long replayFrom, Consumer<Map<String, Object>> consumer) {
        if (!running.get()) { throw new IllegalStateException(
                String.format("Connector[%s} has not been started", parameters.endpoint())); }
        if (replay.putIfAbsent(topic, replayFrom) != null) { throw new IllegalStateException(
                String.format("Already subscribed to %s [%s]", topic, parameters.endpoint())); }
        ClientSessionChannel channel = client.getChannel(topic);
        SubscriptionImpl subscription = new SubscriptionImpl(topic);
        CompletableFuture<TopicSubscription> future = new CompletableFuture<>();
        channel.subscribe((c, message) -> consumer.accept(message.getDataAsMap()), (c, message) -> {
            if (message.isSuccessful()) {
                future.complete(subscription);
            } else {
                Object error = message.get(ERROR);
                future.completeExceptionally(
                        new CannotSubscribe(parameters.endpoint(), topic, replayFrom, error != null ? error : message));
            }
        });
        return future;
    }

    /**
     * Subscribe to a topic, receiving events from the earliest event position in the stream
     * 
     * @param topic
     *            - the topic to subscribe to
     * @param consumer
     *            - the consumer of the events
     * @return a Future returning the Subscription - on completion returns a Subscription or throws a CannotSubscribe
     *         exception
     */
    public Future<TopicSubscription> subscribeEarliest(String topic, Consumer<Map<String, Object>> consumer) {
        return subscribe(topic, REPLAY_FROM_EARLIEST, consumer);
    }

    /**
     * Subscribe to a topic, receiving events from the latest event position in the stream
     * 
     * @param topic
     *            - the topic to subscribe to
     * @param consumer
     *            - the consumer of the events
     * @return a Future returning the Subscription - on completion returns a Subscription or throws a CannotSubscribe
     *         exception
     */
    public Future<TopicSubscription> subscribeTip(String topic, Consumer<Map<String, Object>> consumer) {
        return subscribe(topic, REPLAY_FROM_TIP, consumer);
    }

    private Future<Boolean> connect() {
        CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();
        replay.clear();
        try {
            httpClient.start();
        } catch (Exception e) {
            log.error("Unable to start HTTP transport[{}]", parameters.endpoint(), e);
            running.set(false);
            future.complete(false);
            return future;
        }
        LongPollingTransport httpTransport = new LongPollingTransport(parameters.longPollingOptions(), httpClient) {
            @Override
            protected void customize(Request request) {
                request.header(AUTHORIZATION, parameters.bearerToken());
            }
        };
        client = new BayeuxClient(parameters.endpoint().toExternalForm(), httpTransport);
        client.addExtension(new ReplayExtension(replay));
        client.handshake((c, m) -> {
            if (!m.isSuccessful()) {
                future.completeExceptionally(new ConnectException(
                        String.format("Cannot connect [%s] : %s", parameters.endpoint(), m.get(ERROR))));
                running.set(false);
            } else {
                keepAlive = scheduler.scheduleAtFixedRate(() -> {
                    if (running.get()) {
                        client.handshake();
                    }
                }, parameters.keepAlive(), parameters.keepAlive(), parameters.keepAliveUnit());
                future.complete(true);
            }
        });

        return future;
    }
}
