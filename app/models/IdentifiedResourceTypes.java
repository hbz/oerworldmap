package models;

public enum IdentifiedResourceTypes {
	
	ORGANIZATION("Organization"), EVENT("Event"), PERSON("Person");
	private final String mType;
	
	private IdentifiedResourceTypes(final String aResourceType){
		mType = aResourceType;
	}
	
	public String toString(){
		return mType;
	}
}
