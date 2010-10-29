package net.sourceforge.metrics.persistence;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.metrics.calculators.CohesionCalculator;
import net.sourceforge.metrics.calculators.CohesionCalculator.CohesionPreferences;
import net.sourceforge.metrics.core.Metric;
import net.sourceforge.metrics.core.MetricsPlugin;
import net.sourceforge.metrics.core.sources.AbstractMetricSource;
import net.sourceforge.metrics.core.sources.Cache;

import org.eclipse.jdt.core.IJavaElement;

public class MetricsDBTransaction implements IDatabaseConstants {
	/** The singleton metrics plugin. */
	protected MetricsPlugin plugin = MetricsPlugin.getDefault();

	/**
	 * Save metric values for the indicated Java element and all of its
	 * subelements to a database.
	 * @param element
	 *            the highest level element to be saved, e.g. a project
	 * @throws InvocationTargetException
	 * @throws SQLException 
	 */
	public void saveMeasurementsToDB(IJavaElement element)
	// , IProgressMonitor monitor)
			throws InvocationTargetException, SQLException {
		Database db = new Database();
		db.loadDriver();
		Connection connection = null;
		/* Store the Statement and Prepared statement object references
		 * in a list for convenience. */
		List<Statement> statements = new ArrayList<Statement>();
		Statement statement = null;
		ResultSet resultSet = null;
		
		/* Statements, PreparedStatements, Connections and ResultSets
		 * are resources that should be released explicitly after use, hence the
		 * try-catch-finally pattern used below. */
		try {
			connection = db.prepareConnection();

			/* Creating a statement object that we can use for running various
			 * SQL statements commands against the database. */
			statement = connection.createStatement(
			// ResultSet.TYPE_FORWARD_ONLY,
					// ResultSet.CONCUR_READ_ONLY,
					// ResultSet.CLOSE_CURSORS_AT_COMMIT
					);
			statements.add(statement);
			saveAllMeasurements(connection, statements, element);
			statement.close();
			connection.commit();
			System.out.println("Committed the transaction");

			// In embedded mode, an application should shut down the database.
			db.shutDownEmbedded();
		} catch (SQLException sqle) {
			Database.printSQLException(sqle);
			throw sqle;
		} finally {
			// release all open resources to avoid unnecessary memory usage
			db.releaseResources(connection, statements, resultSet);
		}
	}

	/**
	 * Save all the metric values for all elements, starting at
	 * the root element
	 * @param connection
	 * @param statements
	 * @param element
	 * @throws SQLException
	 */
	private void saveAllMeasurements(Connection connection,
			List<Statement> statements, IJavaElement element)
			throws SQLException {
		String handle = element.getHandleIdentifier();
		AbstractMetricSource root = Cache.singleton.get(handle);

		String[] metricIDs = plugin.getMetricIds();
		// TODO maybePrintCycles(root, pOut, monitor);
		int prefKey = getPreferencesKey(connection, statements);
		PreparedStatement deleteMetricValuesStatement =
			addDeleteMetricValuesPreparedStatement(connection, statements);
		PreparedStatement insertMetricValuesStatement =
			addInsertMetricValuesPreparedStatement(connection, statements);

		// Save all the metric values for all elements, starting at
		// the root element
		for (int i = 0; i < metricIDs.length; i++) {
			System.out.println(metricIDs[i]);
			// monitor.subTask("Exporting: " + names[i]);
			// MetricDescriptor md = plugin.getMetricDescriptor(names[i]);
			saveMetricValues(deleteMetricValuesStatement,
					insertMetricValuesStatement, metricIDs[i], root, handle, prefKey);
			connection.commit();
			// monitor.worked(1);
		}
		// monitor.done();
	}

