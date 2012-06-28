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

public class ConnectionTest extends TestCase {
	
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
	
	public void testQuery() {
		
		try {
			Cube cube = getFoodmartCube("Sales");
			Query query = new Query("my query", cube);
			QueryAxis qa = query.getAxis(Axis.COLUMNS);
			qa.setMdxSetExpression("Product.Drink");
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
