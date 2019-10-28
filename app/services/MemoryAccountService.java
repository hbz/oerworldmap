package services;

import java.util.HashMap;
import java.util.Map;

public class MemoryAccountService implements  AccountService {
  private Map<String, String> mAccounts = new HashMap<>();

  @Override
  public void setPermissions(String aId, String aUser) {}

  @Override
  public boolean removePermissions(String aId) {
    return true;
  }

  @Override
  public void setApache2Ctl(String aApache2ctl) {}

  @Override
  public boolean deleteUser(String username) {
    return mAccounts.remove(username) == null;
  }

  @Override
  public String getProfileId(String username) {
    return mAccounts.get(username);
  }

  @Override
  public void setProfileId(String username, String profileId) {
    mAccounts.put(username, profileId);
  }

  @Override
  public String getUsername(String profileId) {
    return mAccounts.entrySet()
      .stream()
      .filter(entry -> entry.getValue().equals(profileId))
      .map(Map.Entry::getKey)
      .findFirst()
      .orElse(null);
  }
}
