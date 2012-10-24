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

public class QueryHierarchy extends AbstractQueryObject implements Named {

	protected QueryAxis axis;
    private final Query query;
	private final Hierarchy hierarchy;
	
	private NamedList<QueryLevel> queryLevels = new NamedListImpl<QueryLevel>();
	
	private NamedList<QueryLevel> activeLevels = new NamedListImpl<QueryLevel>();
	
	private NamedList<CalculatedMember> calculatedMembers = new NamedListImpl<CalculatedMember>();

	private NamedList<CalculatedMember> activeCalculatedMembers = new NamedListImpl<CalculatedMember>();

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
    
//    /**
//     * Returns the Olap4j's QueryLevel object according to the name
//     * given as a parameter. If no Level of the given name is found,
//     * a null value will be returned.
//     * @param name The name of the Level you want the object for.
//     * @return The QueryLevel object, null if no Level of that
//     * name can be found.
//     */
//    public QueryLevel getLevel(String name) {
//        return queryLevels.get(name);
//    }
//
//    /**
//     * Returns the Olap4j's QueryLevel object according to the Level
//     * given as a parameter. If no QueryLevel is found,
//     * a null value will be returned.
//     * @param hierarchy The Level of the QueryLevel you want the object for.
//     * @return The QueryLevel object, null if no hierarchy of that
//     * name can be found.
//     */
//    public QueryLevel getLevel(Level level) {
//    	if (level == null) {
//    		return null;
//    	}
//    	return queryLevels.get(level.getName());
//    }
    
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
    
    
    public void includeLevel(String levelName) {
    	QueryLevel ql = queryLevels.get(levelName);
    	if (!activeLevels.contains(ql)) {
    		activeLevels.add(ql);
    	}
    }

    public void includeLevel(Level l) throws OlapException {
    	if (!l.getHierarchy().equals(hierarchy)) {
    		throw new OlapException(
    				"You cannot include level " + l.getUniqueName() 
    				+ " on hierarchy " + hierarchy.getUniqueName());
    	}
    	QueryLevel ql = queryLevels.get(l.getName());
    	if (!activeLevels.contains(l)) {
    		activeLevels.add(ql);
    	}
    }
    
    public void includeMembers(List<Member> members) throws OlapException {
    	for (Member m : members) {
    		includeMember(m);
    	}
    }

    public void include(String uniqueMemberName) throws OlapException {
    	List<IdentifierSegment> nameParts = IdentifierParser.parseIdentifier(uniqueMemberName);
    	this.include(nameParts);
    }
    
    public void include(List<IdentifierSegment> nameParts) throws OlapException {
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
    
    public void exclude(String uniqueMemberName) throws OlapException {
    	List<IdentifierSegment> nameParts = IdentifierParser.parseIdentifier(uniqueMemberName);
    	this.exclude(nameParts);
    }
    
    public void exclude(List<IdentifierSegment> nameParts) throws OlapException {
        Member member = this.query.getCube().lookupMember(nameParts);
        if (member == null) {
            throw new OlapException(
                "Unable to find a member with name " + nameParts);
        }
        this.exclude(member);
    }
    
    public void excludeMembers(List<Member> members) {
    	for (Member m : members) {
    		exclude(m);
    	}
    }

    public void exclude(Member m) {
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








