/**
 * 
 */
package org.saiku.query;

import org.apache.commons.lang.StringUtils;


/**
 * @author pstoellberger
 *
 */
public abstract class AbstractQueryObject implements IQuerySet {

	
	private SortOrder sortOrder;
	private HierarchizeMode hierarchizeMode;
	private boolean nonEmpty;
	private String nonEmptyMeasure;
	private String mdxExpression;

	
	public abstract String getName();
	
	/* (non-Javadoc)
	 * @see org.saiku.query.IQuerySet#sort(org.saiku.query.SortOrder)
	 */
	@Override
	public void sort(SortOrder order) {
		this.sortOrder = order;

	}

	/* (non-Javadoc)
	 * @see org.saiku.query.IQuerySet#getSortOrder()
	 */
	@Override
	public SortOrder getSortOrder() {
		return sortOrder;
	}

	/* (non-Javadoc)
	 * @see org.saiku.query.IQuerySet#clearSort()
	 */
	@Override
	public void clearSort() {
		this.sortOrder = null;

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
	 * @see org.saiku.query.IQuerySet#isNonEmpty()
	 */
	@Override
	public boolean isNonEmpty() {
		return nonEmpty;
	}

	/* (non-Javadoc)
	 * @see org.saiku.query.IQuerySet#setNonEmpty(boolean)
	 */
	@Override
	public void setNonEmpty(boolean nonEmpty) {
		this.nonEmpty = nonEmpty;
		if (!nonEmpty) {
			this.nonEmptyMeasure = null;
		}

	}

	/* (non-Javadoc)
	 * @see org.saiku.query.IQuerySet#setNonEmpty(java.lang.String)
	 */
	@Override
	public void setNonEmpty(String measureUniqueName) {
		this.nonEmpty = true;
		this.nonEmptyMeasure = measureUniqueName;

	}

	/* (non-Javadoc)
	 * @see org.saiku.query.IQuerySet#getNonEmptyMeasureLiteral()
	 */
	@Override
	public String getNonEmptyMeasureLiteral() {
		return nonEmptyMeasure;
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
		result = prime * result + (nonEmpty ? 1231 : 1237);
		result = prime * result
				+ ((nonEmptyMeasure == null) ? 0 : nonEmptyMeasure.hashCode());
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
		if (nonEmpty != other.nonEmpty)
			return false;
		if (nonEmptyMeasure == null) {
			if (other.nonEmptyMeasure != null)
				return false;
		} else if (!nonEmptyMeasure.equals(other.nonEmptyMeasure))
			return false;
		if (sortOrder != other.sortOrder)
			return false;
		if (!StringUtils.equals(getName(), other.getName()))
			return false;
		return true;
	}

}
