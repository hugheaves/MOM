/*
 * $Log: ParamMetaData.java,v $
 * Revision 1.1  2008/05/08 18:50:08  hugh
 * Updated.
 *
 * Revision 1.2  2008-02-25 20:47:26  hugh
 * Made enum Type publicly visible
 *
 * Revision 1.1  2008-02-19 19:06:33  hugh
 * Initial revision.
 *
 */

package edu.vcu.sysbio;

/**
 * @author Hugh
 * @since Feb 19, 2008
 * @version $Revision: 1.1 $
 *
 */
public class ParamMetaData {

	/* The parameter name (i.e. "k", "j", etc.) used internally by the search method */
	private String name;

	/* Human readable parameter descriptions. (i.e. maximum match length, etc.) */
	private String description;

	/* A default value, or "null" of there isn't one */
	private String defaultValue;

	/* the "Type" of this parameter - is it an integer or string value */
	public enum Type {
		INTEGER, INPUT_FILE, OUTPUT_FILE
	};

	private Type type;

	/**
	 * Construct a ParamMetaData object
	 * @param name
	 * @param description
	 * @param defaultValue
	 * @param type
	 */
	public ParamMetaData(String name, String description, String defaultValue,
			Type type) {
		super();
		this.name = name;
		this.description = description;
		this.defaultValue = defaultValue;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

}
