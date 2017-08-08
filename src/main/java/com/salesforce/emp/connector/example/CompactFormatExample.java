/*
 * Copyright (c) 2016, salesforce.com, inc. All rights reserved. Licensed under the BSD 3-Clause license. For full
 * license text, see LICENSE.TXT file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.emp.connector.example;

import static com.salesforce.emp.connector.LoginHelper.login;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;

import com.salesforce.emp.connector.BayeuxParameters;
import com.salesforce.emp.connector.EmpConnector;
import com.salesforce.emp.connector.EmpEvent;
import com.salesforce.emp.connector.PayloadFormat;
import com.salesforce.emp.connector.TopicSubscription;
import com.salesforce.emp.connector.schema.PlatformEventSchemaRepository;

/**
 * An example of using the EMP connector using login credentials, retrieving compact formatted Platform Events.  The Bayeux messages that 
 * we receive will contain B64-encoded bytes for event data encoded in Avro.
 *
 * @author lmcalpin
 * @since 212
 */
public class CompactFormatExample {
    public static void main(String[] argv) throws Exception {
        if (argv.length < 3 || argv.length > 4) {
            System.err.println("Usage: LoginExample username password topic [replayFrom]");
            System.exit(1);
        }
        long replayFrom = EmpConnector.REPLAY_FROM_EARLIEST;
        if (argv.length == 4) {
            replayFrom = Long.parseLong(argv[3]);
        }

        BayeuxParameters params;
        try {
            params = login(argv[0], argv[1]);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
            throw e;
        }

        Map<String, Schema> schemas = new HashMap<>();
        PlatformEventSchemaRepository schemaRepository = new PlatformEventSchemaRepository(params);
        Consumer<EmpEvent<?>> consumer = event -> {
            byte[] payload = (byte[])event.getPayload();
            Schema schema = schemas.computeIfAbsent(event.getSchemaId(), s -> schemaRepository.getSchema(s));
            try {
                BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(new ByteArrayInputStream(payload), null);
                final GenericDatumReader<Object> reader = new GenericDatumReader<>(schema);
                GenericRecord deserializedPayload = (GenericRecord)reader.read(null, decoder);
                System.out.println(String.format("Received:%d\n%s", event.getReplayId(), deserializedPayload));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
        EmpConnector connector = new EmpConnector(params);

        connector.start().get(5, TimeUnit.SECONDS);

        TopicSubscription subscription = connector.subscribe(argv[2], replayFrom, PayloadFormat.COMPACT, consumer)
                                                  .get(5, TimeUnit.SECONDS);

        System.out.println(String.format("Subscribed: %s", subscription));
    }
}
