package org.saiku.query;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import org.olap4j.Axis;

public class QueryAxis extends AbstractQueryObject {

    protected final List<QueryHierarchy> hierarchies = new HierarchyList();

    private final Query query;
    protected Axis location = null;
    
    public QueryAxis(Query query, Axis location) {
        super();
        this.query = query;
        this.location = location;
    }

    public Axis getLocation() {
        return location;
    }


    /**
     * Returns the name of this QueryAxis.
     *
     * @return the name of this axis, for example "ROWS", "COLUMNS".
     */
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
        this.getQueryHierarchies().add(index, hierarchy);
    }

    /**
     * Removes a {@link QueryHierarchy} object on this axis.
     * @param hierarchy The {@link QueryHierarchy} object to remove
     * from this axis.
     */
    public void removeHierarchy(QueryHierarchy hierarchy) {
        this.getQueryHierarchies().remove(hierarchy);
    }
	
    private class HierarchyList extends AbstractList<QueryHierarchy> {
        private final List<QueryHierarchy> list =
            new ArrayList<QueryHierarchy>();

        public QueryHierarchy get(int index) {
            return list.get(index);
        }

        public int size() {
            return list.size();
        }

        public QueryHierarchy set(int index, QueryHierarchy hierarchy) {
            if (hierarchy.getAxis() != null
                && hierarchy.getAxis() != QueryAxis.this)
            {
                hierarchy.getAxis().getQueryHierarchies().remove(hierarchy);
            }
            hierarchy.setAxis(QueryAxis.this);
            return list.set(index, hierarchy);
        }

        public void add(int index, QueryHierarchy hierarchy) {
            if (this.contains(hierarchy)) {
                throw new IllegalStateException(
                    "hierarchy already on this axis");
            }
            if (hierarchy.getAxis() != null
                && hierarchy.getAxis() != QueryAxis.this)
            {
                // careful! potential for loop
                hierarchy.getAxis().getQueryHierarchies().remove(hierarchy);
            }
            hierarchy.setAxis(QueryAxis.this);
            if (index >= list.size()) {
                list.add(hierarchy);
            } else {
                list.add(index, hierarchy);
            }
        }

        public QueryHierarchy remove(int index) {
            QueryHierarchy hierarchy = list.remove(index);
            hierarchy.setAxis(null);
            return hierarchy;
        }
    }

}


