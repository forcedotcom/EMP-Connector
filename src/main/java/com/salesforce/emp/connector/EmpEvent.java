package com.salesforce.emp.connector;

import java.util.Map;

/**
 * A Platform Event that was published in Salesforce.
 * 
 * @author lmcalpin
 * @since 212
 */
public class EmpEvent<T> {
    private final long replayId;
    private final String schemaId;
    private final PayloadFormat payloadFormat;
    private final T payload;
    
    public EmpEvent(long replayId, String schemaId, PayloadFormat payloadFormat, T payload) {
        this.replayId = replayId;
        this.schemaId = schemaId;
        this.payloadFormat = payloadFormat;
        this.payload = payload;
    }
    
    public long getReplayId() {
        return replayId;
    }
    public String getSchemaId() {
        return schemaId;
    }
    public PayloadFormat getPayloadFormat() {
        return payloadFormat;
    }
    public T getPayload() {
        return payload;
    }
}
