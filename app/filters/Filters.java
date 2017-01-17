package filters;

import play.filters.cors.CORSFilter;
import play.mvc.EssentialFilter;
import play.filters.gzip.GzipFilter;
import play.http.HttpFilters;

import javax.inject.Inject;

/**
 * Created by fo on 27.09.16.
 */
public class Filters  implements HttpFilters {

  private EssentialFilter[] filters;

  @Inject
  public Filters(GzipFilter gzipFilter, CORSFilter corsFilter) {
    filters = new EssentialFilter[] { gzipFilter.asJava(), corsFilter.asJava() };
  }

  public EssentialFilter[] filters() {
    return filters;
  }

}
