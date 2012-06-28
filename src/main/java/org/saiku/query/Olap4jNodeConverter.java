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
import org.olap4j.mdx.MemberNode;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.SelectNode;
import org.olap4j.mdx.Syntax;
import org.olap4j.mdx.WithSetNode;

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
        ParseTreeNode axisNode = null;
        if (axis.isMdxSetExpression()) {
        	ParseTreeNode expression = new CallNode(null, axis.getMdxSetExpression() , Syntax.Infix, new ArrayList<ParseTreeNode>());
        	WithSetNode withNode = new WithSetNode(null, getIdentifier(axis), expression);
        	withList.add(withNode);
        	axisNode = withNode.getIdentifier();
        }

        return new AxisNode(
            null,
            axis.isNonEmpty(),
            axis.getLocation(),
            new ArrayList<IdentifierNode>(),
            axisNode);
    }

    private static List<AxisNode> toOlap4j(List<ParseTreeNode> withList, List<QueryAxis> axes) {
        final ArrayList<AxisNode> axisList = new ArrayList<AxisNode>();
        for (QueryAxis axis : axes) {
            AxisNode axisNode = toOlap4j(withList, axis);
            if (axisNode != null) {
                axisList.add(toOlap4j(withList, axis));
            }
        }
        return axisList;
    }
    
    private static IdentifierNode getIdentifier(QueryAxis axis) {
    	return IdentifierNode.ofNames("Axis" + axis.getLocation().name());
    }
}










