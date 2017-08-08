package com.salesforce.emp.connector.schema;

import org.apache.avro.Schema;

import com.salesforce.emp.connector.BayeuxParameters;
import com.salesforce.emp.util.RestUtil;

/**
 * Retrieves Avro schema that have been registered in the Platform Event schema repository. 
 * 
 * @author lmcalpin
 * @since 212
 */
public class PlatformEventSchemaRepository {
    private final BayeuxParameters bayeuxParams;
    private final RestUtil restUtil;
    
    public PlatformEventSchemaRepository(BayeuxParameters bayeuxParams) {
        this.bayeuxParams = bayeuxParams;
        this.restUtil = new RestUtil(bayeuxParams);
    }

    public Schema getSchema(String schemaId) {
        Schema.Parser parser = new Schema.Parser();
        return parser.parse(getSchemaAsString(schemaId));
    }
    
    public String getSchemaAsString(String schemaId) {
        try {
            return restUtil.get(bayeuxParams.host().toExternalForm() + "/services/data/v41.0/event/eventSchema/" + schemaId);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
