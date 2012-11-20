package org.saiku.query;


public interface ISortableQuerySet extends IQuerySet {

	/**
     * Sorts the Hierarchy members by name in the
     * order supplied as a parameter.
     * @param order The {@link SortOrder} to use.
     */
    public void sort(SortOrder order);
    
    /**
     * Sorts the Hierarchy members by name in the
     * order supplied as a parameter using the given
     * sort evaluation literal
     * @param order The {@link SortOrder} to use.
     */
    public void sort(SortOrder order, String sortEvaluationLiteral);

    /**
     * Returns the current order in which the
     * Hierarchy members are sorted.
     * @return A value of {@link SortOrder}
     */
    public SortOrder getSortOrder();
    
    /**
     * Returns the current literal used for sorting
     * @return A sort evaluation literal
     */
    public String getSortEvaluationLiteral();

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
    
}
