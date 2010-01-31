package net.sourceforge.metrics.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

/**
 * <p>
 * This class provides JDBC access to a Derby database.
 * Derby applications can run against Derby running in an embedded
 * or a client/server framework.</p>
 * <p>
 * When Derby runs in an embedded framework, the JDBC application and Derby
 * run in the same Java Virtual Machine (JVM). The application
 * starts up the Derby engine.</p>
 * <p>
 * When Derby runs in a client/server framework, the application runs in a
 * different JVM from Derby. The application only needs to load the client
 * driver, and the connectivity framework (in this case the Derby Network
 * Server) provides network connections.</p>
 */
public class Database
// TODO While this code is mostly generic, many of the comments refer to Derby
// specific behavior.  Ultimately, we should make swapping out various databases
// easy.
{
    protected String protocol = "jdbc:derby:";
    
    /** The user name to use to access the database. */
    protected String user = "APP";

    /** The user's password to use to access the database. */
    protected String password = "APP";
    
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
    
    /** The URL to use to access the embedded database. */
    protected String embeddedURL =
	protocol + databaseName + ";";
//	"user=" + user + ";password=" + password + ";";
    
    /** The URL to use to access the database server. */
    protected String clientURL =
	protocol + "//" + host + ":" + port + "/" + databaseName + ";";
//	"user=" + user + ";password=" + password + ";";
    
    /** Indicates whether client or embedded databases should be used. */
    protected boolean isEmbedded = true;
    
    /** The driver class for use in accessing a database. */
    protected String driver = clientDriver;


    /**
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * @param protocol the protocol to set
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
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
     * @param port the port to set
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
     * @param databaseName the databaseName to set
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
     * @param embeddedDriver the embeddedDriver to set
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
     * @param clientDriver the clientDriver to set
     */
    public void setClientDriver(String clientDriver) {
        this.clientDriver = clientDriver;
    }

    /**
     * @return the embeddedURL
     */
    public String getEmbeddedURL() {
        return embeddedURL;
    }

    /**
     * @param embeddedURL the embeddedURL to set
     */
    public void setEmbeddedURL(String embeddedURL) {
        this.embeddedURL = embeddedURL;
    }

    /**
     * @return the clientURL
     */
    public String getClientURL() {
        return clientURL;
    }

    /**
     * @param clientURL the clientURL to set
     */
    public void setClientURL(String clientURL) {
        this.clientURL = clientURL;
    }

    /**
     * @return the isEmbedded
     */
    public boolean isEmbedded() {
        return isEmbedded;
    }

    /**
     * @param isEmbedded the isEmbedded to set
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
     * @param driver the driver to set
     */
    public void setDriver(String driver) {
        this.driver = driver;
    }

    /**
     * <p>
     * Starts the demo by creating a new instance of this class and running
     * the <code>go()</code> method.</p>
     * <p>
     * When you run this application, you may give one of the following
     * arguments:
     *  <ul>
          <li><code>embedded</code> - default, if none specified. Will use
     *        Derby's embedded driver. This driver is included in the derby.jar
     *        file.</li>
     *    <li><code>derbyclient</code> - will use the Derby client driver to
     *        access the Derby Network Server. This driver is included in the
     *        derbyclient.jar file.</li>
     *  </ul>
     * <p>
     * When you are using a client/server framework, the network server must
     * already be running when trying to obtain client connections to Derby.
     * This demo program will will try to connect to a network server on this
     * host (the localhost), see the <code>protocol</code> instance variable.
     * </p>
     * <p>
     * When running this demo, you must include the correct driver in the
     * classpath of the JVM. See <a href="example.html">example.html</a> for
     * details.
     * </p>
     * @param args This program accepts one optional argument specifying which
     *        connection framework (JDBC driver) to use (see above). The default
     *        is to use the embedded JDBC driver.
     */
    public static void main(String[] args)
    {
        new Database().go();
        System.out.println("Database test finished");
    }

    /**
     * This is a test driver that
     * starts the actual demo activities. This includes loading the correct
     * JDBC driver, creating a database by making a connection to Derby,
     * creating a table in the database, and inserting, updating and retrieving
     * some data. Some of the retrieved data is then verified (compared) against
     * the expected results. Finally, the table is deleted and, if the embedded
     * framework is used, the database is shut down.</p>
     * <p>
     * Generally, when using a client/server framework, other clients may be
     * (or want to be) connected to the database, so you should be careful about
     * doing shutdown unless you know that no one else needs to access the
     * database until it is rebooted. That is why this demo will not shut down
     * the database unless it is running Derby embedded.</p>
     */
    private void go()
    {
        System.out.println("SimpleApp starting in " +
        	(isEmbedded?"embedded":"client/server") + " mode");

        /* load the desired JDBC driver */
        loadDriver();
        Connection connection = null;
        /* We are storing the Statement and Prepared statement object references
         * in an array list for convenience.         */
        ArrayList<Statement> statements = new ArrayList<Statement>(); // list of Statements, PreparedStatements
        Statement statement = null;
        ResultSet resultSet = null;
        /* We will be using Statement and PreparedStatement objects for
         * executing SQL. These objects, as well as Connections and ResultSets,
         * are resources that should be released explicitly after use, hence
         * the try-catch-finally pattern used below.
         */
        try
        {
            connection = prepareConnection();

            /* Creating a statement object that we can use for running various
             * SQL statements commands against the database.*/
	    statement = connection.createStatement(
//		    ResultSet.TYPE_FORWARD_ONLY,
//		    ResultSet.CONCUR_READ_ONLY,
//		    ResultSet.CLOSE_CURSORS_AT_COMMIT
		    );
            statements.add(statement);

            resultSet = getMetricLevels(statement);
	    resultSet.close();
	    PreparedStatement psInsert = connection
		    .prepareStatement("INSERT INTO JOOMP.MetricValues values (?, ?, ?)");
	    statements.add(psInsert);
	    saveMetricValue(psInsert);
	    connection.commit();
            System.out.println("Committed the transaction");

            // In embedded mode, an application should shut down the database.
            shutDownEmbedded();
        }
        catch (SQLException sqle) {
            printSQLException(sqle);
        } finally {
            // release all open resources to avoid unnecessary memory usage
            releaseResources(connection, statements, resultSet);
        }
    }

    /**
     * In embedded mode, an application should shut down the database.
     * If the application fails to shut down the database,
     * Derby will not perform a checkpoint when the JVM shuts down.
     * This means that it will take longer to boot (connect to) the
     * database the next time, because Derby needs to perform a recovery
     * operation.
     *
     * It is also possible to shut down the Derby system/engine, which
     * automatically shuts down all booted databases.
     *
     * Explicitly shutting down the database or the Derby engine with
     * the connection URL is preferred. This style of shutdown will
     * always throw an SQLException.
     *
     * Not shutting down when in a client environment, see method
     * Javadoc.
     */
    public void shutDownEmbedded() {
	if (isEmbedded)
	{
	    shutDown();
	}
    }

    protected void shutDown() {
	try
	{
	    // the shutdown=true attribute shuts down Derby
	    DriverManager.getConnection("jdbc:derby:;shutdown=true");

	    // To shut down a specific database only, but keep the
	    // engine running (for example for connecting to other
	    // databases), specify a database in the connection URL:
	    //DriverManager.getConnection("jdbc:derby:" + dbName + ";shutdown=true");
	}
	catch (SQLException se)
	{
	    if (( (se.getErrorCode() == 50000)
	            && ("XJ015".equals(se.getSQLState()) ))) {
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
     * By default, the schema APP will be used when no username is
     * provided.  Otherwise, the schema name is the same as the user name.
     * Note that user authentication is off by default, meaning that any
     * user can connect to your database using any password. To enable
     * authentication, see the Derby Developer's Guide.
     *
     * This connection specifies create=true in the connection URL to
     * cause the database to be created when connecting for the first
     * time. To remove the database, remove the directory derbyDB (the
     * same as the database name) and its contents.
     *
     * The directory derbyDB will be created under the directory that
     * the system property derby.system.home points to, or the current
     * directory (user.dir) if derby.system.home is not set.
     */
     // TODO make consistent
    public Connection prepareConnection() throws SQLException {
        Properties props = new Properties(); // connection properties
        // providing a user name and password is optional in the embedded
        // and derbyclient frameworks
//        props.put("user", user);
//        props.put("password", password);

	Connection connection;
	connection = DriverManager.getConnection(clientURL, props);
//	connection = DriverManager.getConnection(protocol + databaseName
//	        + ";create=true", props);

	System.out.println("Connected to and created database " + databaseName);

	// We want to control transactions manually. Autocommit is on by
	// default in JDBC.
	//connection.setAutoCommit(false);
	return connection;
    }

    /**
     * Release all open resources to avoid unnecessary memory usage
     * @param connection
     * @param statements
     * @param resultSet
     */
    protected void releaseResources(Connection connection,
	    ArrayList<Statement> statements, ResultSet resultSet) {
	// ResultSet
	try {
	    if (resultSet != null) {
	        resultSet.close();
	        resultSet = null;
	    }
	} catch (SQLException sqle) {
	    printSQLException(sqle);
	}

	// Statements and PreparedStatements
	int i = 0;
	while (!statements.isEmpty()) {
	    // PreparedStatement extend Statement
	    Statement st = (Statement)statements.remove(i);
	    try {
	        if (st != null) {
	            st.close();
	            st = null;
	        }
	    } catch (SQLException sqle) {
	        printSQLException(sqle);
	    }
	}

	//Connection
	try {
	    if (connection != null) {
	        connection.close();
	        connection = null;
	    }
	} catch (SQLException sqle) {
	    printSQLException(sqle);
	}
    }
    
    private ResultSet getMetricLevels(Statement statement) throws SQLException {
	ResultSet resultSet;
	resultSet = statement.executeQuery(
	        "SELECT * from JOOMP.MetricLevels");
	ResultSetMetaData metaData = resultSet.getMetaData();
	String columnLabel1 = metaData.getColumnLabel(1);
	String columnLabel2 = metaData.getColumnLabel(2);
	System.out.println(columnLabel1 + "\t" + columnLabel2);
	while (resultSet.next()) {
	    String result =
		"\t" + resultSet.getInt(1) + ":\t" + resultSet.getString(2);
	    System.out.println(result);
	}
	return resultSet;
    }

    private void saveMetricValue(PreparedStatement insertPreparedStatement)
	    throws SQLException {
	insertPreparedStatement.setString(1, "testClassName");
	insertPreparedStatement.setString(2, "KAC");
	insertPreparedStatement.setDouble(3, 666.6);
	insertPreparedStatement.executeUpdate();
    }

    /**
     * Loads the appropriate JDBC driver for this environment/framework. For
     * example, if we are in an embedded environment, we load Derby's
     * embedded Driver, <code>org.apache.derby.jdbc.EmbeddedDriver</code>.
     */
    public void loadDriver() {
        /*
         *  The JDBC driver is loaded by loading its class.
         *  If you are using JDBC 4.0 (Java SE 6) or newer, JDBC drivers may
         *  be automatically loaded, making this code optional.
         *
         *  In an embedded environment, this will also start up the Derby
         *  engine (though not any databases), since it is not already
         *  running. In a client environment, the Derby engine is being run
         *  by the network server framework.
         *
         *  In an embedded environment, any static Derby system properties
         *  must be set before loading the driver to take effect.
         */
        try {
            Class.forName(driver).newInstance();
            System.out.println("Loaded the appropriate driver");
        } catch (ClassNotFoundException cnfe) {
            System.err.println("\nUnable to load the JDBC driver " + driver);
            System.err.println("Please check your CLASSPATH.");
            cnfe.printStackTrace(System.err);
        } catch (InstantiationException ie) {
            System.err.println(
                        "\nUnable to instantiate the JDBC driver " + driver);
            ie.printStackTrace(System.err);
        } catch (IllegalAccessException iae) {
            System.err.println(
                        "\nNot allowed to access the JDBC driver " + driver);
            iae.printStackTrace(System.err);
        }
    }

    /**
     * Prints details of an SQLException chain to <code>System.err</code>.
     * Details included are SQL State, Error code, Exception message.
     *
     * @param e the SQLException from which to print details.
     */
    public static void printSQLException(SQLException e)
    {
        // Unwraps the entire exception chain to unveil the real cause of the
        // Exception.
        while (e != null)
        {
            // TODO change to log messages
            System.err.println("\n----- SQLException -----");
            System.err.println("  SQL State:  " + e.getSQLState());
            System.err.println("  Error Code: " + e.getErrorCode());
            System.err.println("  Message:    " + e.getMessage());
            // TODO for stack traces, refer to derby.log or uncomment this:
            e.printStackTrace(System.err);
            e = e.getNextException();
        }
    }

}
