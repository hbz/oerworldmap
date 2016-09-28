package filters;

import play.api.mvc.EssentialFilter;
import play.filters.gzip.GzipFilter;
import play.http.HttpFilters;

import javax.inject.Inject;

/**
 * Created by fo on 27.09.16.
 */
public class Filters  implements HttpFilters {

  @Inject
  GzipFilter gzipFilter;

  public EssentialFilter[] filters() {
    return new EssentialFilter[] { gzipFilter };
  }

}
