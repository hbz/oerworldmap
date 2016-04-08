package services;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import models.Commit;
import models.TripleCommit;

/**
 * Created by fo on 05.04.16.
 */
public class IndexQueue extends UntypedActor {

  public static Props props(final ResourceIndexer aResourceIndexer) {

    return Props.create(new Creator<IndexQueue>() {
      @Override
      public IndexQueue create() {
        return new IndexQueue(aResourceIndexer);
      }
    });

  }

  private final ResourceIndexer mResourceIndexer;

  public IndexQueue(ResourceIndexer aResourceIndexer) {
    mResourceIndexer = aResourceIndexer;
  }

  @Override
  public void onReceive(Object aMessage) {

    if (aMessage instanceof Commit) {
      Commit commit = (Commit) aMessage;
      mResourceIndexer.index(commit);
    } else {
      unhandled(aMessage);
    }

  }

}
