package org.saiku.query.mdx;

import java.util.ArrayList;
import java.util.List;

import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.parser.MdxParser;

public class GenericFilter extends AbstractFilterFunction {

	private String filterExpression;
	private MdxFunctionType type;

	public GenericFilter(String filterExpression) {
		this.filterExpression = filterExpression;
		this.type = MdxFunctionType.Filter;
	}
	
	public String getFilterExpression() {
		return filterExpression;
	}

	@Override
	public List<ParseTreeNode> getArguments(MdxParser parser) {
		List<ParseTreeNode> arguments = new ArrayList<ParseTreeNode>();
		ParseTreeNode filterExp =  parser.parseExpression(filterExpression);		
		arguments.add(filterExp);
		return arguments;
	}

	@Override
	public MdxFunctionType getFunctionType() {
		return type;
	}
}
