package org.saiku.query;

import junit.framework.TestCase;

import org.olap4j.Axis;
import org.olap4j.CellSet;
import org.olap4j.OlapConnection;
import org.olap4j.OlapWrapper;
import org.olap4j.impl.IdentifierParser;
import org.olap4j.mdx.SelectNode;
import org.olap4j.metadata.Catalog;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Measure;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.NamedList;
import org.olap4j.metadata.Schema;
import org.saiku.query.ISortableQuerySet.HierarchizeMode;
import org.saiku.query.QueryDetails.Location;
import org.saiku.query.mdx.GenericFilter;
import org.saiku.query.mdx.IFilterFunction.MdxFunctionType;
import org.saiku.query.mdx.NFilter;
import org.saiku.query.mdx.NameFilter;
import org.saiku.query.mdx.NameLikeFilter;
import org.saiku.query.metadata.CalculatedMeasure;
import org.saiku.query.metadata.CalculatedMember;

public class QueryTest extends TestCase {

	private TestContext context = TestContext.instance();


	public void testSimpleQuery() {
		try {
			Cube cube = getFoodmartCube("Sales");
			Query query = new Query("my query", cube);
			QueryAxis qa = query.getAxis(Axis.COLUMNS);
			
			QueryHierarchy products = query.getHierarchy("[Product]");

			products.includeLevel("Product Family");
			products.includeLevel("Product Category");
			qa.addHierarchy(products);

			
			SelectNode mdx = query.getSelect();
			String mdxString = mdx.toString();
			if (TestContext.DEBUG) {
				System.out.println(TestUtil.toJavaString(mdxString));
			}
			String expectedQuery = 
					"WITH\n"
							+ "SET [~COLUMNS] AS\n"
							+ "    Hierarchize({{[Product].[Product Family].Members}, {[Product].[Product Category].Members}})\n"
							+ "SELECT\n"
							+ "[~COLUMNS] ON COLUMNS\n"
							+ "FROM [Sales]";
			TestUtil.assertEqualsVerbose(expectedQuery, mdxString);

			QueryHierarchy time = query.getHierarchy("[Time.Weekly]");
			time.includeLevel("Week");
			qa.addHierarchy(time);
			
			mdx = query.getSelect();
			mdxString = mdx.toString();
			if (TestContext.DEBUG) {
				System.out.println(TestUtil.toJavaString(mdxString));
			}
			expectedQuery = 
					"WITH\n"
			                + "SET [~COLUMNS_Product_Product] AS\n"
			                + "    Hierarchize({{[Product].[Product Family].Members}, {[Product].[Product Category].Members}})\n"
			                + "SET [~COLUMNS_Time_Time.Weekly] AS\n"
			                + "    {[Time.Weekly].[Week].Members}\n"
			                + "SELECT\n"
			                + "CrossJoin([~COLUMNS_Product_Product], [~COLUMNS_Time_Time.Weekly]) ON COLUMNS\n"
			                + "FROM [Sales]";
			TestUtil.assertEqualsVerbose(expectedQuery, mdxString);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	public void testTopPercentQuery() {
		try {
			Cube cube = getFoodmartCube("Sales");
			Query query = new Query("my query", cube);
			QueryAxis qa = query.getAxis(Axis.COLUMNS);
			qa.setMdxSetExpression("Product.Drink.Children");
			qa.addFilter(new GenericFilter("[Measures].[Unit Sales] > 1"));
			qa.addFilter(new NFilter(MdxFunctionType.TopPercent, 100, "[Measures].[Customer Count]"));
			qa.setHierarchizeMode(HierarchizeMode.PRE);
			SelectNode mdx = query.getSelect();
			String mdxString = mdx.toString();
			if (TestContext.DEBUG) {
				System.out.println(TestUtil.toJavaString(mdxString));
			}
			String expectedQuery = 
							"SELECT\n"
							+ "Hierarchize(TopPercent(Filter({Product.Drink.Children}, ([Measures].[Unit Sales] > 1)), 100, [Measures].[Customer Count])) ON COLUMNS\n"
							+ "FROM [Sales]";
			TestUtil.assertEqualsVerbose(expectedQuery, mdxString);

			CellSet results = query.execute();
			String s = TestUtil.toString(results);
			//	        System.out.println(TestUtil.toJavaString(s));
			TestUtil.assertEqualsVerbose(
					"Axis #0:\n"
							+ "{}\n"
							+ "Axis #1:\n"
							+ "{[Product].[Drink].[Alcoholic Beverages]}\n"
							+ "{[Product].[Drink].[Beverages]}\n"
							+ "{[Product].[Drink].[Dairy]}\n"
							+ "Row #0: 6,838\n"
							+ "Row #0: 13,573\n"
							+ "Row #0: 4,186\n",
							s);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	public void testBasicQueryModel() {

		try {
			Cube cube = getFoodmartCube("Sales");
			Query query = new Query("my query", cube);
			QueryAxis qa = query.getAxis(Axis.COLUMNS);
			QueryHierarchy products = query.getHierarchy("[Product]");

			products.includeLevel("Product Family");
			products.excludeMember("[Product].[Food]");
			products.includeMember("[Product].[Drink].[Beverages]");
			products.includeMember("[Product].[Non-Consumable].[Checkout]");
			qa.addHierarchy(products);

			SelectNode mdx = query.getSelect();
			String mdxString = mdx.toString();
			if (TestContext.DEBUG) {
				System.out.println(TestUtil.toJavaString(mdxString));
			}
			String expectedQuery = 
					"WITH\n"
			                + "SET [~Product_Product_Product Family] AS\n"
			                + "    Except({[Product].[Product Family].Members}, {[Product].[Food]})\n"
			                + "SET [~Product_Product_Product Department] AS\n"
			                + "    Exists({[Product].[Drink].[Beverages], [Product].[Non-Consumable].[Checkout]}, [~Product_Product_Product Family])\n"
			                + "SET [~COLUMNS] AS\n"
			                + "    Hierarchize({[~Product_Product_Product Family], [~Product_Product_Product Department]})\n"
			                + "SELECT\n"
			                + "[~COLUMNS] ON COLUMNS\n"
			                + "FROM [Sales]";
			TestUtil.assertEqualsVerbose(expectedQuery, mdxString);

			CellSet results = query.execute();
			String s = TestUtil.toString(results);
			// System.out.println(TestUtil.toJavaString(s));
			TestUtil.assertEqualsVerbose(
					"Axis #0:\n"
							+ "{}\n"
							+ "Axis #1:\n"
							+ "{[Product].[Drink]}\n"
							+ "{[Product].[Drink].[Beverages]}\n"
							+ "{[Product].[Non-Consumable]}\n"
							+ "{[Product].[Non-Consumable].[Checkout]}\n"
							+ "Row #0: 24,597\n"
							+ "Row #0: 13,573\n"
							+ "Row #0: 50,236\n"
							+ "Row #0: 1,779\n",
							s);


		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	public void testHierarchyConsistency() {

		try {
			Cube cube = getFoodmartCube("Sales");
			Query query = new Query("my query", cube);
			QueryAxis qa = query.getAxis(Axis.COLUMNS);
			QueryHierarchy products = query.getHierarchy("[Product]");

			products.includeLevel("Product Family");
			products.excludeMember("[Product].[Drink]");
			products.includeMember("[Product].[Drink].[Beverages]");
			products.includeMember("[Product].[Non-Consumable].[Checkout]");
			products.includeLevel("Product Category");
			qa.addHierarchy(products);

			SelectNode mdx = query.getSelect();
			String mdxString = mdx.toString();
			if (TestContext.DEBUG) {
				System.out.println(TestUtil.toJavaString(mdxString));
			}
			String expectedQuery = 
					"WITH\n"
			                + "SET [~Product_Product_Product Family] AS\n"
			                + "    Except({[Product].[Product Family].Members}, {[Product].[Drink]})\n"
			                + "SET [~Product_Product_Product Department] AS\n"
			                + "    Exists({[Product].[Drink].[Beverages], [Product].[Non-Consumable].[Checkout]}, [~Product_Product_Product Family])\n"
			                + "SET [~Product_Product_Product Category] AS\n"
			                + "    Exists({[Product].[Product Category].Members}, [~Product_Product_Product Department])\n"
			                + "SET [~COLUMNS] AS\n"
			                + "    Hierarchize({[~Product_Product_Product Family], [~Product_Product_Product Department], [~Product_Product_Product Category]})\n"
			                + "SELECT\n"
			                + "[~COLUMNS] ON COLUMNS\n"
			                + "FROM [Sales]";
			TestUtil.assertEqualsVerbose(expectedQuery, mdxString);

			CellSet results = query.execute();
			String s = TestUtil.toString(results);
			//	        System.out.println(TestUtil.toJavaString(s));
			TestUtil.assertEqualsVerbose(
					"Axis #0:\n"
							+ "{}\n"
							+ "Axis #1:\n"
							+ "{[Product].[Food]}\n"
							+ "{[Product].[Non-Consumable]}\n"
							+ "{[Product].[Non-Consumable].[Checkout]}\n"
							+ "{[Product].[Non-Consumable].[Checkout].[Hardware]}\n"
							+ "{[Product].[Non-Consumable].[Checkout].[Miscellaneous]}\n"
							+ "Row #0: 191,940\n"
							+ "Row #0: 50,236\n"
							+ "Row #0: 1,779\n"
							+ "Row #0: 810\n"
							+ "Row #0: 969\n",
							s);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	public void testLowestLevelsOnly() {

		try {
			Cube cube = getFoodmartCube("Sales");
			Query query = new Query("my query", cube);
			query.setLowestLevelsOnly(true);
			
			QueryAxis columns = query.getAxis(Axis.COLUMNS);
			QueryAxis filter = query.getAxis(Axis.FILTER);
			QueryHierarchy products = query.getHierarchy("[Product]");

			products.includeLevel("Product Family");
			products.excludeMember("[Product].[Drink]");
			products.includeMember("[Product].[Drink].[Beverages]");
			products.includeMember("[Product].[Non-Consumable].[Checkout]");
			products.includeLevel("Product Category");
			columns.addHierarchy(products);
			
			QueryHierarchy time = query.getHierarchy("[Time]");

			time.includeMember("[Time].[1997].[Q1]");
			time.includeLevel("Month");
			
			filter.addHierarchy(time);
			

			SelectNode mdx = query.getSelect();
			String mdxString = mdx.toString();
			if (TestContext.DEBUG) {
				System.out.println(TestUtil.toJavaString(mdxString));
			}
			String expectedQuery = 
					"WITH\n"
			                + "SET [~Time_Time_Quarter] AS\n"
			                + "    {[Time].[1997].[Q1]}\n"
			                + "SET [~Time_Time_Month] AS\n"
			                + "    Exists({[Time].[Month].Members}, [~Time_Time_Quarter])\n"
			                + "SET [~FILTER] AS\n"
			                + "    [~Time_Time_Month]\n"
			                + "SET [~Product_Product_Product Family] AS\n"
			                + "    Except({[Product].[Product Family].Members}, {[Product].[Drink]})\n"
			                + "SET [~Product_Product_Product Department] AS\n"
			                + "    Exists({[Product].[Drink].[Beverages], [Product].[Non-Consumable].[Checkout]}, [~Product_Product_Product Family])\n"
			                + "SET [~Product_Product_Product Category] AS\n"
			                + "    Exists({[Product].[Product Category].Members}, [~Product_Product_Product Department])\n"
			                + "SET [~COLUMNS] AS\n"
			                + "    [~Product_Product_Product Category]\n"
			                + "SELECT\n"
			                + "[~COLUMNS] ON COLUMNS\n"
			                + "FROM [Sales]\n"
			                + "WHERE [~FILTER]";
			TestUtil.assertEqualsVerbose(expectedQuery, mdxString);

			CellSet results = query.execute();
			String s = TestUtil.toString(results);
//				        System.out.println(TestUtil.toJavaString(s));
			TestUtil.assertEqualsVerbose(
					"Axis #0:\n"
			                + "{[Time].[1997].[Q1].[1]}\n"
			                + "{[Time].[1997].[Q1].[2]}\n"
			                + "{[Time].[1997].[Q1].[3]}\n"
			                + "Axis #1:\n"
			                + "{[Product].[Non-Consumable].[Checkout].[Hardware]}\n"
			                + "{[Product].[Non-Consumable].[Checkout].[Miscellaneous]}\n"
			                + "Row #0: 259\n"
			                + "Row #0: 293\n",
							s);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	public void testBasicCalculatedMember() {

		try {
			Cube cube = getFoodmartCube("Sales");
			Query query = new Query("my query", cube);
			QueryAxis columns = query.getAxis(Axis.COLUMNS);
			QueryAxis rows = query.getAxis(Axis.ROWS);
			QueryHierarchy products = query.getHierarchy("[Product]");
			CalculatedMember cm =
					query.createCalculatedMember(
							products, 
							"Consumable", 
							"Aggregate({Product.Drink, Product.Food})",  
							null);

			products.includeCalculatedMember(cm);
			products.includeLevel("Product Family");
			products.excludeMember("[Product].[Non-Consumable]");
			NFilter top2filter = new NFilter(MdxFunctionType.TopCount, 2, "Measures.[Unit Sales]");
			products.addFilter(top2filter);
			columns.addHierarchy(products);

			QueryHierarchy edu = query.getHierarchy("[Education Level]");
			edu.includeLevel("Education Level");
			columns.addHierarchy(edu);

			QueryHierarchy gender = query.getHierarchy("[Gender]");
			gender.includeMember("[Gender].[F]");
			rows.addHierarchy(gender);


			SelectNode mdx = query.getSelect();
			String mdxString = mdx.toString();
			if (TestContext.DEBUG) {
				System.out.println(TestUtil.toJavaString(mdxString));
			}
			String expectedQuery = 
					"WITH\n"
			                + "MEMBER [Product].[Consumable] AS\n"
			                + "    Aggregate({Product.Drink, Product.Food})\n"
			                + "SET [~COLUMNS_Product_Product] AS\n"
			                + "    TopCount({{[Product].[Consumable]}, Except({[Product].[Product Family].Members}, {[Product].[Non-Consumable]})}, 2, Measures.[Unit Sales])\n"
			                + "SET [~COLUMNS_Education Level_Education Level] AS\n"
			                + "    {[Education Level].[Education Level].Members}\n"
			                + "SET [~ROWS] AS\n"
			                + "    {[Gender].[F]}\n"
			                + "SELECT\n"
			                + "CrossJoin([~COLUMNS_Product_Product], [~COLUMNS_Education Level_Education Level]) ON COLUMNS,\n"
			                + "[~ROWS] ON ROWS\n"
			                + "FROM [Sales]";
			TestUtil.assertEqualsVerbose(expectedQuery, mdxString);

			CellSet results = query.execute();
			String s = TestUtil.toString(results);
			//	        System.out.println(TestUtil.toJavaString(s));
			TestUtil.assertEqualsVerbose(
					"Axis #0:\n"
							+ "{}\n"
							+ "Axis #1:\n"
							+ "{[Product].[Consumable], [Education Level].[Bachelors Degree]}\n"
							+ "{[Product].[Consumable], [Education Level].[Graduate Degree]}\n"
							+ "{[Product].[Consumable], [Education Level].[High School Degree]}\n"
							+ "{[Product].[Consumable], [Education Level].[Partial College]}\n"
							+ "{[Product].[Consumable], [Education Level].[Partial High School]}\n"
							+ "{[Product].[Food], [Education Level].[Bachelors Degree]}\n"
							+ "{[Product].[Food], [Education Level].[Graduate Degree]}\n"
							+ "{[Product].[Food], [Education Level].[High School Degree]}\n"
							+ "{[Product].[Food], [Education Level].[Partial College]}\n"
							+ "{[Product].[Food], [Education Level].[Partial High School]}\n"
							+ "Axis #2:\n"
							+ "{[Gender].[F]}\n"
							+ "Row #0: 27,748\n"
							+ "Row #0: 6,747\n"
							+ "Row #0: 30,836\n"
							+ "Row #0: 10,437\n"
							+ "Row #0: 31,248\n"
							+ "Row #0: 24,563\n"
							+ "Row #0: 6,028\n"
							+ "Row #0: 27,254\n"
							+ "Row #0: 9,265\n"
							+ "Row #0: 27,704\n",
							s);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	public void testParentCalculatedMember() {

		try {
			Cube cube = getFoodmartCube("Sales");
			Query query = new Query("my query2", cube);
			QueryAxis columns = query.getAxis(Axis.COLUMNS);
			QueryAxis rows = query.getAxis(Axis.ROWS);
			QueryHierarchy products = query.getHierarchy("[Product]");

			Member parent = 
					cube.lookupMember(IdentifierParser.parseIdentifier("[Product].[Drink]"));

			CalculatedMember cm =
					query.createCalculatedMember(
							products,
							parent,
							"BeverageDairy", 
							"Aggregate({[Product].[Drink].[Beverages], [Product].[Drink].[Dairy]})",  
							null);

			products.includeCalculatedMember(cm);
			columns.addHierarchy(products);

			QueryHierarchy gender = query.getHierarchy("[Gender]");
			gender.includeMember("[Gender].[F]");
			rows.addHierarchy(gender);

			SelectNode mdx = query.getSelect();
			String mdxString = mdx.toString();
			if (TestContext.DEBUG) {
				System.out.println(TestUtil.toJavaString(mdxString));
			}
			String expectedQuery = 
					"WITH\n"
							+ "MEMBER [Product].[Drink].[BeverageDairy] AS\n"
							+ "    Aggregate({[Product].[Drink].[Beverages], [Product].[Drink].[Dairy]})\n"
							+ "SET [~COLUMNS] AS\n"
							+ "    {[Product].[Drink].[BeverageDairy]}\n"
							+ "SET [~ROWS] AS\n"
							+ "    {[Gender].[F]}\n"
							+ "SELECT\n"
							+ "[~COLUMNS] ON COLUMNS,\n"
							+ "[~ROWS] ON ROWS\n"
							+ "FROM [Sales]";
			TestUtil.assertEqualsVerbose(expectedQuery, mdxString);

			CellSet results = query.execute();
			String s = TestUtil.toString(results);
			//	        System.out.println(TestUtil.toJavaString(s));
			TestUtil.assertEqualsVerbose(
					"Axis #0:\n"
							+ "{}\n"
							+ "Axis #1:\n"
							+ "{[Product].[Drink].[BeverageDairy]}\n"
							+ "Axis #2:\n"
							+ "{[Gender].[F]}\n"
							+ "Row #0: 8,763\n",
							s);


		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	public void testCalculatedMeasure() {

		try {
			Cube cube = getFoodmartCube("Sales");
			Query query = new Query("my query2", cube);
			QueryAxis rows = query.getAxis(Axis.ROWS);
			QueryAxis columns = query.getAxis(Axis.COLUMNS);
			QueryHierarchy products = query.getHierarchy("[Product]");
			products.includeMember("[Product].[Drink]");
			rows.addHierarchy(products);

			CalculatedMeasure cm =
					query.createCalculatedMeasure(
							"Double Profit", 
							"( [Measures].[Store Sales] - [Measures].[Store Cost]) * 2",  
							null);

			assertEquals(query.getCalculatedMeasures().size(), 1);
			
			query.getDetails().add(cm);
			
			Measure m = cube.getMeasures().get(0);
			
			query.getDetails().add(m);
			
			SelectNode mdx = query.getSelect();
			String mdxString = mdx.toString();
			if (TestContext.DEBUG) {
				System.out.println(TestUtil.toJavaString(mdxString));
			}
			String expectedQuery = 
	                "WITH\n"
	                        + "MEMBER [Measures].[Double Profit] AS\n"
	                        + "    (([Measures].[Store Sales] - [Measures].[Store Cost]) * 2)\n"
	                        + "SET [~ROWS] AS\n"
	                        + "    {[Product].[Drink]}\n"
	                        + "SELECT\n"
	                        + "{[Measures].[Double Profit], [Measures].[Unit Sales]} ON COLUMNS,\n"
	                        + "[~ROWS] ON ROWS\n"
	                        + "FROM [Sales]";
	                        
			TestUtil.assertEqualsVerbose(expectedQuery, mdxString);

			CellSet results = query.execute();
			String s = TestUtil.toString(results);
			TestUtil.assertEqualsVerbose(
	                "Axis #0:\n"
	                        + "{}\n"
	                        + "Axis #1:\n"
	                        + "{[Measures].[Double Profit]}\n"
	                        + "{[Measures].[Unit Sales]}\n"
	                        + "Axis #2:\n"
	                        + "{[Product].[Drink]}\n"
	                        + "Row #0: 58,717.95\n"
	                        + "Row #0: 24,597\n",
							s);

			
			QueryHierarchy gender = query.getHierarchy("[Gender]");
			gender.includeLevel("Gender");
			columns.addHierarchy(gender);
			mdx = query.getSelect();
			mdxString = mdx.toString();
			expectedQuery = 
		            "WITH\n"
		                    + "SET [~COLUMNS] AS\n"
		                    + "    {[Gender].[Gender].Members}\n"
		                    + "MEMBER [Measures].[Double Profit] AS\n"
		                    + "    (([Measures].[Store Sales] - [Measures].[Store Cost]) * 2)\n"
		                    + "SET [~ROWS] AS\n"
		                    + "    {[Product].[Drink]}\n"
		                    + "SELECT\n"
		                    + "CrossJoin([~COLUMNS], {[Measures].[Double Profit], [Measures].[Unit Sales]}) ON COLUMNS,\n"
		                    + "[~ROWS] ON ROWS\n"
		                    + "FROM [Sales]";
	                        
			TestUtil.assertEqualsVerbose(expectedQuery, mdxString);

			query.getDetails().setLocation(Location.TOP);
			mdx = query.getSelect();
			mdxString = mdx.toString();
			expectedQuery = 
		            "WITH\n"
		                    + "SET [~COLUMNS] AS\n"
		                    + "    {[Gender].[Gender].Members}\n"
		                    + "MEMBER [Measures].[Double Profit] AS\n"
		                    + "    (([Measures].[Store Sales] - [Measures].[Store Cost]) * 2)\n"
		                    + "SET [~ROWS] AS\n"
		                    + "    {[Product].[Drink]}\n"
		                    + "SELECT\n"
		                    + "CrossJoin({[Measures].[Double Profit], [Measures].[Unit Sales]}, [~COLUMNS]) ON COLUMNS,\n"
		                    + "[~ROWS] ON ROWS\n"
		                    + "FROM [Sales]";
	                        
			TestUtil.assertEqualsVerbose(expectedQuery, mdxString);
			
			if (TestContext.DEBUG) {
				System.out.println(TestUtil.toJavaString(mdxString));
			}

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	

	
	public void testFilters() {

		try {
			Cube cube = getFoodmartCube("Sales");
			Query query = new Query("my query2", cube);
			QueryAxis rows = query.getAxis(Axis.ROWS);
			QueryAxis columns = query.getAxis(Axis.COLUMNS);
			QueryHierarchy time = query.getHierarchy("[Time]");
			time.includeLevel("Quarter");
			time.addFilter(new NameFilter(time.getHierarchy(), "Q1", "Q2"));
			
			rows.addHierarchy(time);
			
			QueryHierarchy products = query.getHierarchy("[Product]");
			products.includeMember("[Product].[Drink]");
			columns.addHierarchy(products);
			
			SelectNode mdx = query.getSelect();
			String mdxString = mdx.toString();
			if (TestContext.DEBUG) {
				System.out.println(TestUtil.toJavaString(mdxString));
			}
			String expectedQuery = 
					"WITH\n"
			                + "SET [~COLUMNS] AS\n"
			                + "    {[Product].[Drink]}\n"
			                + "SET [~ROWS] AS\n"
			                + "    Filter({[Time].[Quarter].Members}, (([Time].CurrentMember.Name  =  \"Q1\")  OR  ([Time].CurrentMember.Name  =  \"Q2\")))\n"
			                + "SELECT\n"
			                + "[~COLUMNS] ON COLUMNS,\n"
			                + "[~ROWS] ON ROWS\n"
			                + "FROM [Sales]";
	                        
			TestUtil.assertEqualsVerbose(expectedQuery, mdxString);

			CellSet results = query.execute();
			String s = TestUtil.toString(results);

			TestUtil.assertEqualsVerbose(
					"Axis #0:\n"
			                + "{}\n"
			                + "Axis #1:\n"
			                + "{[Product].[Drink]}\n"
			                + "Axis #2:\n"
			                + "{[Time].[1997].[Q1]}\n"
			                + "{[Time].[1997].[Q2]}\n"
			                + "{[Time].[1998].[Q1]}\n"
			                + "{[Time].[1998].[Q2]}\n"
			                + "Row #0: 5,976\n"
			                + "Row #1: 5,895\n"
			                + "Row #2: \n"
			                + "Row #3: \n",
							s);

			
			if (TestContext.DEBUG) {
				System.out.println(TestUtil.toJavaString(mdxString));
			}

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	public void testNameLikeFilter() {

		try {
			Cube cube = getFoodmartCube("Sales");
			Query query = new Query("my query2", cube);
			QueryAxis rows = query.getAxis(Axis.ROWS);
			rows.setNonEmpty(true);
			QueryAxis columns = query.getAxis(Axis.COLUMNS);
			QueryHierarchy time = query.getHierarchy("[Time]");
			time.includeLevel("Quarter");
			time.includeLevel("Month");
			time.addFilter(new NameLikeFilter(time.getHierarchy(), "Q", "2"));
			
			rows.addHierarchy(time);
			
			QueryHierarchy products = query.getHierarchy("[Product]");
			products.includeMember("[Product].[Drink]");
			columns.addHierarchy(products);
			
			SelectNode mdx = query.getSelect();
			String mdxString = mdx.toString();
			if (TestContext.DEBUG) {
				System.out.println(TestUtil.toJavaString(mdxString));
			}
			String expectedQuery = 
					"WITH\n"
			                + "SET [~COLUMNS] AS\n"
			                + "    {[Product].[Drink]}\n"
			                + "SET [~ROWS] AS\n"
			                + "    Filter(Hierarchize({{[Time].[Quarter].Members}, {[Time].[Month].Members}}), ((Instr([Time].CurrentMember.Name, \"Q\")  >  0)  OR  (Instr([Time].CurrentMember.Name, \"2\")  >  0)))\n"
			                + "SELECT\n"
			                + "[~COLUMNS] ON COLUMNS,\n"
			                + "NON EMPTY [~ROWS] ON ROWS\n"
			                + "FROM [Sales]";
	                        
			TestUtil.assertEqualsVerbose(expectedQuery, mdxString);

			CellSet results = query.execute();
			String s = TestUtil.toString(results);
			TestUtil.assertEqualsVerbose(
					"Axis #0:\n"
			                + "{}\n"
			                + "Axis #1:\n"
			                + "{[Product].[Drink]}\n"
			                + "Axis #2:\n"
			                + "{[Time].[1997].[Q1]}\n"
			                + "{[Time].[1997].[Q1].[2]}\n"
			                + "{[Time].[1997].[Q2]}\n"
			                + "{[Time].[1997].[Q3]}\n"
			                + "{[Time].[1997].[Q4]}\n"
			                + "{[Time].[1997].[Q4].[12]}\n"
			                + "Row #0: 5,976\n"
			                + "Row #1: 1,951\n"
			                + "Row #2: 5,895\n"
			                + "Row #3: 6,065\n"
			                + "Row #4: 6,661\n"
			                + "Row #5: 2,419\n",
							s);

			time.clearFilters();
			QueryLevel month = time.getActiveLevel("Month");
			month.addFilter(new NameLikeFilter(time.getHierarchy(), "2"));
			QueryLevel quarter = time.getActiveLevel("Quarter");
			quarter.addFilter(new NameLikeFilter(time.getHierarchy(), "Q"));
			
			mdx = query.getSelect();
			mdxString = mdx.toString();
			if (TestContext.DEBUG) {
				System.out.println(TestUtil.toJavaString(mdxString));
			}
			expectedQuery = 
					"WITH\n"
			                + "SET [~COLUMNS] AS\n"
			                + "    {[Product].[Drink]}\n"
			                + "SET [~Time_Time_Quarter] AS\n"
			                + "    Filter({[Time].[Quarter].Members}, (Instr([Time].CurrentMember.Name, \"Q\")  >  0))\n"
			                + "SET [~Time_Time_Month] AS\n"
			                + "    Exists(Filter({[Time].[Month].Members}, (Instr([Time].CurrentMember.Name, \"2\")  >  0)), [~Time_Time_Quarter])\n"
			                + "SET [~ROWS] AS\n"
			                + "    Hierarchize({[~Time_Time_Quarter], [~Time_Time_Month]})\n"
			                + "SELECT\n"
			                + "[~COLUMNS] ON COLUMNS,\n"
			                + "NON EMPTY [~ROWS] ON ROWS\n"
			                + "FROM [Sales]";
	                        
			TestUtil.assertEqualsVerbose(expectedQuery, mdxString);

			results = query.execute();
			s = TestUtil.toString(results);
			TestUtil.assertEqualsVerbose(
					"Axis #0:\n"
			                + "{}\n"
			                + "Axis #1:\n"
			                + "{[Product].[Drink]}\n"
			                + "Axis #2:\n"
			                + "{[Time].[1997].[Q1]}\n"
			                + "{[Time].[1997].[Q1].[2]}\n"
			                + "{[Time].[1997].[Q2]}\n"
			                + "{[Time].[1997].[Q3]}\n"
			                + "{[Time].[1997].[Q4]}\n"
			                + "{[Time].[1997].[Q4].[12]}\n"
			                + "Row #0: 5,976\n"
			                + "Row #1: 1,951\n"
			                + "Row #2: 5,895\n"
			                + "Row #3: 6,065\n"
			                + "Row #4: 6,661\n"
			                + "Row #5: 2,419\n",
							s);

			

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	public void testVisualTotals() {

		try {
			Cube cube = getFoodmartCube("Sales");
			Query query = new Query("my query2", cube);
			QueryAxis rows = query.getAxis(Axis.ROWS);
			rows.setNonEmpty(true);
			QueryAxis columns = query.getAxis(Axis.COLUMNS);
			
			QueryHierarchy time = query.getHierarchy("[Time]");
			QueryLevel quarter = time.includeLevel("Quarter");
			QueryLevel month = time.includeLevel("Month");
			
			month.addFilter(new NameLikeFilter(time.getHierarchy(), "2"));
			quarter.addFilter(new NameLikeFilter(time.getHierarchy(), "Q"));
			
			time.setVisualTotals(true);
			rows.addHierarchy(time);
			
			QueryHierarchy products = query.getHierarchy("[Product]");
			products.includeMember("[Product].[Drink]");
			columns.addHierarchy(products);
			
			SelectNode mdx = query.getSelect();
			String mdxString = mdx.toString();
			if (TestContext.DEBUG) {
				System.out.println(TestUtil.toJavaString(mdxString));
			}
			String expectedQuery = 
					"WITH\n"
			                + "SET [~COLUMNS] AS\n"
			                + "    {[Product].[Drink]}\n"
			                + "SET [~Time_Time_Quarter] AS\n"
			                + "    Filter({[Time].[Quarter].Members}, (Instr([Time].CurrentMember.Name, \"Q\")  >  0))\n"
			                + "SET [~Time_Time_Month] AS\n"
			                + "    Exists(Filter({[Time].[Month].Members}, (Instr([Time].CurrentMember.Name, \"2\")  >  0)), [~Time_Time_Quarter])\n"
			                + "SET [~ROWS] AS\n"
			                + "    VisualTotals(Hierarchize({[~Time_Time_Quarter], [~Time_Time_Month]}))\n"
			                + "SELECT\n"
			                + "[~COLUMNS] ON COLUMNS,\n"
			                + "NON EMPTY [~ROWS] ON ROWS\n"
			                + "FROM [Sales]";
	                        
			TestUtil.assertEqualsVerbose(expectedQuery, mdxString);

			CellSet results = query.execute();
			String s = TestUtil.toString(results);
			TestUtil.assertEqualsVerbose(
					"Axis #0:\n"
			                + "{}\n"
			                + "Axis #1:\n"
			                + "{[Product].[Drink]}\n"
			                + "Axis #2:\n"
			                + "{[Time].[1997].[Q1]}\n"
			                + "{[Time].[1997].[Q1].[2]}\n"
			                + "{[Time].[1997].[Q2]}\n"
			                + "{[Time].[1997].[Q3]}\n"
			                + "{[Time].[1997].[Q4]}\n"
			                + "{[Time].[1997].[Q4].[12]}\n"
			                + "Row #0: 1,951\n"
			                + "Row #1: 1,951\n"
			                + "Row #2: 5,895\n"
			                + "Row #3: 6,065\n"
			                + "Row #4: 2,419\n"
			                + "Row #5: 2,419\n",
							s);

			

			time.setVisualTotalsPattern("Total - *");
			mdx = query.getSelect();
			mdxString = mdx.toString();
			if (TestContext.DEBUG) {
				System.out.println(TestUtil.toJavaString(mdxString));
			}
			expectedQuery = 
					"WITH\n"
			                + "SET [~COLUMNS] AS\n"
			                + "    {[Product].[Drink]}\n"
			                + "SET [~Time_Time_Quarter] AS\n"
			                + "    Filter({[Time].[Quarter].Members}, (Instr([Time].CurrentMember.Name, \"Q\")  >  0))\n"
			                + "SET [~Time_Time_Month] AS\n"
			                + "    Exists(Filter({[Time].[Month].Members}, (Instr([Time].CurrentMember.Name, \"2\")  >  0)), [~Time_Time_Quarter])\n"
			                + "SET [~ROWS] AS\n"
			                + "    VisualTotals(Hierarchize({[~Time_Time_Quarter], [~Time_Time_Month]}), \"Total - *\")\n"
			                + "SELECT\n"
			                + "[~COLUMNS] ON COLUMNS,\n"
			                + "NON EMPTY [~ROWS] ON ROWS\n"
			                + "FROM [Sales]";
	                        
			TestUtil.assertEqualsVerbose(expectedQuery, mdxString);

			results = query.execute();
			s = TestUtil.toString(results);
			TestUtil.assertEqualsVerbose(
					"Axis #0:\n"
			                + "{}\n"
			                + "Axis #1:\n"
			                + "{[Product].[Drink]}\n"
			                + "Axis #2:\n"
			                + "{[Time].[1997].[Total - Q1]}\n"
			                + "{[Time].[1997].[Q1].[2]}\n"
			                + "{[Time].[1997].[Q2]}\n"
			                + "{[Time].[1997].[Q3]}\n"
			                + "{[Time].[1997].[Total - Q4]}\n"
			                + "{[Time].[1997].[Q4].[12]}\n"
			                + "Row #0: 1,951\n"
			                + "Row #1: 1,951\n"
			                + "Row #2: 5,895\n"
			                + "Row #3: 6,065\n"
			                + "Row #4: 2,419\n"
			                + "Row #5: 2,419\n",
							s);

			
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	public void testRange() {

		try {
			Cube cube = getFoodmartCube("Sales");
			Query query = new Query("my query", cube);
			QueryAxis rows = query.getAxis(Axis.ROWS);
			QueryAxis columns = query.getAxis(Axis.COLUMNS);
			
			QueryHierarchy time = query.getHierarchy("[Time]");
			Level years = time.getHierarchy().getLevels().get(0);
			Member start = years.getMembers().get(0);
			Member end = years.getMembers().get(1);			
			time.includeRange(start, end);
			
			rows.addHierarchy(time);
			
			QueryHierarchy products = query.getHierarchy("[Product]");
			products.includeMember("[Product].[Drink]");
			columns.addHierarchy(products);
			
			SelectNode mdx = query.getSelect();
			String mdxString = mdx.toString();
			if (TestContext.DEBUG) {
				System.out.println(TestUtil.toJavaString(mdxString));
			}
			String expectedQuery = 
					"WITH\n"
			                + "SET [~COLUMNS] AS\n"
			                + "    {[Product].[Drink]}\n"
			                + "SET [~ROWS] AS\n"
			                + "    ([Time].[1997] : [Time].[1998])\n"
			                + "SELECT\n"
			                + "[~COLUMNS] ON COLUMNS,\n"
			                + "[~ROWS] ON ROWS\n"
			                + "FROM [Sales]";
	                        
			TestUtil.assertEqualsVerbose(expectedQuery, mdxString);

			CellSet results = query.execute();
			String s = TestUtil.toString(results);

			TestUtil.assertEqualsVerbose(
					"Axis #0:\n"
			                + "{}\n"
			                + "Axis #1:\n"
			                + "{[Product].[Drink]}\n"
			                + "Axis #2:\n"
			                + "{[Time].[1997]}\n"
			                + "{[Time].[1998]}\n"
			                + "Row #0: 24,597\n"
			                + "Row #1: \n",
							s);

			time.includeMember("[Time].[1997].[Q1]");
			mdx = query.getSelect();
			mdxString = mdx.toString();
			if (TestContext.DEBUG) {
				System.out.println(TestUtil.toJavaString(mdxString));
			}
			
			expectedQuery =
					"WITH\n"
			                + "SET [~COLUMNS] AS\n"
			                + "    {[Product].[Drink]}\n"
			                + "SET [~Time_Time_Year] AS\n"
			                + "    ([Time].[1997] : [Time].[1998])\n"
			                + "SET [~Time_Time_Quarter] AS\n"
			                + "    Exists({[Time].[1997].[Q1]}, [~Time_Time_Year])\n"
			                + "SET [~ROWS] AS\n"
			                + "    Hierarchize({[~Time_Time_Year], [~Time_Time_Quarter]})\n"
			                + "SELECT\n"
			                + "[~COLUMNS] ON COLUMNS,\n"
			                + "[~ROWS] ON ROWS\n"
			                + "FROM [Sales]";
			
			TestUtil.assertEqualsVerbose(expectedQuery, mdxString);
			
			results = query.execute();
			s = TestUtil.toString(results);

			TestUtil.assertEqualsVerbose(
					"Axis #0:\n"
			                + "{}\n"
			                + "Axis #1:\n"
			                + "{[Product].[Drink]}\n"
			                + "Axis #2:\n"
			                + "{[Time].[1997]}\n"
			                + "{[Time].[1997].[Q1]}\n"
			                + "{[Time].[1998]}\n"
			                + "Row #0: 24,597\n"
			                + "Row #1: 5,976\n"
			                + "Row #2: \n",
							s);

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	public void testRangeExp() {

		try {
			Cube cube = getFoodmartCube("Sales");
			Query query = new Query("my query", cube);
			QueryAxis rows = query.getAxis(Axis.ROWS);
			QueryAxis columns = query.getAxis(Axis.COLUMNS);
			
			QueryHierarchy time = query.getHierarchy("[Time]");
			QueryLevel quarterLevel = time.includeLevel("Year");
						
			quarterLevel.setRangeExpressions("[Time].[1998].Lag(1)", "[Time].[1998]");
			
			rows.addHierarchy(time);
			
			QueryHierarchy products = query.getHierarchy("[Product]");
			products.includeMember("[Product].[Drink]");
			columns.addHierarchy(products);
			
			SelectNode mdx = query.getSelect();
			String mdxString = mdx.toString();
			if (TestContext.DEBUG) {
				System.out.println(TestUtil.toJavaString(mdxString));
			}
			String expectedQuery = 
					"WITH\n"
			                + "SET [~COLUMNS] AS\n"
			                + "    {[Product].[Drink]}\n"
			                + "SET [~ROWS] AS\n"
			                + "    {([Time].[1998].Lag(1) : [Time].[1998])}\n"
			                + "SELECT\n"
			                + "[~COLUMNS] ON COLUMNS,\n"
			                + "[~ROWS] ON ROWS\n"
			                + "FROM [Sales]";
	                        
			TestUtil.assertEqualsVerbose(expectedQuery, mdxString);

			CellSet results = query.execute();
			String s = TestUtil.toString(results);

			TestUtil.assertEqualsVerbose(
					"Axis #0:\n"
			                + "{}\n"
			                + "Axis #1:\n"
			                + "{[Product].[Drink]}\n"
			                + "Axis #2:\n"
			                + "{[Time].[1997]}\n"
			                + "{[Time].[1998]}\n"
			                + "Row #0: 24,597\n"
			                + "Row #1: \n",
							s);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	
	public void testAllLevel() {
		try {
		Cube cube = getFoodmartCube("Sales");
		Query query = new Query("all Level", cube);
		QueryAxis rows = query.getAxis(Axis.ROWS);
		
		QueryHierarchy store = query.getHierarchy("[Store]");
		Level allStores = store.getHierarchy().getLevels().get(0);
		store.includeLevel(allStores);
		rows.addHierarchy(store);
		
		QueryHierarchy products = query.getHierarchy("[Product]");
		products.includeLevel("(All)");
		rows.addHierarchy(products);
		
		SelectNode mdx = query.getSelect();
		String mdxString = mdx.toString();
		if (TestContext.DEBUG) {
			System.out.println(TestUtil.toJavaString(mdxString));
		}
		String expectedQuery = 
				"WITH\n"
		                + "SET [~ROWS_Store_Store] AS\n"
		                + "    {[Store].[All Stores]}\n"
		                + "SET [~ROWS_Product_Product] AS\n"
		                + "    {[Product].[All Products]}\n"
		                + "SELECT\n"
		                + "CrossJoin([~ROWS_Store_Store], [~ROWS_Product_Product]) ON ROWS\n"
		                + "FROM [Sales]";
                        
		TestUtil.assertEqualsVerbose(expectedQuery, mdxString);

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	public void testMidComplexLevel() {
		try {
		Cube cube = getFoodmartCube("Sales");
		Query query = new Query("mid complex Level", cube);
		QueryAxis rows = query.getAxis(Axis.ROWS);
		QueryAxis columns = query.getAxis(Axis.COLUMNS);
		
		QueryHierarchy store = query.getHierarchy("[Store]");
		Level allStores = store.getHierarchy().getLevels().get(0);
		store.includeLevel(allStores);
		columns.addHierarchy(store);
		
		QueryHierarchy products = query.getHierarchy("[Product]");
		products.includeLevel("(All)");
		products.includeLevel("Product Family");
		products.includeMember("[Product].[Food].[Baking Goods]");
		products.includeLevel("Product Category");
		rows.addHierarchy(products);
		
		SelectNode mdx = query.getSelect();
		String mdxString = mdx.toString();
		if (TestContext.DEBUG) {
			System.out.println(TestUtil.toJavaString(mdxString));
		}
		String expectedQuery = 
				"WITH\n"
		                + "SET [~COLUMNS] AS\n"
		                + "    {[Store].[All Stores]}\n"
		                + "SET [~Product_Product_(All)] AS\n"
		                + "    {[Product].[All Products]}\n"
		                + "SET [~Product_Product_Product Family] AS\n"
		                + "    Exists({[Product].[Product Family].Members}, [~Product_Product_Product Department])\n"
		                + "SET [~Product_Product_Product Department] AS\n"
		                + "    {[Product].[Food].[Baking Goods]}\n"
		                + "SET [~Product_Product_Product Category] AS\n"
		                + "    Exists({[Product].[Product Category].Members}, [~Product_Product_Product Department])\n"
		                + "SET [~ROWS] AS\n"
		                + "    Hierarchize({[~Product_Product_(All)], [~Product_Product_Product Family], [~Product_Product_Product Department], [~Product_Product_Product Category]})\n"
		                + "SELECT\n"
		                + "[~COLUMNS] ON COLUMNS,\n"
		                + "[~ROWS] ON ROWS\n"
		                + "FROM [Sales]";
                        
		TestUtil.assertEqualsVerbose(expectedQuery, mdxString);
		
		CellSet results = query.execute();
		String s = TestUtil.toString(results);
		TestUtil.assertEqualsVerbose(
				 "Axis #0:\n"
			                + "{}\n"
			                + "Axis #1:\n"
			                + "{[Store].[All Stores]}\n"
			                + "Axis #2:\n"
			                + "{[Product].[All Products]}\n"
			                + "{[Product].[Food]}\n"
			                + "{[Product].[Food].[Baking Goods]}\n"
			                + "{[Product].[Food].[Baking Goods].[Baking Goods]}\n"
			                + "{[Product].[Food].[Baking Goods].[Jams and Jellies]}\n"
			                + "Row #0: 266,773\n"
			                + "Row #1: 191,940\n"
			                + "Row #2: 20,245\n"
			                + "Row #3: 8,357\n"
			                + "Row #4: 11,888\n",
						s);

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	public void testQMParameter() {
		try {
		Cube cube = getFoodmartCube("Sales");
		Query query = new Query("mid complex Level", cube);
		QueryAxis rows = query.getAxis(Axis.ROWS);
		QueryAxis columns = query.getAxis(Axis.COLUMNS);
		
		QueryHierarchy store = query.getHierarchy("[Store]");
		Level allStores = store.getHierarchy().getLevels().get(0);
		store.includeLevel(allStores);
		columns.addHierarchy(store);
		
		QueryHierarchy products = query.getHierarchy("[Product]");
		products.includeLevel("(All)");
		QueryLevel family = products.includeLevel("Product Family");
		family.setParameterName("FamilyParameter");
		QueryLevel category = products.includeLevel("Product Category");
		category.setParameterName("CategoryParameter");
		rows.addHierarchy(products);
		
		query.setParameter("FamilyParameter", "Product.Food");
		
		SelectNode mdx = query.getSelect();
		String mdxString = mdx.toString();
		
		if (TestContext.DEBUG) {
			System.out.println(TestUtil.toJavaString(mdxString));
		}
		String expectedQuery = 
				"WITH\n"
		                + "SET [~COLUMNS] AS\n"
		                + "    {[Store].[All Stores]}\n"
		                + "SET [~Product_Product_(All)] AS\n"
		                + "    {[Product].[All Products]}\n"
		                + "SET [~Product_Product_Product Family] AS\n"
		                + "    {[Product].[Food]}\n"
		                + "SET [~Product_Product_Product Category] AS\n"
		                + "    Exists({[Product].[Product Category].Members}, [~Product_Product_Product Family])\n"
		                + "SET [~ROWS] AS\n"
		                + "    Hierarchize({[~Product_Product_(All)], [~Product_Product_Product Family], [~Product_Product_Product Category]})\n"
		                + "SELECT\n"
		                + "[~COLUMNS] ON COLUMNS,\n"
		                + "[~ROWS] ON ROWS\n"
		                + "FROM [Sales]";
                        
		TestUtil.assertEqualsVerbose(expectedQuery, mdxString);
		
		query.setParameter("FamilyParameter", "Non-Consumable");
		
		
		String mdxString2 = query.getMdx();
		String expectedQuery2 = 
				"WITH\n"
		                + "SET [~COLUMNS] AS\n"
		                + "    {[Store].[All Stores]}\n"
		                + "SET [~Product_Product_(All)] AS\n"
		                + "    {[Product].[All Products]}\n"
		                + "SET [~Product_Product_Product Family] AS\n"
		                + "    {[Product].[Non-Consumable]}\n"
		                + "SET [~Product_Product_Product Category] AS\n"
		                + "    Exists({[Product].[Product Category].Members}, [~Product_Product_Product Family])\n"
		                + "SET [~ROWS] AS\n"
		                + "    Hierarchize({[~Product_Product_(All)], [~Product_Product_Product Family], [~Product_Product_Product Category]})\n"
		                + "SELECT\n"
		                + "[~COLUMNS] ON COLUMNS,\n"
		                + "[~ROWS] ON ROWS\n"
		                + "FROM [Sales]";
                        
		TestUtil.assertEqualsVerbose(expectedQuery2, mdxString2);
		
		query.setParameter("FamilyParameter", null);
		query.setParameter("CategoryParameter", "Product.Drink.Dairy");
		
		String mdxString3 = query.getMdx();
		String expectedQuery3 = 
            "WITH\n"
            + "SET [~COLUMNS] AS\n"
            + "    {[Store].[All Stores]}\n"
            + "SET [~Product_Product_(All)] AS\n"
            + "    {[Product].[All Products]}\n"
            + "SET [~Product_Product_Product Family] AS\n"
            + "    Exists({[Product].[Product Family].Members}, [~Product_Product_Product Category])\n"
            + "SET [~Product_Product_Product Category] AS\n"
            + "    {[Product].[Drink].[Dairy]}\n"
            + "SET [~ROWS] AS\n"
            + "    Hierarchize({[~Product_Product_(All)], [~Product_Product_Product Family], [~Product_Product_Product Category]})\n"
            + "SELECT\n"
            + "[~COLUMNS] ON COLUMNS,\n"
            + "[~ROWS] ON ROWS\n"
            + "FROM [Sales]";
		TestUtil.assertEqualsVerbose(expectedQuery3, mdxString3);
		

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	public Cube getFoodmartCube(String cubeName) throws Exception {
		OlapConnection connection = context.createConnection();
		final OlapWrapper wrapper = connection;
		OlapConnection olapConnection = (OlapConnection) wrapper.unwrap(OlapConnection.class);
		Catalog catalog = olapConnection.getOlapCatalogs().get("FoodMart");
		NamedList<Schema> schemas = catalog.getSchemas();
		if (schemas.size() == 0) {
			return null;
		}

		// Use the first schema
		Schema schema = schemas.get(0);

		// Get a list of cube objects and dump their names
		NamedList<Cube> cubes = schema.getCubes();

		if (cubes.size() == 0) {
			// no cubes where present
			return null;
		}

		// take the first cube
		return cubes.get(cubeName);
	}
}
