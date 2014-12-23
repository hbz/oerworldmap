package oerworldmap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.common.xcontent.XContentBuilder;

import static org.elasticsearch.common.xcontent.XContentFactory.*;

/**
 * Allow to make simple use of JSON data structure examples for elasticsearch, as described
 * in http://www.elasticsearch.org/guide/en/elasticsearch/client/java-api/1.3/index_.html
 */
public class ElasticsearchDemoData {
    
    public static final String JSON_STRING = "{" +
        "\"user\":\"kimchy\"," +
        "\"postDate\":\"2013-01-30\"," +
        "\"message\":\"I am a simple JSON string\"" +
    "}";

    
    public static final Map<String, Object> JSON_MAP;
    static
    {
	JSON_MAP = new HashMap<String, Object>();
	JSON_MAP.put("user","kimchy");
	JSON_MAP.put("postDate",new Date());
	JSON_MAP.put("message","I am a happy map");
    }
    
    
    public static String JSON_SERIALIZED_BEAN = null;
    private static final ObjectMapper mapper = new ObjectMapper();
    static{
	try {
	    JSON_SERIALIZED_BEAN = mapper.writeValueAsString(new ElasticsearchDemoBean());
	} catch (JsonProcessingException e) {
	    e.printStackTrace();
	}
    }
    
    public static String JSON_BUILT_CONTENT = null;
    public static XContentBuilder JSON_BUILDER = null;
    static{
	try {
	    JSON_BUILDER = jsonBuilder()
	        .startObject()
	            .field("user", "kimchy")
	            .field("postDate", new Date())
	            .field("message", "I was created by a JSON builder")
	        .endObject();
	     JSON_BUILT_CONTENT = JSON_BUILDER.string();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
}
