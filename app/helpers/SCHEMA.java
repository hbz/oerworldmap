package helpers;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Created by fo on 28.04.16. The schema.org vocabulary
 */
public class SCHEMA {

  private static final String BASE = "http://schema.org/";

  private static Resource resource(String local) {
    return ResourceFactory.createResource(BASE + local);
  }

  private static Property property(String local) {
    return ResourceFactory.createProperty(BASE, local);
  }

  public static final Property comment = property("comment");

  public static final Property name = property("name");

  public static final Property location = property("location");

  public static final Property image = property("image");

}
