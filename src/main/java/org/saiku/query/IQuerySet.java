package org.saiku.query;

import java.util.List;

public interface IQuerySet {
	
	/**
	 * Get the Name of the current query object
	 * @return
	 */
	public String getName();
	
	/**
     * Sorts the Hierarchy members by name in the
     * order supplied as a parameter.
     * @param order The {@link SortOrder} to use.
     */
    public void sort(SortOrder order);

    /**
     * Returns the current order in which the
     * Hierarchy members are sorted.
     * @return A value of {@link SortOrder}
     */
    public SortOrder getSortOrder();

    /**
     * Clears the current sorting settings.
     */
    public void clearSort();

    
    /**
     * Returns the current mode of hierarchization, or null
     * if no hierarchization is currently performed.
     *
     * <p>This capability is only available when a single Hierarchy is
     * selected on an axis
     *
     * @return Either a hierarchization mode value or null
     *     if no hierarchization is currently performed.
     */
    public HierarchizeMode getHierarchizeMode();

    /**
     * Triggers the hierarchization of the included members within this
     * QueryHierarchy.
     *
     * <p>The Hierarchy inclusions will be wrapped in an MDX Hierarchize
     * function call.
     *
     * <p>This capability is only available when a single Hierarchy is
     * selected on an axis.
     *
     * @param hierarchizeMode If parents should be included before or after
     * their children. (Equivalent to the POST/PRE MDX literal for the
     * Hierarchize() function)
     * inside the Hierarchize() MDX function call.
     */
    public void setHierarchizeMode(HierarchizeMode hierarchizeMode);

    /**
     * Tells the QueryHierarchy not to hierarchize its included
     * selections.
     *
     * <p>This capability is only available when a single Hierarchy is
     * selected on an axis.
     */
    public void clearHierarchizeMode();
    
    /**
     * Defines in which way the hierarchize operation
     * should be performed.
     */
    public static enum HierarchizeMode {
        /**
         * Parents are placed before children.
         */
        PRE,
        /**
         * Parents are placed after children
         */
        POST
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
    public boolean isNonEmpty();
    
    /**
     * Returns the measure literal used for filtering the query group for empty cells
     * @return nonEmptyMeasure - non empty measure literal
     */
    public String getNonEmptyMeasureLiteral();

    /**
     * Sets whether this Query Group filters out empty rows.
     *
     * @param nonEmpty Whether this axis should filter out empty rows
     *
     * @see #isNonEmpty()
     */
    public void setNonEmpty(boolean nonEmpty);
    
    /**
     * Sets the query group to filter out empty rows based on the 
     * given measure name
     * 
     * @param measureUniqueName
     * 
     * @see #setNonEmpty(boolean)
     */
    public void setNonEmpty(String measureUniqueName);
    
    /**
     * Instead of using the query objects you can set an mdx expression
     * that represents the current query object.
     * NOTE: Since we cannot validate if the mdx set makes sense at this point
     * of the query, you will have to be carefuly how you use this feature
     * @param mdxSetExpression
     */
    public void setMdxSetExpression(String mdxSetExpression);
    
    /**
     * Returns an arbitrary mdx set instead of computing the mdx based on the
     * query objects
     * @return mdxSetExpression - Arbitrary MDX Expression of this query object
     */
    public String getMdxSetExpression();
    
    public boolean isMdxSetExpression();
    
    
    public void addFilterExpression(String filterMdxExpression);
    public void setFilterExpression(int index, String filterMdxExpression);
    public List<String> getFilterExpressions();
    public void clearFilterExpressions();
    
}