	/**
	 * Gets the primary key for the row in the table corresponding
	 * to the current user preferences.  If such a row does not already exist,
	 * it is created.
	 * @return the key
	 */
	private int getPreferencesKey(Connection connection,
			List<Statement> statements)
	throws SQLException {
		int key = 1;
		
		if (plugin == null) {
			insertPreference(connection, statements, 1, 0, 0, 0, 0, 0, 0, 0, 0,
					0, 0, 0, 0, "");
			return key;
		}
		CohesionPreferences prefs = CohesionCalculator.getPrefs();
		int useOrig = getInt(prefs.getUseOriginalDefinitions());
		int connectIfc = getInt(prefs.getConnectInterfaceMethods());
		int countAbstract = getInt(prefs.getCountAbstractMethods());
		int countConstructors = getInt(prefs.getCountConstructors());
		int countDeprecated = getInt(prefs.getCountDeprecatedMethods());
		int countInheritedAttributes = getInt(prefs.getCountInheritedAttributes());
		int countInheritedMethods = getInt(prefs.getCountInheritedMethods());
		int countInners = getInt(prefs.getCountInners());
		int countLoggers = getInt(prefs.getCountLoggers());
		int countObjectsMethods = getInt(prefs.getCountObjectsMethods());
		int countPublicMethodsOnly = getInt(prefs.getCountPublicMethodsOnly());
		int countStaticAttributes = getInt(prefs.getCountStaticAttributes());
		int countStaticMethods = getInt(prefs.getCountStaticMethods());
		String ignoreMembersPattern = prefs.getIgnoreMembersPattern();

		String selectString = buildSelectPreferenceString(useOrig, connectIfc,
				countAbstract, countConstructors, countDeprecated,
				countInheritedAttributes, countInheritedMethods, countInners,
				countLoggers, countObjectsMethods, countPublicMethodsOnly,
				countStaticAttributes, countStaticMethods, ignoreMembersPattern);
		Statement selectStatement = connection.createStatement();
		statements.add(selectStatement);
		ResultSet resultSet = selectStatement.executeQuery(selectString);

		// If there is an existing row with these preferences, use the key
		if (resultSet.next()) {
			key = resultSet.getInt(1);
			resultSet.close();
		}
		// If no existing row exists, insert one
		else {
			resultSet.close();
			insertPreference(connection, statements, useOrig, connectIfc,
					countAbstract, countConstructors, countDeprecated,
					countInheritedAttributes, countInheritedMethods,
					countInners, countLoggers, countObjectsMethods,
					countPublicMethodsOnly, countStaticAttributes,
					countStaticMethods, ignoreMembersPattern);
			
			// Get the newly created key
			selectStatement = connection.createStatement();
			statements.add(selectStatement);
			resultSet = selectStatement.executeQuery(selectString);
			if (resultSet.next()) {
				key = resultSet.getInt(1);
			}
			resultSet.close();
		}
		connection.commit();
		return key;
	}

	private String buildSelectPreferenceString(int useOrig, int connectIfc,
			int countAbstract, int countConstructors, int countDeprecated,
			int countInheritedAttributes, int countInheritedMethods,
			int countInners, int countLoggers, int countObjectsMethods,
			int countPublicMethodsOnly, int countStaticAttributes,
			int countStaticMethods, String ignoreMembersPattern) {
		String selectString = SELECT + PREFERENCE_ID_FIELD +
			FROM + USER_PREFERENCES_TABLE +
			WHERE +
			USE_ORIGINALS_PREF + " = " + useOrig + " " + AND +
			CONNECT_INTERFACE_METHODS_PREF + " = " + connectIfc + " " + AND +
			COUNT_ABSTRACT_METHODS_PREF + " = " + countAbstract + " " + AND +
			COUNT_CONSTRUCTORS_PREF + " = " + countConstructors + " " + AND +
			COUNT_DEPRECATED_PREF + " = " + countDeprecated + " " + AND +
			COUNT_INHERITED_ATTRIBUTES_PREF + " = " + countInheritedAttributes + " " + AND +
			COUNT_INHERITED_METHODS_PREF + " = " + countInheritedMethods + " " + AND +
			COUNT_INNERS_PREF + " = " + countInners + " " + AND +
			COUNT_LOGGERS_PREF + " = " + countLoggers + " " + AND +
			COUNT_OBJECTS_METHODS_PREF + " = " + countObjectsMethods + " " + AND +
			COUNT_PUBLIC_METHODS_ONLY_PREF + " = " + countPublicMethodsOnly + " " + AND +
			COUNT_STATIC_ATTRIBUTES_PREF + " = " + countStaticAttributes + " " + AND +
			COUNT_STATIC_METHODS_PREF + " = " + countStaticMethods + " " + AND +
			IGNORE_MEMBERS_PATTERN_PREF + " = '" + ignoreMembersPattern + "'"
			;
		return selectString;
	}

