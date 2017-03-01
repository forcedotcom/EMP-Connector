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
public class ReplayExtension extends Adapter {
    private static final String EXTENSION_NAME = "replay";
    private final ConcurrentMap<String, Long> dataMap;
    private final AtomicBoolean supported = new AtomicBoolean();

    public ReplayExtension(ConcurrentMap<String, Long> dataMap) {
        this.dataMap = dataMap;
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