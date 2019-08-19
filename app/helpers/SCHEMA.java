package helpers;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Created by fo on 28.04.16. The schema.org vocabulary
 */
public class SCHEMA {

  private static final String BASE = "http://schema.org/";

  private static Property property(String local) {
    return ResourceFactory.createProperty(BASE, local);
  }

  public static final Property comment = property("comment");

  public static final Property name = property("name");

  public static final Property location = property("location");

  public static final Property image = property("image");

  public static final Property agent = property("agent");

  public static final Property object = property("object");

  public static final Property description = property("description");

  public static final Property text = property("text");

  public static final Property startTime = property("startTime");

  public static final Property dateCreated = property("dateCreated");

  public static final Property author = property("author");

  public static final Property provider = property("provider");

}
