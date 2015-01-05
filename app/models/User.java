package models;

import javax.annotation.Nonnull;

public class User {

  public String email;

  // the default constructor is explicitly needed to make
  // User be processable in Play forms since we have invented
  // another constructor.
  public User(){}
  
  
  public User(@Nonnull String aEmail){
      email = aEmail;
  }
  
  public static User byEmail(@Nonnull String aEmail){
      
      // TODO: still fake; match against elasticsearch
      if (aEmail.startsWith("exist")){
	  return new User(aEmail);
      }
      else {
	  return null;
      }
  }
}
