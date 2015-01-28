package test.models;

import static org.junit.Assert.*;

import org.junit.Test;

import models.Resource;
import helpers.JsonLdConstants;

public class ResourceTest {

  @Test
  public void testConstructorWithoutId() {
    Resource resource = new Resource("Type");
    assertEquals(resource.get(JsonLdConstants.TYPE), "Type");
    assertNotNull(resource.get(JsonLdConstants.ID));
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
    resource.set(property, value);
    assertEquals(resource.get(property), value);
  }

  @Test
  public void testToString() {
    Resource resource = new Resource("Type", "id");
    String property = "property";
    String value = "value";
    resource.set(property, value);
    String expected = "{\"@type\":\"Type\",\"@id\":\"id\",\"property\":\"value\"}";
    assertEquals(expected, resource.toString());
  }

}
