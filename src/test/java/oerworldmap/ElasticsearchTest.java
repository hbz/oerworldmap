package oerworldmap;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.util.Map;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class ElasticsearchTest {

	protected static Node mNode;
	protected static Client mClient;
	
	private final String INDEX = "testindex";
	private final String TYPE = "testtype";
	private String ID = "0";

	@BeforeClass
	public static void makeIndex() throws IOException {
	    mNode = nodeBuilder().local(true).node();
	    mClient = mNode.client();
	}
	
	@Test
	public void testJsonBuilder(){
	    // test demo data from JSON builder
	    IndexResponse indexReponse = index(ElasticsearchDemoData.JSON_BUILDER);
	    assertIndexResponse(indexReponse);
	    assertGetResponse(ID);
	    assertDeleted(ID);
	}
	
	@Test
	public void testJsonString(){
	    // test manually created JSON string
	    IndexResponse indexReponse = index(ElasticsearchDemoData.JSON_STRING);
	    assertIndexResponse(indexReponse);
	    assertGetResponse(ID);
	    assertDeleted(ID);
	}
	
	@Test
	public void testJsonMap(){
	    incrementID();
	    // test manually created JSON string
	    IndexResponse indexReponse = index(ElasticsearchDemoData.JSON_MAP);
	    assertIndexResponse(indexReponse);
	    assertGetResponse(ID);
	    assertDeleted(ID);
	}
	
	@Test
	public void testJsonSerializedBean(){
	    // test manually created JSON string
	    IndexResponse indexReponse = index(new ElasticsearchDemoBean(), new ElasticsearchDemoBean());
	    assertIndexResponse(indexReponse);
	    assertGetResponse(ID);
	    assertDeleted(ID);
	}
	
	@Test
	public void testBulking(){
	    BulkRequestBuilder bulkRequest = mClient.prepareBulk();
	    incrementID();
	    bulkRequest.add(mClient.prepareIndex(INDEX, TYPE, ID)
		    .setSource(ElasticsearchDemoData.JSON_BUILDER));
	    incrementID();
	    bulkRequest.add(mClient.prepareIndex(INDEX, TYPE, ID)
		    .setSource(ElasticsearchDemoData.JSON_MAP));
	    BulkResponse bulkResponse = bulkRequest.execute().actionGet();
	    Assert.assertFalse(bulkResponse.hasFailures());
	}
	
	@Test
	public void testSearching(){
	    // index
	    index(ElasticsearchDemoData.JSON_BUILDER);
	    index(ElasticsearchDemoData.JSON_STRING);
	    index(ElasticsearchDemoData.JSON_MAP);
	    
	    // refresh
	    mNode.client().admin().indices().prepareRefresh().execute().actionGet();
	    
	    // search
	    SearchResponse searchResponse = mClient.prepareSearch(INDEX)
		    .setTypes(TYPE)
		    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
		    .setQuery(QueryBuilders.termQuery("user","kimchy"))
		    .setExplain(true)
		    .execute().actionGet();
	    
	    // validate
	    Assert.assertTrue(searchResponse.getHits().getHits()[0].getSourceAsString().contains("postDate"));
	    Assert.assertTrue(searchResponse.getHits().getHits()[0].getSourceAsString().contains("message"));
	}
	
	private IndexResponse index(XContentBuilder aDocument){
	    incrementID();
	    IndexResponse indexReponse = mClient.prepareIndex(INDEX, TYPE, ID)
		    .setSource(aDocument).execute().actionGet();
	    return indexReponse;
	}
	
	private IndexResponse index(String aDocument){
	    incrementID();
	    IndexResponse indexReponse = mClient.prepareIndex(INDEX, TYPE, ID)
		    .setSource(aDocument).execute().actionGet();
	    return indexReponse;
	}
	
	private IndexResponse index(Map<String, Object> aDocument){
	    incrementID();
	    IndexResponse indexReponse = mClient.prepareIndex(INDEX, TYPE, ID)
		    .setSource(aDocument).execute().actionGet();
	    return indexReponse;
	}
	
	private IndexResponse index(ElasticsearchDemoBean... aDocument){
	    incrementID();
	    IndexResponse indexReponse = mClient.prepareIndex(INDEX, TYPE, ID)
		    .setSource(aDocument).execute().actionGet();
	    return indexReponse;
	}
	
	// a set of assertions for an index response carried out on each kind of test
	private void assertIndexResponse(IndexResponse aIndexResponse){
	    Assert.assertNotNull(aIndexResponse);
	    Assert.assertEquals(INDEX, aIndexResponse.getIndex());
	    Assert.assertEquals(TYPE, aIndexResponse.getType());
	    Assert.assertEquals(ID, aIndexResponse.getId());
	}

	// a set of assertions for a get response carried out on each kind of test
	private void assertGetResponse(String aID) {
	    GetResponse getResponse = mClient.prepareGet(INDEX, TYPE, aID)
		    .setOperationThreaded(false).execute().actionGet();
	    
	    Assert.assertNotNull(getResponse);
	    Assert.assertEquals(INDEX, getResponse.getIndex());
	    Assert.assertEquals(TYPE, getResponse.getType());
	    Assert.assertEquals(aID, getResponse.getId());
	    
	    documentExists(getResponse, true);
	}
	
	// a set of assertions for a delete response carried out on each kind of test
	private void assertDeleted(String aID) {
	    DeleteResponse deleteDesponse = mClient.prepareDelete(INDEX, TYPE, aID)
		    .execute().actionGet();
	    
	    Assert.assertNotNull(deleteDesponse);
	    Assert.assertEquals(INDEX, deleteDesponse.getIndex());
	    Assert.assertEquals(TYPE, deleteDesponse.getType());
	    Assert.assertEquals(aID, deleteDesponse.getId());
	    
	    GetResponse getResponse = mClient.prepareGet(INDEX, TYPE, aID)
		    .setOperationThreaded(false).execute().actionGet();
	    
	    documentExists(getResponse, false);
	}
	
	private void documentExists(GetResponse aResponse, boolean shallExist){
	    Assert.assertEquals((aResponse.getSource() != null), shallExist);
	    Assert.assertEquals((aResponse.getVersion() > 0.0), shallExist);
	    Assert.assertEquals(aResponse.isExists(), shallExist);
	}

	private String incrementID(){
	    return String.valueOf(Integer.valueOf(ID)+1);
	}
	
	@AfterClass
	public static void closeElasticsearch() {
	    mClient.close();
	    mNode.close();
	}
}