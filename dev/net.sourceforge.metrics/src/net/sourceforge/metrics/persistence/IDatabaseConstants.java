package net.sourceforge.metrics.persistence;

public interface IDatabaseConstants {
	
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
	String METRIC_LEVELS_TABLE = SCHEMA_NAME + ".MetricLevels ";
	String METRIC_VALUES_TABLE = SCHEMA_NAME + ".MetricValues ";
	//String SOURCE_COMPOSED_OF_TABLE = SCHEMA_NAME + ".SourceComposedOf ";
	//String SOURCE_ID_TABLE = SCHEMA_NAME + ".SourceID ";

	// Field names
	
	String ACRONYM_FIELD = "acronym ";
	String HANDLE_FIELD = "handle ";
	String LEVEL_ID_FIELD = "levelid ";
	String LEVEL_NAME_FIELD = "levelname ";
	String METRIC_ID_FIELD = "metricid ";
	String METRIC_NAME_FIELD = "metricname ";
	String SOURCE_ID_FIELD = "sourceid ";
	String SOURCE_NAME_FIELD = "sourcename ";
	String VALUE_FIELD = "value ";
	
	// Field Types
	
	String ACRONYM_FIELD_TYPE = "varchar(10) not null ";
	String HANDLE_FIELD_TYPE = "varchar(400) not null ";
	String LEVEL_ID_FIELD_TYPE = "int not null ";
	String LEVEL_NAME_FIELD_TYPE = "varchar(30) not null ";
	String METRIC_ID_FIELD_TYPE = "int not null ";
	String METRIC_NAME_FIELD_TYPE = "varchar(30) not null ";
	String SOURCE_ID_FIELD_TYPE = "int not null ";
	String SOURCE_NAME_FIELD_TYPE = "varchar(120) not null ";
	String VALUE_FIELD_TYPE = "double not null ";

}