package org.saiku.query.mdx;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.olap4j.mdx.LiteralNode;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.parser.MdxParser;

public class NFilter extends AbstractFilterFunction {

	private String filterExpression;
	private int n;
	private MdxFunctionType type;

	public NFilter(MdxFunctionType type, int n,  String filterExpression) {
		if (MdxFunctionType.Filter.equals(type)) {
			throw new IllegalArgumentException("Cannot use Filter() as TopN Filter");
		}
		this.filterExpression = filterExpression;
		this.n = n;
		this.type = type;
	}
	
	public int getN() {
		return n;
	}

	public String getFilterExpression() {
		return filterExpression;
	}

	@Override
	public List<ParseTreeNode> getArguments(MdxParser parser) {
		List<ParseTreeNode> arguments = new ArrayList<ParseTreeNode>();
		ParseTreeNode nfilter =  LiteralNode.createNumeric(null, new BigDecimal(n), false);
		arguments.add(nfilter);
		if (filterExpression != null) {
			ParseTreeNode topn =  parser.parseExpression(filterExpression);
			arguments.add(topn);
		}
		
		return arguments;
	}

	@Override
	public MdxFunctionType getFunctionType() {
		return type;
	}
}
