package net.sourceforge.metrics.persistence;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.metrics.core.Metric;
import net.sourceforge.metrics.core.MetricsPlugin;
import net.sourceforge.metrics.core.sources.AbstractMetricSource;
import net.sourceforge.metrics.core.sources.Cache;

import org.eclipse.jdt.core.IJavaElement;

public class MetricsDBTransaction {
	/** The singleton metrics plugin. */
	protected MetricsPlugin plugin = MetricsPlugin.getDefault();

	/**
	 * Save metric values for the indicated Java element and all of its
	 * subelements to a database.
	 * 
	 * @param element
	 *            the highest level element to be saved, e.g. a project
	 * @throws InvocationTargetException
	 */
	public void saveToDB(IJavaElement element)
	// , IProgressMonitor monitor)
			throws InvocationTargetException {
		Database db = new Database();
		db.loadDriver();
		Connection connection = null;
		/*
		 * We are storing the Statement and Prepared statement object references
		 * in an array list for convenience.
		 */
		ArrayList<Statement> statements = new ArrayList<Statement>();
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
			statement = connection.createStatement(
			// ResultSet.TYPE_FORWARD_ONLY,
					// ResultSet.CONCUR_READ_ONLY,
					// ResultSet.CLOSE_CURSORS_AT_COMMIT
					);
			statements.add(statement);

			saveAll(connection, statements, element);

			statement.close();
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
	}

	private void saveAll(Connection connection,
			ArrayList<Statement> statements, IJavaElement element)
			throws SQLException {
		String handle = element.getHandleIdentifier();
		AbstractMetricSource root = Cache.singleton.get(handle);

		PreparedStatement deleteSourceIDsStatement =
			addDeleteSourceIDsPreparedStatement(connection, statements);
		PreparedStatement insertSourceIDsStatement =
			addInsertSourceIDsPreparedStatement(connection, statements);
		// Save all the identifier information for all elements, starting at
		// the root element
		saveIdentifierInfo(deleteSourceIDsStatement, insertSourceIDsStatement,
				handle);

		String[] metricIDs = plugin.getMetricIds();
		// TODO maybePrintCycles(root, pOut, monitor);
		PreparedStatement deleteMetricValuesStatement =
			addDeleteMetricValuesPreparedStatement(connection, statements);
		PreparedStatement insertMetricValuesStatement =
			addInsertMetricValuesPreparedStatement(connection, statements);

		// Save all the metric values for all elements, starting at
		// the root element
		for (int i = 0; i < metricIDs.length; i++) {
			// monitor.subTask("Exporting: " + names[i]);
			// MetricDescriptor md = plugin.getMetricDescriptor(names[i]);
			saveMetricValues(deleteMetricValuesStatement,
					insertMetricValuesStatement, metricIDs[i], root, handle);
			// monitor.worked(1);
		}
		// monitor.done();
	}

	private PreparedStatement addInsertMetricValuesPreparedStatement(
			Connection connection, ArrayList<Statement> statements)
			throws SQLException {
		PreparedStatement statement = connection
				.prepareStatement("INSERT INTO JOOMP.MetricValues values (?, ?, ?)");
		statements.add(statement);
		return statement;
	}

	private PreparedStatement addDeleteMetricValuesPreparedStatement(
			Connection connection, ArrayList<Statement> statements)
			throws SQLException {
		PreparedStatement statement = connection
				.prepareStatement("DELETE FROM JOOMP.MetricValues WHERE handle = ? AND metricacronym = ?");
		statements.add(statement);
		return statement;
	}

	private PreparedStatement addInsertSourceIDsPreparedStatement(
			Connection connection, ArrayList<Statement> statements)
			throws SQLException {
		PreparedStatement statement = connection
				.prepareStatement("INSERT INTO JOOMP.SourceID values (?, ?, ?, ?)");
		statements.add(statement);
		return statement;
	}

	private PreparedStatement addDeleteSourceIDsPreparedStatement(
			Connection connection, ArrayList<Statement> statements)
			throws SQLException {
		PreparedStatement statement = connection
				.prepareStatement("DELETE FROM JOOMP.SourceID WHERE handle = ?");
		statements.add(statement);
		return statement;
	}

	private void saveIdentifierInfo(PreparedStatement deleteStatement,
			PreparedStatement insertStatement, String parentHandle) {
		deleteIdentifierInfo(deleteStatement, parentHandle);
		AbstractMetricSource metricSource = Cache.singleton.get(parentHandle);
		insertIdentifierInfo(metricSource, insertStatement, parentHandle);
		List<String> childHandles = metricSource.getChildHandles();
		for (String childHandle : childHandles) {
			saveIdentifierInfo(deleteStatement, insertStatement, childHandle);
		}
	}

	private void insertIdentifierInfo(AbstractMetricSource metricSource,
			PreparedStatement insertStatement, String parentHandle) {
		try {
			String name = metricSource.getName();
			int level = metricSource.getLevel();
			// TODO uids
			insertStatement.setInt(1, 0); // uid
			insertStatement.setString(2, parentHandle);
			insertStatement.setString(3, name);
			insertStatement.setInt(4, level);
			insertStatement.executeUpdate();
		} catch (SQLException sqle) {
			Database.printSQLException(sqle);
		}
	}

	private void deleteIdentifierInfo(PreparedStatement deleteStatement,
			String parentHandle) {
		try {
			deleteStatement.setString(1, parentHandle);
			deleteStatement.executeUpdate();
		} catch (Exception e) {
			// Ignore - many times the delete will have nothing to delete
		}
	}

	/**
	 * Saves the metric values for the current element and all its subelements
	 * (recursively).
	 * 
	 * @param insertStatement
	 * @param metricId
	 * @param metricSource
	 * @param parentHandle
	 * @throws SQLException
	 */
	private void saveMetricValues(PreparedStatement deleteStatement,
			PreparedStatement insertStatement, String metricId,
			AbstractMetricSource metricSource, String parentHandle)
			throws SQLException {
		saveMetricValue(deleteStatement, insertStatement, metricId,
				metricSource, parentHandle);
		List<String> handles = metricSource.getChildHandles();
		for (String handle : handles) {
			AbstractMetricSource child = Cache.singleton.get(handle);
			saveMetricValues(deleteStatement, insertStatement, metricId, child,
					handle);
		}
	}

	private void saveMetricValue(PreparedStatement deleteStatement,
			PreparedStatement insertStatement, String metricId,
			AbstractMetricSource metricSource, String handle) {
		deleteOldMetricValue(deleteStatement, metricId, handle);
		insertNewMetricValue(insertStatement, metricId, metricSource, handle);
	}

	private void insertNewMetricValue(PreparedStatement insertStatement,
			String metricId, AbstractMetricSource metricSource, String handle) {
		// Insert the new values
		Metric metric = metricSource.getValue(metricId);
		if (metric != null) {
			double value = metric.getValue();
			try {
				insertStatement.setString(1, handle);
				insertStatement.setString(2, metricId);
				insertStatement.setDouble(3, value);
				insertStatement.executeUpdate();
			} catch (SQLException e) {
				Database.printSQLException(e);
			}
		}
	}

	private void deleteOldMetricValue(PreparedStatement deleteStatement,
			String metricId, String handle) {
		// Delete old metric values from prior runs
		try {
			deleteStatement.setString(1, handle);
			deleteStatement.setString(2, metricId);
			deleteStatement.executeUpdate();
		} catch (SQLException e) {
			// quietly swallow the exception. In many cases, there will be
			// nothing in the database to delete
		}
	}

}
