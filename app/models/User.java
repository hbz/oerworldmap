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

  @Override
  public String toString() {
    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
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
