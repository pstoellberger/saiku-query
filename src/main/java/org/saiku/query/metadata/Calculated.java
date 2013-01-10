package org.saiku.query.metadata;

import java.util.Map;

public interface Calculated {

	public String getFormula();

	public Map<String, String> getFormatProperties();

	public String getUniqueName();

	public int getSolveOrder();

}