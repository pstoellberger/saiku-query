package org.saiku.query.mdx;

import java.util.List;

import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.parser.MdxParser;


public interface IFilterFunction {
	
	public MdxFunctionType getFunctionType();
	public List<ParseTreeNode> getArguments(MdxParser parser);	
	public ParseTreeNode visit(MdxParser parser, ParseTreeNode parent);

	public enum MdxFunctionType {
		Filter,
		TopCount,
		TopPercent,
		TopSum,
		BottomCount,
		BottomPercent,
		BottomSum;
	}
}
