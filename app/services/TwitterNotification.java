package services;

import play.Logger;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Created by fo on 30.09.16.
 */
public class TwitterNotification implements Notification {

  private Twitter mTwitter;

  public TwitterNotification(String aConsumerKey, String aConsumerSecret, String aAccessToken,
                             String aAccessTokenSecret) {

    ConfigurationBuilder cb = new ConfigurationBuilder();
    cb.setDebugEnabled(true)
      .setOAuthConsumerKey(aConsumerKey)
      .setOAuthConsumerSecret(aConsumerSecret)
      .setOAuthAccessToken(aAccessToken)
      .setOAuthAccessTokenSecret(aAccessTokenSecret);

    mTwitter = new TwitterFactory(cb.build()).getInstance();

  }

  @Override
  public void send(String aStatus) {

    twitter4j.Status status;

    try {
      status = mTwitter.updateStatus(aStatus);
      Logger.info("Successfully updated Twitter status to [" + status.getText() + "].");
    } catch (TwitterException ex) {
      Logger.error("Failed to post update to Twitter", ex);
    }

  }

}
