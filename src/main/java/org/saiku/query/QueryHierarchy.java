package org.saiku.query;

import java.util.List;

import org.olap4j.OlapException;
import org.olap4j.impl.IdentifierParser;
import org.olap4j.impl.Named;
import org.olap4j.impl.NamedListImpl;
import org.olap4j.mdx.IdentifierSegment;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.NamedList;
import org.saiku.query.metadata.CalculatedMember;

public class QueryHierarchy extends AbstractSortableQuerySet implements Named {

	protected QueryAxis axis;
    private final Query query;
	private final Hierarchy hierarchy;
	
	private NamedList<QueryLevel> queryLevels = new NamedListImpl<QueryLevel>();
	
	private NamedList<QueryLevel> activeLevels = new NamedListImpl<QueryLevel>();
	
	private NamedList<CalculatedMember> calculatedMembers = new NamedListImpl<CalculatedMember>();

	private NamedList<CalculatedMember> activeCalculatedMembers = new NamedListImpl<CalculatedMember>();
	
	private boolean consistent = true;
	
	private boolean visualTotals = false;
	private String visualTotalsPattern;

	public QueryHierarchy(Query query, Hierarchy hierarchy) {
        super();
        this.query = query;
        this.hierarchy = hierarchy;
        for (Level level : hierarchy.getLevels()) {
            QueryLevel queryLevel = new QueryLevel(this, level);
            queryLevels.add(queryLevel);
        }
    }

    public Query getQuery() {
        return query;
    }

    public QueryAxis getAxis() {
        return axis;
    }
    
    /**
     * Only internal use!
     * @param axis
     */
    protected void setAxis(QueryAxis axis) {
        this.axis = axis;
    }

    public String getName() {
        return hierarchy.getName();
    }
    
    public String getUniqueName() {
    	return hierarchy.getUniqueName();
    }
    
    public String getCaption() {
    	return hierarchy.getCaption();
    }
    
    public boolean isConsistent() {
    	return consistent;
    }
    
    public void setConsistent(boolean consistent) {
    	this.consistent = consistent;
    }
    
    /**
     * Should the hierarchy return visual totals
	 * @return is visualTotals
	 */
	public boolean isVisualTotals() {
		return (visualTotals | query.isVisualTotals());
	}

	/**
	 * @param visualTotals should the hierarchy use visual totals
	 */
	public void setVisualTotals(boolean visualTotals) {
		this.visualTotals = visualTotals;
		if(!visualTotals) {
			this.visualTotalsPattern = null;
		}
	}
	
	public void setVisualTotalsPattern(String pattern) {
		this.visualTotalsPattern = pattern;
		this.visualTotals = true;
	}
	
	public String getVisualTotalsPattern() {
		return (visualTotalsPattern == null ? query.getVisualTotalsPattern() : visualTotalsPattern);
	}
	
	public boolean needsHierarchize() {
		return ((visualTotals | activeLevels.size() > 1) 
				&& getHierarchizeMode() == null);
	}

    /**
     * Returns the underlying Hierarchy object onto which
     * this query Hierarchy is based.
     * <p>Returns a mutable object so operations on it have
     * unpredictable consequences.
     * @return The underlying Hierarchy representation.
     */
    public Hierarchy getHierarchy() {
        return hierarchy;
    }
    
    public void addCalculatedMember(CalculatedMember cm) {
    	calculatedMembers.add(cm);
    }
    
    public NamedList<CalculatedMember> getCalculatedMembers() {
    	return calculatedMembers;
    }
    
    public List<CalculatedMember> getActiveCalculatedMembers() {
    	return activeCalculatedMembers;
    }
    
    
    public List<QueryLevel> getActiveQueryLevels() {
    	return activeLevels;
    }
    

    public QueryLevel getActiveLevel(String levelName) {
    	return activeLevels.get(levelName);

    }
    
    public QueryLevel includeLevel(String levelName) {
    	QueryLevel ql = queryLevels.get(levelName);
    	if (!activeLevels.contains(ql)) {
    		activeLevels.add(ql);
    	}
    	return ql;
    }

