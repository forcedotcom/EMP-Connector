/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.TXT file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.emp.connector;

import java.util.concurrent.ConcurrentMap;

/**
 * The Bayeux extension for payload formats
 *
 * @author lmcalpin
 * @since 212
 */
public class FormatExtension extends ClientExtension<String> {
    private static final String EXTENSION_NAME = "payload.format";

    public FormatExtension(ConcurrentMap<String, String> dataMap) {
        super(dataMap);
    }

    @Override
    public String getExtensionName() {
        return EXTENSION_NAME;
    }
}