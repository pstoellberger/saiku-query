package org.saiku.query.metadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olap4j.OlapException;
import org.olap4j.impl.Named;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.metadata.Dimension;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Measure.Aggregator;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.NamedList;
import org.olap4j.metadata.Property;

public class CalculatedMember implements Member, Named {


	private Dimension dimension;
	private Hierarchy hierarchy;
	private String name;
	private String caption;
	private String uniqueName;
	private Type memberType;
	private String formula;
	
	private Map<Property, Object> properties = new HashMap<Property, Object>();
	private String description;
	private Level level;


	public CalculatedMember(
			Dimension dimension,
			Hierarchy hierarchy,
			String name,
			String caption,
			String description,
			String uniqueName,
			Type memberType,
			String formula,
			Map<Property, String> properties)
	{
		this.dimension = dimension;
		this.hierarchy = hierarchy;
		this.level = hierarchy.getLevels().get(0);
		this.name = name;
		this.caption = caption;
		this.description = description;
		this.uniqueName = uniqueName;
		this.memberType = memberType;
		this.formula = formula;
		if (properties != null) {
			this.properties.putAll(properties);
		}
	}
	


	public Dimension getDimension() {
		return dimension;
	}


	public Hierarchy getHierarchy() {
		return hierarchy;
	}
	
	public String getFormula() {
		return formula;
	}

	public Type getMemberType() {
		return memberType;
	}


    public Map<Property, Object> getPropertyValueMap() {
        return properties;
    }
    
	@Override
	public NamedList<Property> getProperties() {
		return level.getProperties();
	}


	public Object getPropertyValue(Property key) throws OlapException {
		if (properties.containsKey(key)) {
			return properties.get(key);
		}
		return null;
	}

	@Override
	public void setProperty(Property key, Object value) throws OlapException {
		properties.put(key, value);
	}


	public String getCaption() {
		return caption;
	}


	public String getDescription() {
		return description;
	}


	public String getName() {
		return name;
	}


	public String getUniqueName() {
		return uniqueName;
	}


	public Aggregator getAggregator() {
		return Aggregator.CALCULATED;
	}


	public boolean isVisible() {
		return true;
	}



	@Override
	public List<Member> getAncestorMembers() {
		throw new UnsupportedOperationException();
	}



	@Override
	public int getChildMemberCount() throws OlapException {
		return 0;
	}



	@Override
	public NamedList<? extends Member> getChildMembers() throws OlapException {
		throw new UnsupportedOperationException();
	}



	@Override
	public Member getDataMember() {
		throw new UnsupportedOperationException();
	}



	@Override
	public int getDepth() {
		return 0;
	}



	@Override
	public ParseTreeNode getExpression() {
		throw new UnsupportedOperationException();
	}



	@Override
	public Level getLevel() {
		return level;
	}



	@Override
	public int getOrdinal() {
		throw new UnsupportedOperationException();
	}



	@Override
	public Member getParentMember() {
		throw new UnsupportedOperationException();
	}



	@Override
	public String getPropertyFormattedValue(Property property) throws OlapException {
		return String.valueOf(getPropertyValue(property));
	}



	@Override
	public int getSolveOrder() {
		throw new UnsupportedOperationException();
	}



	@Override
	public boolean isAll() {
		return false;
	}



	@Override
	public boolean isCalculated() {
		return true;
	}



	@Override
	public boolean isCalculatedInQuery() {
		return true;
	}



	@Override
	public boolean isChildOrEqualTo(Member arg0) {
		return false;
	}



	@Override
	public boolean isHidden() {
		return false;
	}


}
