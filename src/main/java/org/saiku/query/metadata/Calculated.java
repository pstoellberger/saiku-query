package org.saiku.query.metadata;

import java.util.Map;

import org.olap4j.metadata.Property;

public interface Calculated {

	public String getFormula();

	public Map<Property, Object> getPropertyValueMap();

	public String getUniqueName();

	public int getSolveOrder();

}