package models;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

/**
 * @author fo
 */
public class ResourceList extends ModelCommonList{

  public ResourceList(@Nonnull List<ModelCommon> aResourceList, long aTotalItems, String aQuery, int aFrom,
                      int aSize, String aSort, Map<String, List<String>> aFilters, Resource aAggregations) {
    super(aResourceList, aTotalItems, aQuery, aFrom, aSize, aSort, aFilters, aAggregations);
  }

  public ResourceList(Resource aPagedCollection) {
    super(aPagedCollection);
  }


}
