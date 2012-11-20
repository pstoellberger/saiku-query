/*
// $Id: Olap4jNodeConverter.java 516 2012-02-27 21:43:58Z pstoellberger $
//
// Licensed to Julian Hyde under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Julian Hyde licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except in
// compliance with the License. You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
 */
package org.saiku.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.olap4j.Axis;
import org.olap4j.mdx.AxisNode;
import org.olap4j.mdx.CallNode;
import org.olap4j.mdx.CubeNode;
import org.olap4j.mdx.IdentifierNode;
import org.olap4j.mdx.LevelNode;
import org.olap4j.mdx.LiteralNode;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.SelectNode;
import org.olap4j.mdx.Syntax;
import org.olap4j.mdx.WithMemberNode;
import org.olap4j.mdx.WithSetNode;
import org.olap4j.mdx.parser.MdxParser;
import org.olap4j.mdx.parser.impl.DefaultMdxParserImpl;
import org.olap4j.metadata.Measure;
import org.olap4j.metadata.Member;
import org.saiku.query.mdx.IFilterFunction;
import org.saiku.query.metadata.CalculatedMeasure;
import org.saiku.query.metadata.CalculatedMember;

/**
 * Utility class to convert a Query object to a SelectNode.
 */
public class Olap4jNodeConverter extends NodeConverter {

	public static SelectNode toQuery(Query query) {
		List<IdentifierNode> cellpropertyList = Collections.emptyList();
		List<ParseTreeNode> withList = new ArrayList<ParseTreeNode>();
		List<QueryAxis> axisList = new ArrayList<QueryAxis>();
		axisList.add(query.getAxes().get(Axis.COLUMNS));
		axisList.add(query.getAxes().get(Axis.ROWS));

		AxisNode filterAxis = null;
		if (query.getAxes().containsKey(Axis.FILTER)) {
			final QueryAxis axis = query.getAxes().get(Axis.FILTER);
			if (!axis.hierarchies.isEmpty()) {
				filterAxis = toAxis(withList, axis);
			}
		}
		return new SelectNode(
				null,
				withList,
				toAxisList(withList, axisList),
				new CubeNode(
						null,
						query.getCube()),
						filterAxis,
						cellpropertyList);
	}

	private static List<AxisNode> toAxisList(List<ParseTreeNode> withList, List<QueryAxis> axes) {
		final ArrayList<AxisNode> axisList = new ArrayList<AxisNode>();
		for (QueryAxis axis : axes) {
			AxisNode axisNode = toAxis(withList, axis);
			if (axisNode != null) {
				axisList.add(axisNode);
			}
		}
		return axisList;
	}



	/*
	 * This method merges the selections into a single
	 * MDX axis selection.  Right now we do a simple
	 * crossjoin.
	 * It might return null if there are no dimensions placed on the axis.
	 */
	private static AxisNode toAxis(List<ParseTreeNode> withList, QueryAxis axis) {

		ParseTreeNode axisExpression = null;
		if (!axis.isMdxSetExpression()) {
			List<ParseTreeNode> hierarchies = new ArrayList<ParseTreeNode>();

			for(QueryHierarchy h : axis.getQueryHierarchies()) {
				ParseTreeNode hierarchyNode = toHierarchy(withList, h);
				hierarchies.add(hierarchyNode);
			}
			if (hierarchies.size() == 1) {
				axisExpression = hierarchies.get(0);
			}
			else if (hierarchies.size() > 1) {
				axisExpression = generateCrossJoin(hierarchies);
			} else {

			}

		}
		axisExpression = toOlap4jSortedQuerySet(axisExpression, axis);
		ParseTreeNode axisNode = null;
		if (axisExpression != null) {
			WithSetNode withNode = new WithSetNode(null, getIdentifier(axis), axisExpression);
			withList.add(withNode);
			axisNode = withNode.getIdentifier();
		}
		QueryDetails details = axis.getQuery().getDetails();
		
		if (details.getMeasures().size() > 0 && axis.getLocation().equals(details.getAxis())) {
			for (Measure m : details.getMeasures()) {
				if (m.isCalculatedInQuery()) {
					WithMemberNode wm = toOlap4jCalculatedMember((CalculatedMeasure) m);
					withList.add(wm);
				}
			}
			
			
			ParseTreeNode measuresNode = toOlap4jMeasureSet(details.getMeasures());
			if (axisNode == null) {
				axisNode = measuresNode;
			} else {
				List<ParseTreeNode> axisNodes = new ArrayList<ParseTreeNode>();
				if (details.getLocation().equals(QueryDetails.Location.TOP)) {
					axisNodes.add(measuresNode);
					axisNodes.add(axisNode);	
				} else {
					axisNodes.add(axisNode);
					axisNodes.add(measuresNode);
				}
				axisNode = generateCrossJoin(axisNodes);
			}
		}
		
		if (axisNode == null) {
			return null;
		}
		return new AxisNode(
				null,
				axis.isNonEmpty(),
				axis.getLocation(),
				new ArrayList<IdentifierNode>(),
				axisNode);
	}

