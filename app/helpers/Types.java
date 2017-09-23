package helpers;

import com.typesafe.config.Config;
import models.Action;
import models.Record;

import java.util.HashMap;
import java.util.Map;

/**
 * @author pvb
 */
public class Types {

  static private Map<Class, Type> CLASS_MAP;

  static private Map<String, Type> TYPE_MAP;

  private Types(){ /*no instantiation */ }

  public static void init(final Config aConfig){
    CLASS_MAP = new HashMap<>();
    TYPE_MAP = new HashMap<>();

    Type recordType = new Type(Record.class, Record.TYPE,
      aConfig.getString("es.index.webpage.type"),
      aConfig.getString("es.index.webpage.name"));
    CLASS_MAP.put(Record.class, recordType);
    TYPE_MAP.put(Record.TYPE, recordType);

    Type actionType = new Type(Action.class, Action.TYPE,
      aConfig.getString("es.index.action.type"),
      aConfig.getString("es.index.action.name"));
    CLASS_MAP.put(Action.class, actionType);
    TYPE_MAP.put(Action.TYPE, actionType);
  }

  static public String getEsIndexFromClass(final Class aClass){
    return CLASS_MAP.get(aClass).getEsIndex();
  }

  static public String getEsTypeFromClass(final Class aClass){
    return CLASS_MAP.get(aClass).getEsType();
  }

  static public String getEsIndexFromClassType(final String aClassType){
    return TYPE_MAP.get(aClassType).getEsIndex();
  }

  public static class Type {

    final private Class mClass;
    final private String mClassType;
    final private String mEsType;
    final private String mEsIndex;

    public Type(final Class aClass, final String aClassType, final String aEsType, final String aEsIndex){
      mClass = aClass;
      mClassType = aClassType;
      mEsType = aEsType;
      mEsIndex = aEsIndex;
    }

    public Class getClazz(){
      return mClass;
    }

    public String getClassType() {
      return mClassType;
    }

    public String getEsType() {
      return mEsType;
    }

    public String getEsIndex() {
      return mEsIndex;
    }
  }

}
