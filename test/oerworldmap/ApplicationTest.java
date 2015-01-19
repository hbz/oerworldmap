package oerworldmap;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.GET;
import static play.test.Helpers.contentType;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.running;

import org.junit.Test;

import play.mvc.Result;

public class ApplicationTest {
  @Test
  public void runningLandingPage() {
    running(fakeApplication(), () -> {
      Result result = route(fakeRequest(GET, "/user"));
      assertThat(result).isNotNull();
      assertThat(contentType(result)).isEqualTo("text/html");
    });
  }
}
