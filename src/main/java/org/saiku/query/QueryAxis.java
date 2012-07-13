package org.saiku.query;

import java.util.List;

import org.olap4j.Axis;
import org.olap4j.impl.NamedListImpl;
import org.olap4j.metadata.NamedList;

public class QueryAxis extends AbstractQueryObject {

    protected final NamedList<QueryHierarchy> hierarchies = new NamedListImpl<QueryHierarchy>();
    
    private final Query query;
    protected Axis location = null;
    private boolean nonEmpty;
    
    public QueryAxis(Query query, Axis location) {
        super();
        this.query = query;
        this.location = location;
    }

    public Axis getLocation() {
        return location;
    }

    /**
     * Returns whether this Query Group filters out empty rows.
     * If true, axis filters out empty rows, and the MDX to evaluate the axis
     * will be generated with the "NON EMPTY" expression.
     * Other Query Elements will use Filter( Not IsEmpty (<query group set>), <measure>)
     *
     * @return Whether this query group should filter out empty rows
     *
     * @see #setNonEmpty(boolean)
     */
    public boolean isNonEmpty() {
    	return nonEmpty;
    }

    /**
     * Sets whether this Query Group filters out empty rows.
     *
     * @param nonEmpty Whether this axis should filter out empty rows
     *
     * @see #isNonEmpty()
     */
    public void setNonEmpty(boolean nonEmpty) {
    	this.nonEmpty = nonEmpty;
    }

    public String getName() {
        return location.getCaption(null);
    }
    
    /**
     * Returns the Query object belonging to this QueryAxis
     * 
     * @return the query object
     */
    public Query getQuery() {
    	return query;
    }

	public List<QueryHierarchy> getQueryHierarchies() {
		return hierarchies;
	}
	
    /**
     * Places a {@link QueryHierarchy} object on this axis.
     * @param hierarchy The {@link QueryHierarchy} object to add
     * to this axis.
     */
    public void addHierarchy(QueryHierarchy hierarchy) {
    	hierarchy.setAxis(this);
        this.getQueryHierarchies().add(hierarchy);
    }

    /**
     * Places a {@link QueryHierarchy} object on this axis at
     * a specific index.
     * @param hierarchy The {@link QueryHierarchy} object to add
     * to this axis.
     * @param index The position (0 based) onto which to place
     * the QueryHierarchy
     */
    public void addHierarchy(int index, QueryHierarchy hierarchy) {
    	hierarchy.setAxis(this);
        this.getQueryHierarchies().add(index, hierarchy);
    }

    /**
     * Removes a {@link QueryHierarchy} object on this axis.
     * @param hierarchy The {@link QueryHierarchy} object to remove
     * from this axis.
     */
    public void removeHierarchy(QueryHierarchy hierarchy) {
    	hierarchy.setAxis(null);
        this.getQueryHierarchies().remove(hierarchy);
    }
}


