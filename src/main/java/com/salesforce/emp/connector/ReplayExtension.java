/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.TXT file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.emp.connector;

import java.util.concurrent.ConcurrentMap;

/**
 * The Bayeux extension for replay
 *
 * @author hal.hildebrand
 * @since 202
 */
public class ReplayExtension extends ClientExtension<Long> {
    private static final String EXTENSION_NAME = "replay";

    public ReplayExtension(ConcurrentMap<String, Long> dataMap) {
        super(dataMap);
    }

    @Override
    public String getExtensionName() {
        return EXTENSION_NAME;
    }
}