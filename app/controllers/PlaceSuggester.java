package controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.mvc.Result;

/**
 * This class suggests auto completed Places formatted accordingly to
 * schema.org/Place .
 * 
 * @author fo, pvb
 */

public class PlaceSuggester extends OERWorldMap {

  private final static ObjectMapper mMapper = new ObjectMapper();
  private final static JsonNodeFactory mFactory = JsonNodeFactory.instance;
  private final static Map<String, String> mCountriesToIso = new HashMap<>();

  static {
    for (String isoCountry : Locale.getISOCountries()) {
      mCountriesToIso.put(new Locale("", isoCountry).getDisplayCountry(), isoCountry);
    }
  }

  public static Result autocomplete(String aQuery) {
    String url = "https://pelias.mapzen.com/suggest?input=" + aQuery.replace(" ", "%20")
        + "&size=8";
    JsonNode peliasNode = null;
    try {
      peliasNode = mMapper.readTree(readFromUrl(url, 1000));
    } catch (JSONException | IOException e) {
      e.printStackTrace();
    }
    JsonNode schemaNode = peliasToSchema(peliasNode);
    return ok(schemaNode);
  }

  private static String readFromUrl(String aUrl, int aSleepMs) throws IOException, JSONException {
    URLConnection connection = new URL(aUrl).openConnection();
    // TODO ?: connection.setRequestProperty("http.agent", <A_HTTP_AGENT>);
    InputStream is = connection.getInputStream();
    StringBuilder sb = new StringBuilder();
    try {
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(is, Charset.forName("UTF-8")));
      int value;
      while ((value = reader.read()) != -1) {
        sb.append((char) value);
      }
      int sleep = aSleepMs;
      if (sleep < 0)
        sleep = 0;
      Thread.sleep(sleep);
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      is.close();
    }
    return sb.toString();
  }

  private static JsonNode peliasToSchema(JsonNode aPeliasJson) {
    ArrayNode hitsSchema = new ArrayNode(mFactory);
    if (aPeliasJson != null) {
      ArrayNode hitsPelias = (ArrayNode) aPeliasJson.get("features");
      for (int i = 0; i < hitsPelias.size(); i++) {
        hitsSchema.add(peliasToSchemaHit(hitsPelias.get(i)));
      }
    }
    return hitsSchema;
  }

  private static ObjectNode peliasToSchemaHit(JsonNode aPeliasJsonNode) {
    ObjectNode resultSchema = new ObjectNode(mFactory);
    resultSchema.put("@type", "Place");

    ArrayNode coordinatesPelias = (ArrayNode) aPeliasJsonNode.findValue("coordinates");
    resultSchema.put("geo", peliasToSchemaGeo(coordinatesPelias));

    ObjectNode propertiesPelias = (ObjectNode) aPeliasJsonNode.findValue("properties");
    ObjectNode address = peliasToSchemaAddress(propertiesPelias);
    if (address != null) {
      resultSchema.put("address", address);
    }

    return resultSchema;
  }

  private static ObjectNode peliasToSchemaGeo(ArrayNode aCoordinatesPelias) {
    ObjectNode geoSchema = new ObjectNode(mFactory);
    geoSchema.put("@type", "GeoCoordinates");
    String longitude = aCoordinatesPelias.get(0).asText();
    String latitude = aCoordinatesPelias.get(1).asText();
    geoSchema.put("latitude", latitude);
    geoSchema.put("longitude", longitude);
    return geoSchema;
  }

  private static ObjectNode peliasToSchemaAddress(ObjectNode aPropertiesPelias) {
    ObjectNode addressSchema = new ObjectNode(mFactory);
    addressSchema.put("@type", "PostalAddress");

    // Locality (city)
    String locality = getText(aPropertiesPelias, "locality");
    if (locality == null) {
      locality = getText(aPropertiesPelias, "admin2");
    }
    if (locality != null) {
      addressSchema.put("addressLocality", locality);
    }

    ObjectNode address = (ObjectNode) aPropertiesPelias.get("address");
    if (address != null) {

      // Street and number
      StringBuilder streetNumber = new StringBuilder();
      String street = getText(address, "street");
      if (!StringUtils.isEmpty(street)) {
        streetNumber.append(street);
      }
      String number = getText(address, "number");
      if (!StringUtils.isEmpty(number)) {
        if (streetNumber.length() != 0) {
          streetNumber.append("%20");
        }
        streetNumber.append(number);
      }
      if (streetNumber.length() > 0) {
        addressSchema.put("streetAddress", streetNumber.toString());
      }

      // Postal code / zip code
      transferText(address, "zip", addressSchema, "postalCode");

      // Region / administrative level 1
      transferText(aPropertiesPelias, "admin1", addressSchema, "addressRegion");

      // Country / administrative level 0
      JsonNode countryNode = aPropertiesPelias.get("admin0");
      if (countryNode != null) {
        String countryCode = getIso2CountryCode(countryNode.asText());
        if (countryCode != null) {
          addressSchema.put("addressCountry", countryCode);
        }
      }
    }

    if (addressSchema.size() == 0) {
      return null;
    }
    return addressSchema;
  }

  private static void transferText(JsonNode aFromNode, String aFromKey, ObjectNode aToNode,
      String aToKey) {
    if (aFromNode.get(aFromKey) != null) {
      aToNode.put(aToKey, aFromNode.get(aFromKey).asText());
    }
  }

  private static String getText(JsonNode aFromNode, String aFromKey) {
    if (aFromNode.get(aFromKey) != null) {
      return aFromNode.get(aFromKey).asText();
    }
    return null;
  }

  private static String getIso2CountryCode(String aCountry) {
    return mCountriesToIso.get(aCountry);
  }

}