	/**
	 * Insert a row into the user preferences table.
	 * @throws SQLException
	 */
	private void insertPreference(Connection connection,
			List<Statement> statements, int useOrig, int connectIfc,
			int countAbstract, int countConstructors, int countDeprecated,
			int countInheritedAttributes, int countInheritedMethods,
			int countInners, int countLoggers, int countObjectsMethods,
			int countPublicMethodsOnly, int countStaticAttributes,
			int countStaticMethods, String ignoreMembersPattern)
			throws SQLException {
		String insertString = INSERT + USER_PREFERENCES_TABLE +
		"(" +
		USE_ORIGINALS_PREF + ", " +
		CONNECT_INTERFACE_METHODS_PREF + ", " +
		COUNT_ABSTRACT_METHODS_PREF + ", " +
		COUNT_CONSTRUCTORS_PREF + ", " +
		COUNT_DEPRECATED_PREF + ", " +
		COUNT_INHERITED_ATTRIBUTES_PREF + ", " +
		COUNT_INHERITED_METHODS_PREF + ", " +
		COUNT_INNERS_PREF + ", " +
		COUNT_LOGGERS_PREF + ", " +
		COUNT_OBJECTS_METHODS_PREF + ", " +
		COUNT_PUBLIC_METHODS_ONLY_PREF + ", " +
		COUNT_STATIC_ATTRIBUTES_PREF + ", " +
		COUNT_STATIC_METHODS_PREF + ", " +
		IGNORE_MEMBERS_PATTERN_PREF +
		")" +
		VALUES + 
		"(" +
		useOrig + ", " +
		connectIfc + ", " +
		countAbstract + ", " +
		countConstructors + ", " +
		countDeprecated + ", " +
		countInheritedAttributes + ", " +
		countInheritedMethods + ", " +
		countInners + ", " +
		countLoggers + ", " +
		countObjectsMethods + ", " +
		countPublicMethodsOnly + ", " +
		countStaticAttributes + ", " +
		countStaticMethods + ", " +
		"'" + ignoreMembersPattern + "'" +
		")";
		Statement insertStatement = connection.createStatement();
		statements.add(insertStatement);
		insertStatement.executeUpdate(insertString);
	}

	/**
	 * Converts a boolean into an integer for the database
	 * @param bool the value to convert
	 * @return 1 if bool is true; 0 otherwise
	 */
	private int getInt(boolean bool) {
		int value = bool? 1 : 0;
		return value;
	}

	private PreparedStatement addInsertMetricValuesPreparedStatement(
			Connection connection, List<Statement> statements)
			throws SQLException {
		String sqlString =
			INSERT + METRIC_VALUES_TABLE + VALUES + "(?, ?, ?, ?)";
		PreparedStatement statement = connection.prepareStatement(sqlString);
		statements.add(statement);
		return statement;
	}

	private PreparedStatement addDeleteMetricValuesPreparedStatement(
			Connection connection, List<Statement> statements)
			throws SQLException {
		String sqlString = DELETE + METRIC_VALUES_TABLE +
		WHERE + HANDLE_FIELD + " = ? " + AND  + ACRONYM_FIELD + " = ? " +
		AND + USER_PREFERENCES_FOREIGN_KEY + " = ?";
		PreparedStatement statement =
			connection.prepareStatement(sqlString);
		statements.add(statement);
		return statement;
	}

	/**
	 * Saves the metric values for the current element and all its subelements
	 * (recursively).
	 * 
	 * @param insertStatement
	 * @param metricId
	 * @param metricSource
	 * @param parentHandle
	 * @param prefKey the key into the preferences table listing the preferences
	 * in effect when the measurements were taken
	 * @throws SQLException
	 */
	private void saveMetricValues(PreparedStatement deleteStatement,
			PreparedStatement insertStatement, String metricId,
			AbstractMetricSource metricSource, String parentHandle, int prefKey)
			throws SQLException {
		saveMetricValue(deleteStatement, insertStatement, metricId,
				metricSource, parentHandle, prefKey);
		List<String> handles = metricSource.getChildHandles();
		for (String handle : handles) {
			AbstractMetricSource child = Cache.singleton.get(handle);
			saveMetricValues(deleteStatement, insertStatement, metricId, child,
					handle, prefKey);
		}
	}

	private void saveMetricValue(PreparedStatement deleteStatement,
			PreparedStatement insertStatement, String metricId,
			AbstractMetricSource metricSource, String handle,
			int prefKey) {
		deleteOldMetricValue(deleteStatement, metricId, handle, prefKey);
		insertNewMetricValue(insertStatement, metricId, metricSource,
				handle, prefKey);
	}

