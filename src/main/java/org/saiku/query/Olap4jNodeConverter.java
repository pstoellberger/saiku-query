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
import java.util.Map.Entry;

import org.olap4j.Axis;
import org.olap4j.mdx.AxisNode;
import org.olap4j.mdx.CallNode;
import org.olap4j.mdx.CubeNode;
import org.olap4j.mdx.IdentifierNode;
import org.olap4j.mdx.LevelNode;
import org.olap4j.mdx.LiteralNode;
import org.olap4j.mdx.MemberNode;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.PropertyValueNode;
import org.olap4j.mdx.SelectNode;
import org.olap4j.mdx.Syntax;
import org.olap4j.mdx.WithMemberNode;
import org.olap4j.mdx.WithSetNode;
import org.olap4j.mdx.parser.MdxParser;
import org.olap4j.mdx.parser.impl.DefaultMdxParserImpl;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Measure;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.Property;
import org.saiku.query.mdx.IFilterFunction;
import org.saiku.query.metadata.Calculated;
import org.saiku.query.metadata.CalculatedMeasure;
import org.saiku.query.metadata.CalculatedMember;

/**
 * Utility class to convert a Query object to a SelectNode.
 */
abstract class Olap4jNodeConverter {

	public static SelectNode toOlap4j(Query query) {
		List<IdentifierNode> cellpropertyList = Collections.emptyList();
		List<ParseTreeNode> withList = new ArrayList<ParseTreeNode>();
		List<QueryAxis> axisList = new ArrayList<QueryAxis>();
		axisList.add(query.getAxes().get(Axis.COLUMNS));
		axisList.add(query.getAxes().get(Axis.ROWS));

		AxisNode filterAxis = null;
		if (query.getAxes().containsKey(Axis.FILTER)) {
			final QueryAxis axis = query.getAxes().get(Axis.FILTER);
			if (!axis.hierarchies.isEmpty()) {
				filterAxis = toOlap4j(withList, axis);
			}
		}
		return new SelectNode(
				null,
				withList,
				toOlap4j(withList, axisList),
				new CubeNode(
						null,
						query.getCube()),
						filterAxis,
						cellpropertyList);
	}

	private static CallNode generateSetCall(ParseTreeNode... args) {
		return
				new CallNode(
						null,
						"{}",
						Syntax.Braces,
						args);
	}

	private static CallNode generateListSetCall(List<ParseTreeNode> cnodes) {
		return
				new CallNode(
						null,
						"{}",
						Syntax.Braces,
						cnodes);
	}

	private static CallNode generateListTupleCall(List<ParseTreeNode> cnodes) {
		return
				new CallNode(
						null,
						"()",
						Syntax.Parentheses,
						cnodes);
	}

	protected static CallNode generateCrossJoin(List<ParseTreeNode> selections)
	{
		ParseTreeNode sel1 = selections.remove(0);
		if (sel1 instanceof MemberNode) {
			sel1 = generateSetCall(sel1);
		}
		if (selections.size() == 1) {
			ParseTreeNode sel2 = selections.get(0);
			if (sel2 instanceof MemberNode) {
				sel2 = generateSetCall(sel2);
			}
			return new CallNode(
					null, "CrossJoin", Syntax.Function, sel1, sel2);
		} else {
			return new CallNode(
					null, "CrossJoin", Syntax.Function, sel1,
					generateCrossJoin(selections));
		}
	}

	protected static CallNode generateUnion(List<List<ParseTreeNode>> unions) {
		if (unions.size() > 2) {
			List<ParseTreeNode> first = unions.remove(0);
			return new CallNode(
					null, "Union", Syntax.Function,
					generateCrossJoin(first),
					generateUnion(unions));
		} else {
			return new CallNode(
					null, "Union", Syntax.Function,
					generateCrossJoin(unions.get(0)),
					generateCrossJoin(unions.get(1)));
		}
	}

	protected static CallNode generateHierarchizeUnion(
			List<List<ParseTreeNode>> unions)
	{
		return new CallNode(
				null, "Hierarchize", Syntax.Function,
				generateUnion(unions));
	}


