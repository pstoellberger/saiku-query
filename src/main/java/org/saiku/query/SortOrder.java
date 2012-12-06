package org.saiku.query;
public enum SortOrder {
    /**
     * Ascending sort order. Members of
     * the same hierarchy are still kept together.
     */
    ASC,
    /**
     * Descending sort order. Members of
     * the same hierarchy are still kept together.
     */
    DESC,
    /**
     * Sorts in ascending order, but does not
     * maintain members of a same hierarchy
     * together. This is known as a "break
     * hierarchy ascending sort".
     */
    BASC,
    /**
     * Sorts in descending order, but does not
     * maintain members of a same hierarchy
     * together. This is known as a "break
     * hierarchy descending sort".
     */
    BDESC
}
