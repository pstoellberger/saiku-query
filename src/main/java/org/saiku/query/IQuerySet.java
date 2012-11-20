package org.saiku.query;

import java.util.List;

import org.saiku.query.mdx.IFilterFunction;

public interface IQuerySet {
	
	/**
	 * Get the Name of the current query object
	 * @return
	 */
	public String getName();

	
	/**
	 * If the query set does not include any functions, but only specific members, levels, etc.
	 * this function will return true.
	 * @return
	 */
	public boolean isSimple();

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
    
    
    public void addFilter(IFilterFunction filter);
    public void setFilter(int index, IFilterFunction filter);
    public List<IFilterFunction> getFilters();
    public void clearFilters();
    
}