	/*
	 * This method merges the selections into a single
	 * MDX axis selection.  Right now we do a simple
	 * crossjoin.
	 * It might return null if there are no dimensions placed on the axis.
	 */
	private static AxisNode toOlap4j(List<ParseTreeNode> withList, QueryAxis axis) {

		ParseTreeNode axisExpression = null;
		if (!axis.isMdxSetExpression()) {
			List<ParseTreeNode> hierarchies = new ArrayList<ParseTreeNode>();

			for(QueryHierarchy h : axis.getQueryHierarchies()) {
				ParseTreeNode hierarchyNode = toOlap4jHierarchy(withList, h);
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
		axisExpression = toOlap4j(axisExpression, axis);
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

	private static ParseTreeNode toOlap4jHierarchy(List<ParseTreeNode> withList,
			QueryHierarchy h) {
		ParseTreeNode hierarchySet = null;
		MdxParser parser = new DefaultMdxParserImpl();

		if (!h.isMdxSetExpression()) {
			List<Member> inclusions = new ArrayList<Member>();
			List<Member> exclusions = new ArrayList<Member>();
			List<ParseTreeNode> levels = new ArrayList<ParseTreeNode>();
			for (QueryLevel l : h.getActiveQueryLevels()) {
				inclusions.addAll(l.getInclusions());
				exclusions.addAll(l.getExclusions());
				ParseTreeNode levelNode = toOlap4jLevelSet(l.getLevel());
				levelNode = toOlap4j(levelNode, l);
				levels.add(levelNode);
			}
			
			ParseTreeNode levelSet = null;
			if (levels.size() > 0) {
				levelSet = generateListSetCall(levels);
				if (inclusions.size() > 0) {
					ParseTreeNode memberSet = toOlap4jMemberSet(inclusions);
					if (levels.size() > 1) {
						h.setHierarchizeMode(h.getQuery().getDefaultHierarchizeMode());
						levelSet = new CallNode(null, "Exists", Syntax.Function, levelSet, memberSet);						
					} else {
						levelSet = memberSet;
					}
				}
				if (exclusions.size() > 0) {
					ParseTreeNode exceptSet = toOlap4jMemberSet(exclusions);
					levelSet =  new CallNode(null, "Except", Syntax.Function, levelSet, exceptSet);			
				}
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
		hierarchySet = toOlap4j(hierarchySet, h);

		return hierarchySet;
	}

	private static ParseTreeNode toOlap4jLevelSet(Level level) {
		return new CallNode(null, "Members", Syntax.Property, new LevelNode(null, level));
	}

	private static WithMemberNode toOlap4jCalculatedMember(Calculated cm) {
		MdxParser parser = new DefaultMdxParserImpl();
		ParseTreeNode formula = parser.parseExpression(cm.getFormula());
		List<PropertyValueNode> propertyList = new ArrayList<PropertyValueNode>();
		for (Entry<Property, Object> entry : cm.getPropertyValueMap().entrySet()) {
			ParseTreeNode exp = parser.parseExpression(entry.getValue().toString());
			String name = entry.getKey().getName();
			PropertyValueNode prop = new PropertyValueNode(null, name, exp);
			propertyList.add(prop);
		}
		WithMemberNode wm = new WithMemberNode(
				null, 
				IdentifierNode.parseIdentifier(cm.getUniqueName()), 
				formula, 
				propertyList);
		return wm;
	}

	private static ParseTreeNode toOlap4jMemberSet(List<Member> members) {
		List<ParseTreeNode> membernodes = new ArrayList<ParseTreeNode>();
		for (Member m : members) {
			membernodes.add(new MemberNode(null, m));
		}
		return generateListSetCall(membernodes);
	}

	private static ParseTreeNode toOlap4jMeasureSet(List<Measure> measures) {
		List<ParseTreeNode> membernodes = new ArrayList<ParseTreeNode>();
		for (Measure m : measures) {
			membernodes.add(new MemberNode(null, m));
		}
		return generateListSetCall(membernodes);
	}

	
	private static List<AxisNode> toOlap4j(List<ParseTreeNode> withList, List<QueryAxis> axes) {
		final ArrayList<AxisNode> axisList = new ArrayList<AxisNode>();
		for (QueryAxis axis : axes) {
			AxisNode axisNode = toOlap4j(withList, axis);
			if (axisNode != null) {
				axisList.add(axisNode);
			}
		}
		return axisList;
	}

	private static IdentifierNode getIdentifier(QueryAxis axis) {
		return IdentifierNode.ofNames("Axis" + axis.getLocation().name());
	}

	private static ParseTreeNode toOlap4j(ParseTreeNode expression, AbstractQueryObject o) {
		MdxParser parser = new DefaultMdxParserImpl();

		if (o.isMdxSetExpression()) {
			expression =  parser.parseExpression("{" + o.getMdxSetExpression() + "}");
		}

		if (o.getFilters().size() > 0) {
			for (IFilterFunction filter : o.getFilters()) {
				expression = filter.visit(parser, expression);
			}
		}
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
					IQuerySet.HierarchizeMode.PRE))
			{
				// In pre mode, we don't add the "POST" literal.
				expression = new CallNode(
						null,
						"Hierarchize",
						Syntax.Function,
						expression);
			} else if (o.getHierarchizeMode().equals(
					IQuerySet.HierarchizeMode.POST))
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










