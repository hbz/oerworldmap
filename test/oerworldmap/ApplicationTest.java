package oerworldmap;


import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.inMemoryDatabase;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import org.openqa.selenium.htmlunit.HtmlUnitDriver;

public class ApplicationTest {

  //@Test
  public void runningLandingPage() {
    running(testServer(3333, fakeApplication(inMemoryDatabase())), new HtmlUnitDriver(),
      browser -> {
        browser.goTo("http://localhost:3333/resource/");
        assertTrue(browser.pageSource().length() > 1);
      });
  }
}
