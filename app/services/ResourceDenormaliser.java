package services;

import java.util.List;

import models.Resource;

public class ResourceDenormaliser {

  // TODO: add configurable member variables / parameters that specify in what
  // manner exactly Resources should be denormalised ??

  public static List<Resource> denormalise(Resource aResource) {
    // 1. split this into a list of single Resources (assert that any references
    // are saved)
    List<Resource> result = splitResources();
    // 2. ensure references are bi-directional (or make them so if not)
    result.forEach(r -> addMissingBacklinks());
    // 3. ensure that data granulation level is appropriate.
    // or do so if otherwise! that should be necessary for reverse referencing
    // data entries
    result.forEach(r -> checkReferencedData());
    return null;
  }

  private static List<Resource> splitResources() {
    // simply split a Resource
    return null;
  }

  // TODO: necessary?
  private static void addMissingBacklinks() {
    // TODO: iterate over all mentioned Resources and add reverse references to
    // them if necessary
  }

  private static void checkReferencedData() {

  }

}
