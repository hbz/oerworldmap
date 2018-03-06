package services;

import helpers.ElasticsearchTestGrid;
import helpers.JsonLdConstants;
import helpers.JsonTest;
import helpers.ResourceHelpers;
import models.Record;
import models.Resource;
import models.TripleCommit;
import org.elasticsearch.common.geo.GeoPoint;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import play.Logger;
import services.repository.BaseRepository;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class BaseRepositoryTest extends ElasticsearchTestGrid implements JsonTest {

  private static Map<String, String> mMetadata = new HashMap<>();
  private static BaseRepository mBaseRepo;
  final private static QueryContext mDefaultQueryContext;

  static {
    try {
      mBaseRepo = new BaseRepository(mConfig, ElasticsearchTestGrid.getEsRepo());
    } catch (Exception e) {
      e.printStackTrace();
    }
    mDefaultQueryContext = new QueryContext(null);
    mDefaultQueryContext.setElasticsearchFieldBoosts(new SearchConfig().getBoostsForElasticsearch());
  }

  @BeforeClass
  public static void setUp() {
    mMetadata.put(TripleCommit.Header.AUTHOR_HEADER, "Anonymous");
    mMetadata.put(TripleCommit.Header.DATE_HEADER, "2016-04-08T17:34:37.038+02:00");
  }

  @Test
  public void testResourceWithIdentifiedSubObject() throws IOException {
    Logger.warn("Starting testResourceWithIdentifiedSubObject()");
    Resource resource1 = new Resource("Person", "info:id001");
    resource1.put(JsonLdConstants.CONTEXT, "http://schema.org/");
    Resource resource2 = new Resource("Event", "info:OER15");
    resource2.put(JsonLdConstants.CONTEXT, "http://schema.org/");
    resource1.put("attended", resource2);
    Resource expected1 = getResourceFromJsonFile("BaseRepositoryTest/testResourceWithIdentifiedSubObject.OUT.1.json");
    Resource expected2 = getResourceFromJsonFile("BaseRepositoryTest/testResourceWithIdentifiedSubObject.OUT.2.json");
    mBaseRepo.addResource(resource1, mMetadata);
    mBaseRepo.addResource(resource2, mMetadata);
    Assert.assertEquals(expected1, mBaseRepo.getResource("info:id001"));
    Assert.assertEquals(expected2, mBaseRepo.getResource("info:OER15"));
  }

  @Test
  public void testResourceWithUnidentifiedSubObject() throws IOException {
    Logger.warn("Starting testResourceWithUnidentifiedSubObject()");
    Resource resource = new Resource("Person", "info:id002");
    resource.put(JsonLdConstants.CONTEXT, "http://schema.org/");
    Resource value = new Resource("Foo", null);
    resource.put("attended", value);
    Resource expected = getResourceFromJsonFile("BaseRepositoryTest/testResourceWithUnidentifiedSubObject.OUT.1.json");
    mBaseRepo.addResource(resource, mMetadata);
    Assert.assertEquals(expected, mBaseRepo.getResource("info:id002"));
  }

  @Test
  public void testDeleteResourceWithMentionedResources() throws IOException, InterruptedException {
    Logger.warn("Starting testDeleteResourceWithMentionedResources()");
    // setup: 1 Person ("in1") who has 2 affiliations
    Resource in1 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceWithMentionedResources.IN.1.json");
    Resource in2 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceWithMentionedResources.IN.2.json");
    Resource in3 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceWithMentionedResources.IN.3.json");
    Resource expected1 = getResourceFromJsonFile(
      "BaseRepositoryTest/testDeleteResourceWithMentionedResources.OUT.1.json");
    Resource expected2 = getResourceFromJsonFile(
      "BaseRepositoryTest/testDeleteResourceWithMentionedResources.OUT.2.json");

    mBaseRepo.addResource(in1, mMetadata);
    mBaseRepo.addResource(in2, mMetadata);
    mBaseRepo.addResource(in3, mMetadata);
    // delete affiliation "Oh No Company" and check whether it has been removed
    // from referencing resources
    Resource toBeDeleted = mBaseRepo.getResource("info:urn:uuid:49d8b330-e3d5-40ca-b5cb-2a8dfca70987");
    mBaseRepo.deleteResource(toBeDeleted.getAsString(JsonLdConstants.ID), mMetadata);
    Resource result1 = mBaseRepo.getResource("info:urn:uuid:49d8b330-e3d5-40ca-b5cb-2a8dfca70456");
    Resource result2 = mBaseRepo.getResource("info:urn:uuid:49d8b330-e3d5-40ca-b5cb-2a8dfca70123");
    Assert.assertEquals(expected1, result1);
    Assert.assertEquals(expected2, result2);
    Assert.assertNull(mBaseRepo.getResource("info:urn:uuid:49d8b330-e3d5-40ca-b5cb-2a8dfca70987"));
  }

  @Test
  public void testDeleteLastResourceInList() throws IOException, InterruptedException {
    Logger.warn("Starting testDeleteLastResourceInList()");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteLastResourceInList.DB.1.json");
    Resource db2 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteLastResourceInList.DB.2.json");
    Resource out = getResourceFromJsonFile("BaseRepositoryTest/testDeleteLastResourceInList.OUT.1.json");
    mBaseRepo.addResource(db1, mMetadata);
    mBaseRepo.addResource(db2, mMetadata);
    mBaseRepo.deleteResource("urn:uuid:3a25e950-a3c0-425d-946d-9806665ec665", mMetadata);
    Assert.assertNull(mBaseRepo.getResource("urn:uuid:3a25e950-a3c0-425d-946d-9806665ec665"));
    Assert.assertEquals(out, mBaseRepo.getResource("urn:uuid:c7f5334a-3ddb-4e46-8653-4d8c01e25503"));
    mBaseRepo.deleteResource("urn:uuid:c7f5334a-3ddb-4e46-8653-4d8c01e25503", mMetadata);
  }

  @Test
  public void testDeleteResourceFromList() throws IOException, InterruptedException {
    Logger.warn("Starting testDeleteResourceFromList()");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceFromList.DB.1.json");
    Resource db2 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceFromList.DB.2.json");
    Resource db3 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceFromList.DB.3.json");
    Resource out1 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceFromList.OUT.1.json");
    Resource out2 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceFromList.OUT.2.json");
    mBaseRepo.addResource(db1, mMetadata);
    mBaseRepo.addResource(db2, mMetadata);
    mBaseRepo.addResource(db3, mMetadata);
    mBaseRepo.deleteResource("urn:uuid:3a25e950-a3c0-425d-946d-9806665ec665", mMetadata);
    Assert.assertNull(mBaseRepo.getResource("urn:uuid:3a25e950-a3c0-425d-946d-9806665ec665"));
    Assert.assertEquals(out1, mBaseRepo.getResource("urn:uuid:c7f5334a-3ddb-4e46-8653-4d8c01e25503"));
    Assert.assertEquals(out2, mBaseRepo.getResource("urn:uuid:7cfb9aab-1a3f-494c-8fb1-64755faf180c"));
  }

  @Test
  public void testDeletedResourceIsRemovedFromIndex() throws IOException {
    Logger.warn("Starting testDeletedResourceIsRemovedFromIndex()");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testDeletedResourceIsRemovedFromIndex.DB.1.json");
    mBaseRepo.addResource(db1, mMetadata);
    mBaseRepo.deleteResource(db1.getId(), mMetadata);
    Assert.assertEquals(0, mBaseRepo.getResources("about.@id", db1.getId()).size());
  }

  @Test
  public void testRemoveReference() throws IOException {
    Logger.warn("Starting testRemoveReference()");
    Resource in = getResourceFromJsonFile("BaseRepositoryTest/testRemoveReference.IN.json");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testRemoveReference.DB.1.json");
    Resource db2 = getResourceFromJsonFile("BaseRepositoryTest/testRemoveReference.DB.2.json");
    Resource out1 = getResourceFromJsonFile("BaseRepositoryTest/testRemoveReference.OUT.1.json");
    Resource out2 = getResourceFromJsonFile("BaseRepositoryTest/testRemoveReference.OUT.2.json");
    mBaseRepo.addResource(db1, mMetadata);
    mBaseRepo.addResource(db2, mMetadata);
    mBaseRepo.addResource(in, mMetadata);
    Resource get1 = mBaseRepo.getResource(out1.getAsString(JsonLdConstants.ID));
    Resource get2 = mBaseRepo.getResource(out2.getAsString(JsonLdConstants.ID));
    assertEquals(out1, get1);
    assertEquals(out2, get2);
  }


  @Test
  public void testGetResourcesWithWildcard() throws IOException, InterruptedException {
    Logger.warn("Starting testGetResourcesWithWildcard()");
    Resource in1 = getResourceFromJsonFile("BaseRepositoryTest/testGetResourcesWithWildcard.DB.1.json");
    Resource in2 = getResourceFromJsonFile("BaseRepositoryTest/testGetResourcesWithWildcard.DB.2.json");
    mBaseRepo.addResource(in1, mMetadata);
    mBaseRepo.addResource(in2, mMetadata);
    Assert.assertEquals(2, mBaseRepo.getResources("\\*.@id", "info:123").size());
    mBaseRepo.deleteResource(in1.getAsString(JsonLdConstants.ID), mMetadata);
    mBaseRepo.deleteResource(in2.getAsString(JsonLdConstants.ID), mMetadata);
  }

  @Test
  public void testSearchRankingNameHitsFirst() throws IOException, InterruptedException {

    Logger.warn("Starting testSearchRankingNameHitsFirst()");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testSearchRanking.DB.1.json");
    Resource db2 = getResourceFromJsonFile("BaseRepositoryTest/testSearchRanking.DB.2.json");
    Resource db3 = getResourceFromJsonFile("BaseRepositoryTest/testSearchRanking.DB.3.json");
    Resource db4 = getResourceFromJsonFile("BaseRepositoryTest/testSearchRanking.DB.4.json");

    mBaseRepo.addResource(db2, mMetadata);
    mBaseRepo.addResource(db3, mMetadata);
    mBaseRepo.addResource(db4, mMetadata);
    mBaseRepo.addResource(db1, mMetadata);

    try {
      QueryContext queryContext = new QueryContext(null);
      queryContext.setElasticsearchFieldBoosts( //
        new String[]{ //
          "about.name.@value^9.0", //
          "about.name.@value.variations^9.0", //
          "about.name.@value.simple_tokenized^9.0", //
          "about.alternateName.@value^6.0",
          "about.alternateName.@value.variations^6.0", //
          "about.alternateName.@value.simple_tokenized^6.0"});
      List<Resource> actualList = ResourceHelpers.unwrapRecords(mBaseRepo.query("oerworldmap", 0, 10, null, null, queryContext).getItems());
      List<String> actualNameList = getNameList(actualList);
      // must provide 3 hits because search is reduced on "about.name.@value" and
      // "about.alternateName.@value"
      Assert.assertTrue("Result size list is: " + actualNameList.size(), actualNameList.size() == 3);

      // hits 1 and 2 must contain "oerworldmap" in field "name".
      for (int i = 0; i < 2; i++) {
        Assert.assertTrue(actualNameList.get(i).toLowerCase().contains("oerworldmap"));
      }
      // hit 3 must not contain "oerworldmap" in field "name"
      for (int i = 2; i < 3; i++) {
        Assert.assertFalse(actualNameList.get(i).toLowerCase().contains("oerworldmap"));
      }
      // Resources db6 must not be found, since it only contains "oerworldmap"
      // in the field url
      // that is not in the list of searchable fields
      Assert.assertFalse(actualNameList.contains("Another Provider 4"));
    } //
    finally {
      mBaseRepo.deleteResource("urn:uuid:c7f5334a-3ddb-4e46-8653-4d8c01e00001", mMetadata);
      mBaseRepo.deleteResource("urn:uuid:c7f5334a-3ddb-4e46-8653-4d8c01e00002", mMetadata);
      mBaseRepo.deleteResource("urn:uuid:c7f5334a-3ddb-4e46-8653-4d8c01e00003", mMetadata);
      mBaseRepo.deleteResource("urn:uuid:c7f5334a-3ddb-4e46-8653-4d8c01e00004", mMetadata);
      mBaseRepo.deleteResource("urn:uuid:3a25e950-a3c0-425d-946d-980666500001", mMetadata);
    }
  }

  @Test
  public void testExactPersonHits() throws IOException, InterruptedException {
    Logger.warn("Starting testExactPersonHits()");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testExactPersonHits.DB.1.json");
    Resource db2 = getResourceFromJsonFile("BaseRepositoryTest/testExactPersonHits.DB.2.json");
    mBaseRepo.addResource(db1, mMetadata);
    mBaseRepo.addResource(db2, mMetadata);
    List<Resource> searchResults = mBaseRepo.query("Berger", 0, 10, null, null, mDefaultQueryContext).getItems();
    Assert.assertTrue("Did not get expected number of hits (1).", searchResults.size() == 1);
    Assert.assertTrue("Exact search hit was not found.",
      ((Resource) searchResults.get(0).get("about")).getId().equals(db1.getId()));
    mBaseRepo.deleteResource("urn:uuid:e00a2017-0b78-41f9-9171-8aec2f4b9ca2", mMetadata);
    mBaseRepo.deleteResource("urn:uuid:026ef084-8151-4749-8317-e2c5f46e06c6", mMetadata);
  }

  @Test
  public void testZoomedQueryResults() throws IOException, InterruptedException {
    Logger.warn("Starting testZoomedQueryResults()");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testZoomedQueryResults.DB.1.json");
    Resource db2 = getResourceFromJsonFile("BaseRepositoryTest/testZoomedQueryResults.DB.2.json");
    Resource db3 = getResourceFromJsonFile("BaseRepositoryTest/testZoomedQueryResults.DB.3.json");

    mBaseRepo.addResource(db1, mMetadata);
    mBaseRepo.addResource(db2, mMetadata);
    mBaseRepo.addResource(db3, mMetadata);

    QueryContext queryContext = new QueryContext(null);

    // query before zooming
    List<Resource> beforeZoomList = ResourceHelpers.unwrapRecords(mBaseRepo.query("*", 0, 10, null, null, queryContext).getItems());
    Assert.assertTrue(beforeZoomList.size() == 3);
    List<String> beforeZoomNames = getNameList(beforeZoomList);
    Assert.assertTrue(beforeZoomNames.contains("In Zoom Organization 1"));
    Assert.assertTrue(beforeZoomNames.contains("In Zoom Organization 2"));
    Assert.assertTrue(beforeZoomNames.contains("Out Of Zoom Organization 3"));

    // "zoom"
    queryContext.setZoomTopLeft(new GeoPoint(8.0, 2.5));
    queryContext.setZoomBottomRight(new GeoPoint(4.0, 8.0));

    // query after zooming
    List<Resource> afterZoomList = ResourceHelpers.unwrapRecords(mBaseRepo.query("*", 0, 10, null, null, queryContext).getItems());
    Assert.assertTrue(afterZoomList.size() == 2);
    List<String> afterZoomNames = getNameList(afterZoomList);
    Assert.assertTrue(afterZoomNames.contains("In Zoom Organization 1"));
    Assert.assertTrue(afterZoomNames.contains("In Zoom Organization 2"));
    Assert.assertFalse(afterZoomNames.contains("Out Of Zoom Organization 3"));

    mBaseRepo.deleteResource("urn:uuid:eea2cb2a-9f4c-11e5-945f-001999ac0001", mMetadata);
    mBaseRepo.deleteResource("urn:uuid:eea2cb2a-9f4c-11e5-945f-001999ac0002", mMetadata);
    mBaseRepo.deleteResource("urn:uuid:eea2cb2a-9f4c-11e5-945f-001999ac0003", mMetadata);
  }

  @Test
  public void testPolygonFilteredSearch() throws IOException, InterruptedException {
    Logger.warn("Starting testPolygonFilteredSearch()");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testPolygonFilteredSearch.DB.1.json");
    Resource db2 = getResourceFromJsonFile("BaseRepositoryTest/testPolygonFilteredSearch.DB.2.json");
    Resource db3 = getResourceFromJsonFile("BaseRepositoryTest/testPolygonFilteredSearch.DB.3.json");

    mBaseRepo.addResource(db1, mMetadata);
    mBaseRepo.addResource(db2, mMetadata);
    mBaseRepo.addResource(db3, mMetadata);

    QueryContext queryContext = new QueryContext(null);

    // query before filtering
    List<Resource> beforeFilterList = ResourceHelpers.unwrapRecords(mBaseRepo.query("*", 0, 10, null, null, queryContext).getItems());
    Assert.assertTrue(beforeFilterList.size() == 3);
    List<String> beforeFilterNames = getNameList(beforeFilterList);
    Assert.assertTrue(beforeFilterNames.contains("Out Of Polygon Organization 1"));
    Assert.assertTrue(beforeFilterNames.contains("In Polygon Organization 2"));
    Assert.assertTrue(beforeFilterNames.contains("In Polygon Organization 3"));

    // filter into polygon
    List<GeoPoint> polygon = new ArrayList<>();
    polygon.add(new GeoPoint(12.0, 13.0));
    polygon.add(new GeoPoint(12.0, 14.0));
    polygon.add(new GeoPoint(11.0, 14.0));
    polygon.add(new GeoPoint(6.0, 4.0));
    polygon.add(new GeoPoint(6.0, 3.0));
    polygon.add(new GeoPoint(7.0, 3.0));
    queryContext.setPolygonFilter(polygon);

    // query after filtering
    List<Resource> afterFilterList = ResourceHelpers.unwrapRecords(mBaseRepo.query("*", 0, 10, null, null, queryContext).getItems());
    Assert.assertTrue(afterFilterList.size() == 2);
    List<String> afterFilterNames = getNameList(afterFilterList);
    Assert.assertFalse(afterFilterNames.contains("Out Of Polygon Organization 1"));
    Assert.assertTrue(afterFilterNames.contains("In Polygon Organization 2"));
    Assert.assertTrue(afterFilterNames.contains("In Polygon Organization 3"));

    mBaseRepo.deleteResource("urn:uuid:eea2cb2a-9f4c-11e5-945f-001999ac0001", mMetadata);
    mBaseRepo.deleteResource("urn:uuid:eea2cb2a-9f4c-11e5-945f-001999ac0002", mMetadata);
    mBaseRepo.deleteResource("urn:uuid:eea2cb2a-9f4c-11e5-945f-001999ac0003", mMetadata);
  }

  @Test
  public void testZoomedPolygonQueryResults() throws IOException, InterruptedException {
    Logger.warn("Starting testZoomedPolygonQueryResults()");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testZoomedPolygonQueryResults.DB.1.json");
    Resource db2 = getResourceFromJsonFile("BaseRepositoryTest/testZoomedPolygonQueryResults.DB.2.json");
    Resource db3 = getResourceFromJsonFile("BaseRepositoryTest/testZoomedPolygonQueryResults.DB.3.json");

    mBaseRepo.addResource(db1, mMetadata);
    mBaseRepo.addResource(db2, mMetadata);
    mBaseRepo.addResource(db3, mMetadata);

    QueryContext queryContext = new QueryContext(null);

    // query before zooming
    List<Resource> beforeFilterList = ResourceHelpers.unwrapRecords(mBaseRepo.query("*", 0, 10, null, null, queryContext).getItems());
    Assert.assertTrue(beforeFilterList.size() == 3);
    List<String> beforeFilterNames = getNameList(beforeFilterList);
    Assert.assertTrue(beforeFilterNames.contains("Out Of Polygon Zoom Organization 1"));
    Assert.assertTrue(beforeFilterNames.contains("In Polygon Zoom Organization 2"));
    Assert.assertTrue(beforeFilterNames.contains("Out Of Polygon Zoom Organization 3"));

    // filter into polygon
    List<GeoPoint> polygon = new ArrayList<>();
    polygon.add(new GeoPoint(12.0, 13.0));
    polygon.add(new GeoPoint(12.0, 14.0));
    polygon.add(new GeoPoint(11.0, 14.0));
    polygon.add(new GeoPoint(6.0, 4.0));
    polygon.add(new GeoPoint(6.0, 3.0));
    polygon.add(new GeoPoint(7.0, 3.0));
    queryContext.setPolygonFilter(polygon);

    // and

    // "zoom"
    queryContext.setZoomTopLeft(new GeoPoint(8.0, 2.5));
    queryContext.setZoomBottomRight(new GeoPoint(4.0, 8.0));

    // query after zooming
    List<Resource> afterFilterList = ResourceHelpers.unwrapRecords(mBaseRepo.query("*", 0, 10, null, null, queryContext).getItems());
    Assert.assertTrue(afterFilterList.size() == 1);
    List<String> afterFilterNames = getNameList(afterFilterList);
    Assert.assertFalse(afterFilterNames.contains("Out Of Polygon Zoom Organization 1"));
    Assert.assertTrue(afterFilterNames.contains("In Polygon Zoom Organization 2"));
    Assert.assertFalse(afterFilterNames.contains("Out Of Polygon Zoom Organization 3"));

    mBaseRepo.deleteResource("urn:uuid:eea2cb2a-9f4c-11e5-945f-001999ac0001", mMetadata);
    mBaseRepo.deleteResource("urn:uuid:eea2cb2a-9f4c-11e5-945f-001999ac0002", mMetadata);
    mBaseRepo.deleteResource("urn:uuid:eea2cb2a-9f4c-11e5-945f-001999ac0003", mMetadata);
  }

  @Test
  public void testSearchFuzzyWordSplit() throws IOException, InterruptedException {
    Logger.warn("Starting testSearchFuzzyWordSplit()");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testSearchFuzzyWordSplit.DB.1.json");
    mBaseRepo.addResource(db1, mMetadata);

    // query correct spelling:
    List<Resource> correctQuery = ResourceHelpers.unwrapRecords(mBaseRepo.query("Letest", 0, 10, null, null, mDefaultQueryContext).getItems());
    Assert.assertTrue("Could not find \"Letest\".", correctQuery.size() == 1);

    // query with white space inserted
    List<Resource> alternateQuery = ResourceHelpers.unwrapRecords(mBaseRepo.query("Le Test", 0, 10, null, null, mDefaultQueryContext).getItems());
    Assert.assertTrue("Could not find \"Le Test\".", alternateQuery.size() == 1);

    mBaseRepo.deleteResource("urn:uuid:c407eede-7f00-11e5-a636-c48e8ff00001", mMetadata);
    mBaseRepo.deleteResource("urn:uuid:c407eede-7f00-11e5-a636-c48e8ff00002", mMetadata);
  }

  @Test
  public void testSearchFuzzyExtension() throws IOException, InterruptedException {
    Logger.warn("Starting testSearchFuzzyExtension()");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testSearchFuzzyExtension.DB.1.json");
    mBaseRepo.addResource(db1, mMetadata);

    // query correct spelling:
    List<Resource> correctQuery = mBaseRepo.query("foobar.ao", 0, 10, null, null, mDefaultQueryContext).getItems();
    Assert.assertTrue("Could not find \"foobar.ao\".", correctQuery.size() == 1);

    // query with extension being dropped
    List<Resource> alternateQuery = mBaseRepo.query("foobar", 0, 10, null, null, mDefaultQueryContext).getItems();
    Assert.assertTrue("Could not find \"foobar\".", alternateQuery.size() == 1);

    mBaseRepo.deleteResource("urn:uuid:9843bac3-028f-4be8-ac54-92dcfea00001", mMetadata);
    mBaseRepo.deleteResource("urn:uuid:9843bac3-028f-4be8-ac54-92dcfea00002", mMetadata);
  }

  @Test
  public void testSearchFuzzyDiacritica() throws IOException, InterruptedException {
    Logger.warn("Starting testSearchFuzzyDiacritica()");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testSearchFuzzyDiacritica.DB.1.json");
    mBaseRepo.addResource(db1, mMetadata);

    // query with diacritica
    List<Resource> correctQuery = mBaseRepo.query("tóobar.ao", 0, 10, null, null, mDefaultQueryContext).getItems();
    Assert.assertTrue("Could not find \"tóobar.ao\".", correctQuery.size() == 1);

    // query without diacritica
    List<Resource> alternateQuery = mBaseRepo.query("toobar.ao", 0, 10, null, null, mDefaultQueryContext).getItems();
    Assert.assertTrue("Could not find \"toobar.ao\".", alternateQuery.size() == 1);

    mBaseRepo.deleteResource("urn:uuid:9843bac3-028f-4be8-ac54-92dcfeb00001", mMetadata);
    mBaseRepo.deleteResource("urn:uuid:9843bac3-028f-4be8-ac54-92dcfeb00002", mMetadata);
  }

  @Test
  public void testAbbreviatedSearch() throws IOException, InterruptedException {
    Logger.warn("Starting testAbbreviatedSearch()");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testAbbreviatedSearch.DB.1.json");
    mBaseRepo.addResource(db1, mMetadata);

    // query with first letter only --> no hit
    List<Resource> oneLetterQuery = mBaseRepo.query("d", 0, 10, null, null, mDefaultQueryContext).getItems();
    Assert.assertTrue("Search result given by one letter search.", oneLetterQuery.size() == 0);

    // query with first two letters only --> no hit
    List<Resource> twoLettersQuery = mBaseRepo.query("do", 0, 10, null, null, mDefaultQueryContext).getItems();
    Assert.assertTrue("Search result given by two letter search.", twoLettersQuery.size() == 0);

    // query with first first three letters --> no hit
    List<Resource> threeLettersQuery = mBaseRepo.query("don", 0, 10, null, null, mDefaultQueryContext).getItems();
    Assert.assertTrue("Search result given by three letter search.", threeLettersQuery.size() == 0);

    // query with first first eight letters --> no hit
    List<Resource> eightLettersQuery = mBaseRepo.query("dontfind", 0, 10, null, null, mDefaultQueryContext).getItems();
    Assert.assertTrue("Search result given by eight letter search.", eightLettersQuery.size() == 0);

    // query with all letters --> hit
    List<Resource> allLettersQuery = mBaseRepo.query("dontfindmeabbreviated", 0, 10, null, null, mDefaultQueryContext).getItems();
    Assert.assertTrue("No search result given by all letters search.", allLettersQuery.size() == 1);

    mBaseRepo.deleteResource("urn:uuid:9843bac3-028f-4be8-ac54-threeeb00001", mMetadata);
    mBaseRepo.deleteResource("urn:uuid:9843bac3-028f-4be8-ac54-threeeb00002", mMetadata);
  }

  @Test
  public void testSearchSpecialChars() throws IOException, InterruptedException {
    Logger.warn("Starting testSearchSpecialChars()");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testSearchSpecialChars.DB.1.json");
    mBaseRepo.addResource(db1, mMetadata);

    // query without special chars
    List<Resource> withoutChars = mBaseRepo.query("OERforever", 0, 10, null, null, mDefaultQueryContext).getItems();
    Assert.assertTrue("Could not find \"OERforever\".", withoutChars.size() == 1);

    // query with special chars
    List<Resource> withChars = mBaseRepo.query("OERforever!", 0, 10, null, null, mDefaultQueryContext).getItems();
    Assert.assertTrue("Could not find \"OERforever!\".", withChars.size() == 1);

    mBaseRepo.deleteResource("", mMetadata);
  }

  @Test
  public void testSearchHyphenWords() throws IOException, InterruptedException {
    Logger.warn("Starting testSearchHyphenWords()");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testSearchHyphenWords.DB.1.json");
    mBaseRepo.addResource(db1, mMetadata);

    // query complete word
    List<Resource> completeWord = mBaseRepo.query("e-paideia", 0, 10, null, null, mDefaultQueryContext).getItems();
    Assert.assertTrue("Could not find \"e-paideia\".", completeWord.size() == 1);

    // query abbreviated word
    List<Resource> abbreviatedWord = mBaseRepo.query("e-pai", 0, 10, null, null, mDefaultQueryContext).getItems();
    Assert.assertTrue("Accidentally found \"e-pai\".", abbreviatedWord.size() == 0);

    // query without hyphen
    List<Resource> withoutHyphen = mBaseRepo.query("epai", 0, 10, null, null, mDefaultQueryContext).getItems();
    Assert.assertTrue("Accidentally found \"epai\".", withoutHyphen.size() == 0);

    mBaseRepo.deleteResource("", mMetadata);
  }

  @Test
  public void testSearchMissing() throws IOException, InterruptedException {
    Logger.warn("Starting testSearchMissing()");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testSearchMissing.DB.1.json");
    mBaseRepo.addResource(db1, mMetadata);

    Resource db2 = getResourceFromJsonFile("BaseRepositoryTest/testSearchMissing.DB.2.json");
    mBaseRepo.addResource(db2, mMetadata);

    // query all by name
    List<Resource> queryByName = mBaseRepo.query("Service", 0, 10, null, null, mDefaultQueryContext).getItems();
    Assert.assertTrue("Did not find all by name.", queryByName.size() == 2);

    // query for channel subfield
    List<Resource> queryMissingChannel = mBaseRepo.query("NOT(_exists_:about.availableChannel)", 0, 10, null, null, mDefaultQueryContext).getItems();
    Assert.assertTrue("Accidentally found a resource not missing the channel.", queryMissingChannel.size() < 2);
    Assert.assertTrue("Did not find the resource missing the channel.", queryMissingChannel.size() > 0);

    mBaseRepo.deleteResource("", mMetadata);
  }

  @Test
  public void testSearchKeyword() throws IOException, InterruptedException {
    Logger.warn("Starting testSearchKeyword()");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testSearchKeyword.DB.1.json");
    mBaseRepo.addResource(db1, mMetadata);
    List<Resource> queryByKeyword = mBaseRepo.query("TVET", 0, 10, null, null, mDefaultQueryContext).getItems();
    Assert.assertTrue("Did not find resource by keyword.", queryByKeyword.size() == 1);
    List<Resource> queryByLowercaseKeyword = mBaseRepo.query("tvet", 0, 10, null, null, mDefaultQueryContext).getItems();
    Assert.assertTrue("Did not find resource by lowercased keyword.", queryByLowercaseKeyword.size() == 1);
    List<Resource> queryByUppercaseKeyword = mBaseRepo.query("Vocational Education And Training", 0, 10, null, null, mDefaultQueryContext).getItems();
    Assert.assertTrue("Did not find resource by uppercased keyword.", queryByUppercaseKeyword.size() == 1);
    mBaseRepo.deleteResource("", mMetadata);
  }

  @Test
  public void testRankKeyword() throws IOException, InterruptedException {
    Logger.warn("Starting testRankKeyword()");
    for (int i = 1; i <= 8; i++) {
      Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testRankKeyword.IN." + i + ".json");
      mBaseRepo.addResource(db1, mMetadata);
    }
    Resource desired = getResourceFromJsonFile("BaseRepositoryTest/testRankKeyword.IN.3.json");
    List<Resource> rankedList = mBaseRepo.query("TVET", 0, 10, null, null, mDefaultQueryContext).getItems();
    Assert.assertTrue("Did not find desired resource first while searching for keyword.",
      rankedList.get(0).getAsResource(Record.RESOURCE_KEY).getId().equals(desired.getId()));
    mBaseRepo.deleteResource("", mMetadata);
  }

  @Test
  public void testSearchBySubjectClassification() throws IOException, InterruptedException {
    Logger.warn("Starting testSearchBySubjectClassification()");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testSearchBySubjectClassification.DB.1.json");
    mBaseRepo.importResources(Arrays.asList(db1), mMetadata);
    List<Resource> searchBySubject = mBaseRepo.query("Mytestsubject", 0, 10, null, null, mDefaultQueryContext).getItems();
    Assert.assertTrue("Did not find resource by subject.", searchBySubject.size() > 0);
    mBaseRepo.deleteResource("", mMetadata);
  }

  @Test
  public void testSearchByEducationClassification() throws IOException, InterruptedException {
    Logger.warn("Starting testSearchByEducationClassification()");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testSearchByEducationClassification.DB.1.json");
    mBaseRepo.importResources(Arrays.asList(db1), mMetadata);
    List<Resource> searchBySubject = mBaseRepo.query("Mytestaudience", 0, 10, null, null, mDefaultQueryContext).getItems();
    Assert.assertTrue("Did not find resource by audience.", searchBySubject.size() > 0);
    mBaseRepo.deleteResource("", mMetadata);
  }

  @Test
  public void testNoGroundlessHits() throws IOException, InterruptedException {
    Logger.warn("Starting testNoGroundlessHits()");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testNoGroundlessHits.DB.1.json");
    mBaseRepo.addResource(db1, mMetadata);
    QueryContext queryContext = new QueryContext(null);
    queryContext.setElasticsearchFieldBoosts(new SearchConfig().getBoostsForElasticsearch());
    List<Resource> noHit = mBaseRepo.query("Schokolade", 0, 10, null, null, queryContext).getItems();
    Assert.assertTrue("Unintended hit for \"Schokolade\".", noHit.size() == 0);
    noHit = mBaseRepo.query("Cape Town", 0, 10, null, null, queryContext).getItems();
    Assert.assertTrue("Unintended hit for \"Cape Town\".", noHit.size() == 0);
    noHit = mBaseRepo.query("Paris", 0, 10, null, null, queryContext).getItems();
    Assert.assertTrue("Unintended hit for \"Paris\".", noHit.size() == 0);
    noHit = mBaseRepo.query("London", 0, 10, null, null, queryContext).getItems();
    Assert.assertTrue("Unintended hit for \"London\".", noHit.size() == 0);
    List<Resource> aHit = mBaseRepo.query("Education", 0, 10, null, null, queryContext).getItems();
    Assert.assertTrue("Missing hit for \"Education\".", aHit.size() > 0);
    mBaseRepo.deleteResource("", mMetadata);
  }

  // @Test
  // TODO: this test has to be skipped until we find a new way to parse the long expressions that have to be analyzed
  // Using the default setting (indices.query.bool.max_clause_count: 1024), parsing "các tiểu vương quốc a-rập thống nhất"
  // (which is an expression for "united arab emirates") fails due to more than 1024 boolean clauses necessary.
  // However, a working setting could not be found as high max_clause_count values lead to test timeouts during the
  // query initialization phase.
  public void testCountrySynonyms() throws IOException, InterruptedException {
    Logger.warn("Starting testCountrySynonyms()");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testCountrySynonyms.DB.1.json");
    mBaseRepo.addResource(db1, mMetadata);
    QueryContext queryContext = new QueryContext(null);
    queryContext.setElasticsearchFieldBoosts(new SearchConfig().getBoostsForElasticsearch());
    BufferedReader countrySynonyms = new BufferedReader(new FileReader("scripts/country_synonyms.txt"));
    String countryLine = countrySynonyms.readLine();
    while (countryLine != null) {
      String[] split = countryLine.split(",", 2);
      List<String> countries = Arrays.asList(split[1].split(","));
      for (String country : countries) {
        // check all variants of "Ghana" are found
        if (split[0].equals("gh")) {
          List<Resource> hit = mBaseRepo.query(country, 0, 10, null, null, queryContext).getItems();
          Assert.assertTrue("Missing hit for " + country + " while searching for country synonyms.", hit.size() == 1);
        }
        // check no other country variants are found
        else {
          List<Resource> noHit = mBaseRepo.query(country, 0, 10, null, null, queryContext).getItems();
          Assert.assertTrue("Unexpected hit for " + country + " while searching for country synonyms.", noHit.size() == 0);
        }
      }
      countryLine = countrySynonyms.readLine();
    }
    mBaseRepo.deleteResource("urn:uuid:167b8283-fff2-4b4e-b0a0-909083305804", mMetadata);
  }

  @Test
  public void testLocalityNameSearch() throws IOException {
    Logger.warn("Starting testLocalityNameSearch()");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testCountrySynonyms.DB.1.json");
    mBaseRepo.addResource(db1, mMetadata);
    QueryContext queryContext = new QueryContext(null);
    queryContext.setElasticsearchFieldBoosts(new SearchConfig().getBoostsForElasticsearch());
    List<Resource> hit = mBaseRepo.query("Accra", 0, 10, null, null, queryContext).getItems();
    Assert.assertEquals(1, hit.size());
  }

  @Test
  public void testBoostByLinks() throws IOException {
    Logger.warn("Starting testBoostByLinks()");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testBoostByLinks.DB.1.json");
    Resource db2 = getResourceFromJsonFile("BaseRepositoryTest/testBoostByLinks.DB.2.json");
    mBaseRepo.importResources(Arrays.asList(new Resource[]{db1, db2}), mMetadata);
    QueryContext queryContext = new QueryContext(null);
    queryContext.setElasticsearchFieldBoosts(new SearchConfig().getBoostsForElasticsearch());
    List<Resource> hits = mBaseRepo.query("OER", 0, 10, null, null, queryContext).getItems();
    Assert.assertEquals("Did not get expected number of hits (2).", 2, hits.size());
    List<String> names = getNameList(ResourceHelpers.unwrapRecords(hits));
    Assert.assertEquals("Did not get linked hit first.",
      db2.getNestedFieldValue("name.@value", Locale.ENGLISH), names.get(0));
    mBaseRepo.deleteResource("", mMetadata);
  }

  @Test
  public void testBoostByLinksNested() throws IOException {
    Logger.warn("Starting testBoostByLinksNested()");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testBoostByLinksNested.DB.1.json");
    Resource db2 = getResourceFromJsonFile("BaseRepositoryTest/testBoostByLinksNested.DB.2.json");
    Resource db3 = getResourceFromJsonFile("BaseRepositoryTest/testBoostByLinksNested.DB.3.json");
    mBaseRepo.importResources(Arrays.asList(new Resource[]{db1, db2, db3}), mMetadata);
    QueryContext queryContext = new QueryContext(null);
    queryContext.setElasticsearchFieldBoosts(new SearchConfig().getBoostsForElasticsearch());
    List<Resource> hits = mBaseRepo.query("OER", 0, 10, null, null, queryContext).getItems();
    Assert.assertEquals("Did not get expected number of hits (3).", 3, hits.size());
    List<String> names = getNameList(ResourceHelpers.unwrapRecords(hits));
    // The nested link of db3 does not count, so db2 must be first
    Assert.assertEquals("Did not get linked hit first.",
      db2.getNestedFieldValue("name.@value", Locale.ENGLISH), names.get(0));
    mBaseRepo.deleteResource("", mMetadata);
  }

  @Test
  public void testSearchStemmed()  throws IOException {
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testSearchStemmed.DB.1.json");
    mBaseRepo.addResource(db1, mMetadata);
    QueryContext queryContext = new QueryContext(null);
    queryContext.setElasticsearchFieldBoosts(new SearchConfig().getBoostsForElasticsearch());
    List<Resource> literalHit = mBaseRepo.query("vielfältiges Angebot", 0, 10, null, null, queryContext).getItems();
    Assert.assertEquals("Missing hit for literal search.", 1, literalHit.size());
    List<Resource> stemmedHit = mBaseRepo.query("vielfaltig Angebote", 0, 10, null, null, queryContext).getItems();
    Assert.assertEquals("Missing hit for stemming based search.", 1, stemmedHit.size());
    mBaseRepo.deleteResource("urn:uuid:751c2006-4601-4c43-935b-4f7380784dd3", mMetadata);
  }

  private List<String> getNameList(List<Resource> aResourceList) {
    List<String> result = new ArrayList<>();
    for (Resource r : aResourceList) {
      List<?> nameList = (List<?>) r.get("name");
      Resource name = (Resource) nameList.get(0);
      result.add(name.getAsString("@value"));
    }
    return result;
  }

}
