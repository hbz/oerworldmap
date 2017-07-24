package models;

import java.util.HashMap;

/**
 * @author pvb
 */

// TODO: un-dummy

public class Action extends HashMap<String, Object> implements Comparable<Resource> {

  @Override
  public int compareTo(Resource o) {
    return 0;
  }
}
