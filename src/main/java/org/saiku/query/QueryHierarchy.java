package org.saiku.query;

import java.util.HashMap;
import java.util.Map;

import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Level;

public class QueryHierarchy extends AbstractQueryObject {
    protected QueryAxis axis;
    private final Query query;
	private final Hierarchy hierarchy;
	private Map<String, QueryLevel> levelMap = new HashMap<String, QueryLevel>();

    public QueryHierarchy(Query query, Hierarchy hierarchy) {
        super();
        this.query = query;
        this.hierarchy = hierarchy;
        for (Level level : hierarchy.getLevels()) {
            QueryLevel queryLevel = new QueryLevel(this, level);
            levelMap.put(queryLevel.getName(), queryLevel);
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
    
    /**
     * Returns the Olap4j's QueryLevel object according to the name
     * given as a parameter. If no Level of the given name is found,
     * a null value will be returned.
     * @param name The name of the Level you want the object for.
     * @return The QueryLevel object, null if no Level of that
     * name can be found.
     */
    public QueryLevel getLevel(String name) {
        return levelMap.get(name);
    }

    /**
     * Returns the Olap4j's QueryLevel object according to the Level
     * given as a parameter. If no QueryLevel is found,
     * a null value will be returned.
     * @param hierarchy The Level of the QueryLevel you want the object for.
     * @return The QueryLevel object, null if no hierarchy of that
     * name can be found.
     */
    public QueryLevel getLevel(Level level) {
    	if (level == null) {
    		return null;
    	}
    	return levelMap.get(level.getName());
    }

}








