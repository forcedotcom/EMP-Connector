package com.salesforce.emp.connector;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSession;
import org.cometd.bayeux.client.ClientSession.Extension.Adapter;

/**
 * The Bayeux extension for replay
 *
 * @author hal.hildebrand
 * @since 202
 */
public class ReplayExtension extends Adapter {
    private static final String EXTENSION_NAME = "replay";
    private final ConcurrentMap<String, Long> dataMap = new ConcurrentHashMap<>();
    private final AtomicBoolean supported = new AtomicBoolean();

    public ReplayExtension(Map<String, Long> dataMap) {
        this.dataMap.putAll(dataMap);
    }

    @Override
    public boolean rcv(ClientSession session, Message.Mutable message) {
        Object data = message.get(EXTENSION_NAME);
        if (this.supported.get() && data != null) {
            try {
                dataMap.put(message.getChannel(), (Long)data);
            } catch (ClassCastException e) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean rcvMeta(ClientSession session, Message.Mutable message) {
        switch (message.getChannel()) {
        case Channel.META_HANDSHAKE:
            Map<String, Object> ext = message.getExt(false);
            this.supported.set(ext != null && Boolean.TRUE.equals(ext.get(EXTENSION_NAME)));
        }
        return true;
    }

    @Override
    public boolean sendMeta(ClientSession session, Message.Mutable message) {
        switch (message.getChannel()) {
        case Channel.META_HANDSHAKE:
            message.getExt(true).put(EXTENSION_NAME, Boolean.TRUE);
            break;
        case Channel.META_SUBSCRIBE:
            if (supported.get()) {
                message.getExt(true).put(EXTENSION_NAME, dataMap);
            }
            break;
        }
        return true;
    }
}