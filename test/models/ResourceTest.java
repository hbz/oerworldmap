package models;

import static org.junit.Assert.*;

import org.junit.Test;

import models.Resource;
import helpers.JsonLdConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ResourceTest {

  @Test
  public void testConstructorWithoutId() {
    Resource resource = new Resource("Type");
    assertEquals(resource.get(JsonLdConstants.TYPE), "Type");
    assertNull(resource.get(JsonLdConstants.ID));
  }

  @Test
  public void testConstructorWithId() {
    Resource resource = new Resource("Type", "id");
    assertEquals(resource.get(JsonLdConstants.TYPE), "Type");
    assertEquals(resource.get(JsonLdConstants.ID), "id");
  }

  @Test
  public void testSetGetProperty() {
    Resource resource = new Resource("Type", "id");
    String property = "property";
    String value = "value";
    resource.put(property, value);
    assertEquals(resource.get(property), value);
  }

  @Test
  public void testToString() {
    Resource resource = new Resource("Type", "id");
    String property = "property";
    String value = "value";
    resource.put(property, value);
    String expected = "{\"@type\":\"Type\",\"@id\":\"id\",\"property\":\"value\"}";
    assertEquals(expected, resource.toString().replaceAll("[\n \t]", ""));
  }
  
  @Test
  public void testFromFlatMap() {
    Map<String, Object> map = new HashMap<String, Object>();
    String type = "Type";
    String property = "property";
    String value = "value";
    map.put(JsonLdConstants.TYPE, type);
    map.put(property, value);
    Resource resource = Resource.fromMap(map);
    assertEquals(resource.get(property), value);
  }
  
  @Test
  public void testFromNestedMap() {
    
    String property = "nested";
    String type = "Type";
    String id = UUID.randomUUID().toString();
    
    Map<String, Object> map = new HashMap<String, Object>();
    Map<String, Object> value = new HashMap<String, Object>();

    value.put(JsonLdConstants.TYPE, type);
    value.put(JsonLdConstants.ID, id);
    map.put(JsonLdConstants.TYPE, type);
    map.put(JsonLdConstants.ID, id);

    map.put(property, value);
    Resource resource = Resource.fromMap(map);
    assertEquals(resource.get(property), Resource.fromMap(value));
    
  }

  @Test
  public void testArrayAtomicValues() {

    String property = "values";
    String type = "Type";
    String id = UUID.randomUUID().toString();

    Map<String, Object> map = new HashMap<String, Object>();
    List<String> value = new ArrayList<>();

    value.add("foo");
    value.add("bar");
    map.put(JsonLdConstants.TYPE, type);
    map.put(JsonLdConstants.ID, id);

    map.put(property, value);
    Resource resource = Resource.fromMap(map);
    assertEquals((ArrayList) resource.get(property), value);

  }

  @Test
  public void testArrayResourceValues() {

    String property = "values";
    String type = "Type";
    String id = UUID.randomUUID().toString();
    String sid1 = UUID.randomUUID().toString();
    String sid2 = UUID.randomUUID().toString();

    Map<String, Object> map = new HashMap<String, Object>();
    map.put(JsonLdConstants.TYPE, type);
    map.put(JsonLdConstants.ID, id);
    List<Map<String, Object>> value = new ArrayList<>();

    Resource val1 = new Resource(type, sid1);
    Resource val2 = new Resource(type, sid2);

    val1.put("property", "value");
    val2.put("property", "value");
    value.add(val1);
    value.add(val2);

    map.put(property, value);
    Resource resource = Resource.fromMap(map);
    assertEquals(resource.get(property), value);

  }
  
}
