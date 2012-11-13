package org.saiku.query;

import java.util.ArrayList;
import java.util.List;

import org.olap4j.Axis;
import org.olap4j.impl.Named;
import org.olap4j.metadata.Measure;

public class QueryDetails implements Named {

	protected List<Measure> measures = new ArrayList<Measure>();
	
	private Location location = Location.BOTTOM;
	
	private Axis axis;

	private Query query;
	
	public enum Location {
		TOP,
		BOTTOM
	}
	
	public QueryDetails(Query query, Axis axis) {
		this.axis = axis;
		this.query = query;
	}
	
	public void add(Measure measure) {
		if (!measures.contains(measure)) {
			measures.add(measure);
		}
	}
	
	public void set(Measure measure, int position) {
		if (!measures.contains(measure)) {
			measures.add(position, measure);
		} else {
			int oldindex = measures.indexOf(measure);
			if (oldindex <= position) {
				measures.add(position, measure);
				measures.remove(oldindex);
			}
		}
	}
	
	public void remove(Measure measure) {
		measures.remove(measure);
	}
	
	
	public List<Measure> getMeasures() {
		return measures;
	}
	
	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}
	
	public Axis getAxis() {
		return axis;
	}
	
	public void setAxis(Axis axis) {
		this.axis = axis;
	}

	@Override
	public String getName() {
		return "DETAILS";
	}

}
