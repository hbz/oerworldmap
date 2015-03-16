package helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;


/**
 * @author fo
 */
public class JSONFormTest {

  private static void pp(String jsons) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    Object json = objectMapper.readValue(jsons, Object.class);
    String indented = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    System.out.println(indented);
  }

  @Test
  public void testFlatObject() {
    Map<String,String[]> formData= new HashMap<>();
    formData.put("foo", new String[]{"bar"});
    formData.put("baz", new String[]{"bam"});
    String expected = "{\"foo\":\"bar\",\"baz\":\"bam\"}";
    String result = JSONForm.parseFormData(formData).toString();
    assertEquals(expected, result);
  }

  @Test
  public void testNestedObject() {
    Map<String,String[]> formData= new HashMap<>();
    formData.put("foo[bar][baz]", new String[]{"bam"});
    formData.put("foo[bar][qux]", new String[]{"quux"});
    formData.put("foo[corge][grault]", new String[]{"fred"});
    formData.put("foo[garply]", new String[]{"waldo"});
    String expected = "{\"foo\":{\"bar\":{\"qux\":\"quux\",\"baz\":\"bam\"},\"corge\":{\"grault\":\"fred\"},\"garply\":\"waldo\"}}";
    String result = JSONForm.parseFormData(formData).toString();
    assertEquals(expected, result);
  }

  @Test
  public void testFlatArrayWithNumericIndex() {
    Map<String,String[]> formData= new HashMap<>();
    formData.put("foo[0]", new String[]{"bar"});
    formData.put("foo[1]", new String[]{"baz"});
    formData.put("foo[2]", new String[]{"bam"});
    String expected = "{\"foo\":[\"bar\",\"baz\",\"bam\"]}";
    String result = JSONForm.parseFormData(formData).toString();
    assertEquals(expected, result);
  }

  @Test
  public void testFlatArrayWithImplicitIndex() {
    Map<String,String[]> formData= new HashMap<>();
    formData.put("foo", new String[]{"bar", "baz", "bam"});
    String expected = "{\"foo\":[\"bar\",\"baz\",\"bam\"]}";
    String result = JSONForm.parseFormData(formData).toString();
    assertEquals(expected, result);
  }

  @Test
  public void testFlatArrayWithoutImplicitIndex() {
    Map<String,String[]> formData= new HashMap<>();
    formData.put("foo[]", new String[]{"bar", "baz", "bam"});
    String expected = "{\"foo\":[\"bar\",\"baz\",\"bam\"]}";
    String result = JSONForm.parseFormData(formData).toString();
    assertEquals(expected, result);
  }

  @Test
  public void testImplicitArrayWithSingleMember() {
    Map<String,String[]> formData= new HashMap<>();
    formData.put("foo[]", new String[]{"bar"});
    String expected = "{\"foo\":[\"bar\"]}";
    String result = JSONForm.parseFormData(formData).toString();
    assertEquals(expected, result);
  }

  @Test
  public void testNestedArrayWithNumericIndex() {
    Map<String,String[]> formData= new HashMap<>();
    formData.put("foo[0][0]", new String[]{"bar"});
    formData.put("foo[0][1]", new String[]{"baz"});
    formData.put("foo[0][2]", new String[]{"bam"});
    String expected = "{\"foo\":[[\"bar\",\"baz\",\"bam\"]]}";
    String result = JSONForm.parseFormData(formData).toString();
    assertEquals(expected, result);
  }

  @Test
  public void testSkippedIndex() {
    Map<String,String[]> formData= new HashMap<>();
    formData.put("foo[0]", new String[]{"bar"});
    formData.put("foo[2]", new String[]{"bam"});
    String expected = "{\"foo\":[\"bar\",null,\"bam\"]}";
    String result = JSONForm.parseFormData(formData).toString();
    assertEquals(expected, result);
  }

  @Test
  public void testNestedArrayWithImplicitIndex() {
    Map<String,String[]> formData= new HashMap<>();
    formData.put("foo[0]", new String[]{"bar", "baz", "bam"});
    String expected = "{\"foo\":[[\"bar\",\"baz\",\"bam\"]]}";
    String result = JSONForm.parseFormData(formData).toString();
    assertEquals(expected, result);
  }

  @Test
  public void testNestedArrayWithoutImplicitIndex() {
    Map<String,String[]> formData= new HashMap<>();
    formData.put("foo[0][]", new String[]{"bar", "baz", "bam"});
    String expected = "{\"foo\":[[\"bar\",\"baz\",\"bam\"]]}";
    String result = JSONForm.parseFormData(formData).toString();
    assertEquals(expected, result);
  }

  @Test
  public void testInvalidPath() {
    Map<String,String[]> formData= new HashMap<>();
    formData.put("foo[][bar]", new String[]{"bar", "baz", "bam"});
    formData.put("foo[bar][baz", new String[]{"bar"});
    formData.put("foo[0", new String[]{"bar"});
    formData.put("foo[", new String[]{"bar"});
    String expected = "{\"foo[bar][baz\":\"bar\",\"foo[\":\"bar\",\"foo[][bar]\":[\"bar\",\"baz\",\"bam\"],\"foo[0\":\"bar\"}";
    String result = JSONForm.parseFormData(formData).toString();
    assertEquals(expected, result);
  }

  @Test
  public void testNumericValues() {
    Map<String,String[]> formData= new HashMap<>();
    formData.put("latitude", new String[]{"52"});
    formData.put("longitude", new String[]{"13.4148863"});
    String expected = "{\"latitude\":52,\"longitude\":13.4148863}";
    String result = JSONForm.parseFormData(formData).toString();
    assertEquals(expected, result);
  }

}
