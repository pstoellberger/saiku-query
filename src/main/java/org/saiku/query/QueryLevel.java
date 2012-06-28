package org.saiku.query;

import java.util.ArrayList;
import java.util.List;

import org.olap4j.impl.Named;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;

public class QueryLevel extends AbstractQueryObject implements Named {
    private final QueryHierarchy hierarchy;
	private final Level level;
	
	private List<Member> inclusions = new ArrayList<Member>();
	private List<Member> exclusions = new ArrayList<Member>();

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
    
    protected void include(Member m) {
    	if(!inclusions.contains(m)) {
    		inclusions.add(m);
    	}
    }
    
    protected void exclude(Member m) {
    	if(inclusions.contains(m)) {
    		inclusions.add(m);
    	}
    	if(!exclusions.contains(m)) {
    		exclusions.add(m);
    	}
    }
}








