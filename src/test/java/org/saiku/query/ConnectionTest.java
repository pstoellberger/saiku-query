package org.saiku.query;

import junit.framework.TestCase;

import org.olap4j.OlapConnection;
import org.olap4j.OlapException;

public class ConnectionTest extends TestCase {
	
	private TestContext context = TestContext.instance();
	
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
}
