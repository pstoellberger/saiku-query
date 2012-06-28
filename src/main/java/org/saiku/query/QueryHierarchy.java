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

public class QueryHierarchy extends AbstractQueryObject implements Named {
    protected QueryAxis axis;
    private final Query query;
	private final Hierarchy hierarchy;
	
	private NamedList<QueryLevel> queryLevels = new NamedListImpl<QueryLevel>();
	
	private NamedList<QueryLevel> activeLevels = new NamedListImpl<QueryLevel>();

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
    
    public void setAxis(QueryAxis axis) {
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
    
    public List<QueryLevel> getActiveQueryLevels() {
    	return activeLevels;
    }
    
    public void includeLevel(Level l) throws OlapException {
    	if (!l.getHierarchy().equals(hierarchy)) {
    		throw new OlapException(
    				"You cannot include level " + l.getUniqueName() 
    				+ " on hierarchy " + hierarchy.getUniqueName());
    	}
    	QueryLevel ql = queryLevels.get(l.getName());
    	if (!activeLevels.contains(l.getName())) {
    		activeLevels.add(ql);
    	}
    }
    
    public void includeMembers(List<Member> members) throws OlapException {
    	for (Member m : members) {
    		include(m);
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
        this.include(member);
    }


    public void include(Member m) throws OlapException {
    	Level l = m.getLevel();
    	if (!l.getHierarchy().equals(hierarchy)) {
    		throw new OlapException(
    				"You cannot include member " + m.getUniqueName() 
    				+ " on hierarchy " + hierarchy.getUniqueName());
    	}
    	QueryLevel ql = queryLevels.get(l.getName());
    	if (!activeLevels.contains(l.getName())) {
    		activeLevels.add(ql);
    	}
    	ql.include(m);
    }
    
    public void exclude(List<Member> members) {
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
    	if (!activeLevels.contains(l.getName())) {
    		activeLevels.add(ql);
    	}
    	ql.exclude(m);
    }
}








