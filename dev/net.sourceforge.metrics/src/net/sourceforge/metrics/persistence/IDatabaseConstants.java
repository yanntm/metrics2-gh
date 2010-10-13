package net.sourceforge.metrics.persistence;

import net.sourceforge.metrics.core.Constants;

public interface IDatabaseConstants
extends Constants {
	
	// Database commands
	
	String AND = "AND ";
	String AS = "AS ";
	String ASCENDING = "ASC ";
	String CONSTRAINT = "CONSTRAINT ";
	String CREATE = "CREATE TABLE ";
	String DELETE = "DELETE FROM ";
	String DESCENDING = "DESC ";
	String DROP = "DROP TABLE ";
	String FROM = "FROM ";
	String INSERT = "INSERT INTO ";
	String NOT = "NOT ";
	String NULL = "NULL ";
	String OR = "OR ";
	String REFERENCES = "REFERENCES ";
	String SELECT = "SELECT ";
	String ORDER_BY = "ORDER BY ";
	String PRIMARY_KEY = "PRIMARY KEY ";
	String VALUES = "VALUES ";
	String WHERE = "WHERE ";
	
	String TERMINATOR = ";";
	
	/** The name of the schema containing the tables.  */
	String SCHEMA_NAME = "JOOMP";
	
	// The table names
	
	String METRIC_ID_TABLE = SCHEMA_NAME + ".MetricID ";

	/** The table containing ids for the unit being measured
	  - class, method, etc. */
	String METRIC_LEVELS_TABLE = SCHEMA_NAME + ".MetricLevels ";

	/** The table containing the measurements.	 */
	String METRIC_VALUES_TABLE = SCHEMA_NAME + ".MetricValues ";
	
	/** The table containing the user preferences in effect when the
	 * measurements were collected.	 */
	String USER_PREFERENCES_TABLE = SCHEMA_NAME + ".UserPreferences ";
	//String SOURCE_COMPOSED_OF_TABLE = SCHEMA_NAME + ".SourceComposedOf ";
	//String SOURCE_ID_TABLE = SCHEMA_NAME + ".SourceID ";

	// Field names
	
	String ACRONYM_FIELD = "acronym ";
	String HANDLE_FIELD = "handle ";
	String LEVEL_ID_FIELD = "levelid ";
	String LEVEL_NAME_FIELD = "levelname ";
	String METRIC_ID_FIELD = "metricid ";
	String METRIC_NAME_FIELD = "metricname ";
	String PREFERENCE_ID_FIELD = "preferenceid ";
	String SOURCE_ID_FIELD = "sourceid ";
	String SOURCE_NAME_FIELD = "sourcename ";
	String USER_PREFERENCES_FOREIGN_KEY = "user_preferences_key ";
	String VALUE_FIELD = "value ";
	
	// Field names for preferences table can be found in 
	// net.sourceforge.metrics.core.Constants and should correspond
	// to the preference pages
	
	// Field Types
	
	String ACRONYM_FIELD_TYPE = " varchar(10) not null ";
	/** Some databases don't support a boolean type, so we use an int.
	  0 indicates false; every other non-null indicates true. */
	String BOOLEAN_FIELD_TYPE = " int ";
	String GENERATED_INT_KEY = " int generated always as identity ";
	String HANDLE_FIELD_TYPE = " varchar(400) not null ";
	String IGNORE_PATTERN_FIELD_TYPE = " varchar(400) not null ";
	String INT_FIELD_TYPE = " int ";
	String LEVEL_ID_FIELD_TYPE = " int not null ";
	String LEVEL_NAME_FIELD_TYPE = " varchar(30) not null ";
	String METRIC_ID_FIELD_TYPE = " int not null ";
	String METRIC_NAME_FIELD_TYPE = " varchar(30) not null ";
	String SOURCE_ID_FIELD_TYPE = " int not null ";
	String SOURCE_NAME_FIELD_TYPE = " varchar(120) not null ";
	String VALUE_FIELD_TYPE = " double not null ";

}
