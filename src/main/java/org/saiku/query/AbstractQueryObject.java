/**
 * 
 */
package org.saiku.query;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.saiku.query.mdx.IFilterFunction;


/**
 * @author pstoellberger
 *
 */
public abstract class AbstractQueryObject implements IQuerySet {

	
	private SortOrder sortOrder;
	private String sortEvaluationLiteral;
	private HierarchizeMode hierarchizeMode;
	private String mdxExpression;
	
	private List<IFilterFunction> filters = new ArrayList<IFilterFunction>();

	
	public abstract String getName();
	
	/* (non-Javadoc)
	 * @see org.saiku.query.IQuerySet#sort(org.saiku.query.SortOrder)
	 */
	@Override
	public void sort(SortOrder order) {
		this.sortOrder = order;

	}
	
	/* (non-Javadoc)
	 * @see org.saiku.query.IQuerySet#sort(org.saiku.query.SortOrder, java.lang.String)
	 */
	@Override
	public void sort(SortOrder order, String sortEvaluationLiteral) {
		this.sortOrder = order;
		this.sortEvaluationLiteral = sortEvaluationLiteral;
	}

	/* (non-Javadoc)
	 * @see org.saiku.query.IQuerySet#getSortOrder()
	 */
	@Override
	public SortOrder getSortOrder() {
		return sortOrder;
	}

	@Override
	public String getSortEvaluationLiteral() {
		return sortEvaluationLiteral;
	}

	/* (non-Javadoc)
	 * @see org.saiku.query.IQuerySet#clearSort()
	 */
	@Override
	public void clearSort() {
		this.sortOrder = null;
		this.sortEvaluationLiteral = null;
	}

	/* (non-Javadoc)
	 * @see org.saiku.query.IQuerySet#getHierarchizeMode()
	 */
	@Override
	public HierarchizeMode getHierarchizeMode() {
		return this.hierarchizeMode;
	}

	/* (non-Javadoc)
	 * @see org.saiku.query.IQuerySet#setHierarchizeMode(org.saiku.query.IQuerySet.HierarchizeMode)
	 */
	@Override
	public void setHierarchizeMode(HierarchizeMode hierarchizeMode) {
		this.hierarchizeMode = hierarchizeMode;

	}

	/* (non-Javadoc)
	 * @see org.saiku.query.IQuerySet#clearHierarchizeMode()
	 */
	@Override
	public void clearHierarchizeMode() {
		this.hierarchizeMode = null;

	}

	/* (non-Javadoc)
	 * @see org.saiku.query.IQuerySet#setMdxSetExpression(java.lang.String)
	 */
	@Override
	public void setMdxSetExpression(String mdxSetExpression) {
		this.mdxExpression = mdxSetExpression;
		
	}
	
	/* (non-Javadoc)
	 * @see org.saiku.query.IQuerySet#getMdxSetExpression()
	 */
	@Override
	public String getMdxSetExpression() {
		return this.mdxExpression;
	}
	
	/* (non-Javadoc)
	 * @see org.saiku.query.IQuerySet#isMdxSetExpression()
	 */
	@Override
	public boolean isMdxSetExpression() {
		return this.mdxExpression != null;
	}
	
	@Override
	public void addFilter(IFilterFunction filter) {
		filters.add(filter);
	}
	
	@Override
	public void setFilter(int index, IFilterFunction filter) {
		filters.set(index, filter);
	}

	@Override
	public List<IFilterFunction> getFilters() {
		return filters;
	}
	@Override
	public void clearFilters() {
		filters.clear();
	}
	

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((hierarchizeMode == null) ? 0 : hierarchizeMode.hashCode());
		result = prime * result
				+ ((mdxExpression == null) ? 0 : mdxExpression.hashCode());
		result = prime * result
				+ ((sortOrder == null) ? 0 : sortOrder.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractQueryObject other = (AbstractQueryObject) obj;
		if (hierarchizeMode != other.hierarchizeMode)
			return false;
		if (mdxExpression == null) {
			if (other.mdxExpression != null)
				return false;
		} else if (!mdxExpression.equals(other.mdxExpression))
			return false;
		if (sortOrder != other.sortOrder)
			return false;
		if (!StringUtils.equals(getName(), other.getName()))
			return false;
		return true;
	}


}