    public QueryLevel includeLevel(Level l) throws OlapException {
    	if (!l.getHierarchy().equals(hierarchy)) {
    		throw new OlapException(
    				"You cannot include level " + l.getUniqueName() 
    				+ " on hierarchy " + hierarchy.getUniqueName());
    	}
    	QueryLevel ql = queryLevels.get(l.getName());
    	if (!activeLevels.contains(l)) {
    		activeLevels.add(ql);
    	}
    	return ql;
    }
    
    public void excludeLevel(String levelName) {
    	QueryLevel ql = queryLevels.get(levelName);
    	if (activeLevels.contains(ql)) {
    		activeLevels.remove(ql);
    	}
    }

    public void excludeLevel(Level l) throws OlapException {
    	QueryLevel ql = queryLevels.get(l.getName());
    	if (!activeLevels.contains(l)) {
    		activeLevels.remove(ql);
    	}
    }

    
    public void includeMembers(List<Member> members) throws OlapException {
    	for (Member m : members) {
    		includeMember(m);
    	}
    }

    public void includeMember(String uniqueMemberName) throws OlapException {
    	List<IdentifierSegment> nameParts = IdentifierParser.parseIdentifier(uniqueMemberName);
    	this.includeMember(nameParts);
    }
    
    public void includeMember(List<IdentifierSegment> nameParts) throws OlapException {
        Member member = this.query.getCube().lookupMember(nameParts);
        if (member == null) {
            throw new OlapException(
                "Unable to find a member with name " + nameParts);
        }
        this.includeMember(member);
    }


    public void includeCalculatedMember(CalculatedMember m) throws OlapException {
    	Hierarchy h = m.getHierarchy();
    	if (!h.equals(hierarchy)) {
    		throw new OlapException(
    				"You cannot include the calculated member " + m.getUniqueName() 
    				+ " on hierarchy " + hierarchy.getUniqueName());
    	}
    	if(!calculatedMembers.contains(m)) {
    		calculatedMembers.add(m);
    	}
    	activeCalculatedMembers.add(m);
    }
    
    public void excludeCalculatedMember(CalculatedMember m) throws OlapException {
    	calculatedMembers.remove(m);
    	activeCalculatedMembers.remove(m);
    }
    
    public void includeMember(Member m) throws OlapException {
    	Level l = m.getLevel();
    	if (!l.getHierarchy().equals(hierarchy)) {
    		throw new OlapException(
    				"You cannot include member " + m.getUniqueName() 
    				+ " on hierarchy " + hierarchy.getUniqueName());
    	}
    	QueryLevel ql = queryLevels.get(l.getName());
    	if (!activeLevels.contains(ql)) {
    		activeLevels.add(ql);
    	}
    	ql.include(m);
    }
    
    public void excludeMember(String uniqueMemberName) throws OlapException {
    	List<IdentifierSegment> nameParts = IdentifierParser.parseIdentifier(uniqueMemberName);
    	this.excludeMember(nameParts);
    }
    
    public void excludeMember(List<IdentifierSegment> nameParts) throws OlapException {
        Member member = this.query.getCube().lookupMember(nameParts);
        if (member == null) {
            throw new OlapException(
                "Unable to find a member with name " + nameParts);
        }
        this.excludeMember(member);
    }
    
    public void excludeMembers(List<Member> members) {
    	for (Member m : members) {
    		excludeMember(m);
    	}
    }

    public void excludeMember(Member m) {
    	Level l = m.getLevel();
    	if (!l.getHierarchy().equals(hierarchy)) {
    		throw new IllegalArgumentException("You cannot exclude member " + m.getUniqueName() + " on hierarchy " + hierarchy.getUniqueName());
    	}
    	QueryLevel ql = queryLevels.get(l.getName());
    	if (!activeLevels.contains(ql)) {
    		activeLevels.add(ql);
    	}
    	ql.exclude(m);
    }
    
    /* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((hierarchy == null) ? 0 : hierarchy.getUniqueName().hashCode());
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
		QueryHierarchy other = (QueryHierarchy) obj;
		if (hierarchy == null) {
			if (other.hierarchy != null)
				return false;
		} else if (!hierarchy.getUniqueName().equals(other.hierarchy.getUniqueName()))
			return false;
		return true;
	}
	
	
	@Override
	public String toString() {
		return hierarchy.getUniqueName();
	}

}








