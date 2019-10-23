package oerworldmap;


import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.inMemoryDatabase;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import play.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ApplicationTest {

  //@Test
  public void runningLandingPage() {
    running(testServer(3333, fakeApplication(inMemoryDatabase())), new HtmlUnitDriver(),
      browser -> {
        browser.goTo("http://localhost:3333/resource/");
        assertTrue(browser.pageSource().length() > 1);
      });
  }

  @Test
  public void testRestartApache() {
    try {
      Process apache2ctl = Runtime.getRuntime().exec("sudo apache2ctl -k graceful");
      BufferedReader stdInput = new BufferedReader(
        new InputStreamReader(apache2ctl.getInputStream()));
      BufferedReader stdError = new BufferedReader(
        new InputStreamReader(apache2ctl.getErrorStream()));
      System.out.println(IOUtils.toString(stdInput));
      System.out.println(IOUtils.toString(stdError));
    } catch (IOException e) {
      Logger.error("Could not restart Apache", e);
    }
  }

}
