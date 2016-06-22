/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.TXT file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.emp.connector;

import java.net.URL;

public class CannotSubscribe extends Exception {
    private final String topic;
    private final long replayFrom;
    private final Object error;
    private final URL endpoint;

    public CannotSubscribe(URL endpoint, String topic, long replayFrom, Object error) {
        super(String.format("Unable to subscribe to [%s:%s] [%s] : %s", topic, replayFrom, endpoint, error));
        this.endpoint = endpoint;
        this.topic = topic;
        this.replayFrom = replayFrom;
        this.error = error;
    }

    public String getTopic() {
        return topic;
    }

    public long getReplayFrom() {
        return replayFrom;
    }

    public Object getErrror() {
        return error;
    }

    public URL getEndpoint() {
        return endpoint;
    }
}