	private void insertNewMetricValue(PreparedStatement insertStatement,
			String metricId, AbstractMetricSource metricSource, String handle,
			int prefKey) {
		// Insert the new values
		Metric metric = metricSource.getValue(metricId);
		if (metric != null) {
			double value = metric.getValue();
			try {
				insertStatement.setString(1, handle);
				insertStatement.setString(2, metricId);
				insertStatement.setDouble(3, value);
				insertStatement.setInt(4, prefKey);
				insertStatement.executeUpdate();
			} catch (SQLException e) {
				Database.printSQLException(e);
			}
		}
	}

	private void deleteOldMetricValue(PreparedStatement deleteStatement,
			String metricId, String handle, int prefKey) {
		// Delete old metric values from prior runs
		try {
			deleteStatement.setString(1, handle);
			deleteStatement.setString(2, metricId);
			deleteStatement.setInt(3, prefKey);
			deleteStatement.executeUpdate();
		} catch (SQLException e) {
			// quietly swallow the exception. In many cases, there will be
			// nothing in the database to delete
		}
	}
	
	public void initializeDatabase(Connection connection) throws SQLException {
	      Statement statement = connection.createStatement();
	      dropTables(statement);
	      createTables(connection, statement);
	      statement.close();
	}

	public void dropTables(Statement statement) throws SQLException {
		executeAndIgnore(statement, DROP + METRIC_ID_TABLE);
		executeAndIgnore(statement, DROP + METRIC_LEVELS_TABLE);
		executeAndIgnore(statement, DROP + METRIC_VALUES_TABLE);
		executeAndIgnore(statement, DROP + USER_PREFERENCES_TABLE);
//		executeAndIgnore(statement, DROP + SOURCE_COMPOSED_OF_TABLE);
//		executeAndIgnore(statement, DROP + SOURCE_ID_TABLE);
		Connection connection = statement.getConnection();
		connection.commit();
	}

	private void executeAndIgnore(Statement statement, String sqlString) {
		try {
			statement.executeUpdate(sqlString);
		} catch (SQLException e) {
			// quietly ignore - maybe nothing to drop
		}
	}

	public void createTables(Connection connection,
			Statement statement) throws SQLException {
		createMetricIdTable(statement);
		populateMetricIdTable(connection);
		createMetricLevelsTable(statement);
		populateMetricLevelsTable(connection);
		createPreferencesTable(statement);
		createMetricValuesTable(statement);
		connection.commit();
	}

	private void populateMetricLevelsTable(Connection connection) {
		String sqlString =
			INSERT + METRIC_LEVELS_TABLE + VALUES + " (?, ?)";
		try {
			PreparedStatement statement = connection.prepareStatement(sqlString);
			insertMetricLevel(statement, IJavaElement.METHOD, "Method");
			insertMetricLevel(statement, IJavaElement.TYPE, "Type");
			insertMetricLevel(statement, IJavaElement.COMPILATION_UNIT, "Compilation Unit");
			insertMetricLevel(statement, IJavaElement.PACKAGE_FRAGMENT, "Package Fragment");
			insertMetricLevel(statement, IJavaElement.PACKAGE_FRAGMENT_ROOT, "Package Fragment Root");
			insertMetricLevel(statement, IJavaElement.JAVA_PROJECT, "Project");
		} catch (SQLException e) {
			Database.printSQLException(e);
		}
	}

	private void insertMetricLevel(PreparedStatement insertStatement, int id, String name)
			throws SQLException {
		insertStatement.setInt(1, id);
		insertStatement.setString(2, name);
		insertStatement.executeUpdate();
	}

	private void insertMetricId(PreparedStatement insertStatement, int id,
			String name, String acronym) throws SQLException {
		insertStatement.setInt(1, id);
		insertStatement.setString(2, name);
		insertStatement.setString(3, acronym);
		insertStatement.executeUpdate();
	}

