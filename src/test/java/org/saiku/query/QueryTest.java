package org.saiku.query;

import junit.framework.TestCase;

import org.olap4j.Axis;
import org.olap4j.CellSet;
import org.olap4j.OlapConnection;
import org.olap4j.OlapException;
import org.olap4j.OlapWrapper;
import org.olap4j.impl.IdentifierParser;
import org.olap4j.mdx.SelectNode;
import org.olap4j.metadata.Catalog;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.NamedList;
import org.olap4j.metadata.Schema;
import org.saiku.query.IQuerySet.HierarchizeMode;
import org.saiku.query.mdx.GenericFilter;
import org.saiku.query.mdx.IFilterFunction.MdxFunctionType;
import org.saiku.query.mdx.NFilter;
import org.saiku.query.metadata.CalculatedMember;

public class QueryTest extends TestCase {

	private TestContext context = TestContext.instance();


	private OlapConnection connection;

	public void testConnection() {
		OlapConnection con = context.createConnection();
		try {
			assertEquals(1, con.getOlapCatalogs().size());
			assertEquals("FoodMart", con.getOlapCatalogs().get(0).getName());
		} catch (OlapException e) {
			e.printStackTrace();
			fail();
		}
	}

	public void testBasicQuery() {

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
				System.out.println(TestContext.toJavaString(mdxString));
			}
			String expectedQuery = 
					"WITH\n"
							+ "SET [AxisCOLUMNS] AS\n"
							+ "    Hierarchize(TopPercent(Filter({Product.Drink.Children}, ([Measures].[Unit Sales] > 1)), 100, [Measures].[Customer Count]))\n"
							+ "SELECT\n"
							+ "[AxisCOLUMNS] ON COLUMNS\n"
							+ "FROM [Sales]";
			TestContext.assertEqualsVerbose(expectedQuery, mdxString);

			CellSet results = query.execute();
			String s = TestContext.toString(results);
			//	        System.out.println(TestContext.toJavaString(s));
			TestContext.assertEqualsVerbose(
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
			QueryHierarchy products = query.getHierarchy("Product");

			products.includeLevel("Product Family");
			products.exclude("[Product].[Food]");
			products.include("[Product].[Drink].[Beverages]");
			products.include("[Product].[Non-Consumable].[Checkout]");
			qa.addHierarchy(products);

			SelectNode mdx = query.getSelect();
			String mdxString = mdx.toString();
			if (TestContext.DEBUG) {
				System.out.println(TestContext.toJavaString(mdxString));
			}
			String expectedQuery = 
					"WITH\n"
							+ "SET [AxisCOLUMNS] AS\n"
							+ "    Hierarchize(Except(Exists({[Product].[Product Family].Members, [Product].[Product Department].Members}, {[Product].[Drink].[Beverages], [Product].[Non-Consumable].[Checkout]}), {[Product].[Food]}))\n"
							+ "SELECT\n"
							+ "[AxisCOLUMNS] ON COLUMNS\n"
							+ "FROM [Sales]";
			TestContext.assertEqualsVerbose(expectedQuery, mdxString);

			CellSet results = query.execute();
			String s = TestContext.toString(results);
			//	        System.out.println(TestContext.toJavaString(s));
			TestContext.assertEqualsVerbose(
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

	public void testBasicCalculatedMember() {

		try {
			Cube cube = getFoodmartCube("Sales");
			Query query = new Query("my query", cube);
			QueryAxis columns = query.getAxis(Axis.COLUMNS);
			QueryAxis rows = query.getAxis(Axis.ROWS);
			QueryHierarchy products = query.getHierarchy("Product");
			CalculatedMember cm =
					query.createCalculatedMember(
							products, 
							"Consumable", 
							"Aggregate({Product.Drink, Product.Food})",  
							null);

			products.includeCalculatedMember(cm);
			products.includeLevel("Product Family");
			products.exclude("[Product].[Non-Consumable]");
			NFilter top2filter = new NFilter(MdxFunctionType.TopCount, 2, "Measures.[Unit Sales]");
			products.addFilter(top2filter);
			columns.addHierarchy(products);

			QueryHierarchy edu = query.getHierarchy("Education Level");
			edu.includeLevel("Education Level");
			columns.addHierarchy(edu);

			QueryHierarchy gender = query.getHierarchy("Gender");
			gender.include("[Gender].[F]");
			rows.addHierarchy(gender);


			SelectNode mdx = query.getSelect();
			String mdxString = mdx.toString();
			if (TestContext.DEBUG) {
				System.out.println(TestContext.toJavaString(mdxString));
			}
			String expectedQuery = 
					"WITH\n"
							+ "MEMBER [Product].[Consumable] AS\n"
							+ "    Aggregate({Product.Drink, Product.Food})\n"
							+ "SET [AxisCOLUMNS] AS\n"
							+ "    CrossJoin(TopCount({{[Product].[Consumable]}, Except({[Product].[Product Family].Members}, {[Product].[Non-Consumable]})}, 2, Measures.[Unit Sales]), {[Education Level].[Education Level].Members})\n"
							+ "SET [AxisROWS] AS\n"
							+ "    {[Gender].[F]}\n"
							+ "SELECT\n"
							+ "[AxisCOLUMNS] ON COLUMNS,\n"
							+ "[AxisROWS] ON ROWS\n"
							+ "FROM [Sales]";
			TestContext.assertEqualsVerbose(expectedQuery, mdxString);

			CellSet results = query.execute();
			String s = TestContext.toString(results);
			//	        System.out.println(TestContext.toJavaString(s));
			TestContext.assertEqualsVerbose(
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
			QueryHierarchy products = query.getHierarchy("Product");

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

			QueryHierarchy gender = query.getHierarchy("Gender");
			gender.include("[Gender].[F]");
			rows.addHierarchy(gender);

			SelectNode mdx = query.getSelect();
			String mdxString = mdx.toString();
			if (TestContext.DEBUG) {
				System.out.println(TestContext.toJavaString(mdxString));
			}
			String expectedQuery = 
					"WITH\n"
							+ "MEMBER [Product].[Drink].[BeverageDairy] AS\n"
							+ "    Aggregate({[Product].[Drink].[Beverages], [Product].[Drink].[Dairy]})\n"
							+ "SET [AxisCOLUMNS] AS\n"
							+ "    {[Product].[Drink].[BeverageDairy]}\n"
							+ "SET [AxisROWS] AS\n"
							+ "    {[Gender].[F]}\n"
							+ "SELECT\n"
							+ "[AxisCOLUMNS] ON COLUMNS,\n"
							+ "[AxisROWS] ON ROWS\n"
							+ "FROM [Sales]";
			TestContext.assertEqualsVerbose(expectedQuery, mdxString);

			CellSet results = query.execute();
			String s = TestContext.toString(results);
			//	        System.out.println(TestContext.toJavaString(s));
			TestContext.assertEqualsVerbose(
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

	public Cube getFoodmartCube(String cubeName) throws Exception {
		connection = context.createConnection();
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
