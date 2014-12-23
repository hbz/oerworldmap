package oerworldmap;

import static org.elasticsearch.node.NodeBuilder.*;

import java.io.File;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.fest.util.Strings;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class ElasticsearchDemo {
    
    private static final Config CONFIG = ConfigFactory.parseFile(
		new File("conf/application.conf")).resolve();
    
    private static final String CLIENT_TIMEOUT_INTERVAL_KEY = "client.transport.ping_timeout";
    private static final String CLIENT_NODES_SAMPLING_INTERVAL_KEY = "client.transport.nodes_sampler_interval";
    private static final String CLUSTER_KEY = "cluster.name";
    private static final String CLUSTER_NAME_VALIDATION_KEY = "client.transport.sniff";
    private static final String SNIFF_MODE_KEY = "client.transport.sniff"; 
                
    private static final String DEFAULT_CLUSTER_NAME = "elasticsearch";
     
    private Node mNode;
    private Client mClient;
    private TransportClient mRemoteClient;
    private Builder mSettingsBuilder;
    
    public ElasticsearchDemo(){
	mSettingsBuilder = ImmutableSettings.settingsBuilder();
	if (CONFIG != null && !CONFIG.isEmpty() && Strings.isEmpty(CONFIG.getString(CLUSTER_KEY))){
	    mSettingsBuilder.put(CLUSTER_KEY, CONFIG.getString(CLUSTER_KEY));
	}
	else{
	    mSettingsBuilder.put(CLUSTER_KEY, DEFAULT_CLUSTER_NAME);
	}
    }
    
    public void setClustername(String aClustername){
	mSettingsBuilder.put(CLUSTER_KEY, aClustername);
    }
    
    public void setSniffMode(boolean aBoolean){
	mSettingsBuilder.put(SNIFF_MODE_KEY, aBoolean);
    }
    
    public void setClusterNameValidation(boolean aBoolean){
	mSettingsBuilder.put(CLUSTER_NAME_VALIDATION_KEY, aBoolean);
    }
    
    /**
     * @param aTimeoutInterval format is "<count><unit>", e.g. "3s"
     */
    public void setClientTimeout(String aTimeoutInterval){
	mSettingsBuilder.put(CLIENT_TIMEOUT_INTERVAL_KEY, aTimeoutInterval);
    }
    
    /**
     * @param aNodesSamplingInterval format is "<count><unit>", e.g. "3s"
     */
    public void setNodesSamplingInterval(String aNodesSamplingInterval){
	mSettingsBuilder.put(CLIENT_NODES_SAMPLING_INTERVAL_KEY, aNodesSamplingInterval);
    }
    
    public void setupElasticsearchNode(){
	mNode = nodeBuilder().clusterName(mSettingsBuilder.get(CLUSTER_KEY)).node();
    }
    
    public void setupNodeClient(){
	if (mNode != null){
	    mClient = mNode.client();
	}
    }
    
    public void closeNodeClient(){
	if (mClient != null){
	    mNode.close();
	}
    }
    
    @SuppressWarnings("resource")
    public void setupRemoteClient(String[] aHostnames, int[] aPorts){
	if (aHostnames.length == 0 || aHostnames.length != aPorts.length){
	    throw new ElasticsearchIllegalArgumentException
	    	(String.format("Elasticsearch TransportClient setup: count of hostnames (%i) does not fit count of ports (%i).",
		 aHostnames.length, aPorts.length));
	}
	mRemoteClient = new TransportClient(mSettingsBuilder.build())
        	.addTransportAddress(new InetSocketTransportAddress("host1", 9300))
        	.addTransportAddress(new InetSocketTransportAddress("host2", 9300));
    }
    
    public void closeRemoteClient(){
	if (mRemoteClient != null)
	    mRemoteClient.close();
    }
}