	private void populateMetricIdTable(Connection connection) {
		int i = 0;
		String sqlString = INSERT + METRIC_ID_TABLE + VALUES + " (?, ?, ?)";
		try {
			PreparedStatement statement = 
				connection.prepareStatement(sqlString);
			insertMetricId(statement, i++, "NESTED_BLOCK_DEPTH", "NBD");
			insertMetricId(statement, i++, "PARMS", "PAR");
			insertMetricId(statement, i++, "MCCABE", "VG");
			insertMetricId(statement, i++, "NUM_METHODS", "NOM");
			insertMetricId(statement, i++, "NUM_STAT_METHODS", "NSM");
			insertMetricId(statement, i++, "NUM_STAT_FIELDS", "NSF");
			insertMetricId(statement, i++, "NUM_FIELDS", "NOF");
			insertMetricId(statement, i++, "NUM_TYPES", "NOC");
			insertMetricId(statement, i++, "NUM_PACKAGES", "NOP");
			insertMetricId(statement, i++, "NUM_INTERFACES", "NOI");
			insertMetricId(statement, i++, "INHERITANCE_DEPTH", "DIT");
			insertMetricId(statement, i++, "SUBCLASSES", "NSC");
			insertMetricId(statement, i++, "SUPERCLASSES", "NUC");
			insertMetricId(statement, i++, "REUSE_RATIO", "U");
			insertMetricId(statement, i++, "SPECIALIZATION_IN", "SIX");
			insertMetricId(statement, i++, "NORM", "NORM");
			insertMetricId(statement, i++, "WMC", "WMC");
			insertMetricId(statement, i++, "DCD", "DCD");
			insertMetricId(statement, i++, "DCI", "DCI");
			insertMetricId(statement, i++, "LCC", "LCC");
			insertMetricId(statement, i++, "LCOM", "LCOM");
			insertMetricId(statement, i++, "LCOMHS", "LCOMHS");
			insertMetricId(statement, i++, "LCOMCK", "LCOMCK");
			insertMetricId(statement, i++, "TCC", "TCC");
			insertMetricId(statement, i++, "CBO", "CBO");
			insertMetricId(statement, i++, "RMC", "RMC");
			insertMetricId(statement, i++, "CA", "CA");
			insertMetricId(statement, i++, "CE", "CE");
			insertMetricId(statement, i++, "RMI", "RMI");
			insertMetricId(statement, i++, "RMA", "RMA");
			insertMetricId(statement, i++, "RMD", "RMD");
			insertMetricId(statement, i++, "MLOC", "MLOC");
			insertMetricId(statement, i++, "TLOC", "TLOC");
		} catch (SQLException e) {
			Database.printSQLException(e);
		}
	}

	private void createMetricIdTable(Statement statement) throws SQLException {
		String sqlString = CREATE + METRIC_ID_TABLE +
		"(" + METRIC_ID_FIELD + METRIC_ID_FIELD_TYPE + ", " +
		METRIC_NAME_FIELD + METRIC_NAME_FIELD_TYPE + ", " +
		ACRONYM_FIELD + ACRONYM_FIELD_TYPE + NOT + NULL + PRIMARY_KEY +
		")";
		statement.executeUpdate(sqlString);
	}

	private void createMetricLevelsTable(Statement statement) throws SQLException {
		String sqlString = CREATE + METRIC_LEVELS_TABLE +
		"(" + LEVEL_ID_FIELD + LEVEL_ID_FIELD_TYPE + ", " +
		LEVEL_NAME_FIELD + LEVEL_NAME_FIELD_TYPE +
		")";
		statement.executeUpdate(sqlString);
	}

	private void createPreferencesTable(Statement statement) throws SQLException {
		String sqlString = CREATE + USER_PREFERENCES_TABLE +
		"(" + PREFERENCE_ID_FIELD + GENERATED_INT_KEY + ", " +
		USE_ORIGINALS_PREF + BOOLEAN_FIELD_TYPE + ", " +
		IGNORE_MEMBERS_PATTERN_PREF + IGNORE_PATTERN_FIELD_TYPE + ", " +
		COUNT_ABSTRACT_METHODS_PREF + BOOLEAN_FIELD_TYPE + ", " +
		COUNT_CONSTRUCTORS_PREF + BOOLEAN_FIELD_TYPE + ", " +
		COUNT_DEPRECATED_PREF + BOOLEAN_FIELD_TYPE + ", " +
		COUNT_INHERITED_ATTRIBUTES_PREF + BOOLEAN_FIELD_TYPE + ", " +
		COUNT_INHERITED_METHODS_PREF + BOOLEAN_FIELD_TYPE + ", " +
		COUNT_INNERS_PREF + BOOLEAN_FIELD_TYPE + ", " +
		COUNT_LOGGERS_PREF + BOOLEAN_FIELD_TYPE + ", " +
		COUNT_OBJECTS_METHODS_PREF + BOOLEAN_FIELD_TYPE + ", " +
		COUNT_PUBLIC_METHODS_ONLY_PREF + BOOLEAN_FIELD_TYPE + ", " +
		COUNT_STATIC_ATTRIBUTES_PREF + BOOLEAN_FIELD_TYPE + ", " +
		COUNT_STATIC_METHODS_PREF + BOOLEAN_FIELD_TYPE + ", " +
		CONNECT_INTERFACE_METHODS_PREF + BOOLEAN_FIELD_TYPE + ", " +
		CONSTRAINT + "pk_prefval " + PRIMARY_KEY +
		"(" + PREFERENCE_ID_FIELD + ")" +
		")";
		statement.executeUpdate(sqlString);
	}

