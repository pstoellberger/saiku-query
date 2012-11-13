package org.saiku.query;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olap4j.Axis;
import org.olap4j.CellSet;
import org.olap4j.OlapConnection;
import org.olap4j.OlapException;
import org.olap4j.OlapStatement;
import org.olap4j.impl.NamedListImpl;
import org.olap4j.mdx.SelectNode;
import org.olap4j.metadata.Catalog;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Measure;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.Member.Type;
import org.olap4j.metadata.NamedList;
import org.olap4j.metadata.Property;
import org.saiku.query.IQuerySet.HierarchizeMode;
import org.saiku.query.metadata.CalculatedMeasure;
import org.saiku.query.metadata.CalculatedMember;

public class Query {

    protected final String name;
    protected Map<Axis, QueryAxis> axes = new HashMap<Axis, QueryAxis>();
    protected QueryAxis across;
    protected QueryAxis down;
    protected QueryAxis filter;
    protected QueryAxis unused;
    protected final Cube cube;
    protected Map<String, QueryHierarchy> hierarchyMap =
        new HashMap<String, QueryHierarchy>();
    
    protected NamedList<CalculatedMeasure> calculatedMeasures = new NamedListImpl<CalculatedMeasure>();
    
    protected QueryDetails details;
    /**
     * Whether or not to select the default hierarchy and default
     * member on a hierarchy if no explicit selections were performed.
     */
    protected boolean selectDefaultMembers = true;
    private final OlapConnection connection;
	private HierarchizeMode defaultHierarchizeMode = HierarchizeMode.PRE;
	
    /**
     * Constructs a Query object.
     * @param name Any arbitrary name to give to this query.
     * @param cube A Cube object against which to build a query.
     * @throws SQLException If an error occurs while accessing the
     * cube's underlying connection.
     */
    public Query(String name, Cube cube) throws SQLException {
        super();
        this.name = name;
        this.cube = cube;
        final Catalog catalog = cube.getSchema().getCatalog();
        this.connection =
            catalog.getMetaData().getConnection().unwrap(OlapConnection.class);
        this.connection.setCatalog(catalog.getName());
        this.unused = new QueryAxis(this, null);
        for (Hierarchy hierarchy : cube.getHierarchies()) {
            QueryHierarchy queryHierarchy = new QueryHierarchy(
                this, hierarchy);
            unused.getQueryHierarchies().add(queryHierarchy);
            hierarchyMap.put(queryHierarchy.getName(), queryHierarchy);
        }
        across = new QueryAxis(this, Axis.COLUMNS);
        down = new QueryAxis(this, Axis.ROWS);
        filter = new QueryAxis(this, Axis.FILTER);
        axes.put(null, unused);
        axes.put(Axis.COLUMNS, across);
        axes.put(Axis.ROWS, down);
        axes.put(Axis.FILTER, filter);
        details = new QueryDetails(this, Axis.COLUMNS);
    }

    /**
     * Returns the MDX parse tree behind this Query. The returned object is
     * generated for each call to this function. Altering the returned
     * SelectNode object won't affect the query itself.
     * @return A SelectNode object representing the current query structure.
     */
    public SelectNode getSelect() {
        return Olap4jNodeConverter.toOlap4j(this);
    }

    /**
     * Returns the underlying cube object that is used to query against.
     * @return The Olap4j's Cube object.
     */
    public Cube getCube() {
        return cube;
    }

    /**
     * Returns the underlying connection object that is used to query against.
     * @return The Olap4j's Connection object.
     */
    public OlapConnection getConnection() {
    	return connection;
    }
    
    /**
     * Returns the underlying catalog object that is used to query against.
     * @return The Olap4j's Catalog object.
     */
    public Catalog getCatalog() {
    	return cube.getSchema().getCatalog();
    }
    
    /**
     * Returns the Olap4j's QueryHierarchy object according to the name
     * given as a parameter. If no hierarchy of the given name is found,
     * a null value will be returned.
     * @param uniqueName The name of the hierarchy you want the object for.
     * @return The hierarchy object, null if no hierarchy of that
     * name can be found.
     */
    public QueryHierarchy getHierarchy(String uniqueName) {
        return hierarchyMap.get(uniqueName);
    }

