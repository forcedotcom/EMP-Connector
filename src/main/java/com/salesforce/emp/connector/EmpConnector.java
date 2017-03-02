/*
 * Copyright (c) 2016, salesforce.com, inc. All rights reserved. Licensed under the BSD 3-Clause license. For full
 * license text, see LICENSE.TXT file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.emp.connector;

import java.net.ConnectException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.cometd.bayeux.Message;
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
public class EmpConnector {
    private class SubscriptionImpl implements TopicSubscription {
        private final Consumer<Map<String, Object>> consumer;
        private final String topic;

        private SubscriptionImpl(String topic, Consumer<Map<String, Object>> consumer) {
            this.topic = topic;
            this.consumer = consumer;
            subscriptions.add(this);
        }

        /*
         * (non-Javadoc)
         * @see com.salesforce.emp.connector.Subscription#cancel()
         */
        @Override
        public void cancel() {
            subscriptions.remove(this);
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

        private CompletableFuture<TopicSubscription> resubscribe() {
            return subscribe(topic, getReplayFrom(), consumer, this);
        }
    }

    public static long REPLAY_FROM_EARLIEST = -2L;
    public static long REPLAY_FROM_TIP = -1L;

    private static String AUTHORIZATION = "Authorization";
    private static final String ERROR = "error";
    private static final String FAILURE = "failure";
    private static final Logger log = LoggerFactory.getLogger(EmpConnector.class);

    private volatile BayeuxClient client;
    private final Executor exec;
    private final HttpClient httpClient;
    private volatile ScheduledFuture<?> keepAlive;
    private final BayeuxParameters parameters;
    private final ConcurrentMap<String, Long> replay = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean();
    private final Set<SubscriptionImpl> subscriptions = new CopyOnWriteArraySet<>();

    public EmpConnector(BayeuxParameters parameters) {
        this(parameters, Executors.newSingleThreadExecutor());
    }

    public EmpConnector(BayeuxParameters parameters, Executor exec) {
        this.parameters = parameters;
        httpClient = new HttpClient(parameters.sslContextFactory());
        httpClient.getProxyConfiguration().getProxies().addAll(parameters.proxies());
        this.exec = exec;
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
        log.info("starting connector");
        CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();
        future.complete(true);
        return future;
    }

    /**
     * Stop the connector
     */
    public void stop() {
        if (!running.compareAndSet(true, false)) { return; }
        log.info("stopping connector");
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
        SubscriptionImpl subscription = new SubscriptionImpl(topic, consumer);
        CompletableFuture<TopicSubscription> future = subscribe(topic, replayFrom, consumer, subscription);
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
        log.info("connecting to {}", parameters.endpoint());
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
        client = new BayeuxClient(parameters.endpoint().toExternalForm(), httpTransport) {
            @Override
            public void onFailure(Throwable failure, List<? extends Message> messages) {
                log.error("connection failure, reconnecting", failure);
                exec.execute(() -> reconnect());
            }
        };
        client.addExtension(new ReplayExtension(replay));
        client.handshake((c, m) -> {
            if (!m.isSuccessful()) {
                Object error = m.get(ERROR);
                if (error == null) {
                    error = m.get(FAILURE);
                }
                log.info("error in handshake: {}", error);
                future.completeExceptionally(
                        new ConnectException(String.format("Cannot connect [%s] : %s", parameters.endpoint(), error)));
                running.set(false);
            } else {
                log.debug("Handshake successful");
                future.complete(true);
            }
        });

        return future;
    }

    private void reconnect() {
        reconnect(1);
    }

    private void reconnect(int attempt) {
        if (attempt > parameters.reconnectAttempts()) {
            log.error("Cannot reconnect to the server after {} attempts", parameters.reconnectAttempts());
            stop();
            return;
        }
        exec.execute(() -> {
            try {
                connect().get(parameters.reconnectTimeout(), parameters.reconnectTimeoutUnit());
                resubscribe();
            } catch (InterruptedException e) {
                log.warn("reconnect interrupted, discontinuing");
                return;
            } catch (ExecutionException e) {
                log.warn("Exception during reconnect attempt {}", attempt, e.getCause());
                reconnect(attempt + 1);
            } catch (TimeoutException e) {
                log.warn("Connection attempt {} timed out", attempt);
                reconnect(attempt + 1);
            }
        });
    }

    private void resubscribe() {
        subscriptions.forEach(subscription -> {
            try {
                subscription.resubscribe().get(parameters.resubsribeTimeout(), parameters.reconnectTimeoutUnit());
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error("Cannot resubscribe to the topic {} replay from {}", subscription.topic,
                        subscription.getReplayFrom());
                stop();
                return;
            }
        });
    }

    private CompletableFuture<TopicSubscription> subscribe(String topic, long replayFrom,
            Consumer<Map<String, Object>> consumer, SubscriptionImpl subscription) {
        CompletableFuture<TopicSubscription> future = new CompletableFuture<>();
        ClientSessionChannel channel = client.getChannel(topic);
        channel.subscribe((c, message) -> consumer.accept(message.getDataAsMap()), (c, message) -> {
            if (message.isSuccessful()) {
                log.debug("Subscription successful to {} replay from {}: ", topic, replayFrom);
                future.complete(subscription);
            } else {
                Object error = message.get(ERROR);
                if (error == null) {
                    error = message.get(FAILURE);
                }
                log.info("error in subscribing to {} replay from {}: ", topic, replayFrom, error);
                future.completeExceptionally(
                        new CannotSubscribe(parameters.endpoint(), topic, replayFrom, error != null ? error : message));
            }
        });
        return future;
    }
}
