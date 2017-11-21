package models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import play.data.validation.Constraints;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "user")
public class User {

  @Constraints.Email
  @Constraints.Required
  public String email;
  public String name;

  final private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  public String toString() {
    ObjectWriter ow = OBJECT_MAPPER.writer().withDefaultPrettyPrinter();
    String json = null;
    try {
      json = ow.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      System.err.println(String.format("Error while transforming user %s to JSON String.", email));
      e.printStackTrace();
    }
    return json;
  }
}