    /**
     * Returns the Olap4j's QueryHierarchy object according to the Hierarchy
     * given as a parameter. If no QueryHierarchy is found,
     * a null value will be returned.
     * @param hierarchy The Hierarchy of the QueryHierarchy you want the object for.
     * @return The QueryHierarchy object, null if no hierarchy of that
     * name can be found.
     */
    public QueryHierarchy getHierarchy(Hierarchy hierarchy) {
    	if (hierarchy == null) {
    		return null;
    	}
    	return hierarchyMap.get(hierarchy.getUniqueName());
    }


    /**
     * Returns the Olap4j's QueryLevel object according to the name
     * given as a parameter. If no Level of the given name is found,
     * a null value will be returned.
     * @param uniqueName The name of the Level you want the object for.
     * @return The QueryLevel object, null if no Level of that
     * name can be found.
     */
    public QueryLevel getLevel(Hierarchy hierarchy, String uniqueName) {
        QueryHierarchy h =  hierarchyMap.get(hierarchy.getUniqueName());
//        return h.getLevel(uniqueName);
        return null;
    }
    
    /**
     * Returns the Olap4j's QueryLevel object according to the Level
     * given as a parameter. If no Level of the given name is found,
     * a null value will be returned.

     * @param level The Level you want the QueryLevel for
     * @return The QueryLevel object, null if no Level of that
     * name can be found.
     */
    public QueryLevel getLevel(Level level) {
        return getLevel(level.getHierarchy(), level.getUniqueName());
    }


    /**
     * Swaps rows and columns axes. Only applicable if there are two axes.
     */
    public void swapAxes() {
        // Only applicable if there are two axes - plus filter and unused.
        if (axes.size() != 4) {
            throw new IllegalArgumentException();
        }
        List<QueryHierarchy> tmpAcross = new ArrayList<QueryHierarchy>();
        tmpAcross.addAll(across.getQueryHierarchies());

        List<QueryHierarchy> tmpDown = new ArrayList<QueryHierarchy>();
        tmpDown.addAll(down.getQueryHierarchies());

        across.getQueryHierarchies().clear();
        Map<Integer, QueryHierarchy> acrossChildList =
            new HashMap<Integer, QueryHierarchy>();
        for (int cpt = 0; cpt < tmpAcross.size();cpt++) {
            acrossChildList.put(Integer.valueOf(cpt), tmpAcross.get(cpt));
        }

        down.getQueryHierarchies().clear();
        Map<Integer, QueryHierarchy> downChildList =
            new HashMap<Integer, QueryHierarchy>();
        for (int cpt = 0; cpt < tmpDown.size();cpt++) {
            downChildList.put(Integer.valueOf(cpt), tmpDown.get(cpt));
        }

        across.getQueryHierarchies().addAll(tmpDown);

        down.getQueryHierarchies().addAll(tmpAcross);
    }

    /**
     * Returns the query axis for a given axis type.
     *
     * <p>If you pass axis=null, returns a special axis that is used to hold
     * all unused hierarchies. (We may change this behavior in future.)
     *
     * @param axis Axis type
     * @return Query axis
     */
    public QueryAxis getAxis(Axis axis) {
        return this.axes.get(axis);
    }

    /**
     * Returns a map of the current query's axis.
     * <p>Be aware that modifications to this list might
     * have unpredictable consequences.</p>
     * @return A standard Map object that represents the
     * current query's axis.
     */
    public Map<Axis, QueryAxis> getAxes() {
        return axes;
    }
    
    public CalculatedMember createCalculatedMember(
    		QueryHierarchy hierarchy,
    		String name,
    		String formula,
    		Map<Property, Object> properties) 
    {
    	Hierarchy h = hierarchy.getHierarchy();
    	CalculatedMember cm = new CalculatedMember(
    			h.getDimension(), 
    			h, 
    			name, 
    			name,
    			null,
    			Type.FORMULA,
    			formula,
    			null);
    	addCalculatedMember(hierarchy, cm);
    	return cm;
    }
    
