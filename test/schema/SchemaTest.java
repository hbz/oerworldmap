package schema;

import static org.junit.Assert.*;

import models.Resource;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;


/**
 * @author fo
 */
public class SchemaTest {

  @Test
  public void testPerson() throws IOException {
    InputStream in = ClassLoader.getSystemResourceAsStream("person.json");
    String json = IOUtils.toString(in, "UTF-8");
    Resource Person = Resource.fromJson(json);
    assertNotNull(Person);
    assertTrue(Person.validate().isSuccess());
  }


}
