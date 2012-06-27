package org.saiku.query;

import org.olap4j.metadata.Level;

public class QueryLevel extends AbstractQueryObject {
    private final QueryHierarchy hierarchy;
	private final Level level;

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

}








