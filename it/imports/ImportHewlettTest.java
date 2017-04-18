package imports;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class ImportHewlettTest {

  // Tests, whether data is available at all
  @Test
  public void testGetData() throws IOException, InterruptedException {
    final String importsFileName = String.format("import/hewlett/test_%s.json", System.currentTimeMillis());

    final Process p = Runtime.getRuntime().exec(String.format( //
      "python " +
        "import/hewlett/import.py  " +
        "'http://www.hewlett.org/grants/search?order=field_date_of_award&sort=desc&keywords=OER&year=&term_node_tid_depth_1=All&program_id=148' " +
        "'%s'", importsFileName));
    while (p.isAlive()){
      Thread.sleep(100);
    }

    File importsFile = new File(importsFileName);
    Assert.assertTrue(importsFile.exists());
    Assert.assertTrue(importsFile.length() > 100000);
  }
}
