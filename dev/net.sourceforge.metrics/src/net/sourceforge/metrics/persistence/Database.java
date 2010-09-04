package net.sourceforge.metrics.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

import net.sourceforge.metrics.core.Log;

/**
 * <p>
 * This class provides JDBC access to a Derby database. Derby applications can
 * run against Derby running in an embedded or a client/server framework.
 * </p>
 * <p>
 * When Derby runs in an embedded framework, the JDBC application and Derby run
 * in the same Java Virtual Machine (JVM). The application starts up the Derby
 * engine.
 * </p>
 * <p>
 * When Derby runs in a client/server framework, the application runs in a
 * different JVM from Derby. The application only needs to load the client
 * driver, and the connectivity framework (in this case the Derby Network
 * Server) provides network connections.
 * </p>
 */
public class Database
// TODO While this code is mostly generic, many of the comments refer to Derby
// specific behavior. Ultimately, we should make swapping out various databases
// easy.
{
	// TODO initialize many of the fields via system properties, or something
	protected String protocol = "jdbc:derby:";

	// TODO Right now, no access restrictions.  Change?  Probably so.
//	/** The user name to use to access the database. */
//	protected String user = "APP";
//
//	/** The user's password to use to access the database. */
//	protected String password = "APP";

	/** The computer on which to find the database server. */
	protected String host = "localhost";

	/** The port on which to find the database server. */
	protected int port = 1527;

	/** The name of the database in which the data will reside. */
	protected String databaseName = "metrics2DB";

	/** The driver class for use in accessing an embedded database. */
	protected String embeddedDriver = "org.apache.derby.jdbc.EmbeddedDriver";

	/** The client driver class for use in accessing a database server. */
	protected String clientDriver = "org.apache.derby.jdbc.ClientDriver";

	/** Indicates whether client or embedded databases should be used. */
	protected boolean isEmbedded = false;

	/** The driver class for use in accessing a database. */
	protected String driver = clientDriver;

	/**
	 * @return the protocol
	 */
	public String getProtocol() {
		return protocol;
	}

	/**
	 * @param protocol
	 *            the protocol to set
	 */
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @param host
	 *            the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @return the databaseName
	 */
	public String getDatabaseName() {
		return databaseName;
	}

	/**
	 * @param databaseName
	 *            the databaseName to set
	 */
	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	/**
	 * @return the embeddedDriver
	 */
	public String getEmbeddedDriver() {
		return embeddedDriver;
	}

	/**
	 * @param embeddedDriver
	 *            the embeddedDriver to set
	 */
	public void setEmbeddedDriver(String embeddedDriver) {
		this.embeddedDriver = embeddedDriver;
	}

	/**
	 * @return the clientDriver
	 */
	public String getClientDriver() {
		return clientDriver;
	}

	/**
	 * @param clientDriver
	 *            the clientDriver to set
	 */
	public void setClientDriver(String clientDriver) {
		this.clientDriver = clientDriver;
	}

	/**
	 * @return The URL to use to access the embedded database.
	 */
	public String getEmbeddedURL() {
		String embeddedURL = protocol + databaseName + ";";
		// "user=" + user + ";password=" + password + ";";
		return embeddedURL;
	}

	/**
	 * @return the URL of the database for the client to connect to
	 */
	public String getClientURL() {
		String url = protocol + "//" + host + ":" + port + "/" + databaseName
				+ ";";
		// "user=" + user + ";password=" + password + ";";
		return url;
	}

	/**
	 * @return the isEmbedded
	 */
	public boolean isEmbedded() {
		return isEmbedded;
	}

	/**
	 * @param isEmbedded
	 *            the isEmbedded to set
	 */
	public void setEmbedded(boolean isEmbedded) {
		this.isEmbedded = isEmbedded;
	}

	/**
	 * @return the driver
	 */
	public String getDriver() {
		return driver;
	}

	/**
	 * @param driver
	 *            the driver to set
	 */
	public void setDriver(String driver) {
		this.driver = driver;
	}

	/**
	 * In embedded mode, an application should shut down the database. If the
	 * application fails to shut down the database, Derby will not perform a
	 * checkpoint when the JVM shuts down. This means that it will take longer
	 * to boot (connect to) the database the next time, because Derby needs to
	 * perform a recovery operation.
	 * 
	 * It is also possible to shut down the Derby system/engine, which
	 * automatically shuts down all booted databases.
	 * 
	 * Explicitly shutting down the database or the Derby engine with the
	 * connection URL is preferred. This style of shutdown will always throw an
	 * SQLException.
	 * 
	 * Not shutting down when in a client environment, see method Javadoc.
	 */
	public void shutDownEmbedded() {
		if (isEmbedded) {
			shutDown();
		}
	}

	protected void shutDown() {
		try {
			// the shutdown=true attribute shuts down Derby
			DriverManager.getConnection("jdbc:derby:;shutdown=true");

			// To shut down a specific database only, but keep the
			// engine running (for example for connecting to other
			// databases), specify a database in the connection URL:
			// DriverManager.getConnection("jdbc:derby:" + dbName +
			// ";shutdown=true");
		} catch (SQLException se) {
			if (((se.getErrorCode() == 50000) && ("XJ015".equals(se
					.getSQLState())))) {
				// we got the expected exception
				System.out.println("Derby shut down normally");
				// Note that for single database shutdown, the expected
				// SQL state is "08006", and the error code is 45000.
			} else {
				// if the error code or SQLState is different, we have
				// an unexpected exception (shutdown failed)
				System.err.println("Derby did not shut down normally");
				printSQLException(se);
			}
		}
	}

	/**
	 * By default, the schema APP will be used when no username is provided.
	 * Otherwise, the schema name is the same as the user name. Note that user
	 * authentication is off by default, meaning that any user can connect to
	 * your database using any password. To enable authentication, see the Derby
	 * Developer's Guide.
	 * 
	 * This connection specifies create=true in the connection URL to cause the
	 * database to be created when connecting for the first time. To remove the
	 * database, remove the directory derbyDB (the same as the database name)
	 * and its contents.
	 * 
	 * The directory derbyDB will be created under the directory that the system
	 * property derby.system.home points to, or the current directory (user.dir)
	 * if derby.system.home is not set.
	 */
	// TODO make consistent
	public Connection prepareConnection() throws SQLException {
		Properties props = new Properties(); // connection properties
		// providing a user name and password is optional in the embedded
		// and derbyclient frameworks
		// props.put("user", user);
		// props.put("password", password);

		Connection connection;
		connection = DriverManager.getConnection(getClientURL(), props);
		// connection = DriverManager.getConnection(protocol + databaseName
		// + ";create=true", props);

		System.out.println("Connected to and created database " + databaseName);

		// We want to control transactions manually. Autocommit is on by
		// default in JDBC.
		connection.setAutoCommit(false);
		return connection;
	}

	/**
	 * Release all open resources to avoid unnecessary memory usage
	 * 
	 * @param connection
	 * @param statements
	 * @param resultSet
	 */
	public void releaseResources(Connection connection,
			ArrayList<Statement> statements, ResultSet resultSet) {
		// ResultSet
		try {
			if (resultSet != null) {
				resultSet.close();
				// the parameter resultSet should not be assigned 
				// resultSet = null;
			}
		} catch (SQLException sqle) {
			printSQLException(sqle);
		}

		// Statements and PreparedStatements
		int i = 0;
		while (!statements.isEmpty()) {
			// PreparedStatement extend Statement
			Statement st = statements.remove(i);
			try {
				if (st != null) {
					st.close();
					st = null;
				}
			} catch (SQLException sqle) {
				printSQLException(sqle);
			}
		}

		// Connection
		try {
			if (connection != null) {
				connection.close();
				// the parameter connection should not be assigned 
				// connection = null;
			}
		} catch (SQLException sqle) {
			printSQLException(sqle);
		}
	}

	/**
	 * Loads the appropriate JDBC driver for this environment/framework. For
	 * example, if we are in an embedded environment, we load Derby's embedded
	 * Driver, <code>org.apache.derby.jdbc.EmbeddedDriver</code>.
	 */
	public void loadDriver() {
		/*
		 * The JDBC driver is loaded by loading its class. If you are using JDBC
		 * 4.0 (Java SE 6) or newer, JDBC drivers may be automatically loaded,
		 * making this code optional.
		 * 
		 * In an embedded environment, this will also start up the Derby engine
		 * (though not any databases), since it is not already running. In a
		 * client environment, the Derby engine is being run by the network
		 * server framework.
		 * 
		 * In an embedded environment, any static Derby system properties must
		 * be set before loading the driver to take effect.
		 */
		try {
			Class.forName(driver).newInstance();
			System.out.println("Loaded the appropriate driver");
		} catch (ClassNotFoundException cnfe) {
			System.err.println("\nUnable to load the JDBC driver " + driver);
			System.err.println("Please check your CLASSPATH.");
			cnfe.printStackTrace(System.err);
		} catch (InstantiationException ie) {
			System.err.println("\nUnable to instantiate the JDBC driver "
					+ driver);
			ie.printStackTrace(System.err);
		} catch (IllegalAccessException iae) {
			System.err.println("\nNot allowed to access the JDBC driver "
					+ driver);
			iae.printStackTrace(System.err);
		}
	}

	/**
	 * Prints details of an SQLException chain to <code>System.err</code>.
	 * Details included are SQL State, Error code, Exception message.
	 * 
	 * @param l_e
	 *            the SQLException from which to print details.
	 */
	public static void printSQLException(SQLException e) {
		// Unwraps the entire exception chain to unveil the real cause of the
		// Exception.
		SQLException l_e = e;
		while (l_e != null) {
			Log.logError("SQL State:  " + l_e.getSQLState() + "  Error Code: "
					+ l_e.getErrorCode(), l_e);
			// TODO change to log messages
			System.err.println("\n----- SQLException -----");
			System.err.println("  SQL State:  " + l_e.getSQLState());
			System.err.println("  Error Code: " + l_e.getErrorCode());
			System.err.println("  Message:    " + l_e.getMessage());
			// TODO for stack traces, refer to derby.log or uncomment this:
			l_e.printStackTrace(System.err);
			l_e = l_e.getNextException();
		}
	}

}
