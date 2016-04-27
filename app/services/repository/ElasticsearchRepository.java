package services.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.json.simple.parser.ParseException;

import com.typesafe.config.Config;

import helpers.JsonLdConstants;
import models.Record;
import models.Resource;
import models.ResourceList;
import models.TripleCommit;
import play.Logger;
import services.ElasticsearchConfig;
import services.ElasticsearchProvider;
import services.QueryContext;

public class ElasticsearchRepository extends Repository implements Readable, Writable, Queryable, Aggregatable {

  final private ElasticsearchProvider elasticsearch;

  public ElasticsearchRepository(Config aConfiguration) {
    super(aConfiguration);
    ElasticsearchConfig elasticsearchConfig = new ElasticsearchConfig(aConfiguration);
    Settings settings = ImmutableSettings.settingsBuilder().put(elasticsearchConfig.getClientSettings()).build();
    @SuppressWarnings("resource")
    Client client = new TransportClient(settings)
        .addTransportAddress(new InetSocketTransportAddress(elasticsearchConfig.getServer(), 9300));
    elasticsearch = new ElasticsearchProvider(client, elasticsearchConfig);
  }

  public ElasticsearchProvider getElasticsearchProvider() {
    return this.elasticsearch;
  }

  @Override
  public void addResource(@Nonnull final Resource aResource, Map<String, String> aMetadata) throws IOException {
    Record record = new Record(aResource);
    record.put(Record.DATE_CREATED, aMetadata.get(Record.DATE_CREATED));
    record.put(Record.DATE_MODIFIED, aMetadata.get(TripleCommit.Header.DATE_HEADER));
    record.put(Record.AUTHOR, aMetadata.get(TripleCommit.Header.AUTHOR_HEADER));
    elasticsearch.addJson(record.toString(), record.getId(), Record.TYPE);
    elasticsearch.refreshIndex(elasticsearch.getIndex());
  }

  @Override
  public void addResources(@Nonnull List<Resource> aResources, Map<String, String> aMetadata) throws IOException {
    Map<String, String> records = new HashMap<>();
    for (Resource resource : aResources) {
      Record record = new Record(resource);
      record.put(Record.DATE_CREATED, aMetadata.get(Record.DATE_CREATED));
      record.put(Record.DATE_MODIFIED, aMetadata.get(TripleCommit.Header.DATE_HEADER));
      record.put(Record.AUTHOR, aMetadata.get(TripleCommit.Header.AUTHOR_HEADER));
      records.put(record.getId(), record.toString());
    }
    elasticsearch.addJson(records, Record.TYPE);
    elasticsearch.refreshIndex(elasticsearch.getIndex());
  }

  @Override
  public Resource getResource(@Nonnull String aId) {
    return unwrapRecord(Resource.fromMap(elasticsearch.getDocument(Record.TYPE, aId)));
  }

  public List<Resource> getResources(@Nonnull String aField, @Nonnull Object aValue) {
    List<Resource> resources = new ArrayList<>();
    for (Map<String, Object> doc : elasticsearch.getResources(aField, aValue)) {
      resources.add(Resource.fromMap(doc));
    }
    return unwrapRecords(resources);
  }

  @Override
  public List<Resource> getAll(@Nonnull String aType) throws IOException {
    List<Resource> resources = new ArrayList<>();
    for (Map<String, Object> doc : elasticsearch.getResources(Record.RESOURCE_KEY.concat(".")
        .concat(JsonLdConstants.TYPE), aType)) {
      resources.add(Resource.fromMap(doc));
    }
    return unwrapRecords(resources);
  }

  @Override
  public Resource deleteResource(@Nonnull String aId, Map<String, String> aMetadata) {
    Resource resource = getResource(aId.concat(".").concat(Record.RESOURCE_KEY));
    if (null == resource) {
      return null;
    }
    Logger.debug("DELETING " + aId);
    boolean found = elasticsearch.deleteDocument(Record.TYPE, resource.getId());
    elasticsearch.refreshIndex(elasticsearch.getIndex());
    if (found) {
      return resource;
    } else {
      return null;
    }
  }

  @Override
  public Resource aggregate(@Nonnull AggregationBuilder<?> aAggregationBuilder) throws IOException {
    return aggregate(aAggregationBuilder, null);
  }

  public Resource aggregate(@Nonnull AggregationBuilder<?> aAggregationBuilder, QueryContext aQueryContext)
      throws IOException {
    Resource aggregations = Resource
        .fromJson(elasticsearch.getAggregation(aAggregationBuilder, aQueryContext).toString());
    if (null == aggregations) {
      return null;
    }
    return (Resource) aggregations.get("aggregations");
  }

  /**
   * This search method is designed to be able to make use of the complete
   * Elasticsearch query syntax, as described in
   * http://www.elasticsearch.org/guide
   * /en/elasticsearch/reference/current/search-uri-request.html .
   *
   * @param aQueryString
   *          A string describing the query
   * @param aFilters
   * @return A resource resembling the result set of resources matching the
   *         criteria given in the query string
   * @throws IOException
   * @throws ParseException
   */
  @Override
  public ResourceList query(@Nonnull String aQueryString, int aFrom, int aSize, String aSortOrder,
      Map<String, ArrayList<String>> aFilters) throws IOException, ParseException {
    return query(aQueryString, aFrom, aSize, aSortOrder, aFilters, null);
  }

  public ResourceList query(@Nonnull String aQueryString, int aFrom, int aSize, String aSortOrder,
      Map<String, ArrayList<String>> aFilters, QueryContext aQueryContext) throws IOException, ParseException {

    SearchResponse response = elasticsearch.esQuery(aQueryString, aFrom, aSize, aSortOrder, aFilters, aQueryContext);
    Iterator<SearchHit> searchHits = response.getHits().iterator();
    List<Resource> matches = new ArrayList<>();
    while (searchHits.hasNext()) {
      Resource match = unwrapRecord(Resource.fromMap(searchHits.next().sourceAsMap()));
      matches.add(match);
    }
    // FIXME: response.toString returns string serializations of scripted keys
    Resource aAggregations = (Resource) Resource.fromJson(response.toString()).get("aggregations");
    return new ResourceList(matches, response.getHits().getTotalHits(), aQueryString, aFrom, aSize, aSortOrder,
        aFilters, aAggregations);

  }

  private List<Resource> unwrapRecords(List<Resource> aRecords) {
    List<Resource> resources = new ArrayList<>();
    for (Resource rec : aRecords) {
      resources.add(unwrapRecord(rec));
    }
    return resources;
  }

  private Resource unwrapRecord(Resource aRecord) {
    return (Resource) aRecord.get(Record.RESOURCE_KEY);
  }

}
