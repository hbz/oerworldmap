package models;

import play.data.validation.Constraints;

public class User {

  @Constraints.Email
  @Constraints.Required
  public String email;

}
