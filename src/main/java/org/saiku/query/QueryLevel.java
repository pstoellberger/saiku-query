package org.saiku.query;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.olap4j.impl.Named;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;
import org.saiku.query.Parameter.SelectionType;

public class QueryLevel extends AbstractQuerySet implements Named {
    private final QueryHierarchy hierarchy;
	private final Level level;
	
	private List<Member> inclusions = new ArrayList<Member>();
	private List<Member> exclusions = new ArrayList<Member>();
	private Member rangeStart = null;
	private Member rangeEnd = null;
	private String parameterName = null;
	private SelectionType parameterSelectionType = Parameter.SelectionType.INCLUSION;

    public QueryLevel(QueryHierarchy hierarchy, Level level) {
        super();
        this.hierarchy = hierarchy;
        this.level = level;
    }

    public QueryHierarchy getQueryHierarchy() {
        return hierarchy;
    }

    public String getName() {
        return level.getName();
    }
    
    public String getUniqueName() {
    	return level.getUniqueName();
    }
    
    public String getCaption() {
    	return level.getCaption();
    }

    
    @Override
    public boolean isSimple() {
    	return (super.isSimple() && inclusions.isEmpty() && exclusions.isEmpty() && rangeStart == null && rangeEnd == null);
    }
    
    public boolean isRange() {
    	return (rangeStart != null && rangeEnd != null);
    }

    /**
     * Returns the underlying Level object onto which
     * this query Level is based.
     * <p>Returns a mutable object so operations on it have
     * unpredictable consequences.
     * @return The underlying  representation.
     */
    public Level getLevel() {
        return level;
    }

    public List<Member> getInclusions() {
    	return inclusions;
    }
    
    public List<Member> getExclusions() {
    	return exclusions;
    }
    
	public Member getRangeStart() {
		return rangeStart;
	}
	
	public Member getRangeEnd() {
		return rangeEnd;
	}

    protected void include(Member m) {
    	if(!inclusions.contains(m)) {
    		inclusions.add(m);
    	}
    }
    
    protected void exclude(Member m) {
    	if(inclusions.contains(m)) {
    		inclusions.remove(m);
    	}
    	if(!exclusions.contains(m)) {
    		exclusions.add(m);
    	}
    }
    
    protected void setRange(Member start, Member end) {
    	rangeStart = start;
    	rangeEnd = end;
    }
    

    /* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((level == null) ? 0 : level.getUniqueName().hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueryLevel other = (QueryLevel) obj;
		if (level == null) {
			if (other.level != null)
				return false;
		} else if (!level.getUniqueName().equals(other.getLevel().getUniqueName()))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return level.getUniqueName();
	}

	public void setParameterName(String parameter) {
		this.parameterName  = parameter;
		
	}

	public void setParameterSelectionType(SelectionType selectionType) {
		this.parameterSelectionType = selectionType;
		
	}

	/**
	 * @return the parameterName
	 */
	public String getParameterName() {
		return parameterName;
	}

	/**
	 * @return the parameterSelectionType
	 */
	public SelectionType getParameterSelectionType() {
		return parameterSelectionType;
	}
	
	public boolean hasParameter() {
		return (StringUtils.isNotBlank(parameterName));
	}
}








