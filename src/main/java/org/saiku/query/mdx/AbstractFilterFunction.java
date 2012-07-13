package org.saiku.query.mdx;

import java.util.List;

import org.olap4j.mdx.CallNode;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.Syntax;
import org.olap4j.mdx.parser.MdxParser;

public abstract class AbstractFilterFunction implements IFilterFunction {

	@Override
	public ParseTreeNode visit(MdxParser parser, ParseTreeNode parent) {
		List<ParseTreeNode> arguments = getArguments(parser);
		arguments.add(0, parent);		
		return new CallNode(null, getFunctionType().toString(), Syntax.Function, arguments);
	}
}
