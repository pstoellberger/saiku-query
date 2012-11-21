package org.saiku.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.olap4j.mdx.CallNode;
import org.olap4j.mdx.IdentifierNode;
import org.olap4j.mdx.MemberNode;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.PropertyValueNode;
import org.olap4j.mdx.Syntax;
import org.olap4j.mdx.WithMemberNode;
import org.olap4j.mdx.parser.MdxParser;
import org.olap4j.mdx.parser.impl.DefaultMdxParserImpl;
import org.olap4j.metadata.Measure;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.Property;
import org.saiku.query.metadata.Calculated;

public class NodeConverter {
	
	protected static CallNode generateSetCall(ParseTreeNode... args) {
		return
				new CallNode(
						null,
						"{}",
						Syntax.Braces,
						args);
	}

	protected static CallNode generateListSetCall(List<ParseTreeNode> cnodes) {
		return
				new CallNode(
						null,
						"{}",
						Syntax.Braces,
						cnodes);
	}

	protected static CallNode generateListTupleCall(List<ParseTreeNode> cnodes) {
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
	
	
	protected static ParseTreeNode toOlap4jMemberSet(List<Member> members) {
		List<ParseTreeNode> membernodes = new ArrayList<ParseTreeNode>();
		for (Member m : members) {
			membernodes.add(new MemberNode(null, m));
		}
		return generateListSetCall(membernodes);
	}

	protected static ParseTreeNode toOlap4jMeasureSet(List<Measure> measures) {
		List<ParseTreeNode> membernodes = new ArrayList<ParseTreeNode>();
		for (Measure m : measures) {
			membernodes.add(new MemberNode(null, m));
		}
		return generateListSetCall(membernodes);
	}
	
	
	protected static WithMemberNode toOlap4jCalculatedMember(Calculated cm) {
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
	
	protected static IdentifierNode getIdentifier(QueryAxis axis) {
		return IdentifierNode.ofNames("Axis" + axis.getLocation().name());
	}




}