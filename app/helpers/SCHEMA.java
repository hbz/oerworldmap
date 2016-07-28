package helpers;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * Created by fo on 28.04.16.
 * The schema.org vocabulary
 */
public class SCHEMA {

  protected static final String uri ="http://schema.org/";

  /** returns the URI for this schema
   @return the URI for this schema
   */
  public static String getURI() {
    return uri;
  }

  protected static final Resource resource(String local ) {
    return ResourceFactory.createResource( uri + local );
  }

  protected static final Property property(String local ) {
    return ResourceFactory.createProperty( uri, local );
  }

  public static final Property about = property("about");

  public static final Property broader = property("broader");

}
