package com.salesforce.emp.connector;

import java.util.Base64;
import java.util.Map;

import org.cometd.bayeux.Message;

/**
 * An enumeration of the various supported formats for received events.
 * <br>
 * EXPANDED - receive events expanded into JSON consistent with the Streaming API
 * <br>
 * COMPACT - receive events in a compact Avro serialized format
 * 
 * @author lmcalpin
 */
public enum PayloadFormat {
    EXPANDED, COMPACT;
    
    private static final Base64.Decoder base64Decoder = Base64.getDecoder();
    
    public EmpEvent<?> toEvent(Message message) {
        if (this == EXPANDED) {
            Map<String, Object> messageData = message.getDataAsMap();
            String schemaId = (String)messageData.get("schema");
            Map<String, Object> eventMetadata = (Map<String, Object>)messageData.get("event");
            Long replayId = (Long)eventMetadata.get("replayId");
            Map<String, Object> payload = (Map<String, Object>)messageData.get("payload");
            return new EmpEvent<Map<String,Object>>(replayId, schemaId, this, payload);
        } else if (this == COMPACT) {
            Object[] dataList = (Object[])message.getData();
            assert dataList.length == 3;
            @SuppressWarnings("unused")
            String organizationId = (String)dataList[0];
            String schemaId = (String)dataList[1];
            String payloadStr = (String)dataList[2];
            byte[] payload = base64Decoder.decode(payloadStr);
            return new EmpEvent<byte[]>(Long.valueOf(message.getId()), schemaId, this, payload);
        }
        throw new IllegalStateException();
    }
}