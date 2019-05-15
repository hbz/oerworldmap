package services;

public interface AccountService {
  void setPermissions(String aId, String aUser);
  boolean removePermissions(String aId);
  void setApache2Ctl(String aApache2ctl); // FIXME: remove from interface
  boolean deleteUser(String username);
  String getProfileId(String username);
  void setProfileId(String username, String profileId);
  String getUsername(String profileId);
}
