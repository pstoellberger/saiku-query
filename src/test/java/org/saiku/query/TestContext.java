package org.saiku.query;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.DriverManager;
import java.util.Properties;

import org.olap4j.OlapConnection;
import org.olap4j.OlapWrapper;

public class TestContext {
    
	public static final String testPropertiesFile = "connection.properties";
	private static Properties testProperties;

	private static TestContext instance;
	public static boolean DEBUG = false;

	public TestContext() {
		loadProperties();
	}

	public static TestContext instance() {
		if (instance == null) {
			instance = new TestContext();
		}
		return instance;
	}

	public OlapConnection createConnection() {
		try {
			String driver = testProperties.getProperty(Property.Driver.path);
			String url = testProperties.getProperty(Property.ConnectionString.path);
			Class.forName(driver);
			OlapConnection connection = (OlapConnection) DriverManager.getConnection(url, new Properties());
			final OlapWrapper wrapper = connection;
			OlapConnection tmpolapConnection = (OlapConnection) wrapper.unwrap(OlapConnection.class);
			if (tmpolapConnection == null) {
				throw new Exception("Connection is null");
			}
			return tmpolapConnection;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	private static synchronized Properties loadProperties() {
		if (testProperties == null) {
			testProperties = new Properties(System.getProperties());
		}
		try {
			testProperties.load(ClassLoader.getSystemResourceAsStream(testPropertiesFile));
		} catch (IOException e) {
			File propsFile = new File(testPropertiesFile);
			if (propsFile.exists()) {
				try {
					FileReader fr = new FileReader(propsFile);
					testProperties.load(fr);
					return testProperties;
				} catch (IOException e1) {
					throw new RuntimeException("Cannot load test propties file: " + testPropertiesFile, e1);
				}
			}
		}
		return null;

	}

	public enum Property {
		Name("name"),
		Driver("driver"),
		ConnectionString("url");

		public final String path;

		private Property(String path) {
			this.path = path;
		}		
	}
	



}
