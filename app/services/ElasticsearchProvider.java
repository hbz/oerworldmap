package services;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import javax.annotation.Nonnull;

import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;

import play.Logger;

public class ElasticsearchProvider {

  private static ElasticsearchConfig mConfig = new ElasticsearchConfig();
  
  private static Node mNode;
  private static boolean mIsLocal;

  public static Node createServerNode(@Nonnull boolean shallBeLocal) {
    if (mNode != null) {
      Logger.warn("A Server node already exists while trying to create a(nother) server node.");
      if (mIsLocal && !shallBeLocal){
        Logger.error("Local server node exists while trying to create remote server node.");
      }
      else if (!mIsLocal && shallBeLocal){
        Logger.error("Remote server node exists while trying to create local server node.");
      }
    }
    else{
      if (shallBeLocal) {
        mNode = nodeBuilder().local(true).node();
        mIsLocal = true;
      }
      else {
        mNode = nodeBuilder().clusterName(mConfig.getCluster()).node();
        mIsLocal = false;
      }
    }
    return mNode;
  }
  
  public static Node getNode(){
    return mNode;
  }
  
  public static boolean hasNode(){
    return mNode != null;
  }

  public static Client getClient(Node aServerNode) {
    return aServerNode.client();
  }

  public static void createIndex(Client aClient, String aIndex) {
    if (aClient == null) {
      throw new java.lang.IllegalStateException(
          "Trying to set Elasticsearch index with no existing client.");
    }
    if (indexExists(aClient, aIndex)) {
      Logger.warn("Index " + aIndex + " already exists while trying to create it.");
    }
    else{
      aClient.admin().indices().prepareCreate(aIndex).execute().actionGet();  
    }
  }

  // return true if the specified Index already exists on the specified Client.
  public static boolean indexExists(Client aClient, String aIndex) {
    return aClient.admin().indices().prepareExists(aIndex).execute().actionGet().isExists();
  }
}
