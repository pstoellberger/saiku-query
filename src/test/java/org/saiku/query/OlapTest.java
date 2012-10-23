package org.saiku.query;

import junit.framework.TestCase;

import org.olap4j.Axis;
import org.olap4j.CellSet;
import org.olap4j.OlapConnection;
import org.olap4j.OlapException;
import org.olap4j.OlapWrapper;
import org.olap4j.mdx.SelectNode;
import org.olap4j.metadata.Catalog;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.NamedList;
import org.olap4j.metadata.Schema;
import org.saiku.query.IQuerySet.HierarchizeMode;
import org.saiku.query.mdx.GenericFilter;
import org.saiku.query.mdx.IFilterFunction.MdxFunctionType;
import org.saiku.query.mdx.NFilter;
import org.saiku.query.metadata.CalculatedMember;

public class OlapTest extends TestCase {
	
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
	        System.out.println("MDX:\n " + mdxString);
	        CellSet results = query.execute();
	        String s = TestContext.toString(results);
	        System.out.println("RESULTS:\n " + s);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void testBasicQueryModel() {
		
		try {
			Cube cube = getFoodmartCube("Sales");
			Query query = new Query("my query", cube);
			QueryAxis qa = query.getAxis(Axis.COLUMNS);
			QueryHierarchy products = query.getHierarchy("Product");
			
			products.includeLevel("Product Family");
//			products.exclude("[Product].[Drink]");
			products.exclude("[Product].[Food]");
			products.include("[Product].[Drink].[Beverages]");
			products.include("[Product].[Non-Consumable].[Checkout]");
			qa.addHierarchy(products);
			
			
			SelectNode mdx = query.getSelect();
	        String mdxString = mdx.toString();
	        System.out.println("MDX: " + mdxString);
	        CellSet results = query.execute();
	        String s = TestContext.toString(results);
	        System.out.println("RESULTS: " + s);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
					query.createCalculatedMember(products, 
												"Consumable", 
												"Aggregate({Product.Drink, Product.Food})",  
												null);
			products.includeCalculatedMember(cm);
			products.includeLevel("Product Family");
			products.exclude("[Product].[Non-Consumable]");
			NFilter top2filter = new NFilter(MdxFunctionType.TopCount, 2, "Measures.[Unit Sales]");
			products.addFilter(top2filter);
			
			columns.addHierarchy(products);
			
			
			QueryHierarchy gender = query.getHierarchy("Gender");
			gender.include("[Gender].[F]");
			rows.addHierarchy(gender);
						
			
			SelectNode mdx = query.getSelect();
	        String mdxString = mdx.toString();
	        System.out.println("Saiku MDX:\n " + mdxString);
	        CellSet results = query.execute();
	        String s = TestContext.toString(results);
	        System.out.println("RESULTS:\n " + s);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