	/** Creates the table that will hold the metric values. */
	private void createMetricValuesTable(Statement statement) throws SQLException {
		String sqlString = CREATE + METRIC_VALUES_TABLE +
		"(" + HANDLE_FIELD + HANDLE_FIELD_TYPE + ", " +
		ACRONYM_FIELD + ACRONYM_FIELD_TYPE + ", " +
		VALUE_FIELD + VALUE_FIELD_TYPE + ", " +
		USER_PREFERENCES_FOREIGN_KEY + INT_FIELD_TYPE +
		CONSTRAINT + "pref_foreign_key " + REFERENCES +
		USER_PREFERENCES_TABLE + ", " +
		CONSTRAINT + "pk_metricval " + PRIMARY_KEY +
		  "(" + HANDLE_FIELD + ", " + ACRONYM_FIELD+ ", " + 
		    USER_PREFERENCES_FOREIGN_KEY  + ")" +
		")";
		statement.executeUpdate(sqlString);
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
     * @param args
     */
	public static void main(String[] args) {
		Database db = new Database();

		/* load the desired JDBC driver */
		db.loadDriver();
		Connection connection = null;
		/*
		 * We are storing the Statement and Prepared statement object references
		 * in an array list for convenience.
		 */
		List<Statement> statements = new ArrayList<Statement>();
		Statement statement = null;
		ResultSet resultSet = null;
		/*
		 * We will be using Statement and PreparedStatement objects for
		 * executing SQL. These objects, as well as Connections and ResultSets,
		 * are resources that should be released explicitly after use, hence the
		 * try-catch-finally pattern used below.
		 */
		try {
			connection = db.prepareConnection();

			/*
			 * Creating a statement object that we can use for running various
			 * SQL statements commands against the database.
			 */
			statement = connection.createStatement();
			statements.add(statement);
			MetricsDBTransaction transaction = new MetricsDBTransaction();
			transaction.initializeDatabase(connection);
			resultSet = transaction.getMetricLevels(statement);
			resultSet.close();
			String sqlString = INSERT + METRIC_VALUES_TABLE + VALUES + "(?, ?, ?, ?)";
			PreparedStatement psInsert = connection.prepareStatement(sqlString);
			statements.add(psInsert);
			int key = transaction.getPreferencesKey(connection, statements);
			transaction.saveMetricValue(psInsert, key);
			connection.commit();
			System.out.println("Committed the transaction");

			// In embedded mode, an application should shut down the database.
			db.shutDownEmbedded();
		} catch (SQLException sqle) {
			Database.printSQLException(sqle);
		} finally {
			// release all open resources to avoid unnecessary memory usage
			db.releaseResources(connection, statements, resultSet);
		}
		System.out.println("Database test finished");
	}

	// TODO remove - test only
	private ResultSet getMetricLevels(Statement statement) throws SQLException {
		ResultSet resultSet;
		String sqlString = SELECT + "* " + FROM + METRIC_LEVELS_TABLE;
		resultSet = statement.executeQuery(sqlString);
		ResultSetMetaData metaData = resultSet.getMetaData();
		String columnLabel1 = metaData.getColumnLabel(1);
		String columnLabel2 = metaData.getColumnLabel(2);
		System.out.println(columnLabel1 + "\t" + columnLabel2);
		while (resultSet.next()) {
			String result = "\t" + resultSet.getInt(1) + ":\t"
					+ resultSet.getString(2);
			System.out.println(result);
		}
		return resultSet;
	}

	// TODO remove - test only
	private void saveMetricValue(PreparedStatement insertPreparedStatement, int key)
			throws SQLException {
		insertPreparedStatement.setString(1, "testClassName");
		insertPreparedStatement.setString(2, "KAC");
		insertPreparedStatement.setDouble(3, 666.6);
		insertPreparedStatement.setInt(4, key);
		insertPreparedStatement.executeUpdate();
	}

}
