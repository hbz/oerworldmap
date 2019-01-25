package models;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * Created by fo on 07.04.16.
 */
public interface Commit {

  interface Diff {

    abstract class Line {
      public boolean add;
    }

    void apply(Object model);

    void unapply(Object model);

    void append(Diff diff);

    Diff reverse();

    List<Line> getLines();
  }

  interface Header {

    String getAuthor();

    ZonedDateTime getTimestamp();

  }

  Diff getDiff();

  Header getHeader();

  String getId();
}
