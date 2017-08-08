/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.TXT file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.emp.connector;

import java.util.Map;
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
public abstract class ClientExtension<T> extends Adapter {
    private final ConcurrentMap<String, T> dataMap;
    private final AtomicBoolean supported = new AtomicBoolean();

    public ClientExtension(ConcurrentMap<String, T> dataMap) {
        this.dataMap = dataMap;
    }
    
    public abstract String getExtensionName();

    @SuppressWarnings("unchecked")
    @Override
    public boolean rcv(ClientSession session, Message.Mutable message) {
        T data = (T)message.get(getExtensionName());
        if (this.supported.get() && data != null) {
            try {
                dataMap.put(message.getChannel(), data);
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
            this.supported.set(ext != null && Boolean.TRUE.equals(ext.get(getExtensionName())));
        }
        return true;
    }

    @Override
    public boolean sendMeta(ClientSession session, Message.Mutable message) {
        switch (message.getChannel()) {
        case Channel.META_HANDSHAKE:
            message.getExt(true).put(getExtensionName(), Boolean.TRUE);
            break;
        case Channel.META_SUBSCRIBE:
            if (supported.get()) {
                message.getExt(true).put(getExtensionName(), dataMap);
            }
            break;
        }
        return true;
    }
}