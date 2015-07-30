package services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import helpers.JsonTest;

import java.io.IOException;

import models.Resource;

import org.junit.Test;

public class ResourceTrimmerTest implements JsonTest{
  
  private MockResourceRepository mRepo = new MockResourceRepository();
  
  @Test
  public void testSimpleTrim() throws IOException {
    Resource in = getResourceFromJsonFile("resources/ResourceTrimmerTest/simpleTrim.IN.json");
    Resource out = getResourceFromJsonFile("resources/ResourceTrimmerTest/simpleTrim.OUT.json");
    assertNotNull(in);
    Resource trimmed = ResourceTrimmer.trim(in, mRepo);
    assertEquals(out, trimmed);
  }
}
