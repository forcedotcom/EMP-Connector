package com.salesforce.emp.connector;

import java.net.HttpCookie;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.client.ClientSessionChannel.MessageListener;
import org.cometd.client.BayeuxClient;
import org.cometd.client.BayeuxClient.State;
import org.cometd.client.transport.LongPollingTransport;
import org.eclipse.jetty.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hal.hildebrand
 * @since 202
 */
public class Konnnektor {
    private class SubscriptionImpl implements Subscription {
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
    public boolean start(long handshakeTimeout) {
        if (running.compareAndSet(false, true)) { return connect(handshakeTimeout); }
        return true;
    }

    /**
     * Stop the connector
     */
    public void stop() {
        if (!running.compareAndSet(true, false)) { return; }
        if (keepAlive != null) {
            keepAlive.cancel(true);
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
                running.set(false);
            }
        }
    }

    public Subscription subscribe(String topic, long replayFrom, Consumer<Map<String, Object>> consumer) {
        if (!running.get()) { throw new IllegalStateException(
                String.format("Connector[%s} has not been started", parameters.endpoint())); }
        if (replay.putIfAbsent(topic, replayFrom) != null) { throw new IllegalStateException(
                String.format("Already subscribed to %s [%s]", topic, parameters.endpoint())); }
        ClientSessionChannel channel = client.getChannel(topic);
        MessageListener listener = new MessageListener() {
            @Override
            public void onMessage(ClientSessionChannel channel, Message message) {
                consumer.accept(message.getDataAsMap());
            }
        };
        channel.subscribe(listener);
        return new SubscriptionImpl(topic);
    }

    public Subscription subscribeEarliest(String topic, Consumer<Map<String, Object>> consumer) {
        return subscribe(topic, REPLAY_FROM_EARLIEST, consumer);
    }

    public Subscription subscribeTip(String topic, Consumer<Map<String, Object>> consumer) {
        return subscribe(topic, REPLAY_FROM_TIP, consumer);
    }

    private boolean connect(long handshakeTimeout) {
        replay.clear();
        try {
            httpClient.start();
        } catch (Exception e) {
            log.error("Unable to start HTTP transport[{}]", parameters.endpoint(), e);
            running.set(false);
            return false;
        }
        LongPollingTransport httpTransport = new LongPollingTransport(parameters.longPollingOptions(), httpClient) {};
        client = new BayeuxClient(parameters.endpoint().toExternalForm(), httpTransport);
        client.addExtension(new ReplayExtension(replay));
        client.putCookie(new HttpCookie(AUTHORIZATION, parameters.bearerToken()));
        boolean handshook = client.handshake(handshakeTimeout) == State.CONNECTED;
        if (!handshook) {
            running.set(false);
        } else {
            keepAlive = scheduler.scheduleAtFixedRate(() -> {
                if (running.get()) {
                    client.handshake();
                }
            }, parameters.keepAlive(), parameters.keepAlive(), parameters.keepAliveUnit());
        }
        return handshook;
    }
}
