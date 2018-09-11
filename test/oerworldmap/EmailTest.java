package oerworldmap;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import javax.inject.Inject;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;
import org.junit.Test;
import play.Logger;

public class EmailTest {

  @Inject
  private Config mConf = ConfigFactory.parseFile(new File("conf/test.conf")).resolve();

  @Test
  public void testSendEmailWithoutAttachment() {
    sendMail(mConf.getString("mail.smtp.to"), "Lorem ipsum", "Test Subject", null);
  }

  @Test
  public void testSendEmailWithAttachment() {
    File termsOfService = new File("public/pdf/Terms_of_Service.pdf");
    sendMail(mConf.getString("mail.smtp.to"), "Lorem ipsum", "Test Subject", new File[]{termsOfService});
  }

  private void sendMail(String aEmailAddress, String aMessage, String aSubject,
    File[] attachments) {
    try {
      Email mail;
      if (attachments != null && attachments.length > 0) {
        mail = new MultiPartEmail();
        for (File attachment : attachments) {
          ((MultiPartEmail) mail).attach(attachment);
        }
      } else {
        mail = new SimpleEmail();
      }
      mail.setMsg(aMessage);
      mail.setHostName(mConf.getString("mail.smtp.host"));
      mail.setSmtpPort(mConf.getInt("mail.smtp.port"));
      String smtpUser = mConf.getString("mail.smtp.user");
      String smtpPass = mConf.getString("mail.smtp.password");
      if (!smtpUser.isEmpty()) {
        mail.setAuthenticator(new DefaultAuthenticator(smtpUser, smtpPass));
      }
      mail.setSSLOnConnect(mConf.getBoolean("mail.smtp.ssl"));
      mail.setStartTLSEnabled(mConf.getBoolean("mail.smtp.tls"));
      mail.setFrom(mConf.getString("mail.smtp.from"),
        mConf.getString("mail.smtp.sender"));
      mail.setSubject(aSubject);
      mail.addTo(aEmailAddress);
      mail.send();
      Logger.info("Sent\n" + aMessage + "\nto " + aEmailAddress);
    } catch (EmailException e) {
      Logger.error("Failed to send\n" + aMessage + "\nto " + aEmailAddress, e);
    }
  }

}
