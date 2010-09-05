-- This is now obsolete and sitting around temporarily for reference.
-- The table creation is now being done in code.

-- for server
connect 'jdbc:derby://localhost:1527/metrics2DB;create=true';
-- for embedded
-- connect 'jdbc:derby/metrics2DB;create=true';
-- select * from JOOMP.MetricValues;

-- levelname - e.g. package, class, method
DROP TABLE JOOMP.MetricLevels;
CREATE TABLE JOOMP.MetricLevels
(
    levelid int not null unique, 
    levelname varchar(30) not null unique
);
-- These should be kept consistent with
-- net.sourceforge.metrics.core.Constants.java/org.eclipse.jdt.core.IJavaElement
INSERT INTO JOOMP.MetricLevels(levelid, levelname)
       VALUES (9, 'Method');
INSERT INTO JOOMP.MetricLevels(levelid, levelname)
       VALUES (7, 'Type');
INSERT INTO JOOMP.MetricLevels(levelid, levelname)
       VALUES (5, 'Compilation Unit');
INSERT INTO JOOMP.MetricLevels(levelid, levelname)
       VALUES (4, 'Package Fragment');
INSERT INTO JOOMP.MetricLevels(levelid, levelname)
       VALUES (3, 'Package Root');
INSERT INTO JOOMP.MetricLevels(levelid, levelname)
       VALUES (2, 'Project');


-- metricname e.g. lack of cohesion; acronym e.g. LCOMHS
DROP TABLE JOOMP.MetricID;
CREATE TABLE JOOMP.MetricID
(
    metricid int not null, 
    metricname varchar(30) not null, 
    acronym varchar(10) not null primary key
);

-- These should be kept consistent with
-- net.sourceforge.metrics.core.Constants.java and build.xml
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (1, 'Lack of Cohesion', 'LCOM');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (2, 'NESTEDBLOCKDEPTH', 'NBD');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (3, 'PARMS', 'PAR');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (4, 'MCCABE', 'VG');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (5, 'NUM METHODS', 'NOM');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (6, 'NUM STAT METHODS', 'NSM');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (7, 'NUM STAT FIELDS', 'NSF');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (8, 'NUM FIELDS', 'NOF');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (9, 'NUM TYPES', 'NOC');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (10, 'NUM PACKAGES', 'NOP');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (11, 'NUM INTERFACES', 'NOI');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (12, 'INHERITANCE DEPTH', 'DIT');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (13, 'SUBCLASSES', 'NSC');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (14, 'SUPERCLASSES', 'NUC');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (15, 'REUSE RATIO', 'U');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (16, 'SPECIALIZATION IN', 'SIX');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (17, 'NORM', 'NORM');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (18, 'WMC', 'WMC');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (19, 'DCD', 'DCD');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (20, 'DCI', 'DCI');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (21, 'LCC', 'LCC');
-- INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
--    VALUES (1, 'Lack of Cohesion', 'LCOM');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (22, 'LCOMHS', 'LCOMHS');
-- INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
--    VALUES (32, 'LCOMCK', 'LCOMCK'); 
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (23, 'TCC', 'TCC');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (24, 'RMC', 'RMC');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (25, 'CA', 'CA');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (26, 'CE', 'CE');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (27, 'RMI', 'RMI');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (28, 'RMA', 'RMA');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (29, 'RMD', 'RMD');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (30, 'MLOC', 'MLOC');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (31, 'TLOC', 'TLOC');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (32, 'LCOMCK', 'LCOMCK');
INSERT INTO JOOMP.MetricID(metricid, metricname, acronym)
    VALUES (33, 'CBO', 'CBO');

-- sourcename e.g. MyClass or mySignature(int, double)
DROP TABLE JOOMP.SourceID;
CREATE TABLE JOOMP.SourceID
(
    sourceid int not null, 
    handle varchar(400) not null primary key, 
    sourcename varchar(120) not null, 
    levelid int not null
);


DROP TABLE JOOMP.MetricValues;
CREATE TABLE JOOMP.MetricValues
(
    handle varchar(400) not null,
    metricacronym varchar(10) not null,
    value double not null,
    CONSTRAINT pk_metricval PRIMARY KEY (handle,metricacronym)
);
-- CREATE TABLE JOOMP.MetricValues
-- (
--     sourceid int not null,
--     metricid int not null,
--     value double not null
-- );

DROP TABLE JOOMP.SourceComposedOf;
CREATE TABLE JOOMP.SourceComposedOf
(
       parentsourceid int not null,
       childsourceid int not null
);