    public CalculatedMember createCalculatedMember(
    		QueryHierarchy hierarchy,
    		Member parentMember,
    		String name,
    		String formula,
    		Map<Property, Object> properties) 
    {
    	Hierarchy h = hierarchy.getHierarchy();
    	CalculatedMember cm = new CalculatedMember(
    			h.getDimension(), 
    			h, 
    			name, 
    			name,
    			parentMember,
    			Type.FORMULA,
    			formula,
    			null);
    	addCalculatedMember(hierarchy, cm);
    	return cm;
    }
    
    public void addCalculatedMember(QueryHierarchy hierarchy, CalculatedMember cm) {
    	hierarchy.addCalculatedMember(cm);
    }
    
    public NamedList<CalculatedMember> getCalculatedMembers(QueryHierarchy hierarchy) {
    	return hierarchy.getCalculatedMembers();
    }
    
    public NamedList<CalculatedMember> getCalculatedMembers() {
    	NamedList<CalculatedMember> cm = new NamedListImpl<CalculatedMember>();
    	for (QueryHierarchy h : hierarchyMap.values()) {
    		cm.addAll(h.getCalculatedMembers());
    	}
    	return cm;
    }

    public CalculatedMeasure createCalculatedMeasure(
    		String name,
    		String formula,
    		Map<Property, Object> properties) 
    {
    	if (cube.getMeasures().size() > 0) {
    		Measure first = cube.getMeasures().get(0);
    		return createCalculatedMeasure(first.getHierarchy(), name, formula, properties);
    	}
    	throw new RuntimeException("There has to be at least one valid measure in the cube to create a calculated measure!");
    }
    
    public CalculatedMeasure createCalculatedMeasure(
    		Hierarchy measureHierarchy,
    		String name,
    		String formula,
    		Map<Property, Object> properties) 
    {
    	CalculatedMeasure cm = new CalculatedMeasure(
    			measureHierarchy.getDimension(), 
    			measureHierarchy, 
    			name, 
    			name,
    			formula,
    			null);
    	addCalculatedMeasure(cm);
    	return cm;
    }
    
    
    public void addCalculatedMeasure(CalculatedMeasure cm) {
    	calculatedMeasures.add(cm);
    }
    
    public NamedList<CalculatedMeasure> getCalculatedMeasures() {
    	return calculatedMeasures;
    }
    
    public CalculatedMeasure getCalculatedMeasure(String name) {
    	return calculatedMeasures.get(name);
    }
    
    public QueryDetails getDetails() {
    	return details;
    }

    /**
     * Returns the fictional axis into which all unused hierarchies are stored.
     * All hierarchies included in this axis will not be part of the query.
     * @return The QueryAxis representing hierarchies that are currently not
     * used inside the query.
     */
    public QueryAxis getUnusedAxis() {
        return unused;
    }


    /**
     * Executes the query against the current OlapConnection and returns
     * a CellSet object representation of the data.
     *
     * @return A proper CellSet object that represents the query execution
     *     results.
     * @throws OlapException If something goes sour, an OlapException will
     *     be thrown to the caller. It could be caused by many things, like
     *     a stale connection. Look at the root cause for more details.
     */
    public CellSet execute() throws OlapException {
        SelectNode mdx = getSelect();
        final Catalog catalog = getCatalog();
        try {
            this.connection.setCatalog(catalog.getName());
        } catch (SQLException e) {
            throw new OlapException("Error while executing query", e);
        }
        OlapStatement olapStatement = connection.createStatement();
        return olapStatement.executeOlapQuery(mdx);
    }

    /**
     * Returns this query's name. There is no guarantee that it is unique
     * and is set at object instanciation.
     * @return This query's name.
     */
    public String getName() {
        return name;
    }
    
	public void moveHierarchy(QueryHierarchy hierarchy, Axis axis) {
		moveHierarchy(hierarchy, axis, -1);
	}

