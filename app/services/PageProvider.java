package services;

import org.apache.commons.io.IOUtils;
import org.pegdown.PegDownProcessor;
import play.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by fo on 04.07.17.
 */
public class PageProvider {

  private static PegDownProcessor pegDownProcessor = new PegDownProcessor();

  private String mPath;
  private String mExtension;
  private Map<String, Object> mSections;

  public PageProvider(String aPath, String aExtension, Map<String, Object> aSections) {

    mPath = aPath;
    mExtension = aExtension;
    mSections = aSections;

  }

  public Map<String, String> getPage(String aPage, Locale aLocale) {

    String language = aLocale.getLanguage();
    String country = aLocale.getCountry();
    String pageLocalePath = mPath.concat(aPage).concat("_").concat(language).concat("_")
      .concat(country).concat(mExtension);
    String pageLanguagePath = mPath.concat(aPage).concat("_").concat(language).concat(mExtension);
    String pagePath = mPath.concat(aPage).concat(mExtension);

    Map<String, String> page;
    try {
      page = parse(Thread.currentThread().getContextClassLoader().getResourceAsStream(pageLocalePath));
    } catch (NullPointerException | IOException noLocale) {
      try {
        page = parse(Thread.currentThread().getContextClassLoader().getResourceAsStream(pageLanguagePath));
      } catch (NullPointerException | IOException noLanguage) {
        try {
          page = parse(Thread.currentThread().getContextClassLoader().getResourceAsStream(pagePath));
        } catch (NullPointerException | IOException noPage) {
          page = null;
        }
      }
    }

    return page;

  }

  public Map<String, Map<String, String>> getSections(Locale aLocale) {

    Map<String, Map<String, String>> sections = new HashMap<>();

    for (Map.Entry<String, Object> entry: mSections.entrySet()) {
      Map<String, String> pages = new HashMap<>();
      for (Object path : (List)entry.getValue()) {
        Map<String, String> page = getPage(path.toString(), aLocale);
        if (page != null) {
          pages.put(path.toString(), page.get("title"));
        }
      }
      sections.put(entry.getKey(), pages);
    }

    return sections;

  }

  /**
   * Parse a page into front matter and body
   *
   * Adopted from http://stackoverflow.com/questions/11770077/parsing-yaml-front-matter-in-java
   * @param aInputStream The InputStream to parse
   * @return A map of front matter key values and page content
   * @throws IOException
   */
  private Map<String, String> parse(InputStream aInputStream) throws IOException {

    Map<String, String> page = new HashMap<>();

    BufferedReader br = new BufferedReader(new InputStreamReader(aInputStream));

    // detect YAML front matter
    String line = br.readLine();
    while (line.isEmpty()) line = br.readLine();
    if (!line.matches("[-]{3,}")) { // use at least three dashes
      Logger.warn("No YAML Front Matter");
    }
    final String delimiter = line;

    // scan YAML front matter
    line = br.readLine();
    while (!line.equals(delimiter)) {
      String[] entry = line.split(":");
      page.put(entry[0].trim(), entry[1].trim());
      line = br.readLine();
    }

    page.put("body", pegDownProcessor.markdownToHtml(IOUtils.toString(br)));

    return page;

  }

}