	private static ParseTreeNode toHierarchy(List<ParseTreeNode> withList,
			QueryHierarchy h) {
		ParseTreeNode hierarchySet = null;

		if (!h.isMdxSetExpression()) {
			List<ParseTreeNode> levels = new ArrayList<ParseTreeNode>();
			ParseTreeNode existSet = null;
			for (QueryLevel l : h.getActiveQueryLevels()) {
				ParseTreeNode levelNode = toLevel(l);
				levelNode = toOlap4jQuerySet(levelNode, l);
				levels.add(levelNode);
				if (!l.isSimple()) {
					existSet = levelNode;
				}
			}
			ParseTreeNode levelSet = null;
			if (levels.size() > 1) {
				levelSet = generateListSetCall(levels);
			} else if (levels.size() == 1) {
				levelSet = levels.get(0);
			}
			
			if (h.isConsistent() && levels.size() > 1 && existSet != null) {
				levelSet = new CallNode(null, "Exists", Syntax.Function, levelSet, existSet);
			}
			
			if (h.needsHierarchize()) {
				levelSet = new CallNode(
						null,
						"Hierarchize",
						Syntax.Function,
						levelSet);

			}
				
			List<ParseTreeNode> cmNodes = new ArrayList<ParseTreeNode>();
			for (CalculatedMember cm : h.getActiveCalculatedMembers()) {
				WithMemberNode wm = toOlap4jCalculatedMember(cm);
				withList.add(wm);
				cmNodes.add(wm.getIdentifier());
			}
			if (cmNodes.size() > 0) {
				ParseTreeNode cmSet = generateListSetCall(cmNodes);
				if (levelSet != null) {
					hierarchySet = generateSetCall(cmSet, levelSet);
				} else {
					hierarchySet = cmSet;
				}
			} else {
				hierarchySet = levelSet;	
			}
		}
		hierarchySet = toOlap4jSortedQuerySet(hierarchySet, h);

		return hierarchySet;
	}

	private static ParseTreeNode toLevel(QueryLevel level) {
		List<Member> inclusions = new ArrayList<Member>();
		List<Member> exclusions = new ArrayList<Member>();
		inclusions.addAll(level.getInclusions());
		exclusions.addAll(level.getExclusions());
		
		ParseTreeNode baseNode = new CallNode(null, "Members", Syntax.Property, new LevelNode(null, level.getLevel()));
		baseNode = generateSetCall(baseNode);
		
		if (inclusions.size() > 0) {
			baseNode = toOlap4jMemberSet(inclusions);
		}
		if (exclusions.size() > 0) {
			ParseTreeNode exceptSet = toOlap4jMemberSet(exclusions);
			baseNode =  new CallNode(null, "Except", Syntax.Function, baseNode, exceptSet);			
		}
		
		return baseNode;
	}

	private static ParseTreeNode toOlap4jQuerySet(ParseTreeNode expression, IQuerySet o) {
		MdxParser parser = new DefaultMdxParserImpl();

		if (o.isMdxSetExpression()) {
			expression =  parser.parseExpression("{" + o.getMdxSetExpression() + "}");
		}

		if (o.getFilters().size() > 0) {
			for (IFilterFunction filter : o.getFilters()) {
				expression = filter.visit(parser, expression);
			}
		}
		
		return expression;
		
	}
	private static ParseTreeNode toOlap4jSortedQuerySet(ParseTreeNode expression, ISortableQuerySet o) {
		expression = toOlap4jQuerySet(expression, o);
		if (o.getSortOrder() != null) {
			LiteralNode evaluatorNode =
					LiteralNode.createSymbol(
							null,
							o.getSortEvaluationLiteral());
			expression =
					new CallNode(
							null,
							"Order",
							Syntax.Function,
							expression,
							evaluatorNode,
							LiteralNode.createSymbol(
									null, o.getSortOrder().name()));
		} else if (o.getHierarchizeMode() != null) {
			if (o.getHierarchizeMode().equals(
					ISortableQuerySet.HierarchizeMode.PRE))
			{
				// In pre mode, we don't add the "POST" literal.
				expression = new CallNode(
						null,
						"Hierarchize",
						Syntax.Function,
						expression);
			} else if (o.getHierarchizeMode().equals(
					ISortableQuerySet.HierarchizeMode.POST))
			{
				expression = new CallNode(
						null,
						"Hierarchize",
						Syntax.Function,
						expression,
						LiteralNode.createSymbol(
								null, o.getHierarchizeMode().name()));
			} else {
				throw new RuntimeException("Missing value handler.");
			}
		}
		return expression;

	}
}