	public void moveHierarchy(QueryHierarchy hierarchy, Axis axis, int position) {
        QueryAxis oldQueryAxis = findAxis(hierarchy);
        QueryAxis newQueryAxis = getAxis(axis);
		
        if (oldQueryAxis != null && newQueryAxis != null && (position > -1 || (oldQueryAxis.getLocation() != newQueryAxis.getLocation()))) {
            oldQueryAxis.removeHierarchy(hierarchy);
            if (position > -1) {
            	newQueryAxis.addHierarchy(position, hierarchy);
            } else {
            	newQueryAxis.addHierarchy(hierarchy);
            }
        }
    }
	
	
	private QueryAxis findAxis(QueryHierarchy hierarchy) {
		if (getUnusedAxis().getQueryHierarchies().contains(hierarchy)) {
			return getUnusedAxis();
		}
		else {
			Map<Axis,QueryAxis> axes = getAxes();
			for (Axis axis : axes.keySet()) {
				if (axes.get(axis).getQueryHierarchies().contains(hierarchy)) {
					return axes.get(axis);
				}
			}
		
		}
		return null;
	}

    /**
     * Behavior setter for a query. By default, if a hierarchy is placed on
     * an axis but no selections are made, the default hierarchy and
     * the default member will be selected when validating the query.
     * This behavior can be turned off by this setter.
     * @param selectDefaultMembers Enables or disables the default
     * member and hierarchy selection upon validation.
     */
    public void setSelectDefaultMembers(boolean selectDefaultMembers) {
        this.selectDefaultMembers = selectDefaultMembers;
    }
    
    public void setDefaultHierarchizeMode(HierarchizeMode mode) {
    	this.defaultHierarchizeMode = mode;
    }
    
    public HierarchizeMode getDefaultHierarchizeMode() {
    	return this.defaultHierarchizeMode;
    }
    
//  /**
//  * Validates the current query structure. If a hierarchy axis has
//  * been placed on an axis but no selections were performed on it,
//  * the default hierarchy and default member will be selected. This
//  * can be turned off by invoking the
//  * {@link Query#setSelectDefaultMembers(boolean)} method.
//  * @throws OlapException If the query is not valid, an exception
//  * will be thrown and it's message will describe exactly what to fix.
//  */
// public void validate() throws OlapException {
//     try {
//         // First, perform default selections if needed.
//         if (this.selectDefaultMembers) {
//             // Perform default selection on the hierarchys on the rows axis.
//             for (QueryHierarchy hierarchy : this.getAxis(Axis.ROWS)
//                 .getQueryHierarchies())
//             {
//                 if (hierarchy.getInclusions().size() == 0) {
//                     Member defaultMember = hierarchy.gethierarchy()
//                         .getDefaultHierarchy().getDefaultMember();
//                     hierarchy.include(defaultMember);
//                 }
//             }
//             // Perform default selection on the
//             // hierarchys on the columns axis.
//             for (QueryHierarchy hierarchy : this.getAxis(Axis.COLUMNS)
//                 .getQueryHierarchies())
//             {
//                 if (hierarchy.getInclusions().size() == 0) {
//                     Member defaultMember = hierarchy.gethierarchy()
//                         .getDefaultHierarchy().getDefaultMember();
//                     hierarchy.include(defaultMember);
//                 }
//             }
//             // Perform default selection on the hierarchys
//             // on the filter axis.
//             for (QueryHierarchy hierarchy : this.getAxis(Axis.FILTER)
//                 .getQueryHierarchies())
//             {
//                 if (hierarchy.getInclusions().size() == 0) {
//                     Member defaultMember = hierarchy.gethierarchy()
//                         .getDefaultHierarchy().getDefaultMember();
//                     hierarchy.include(defaultMember);
//                 }
//             }
//         }
//
//         // We at least need a hierarchy on the columns axis.
//         if (this.getAxis(Axis.COLUMNS).getQueryHierarchies().size() == 0) {
//             throw new OlapException(
//                 "A valid Query requires at least one hierarchy on the columns axis.");
//         }
//
//         // Try to build a select tree.
//         this.getSelect();
//     } catch (Exception e) {
//         throw new OlapException("Query validation failed.", e);
//     }
// }
}

// End Query.java
