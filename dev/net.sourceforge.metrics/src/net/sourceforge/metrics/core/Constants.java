/*
 * Copyright (c) 2003 Frank Sauer. All rights reserved.
 *
 * Licenced under CPL 1.0 (Common Public License Version 1.0).
 * The licence is available at http://www.eclipse.org/legal/cpl-v10.html.
 *
 *
 * DISCLAIMER OF WARRANTIES AND LIABILITY:
 *
 * THE SOFTWARE IS PROVIDED "AS IS".  THE AUTHOR MAKES  NO REPRESENTATIONS OR WARRANTIES,
 * EITHER EXPRESS OR IMPLIED.  TO THE EXTENT NOT PROHIBITED BY LAW, IN NO EVENT WILL THE
 * AUTHOR  BE LIABLE FOR ANY DAMAGES, INCLUDING WITHOUT LIMITATION, LOST REVENUE,  PROFITS
 * OR DATA, OR FOR SPECIAL, INDIRECT, CONSEQUENTIAL, INCIDENTAL  OR PUNITIVE DAMAGES,
 * HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF  LIABILITY, ARISING OUT OF OR RELATED TO
 * ANY FURNISHING, PRACTICING, MODIFYING OR ANY USE OF THE SOFTWARE, EVEN IF THE AUTHOR
 * HAVE BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 *
 * $id$
 */
package net.sourceforge.metrics.core;

import org.eclipse.jdt.core.IJavaElement;

/**
 * static constants used in many places.  Some tie together portions of plugin.xml
 * with code.
 * 
 * @author Frank Sauer
 */
public interface Constants {

	/** project level source */
    public final static int PROJECT = IJavaElement.JAVA_PROJECT; // now 2, formerly 6;
	/** source folder level source */
    public final static int PACKAGEROOT = IJavaElement.PACKAGE_FRAGMENT_ROOT; // now 3, formerly 5;
	/** package level source */
    public final static int PACKAGEFRAGMENT = IJavaElement.PACKAGE_FRAGMENT; // 4;
	/** compilation unit level source */
    public final static int COMPILATIONUNIT = IJavaElement.COMPILATION_UNIT; // now 5, formerly 3;
	/** class level source */
    public final static int TYPE = IJavaElement.TYPE; // now 7, formerly 2;
	/** method level source */
    public final static int METHOD = IJavaElement.METHOD; // now 9, formerly 1

	// basic metric ids

	/** "NBD" */
	public final static String NESTEDBLOCKDEPTH = "NBD";
	/** "PAR" */
	public final static String PARMS = "PAR";
	/** "VG" */
	public final static String MCCABE = "VG";
	/** "NOM" */
	public final static String NUM_METHODS = "NOM";
	/** "NSM" */
	public final static String NUM_STAT_METHODS = "NSM";
	/** "NSF" */
	public final static String NUM_STAT_FIELDS = "NSF";
	/** "NOF" */
	public final static String NUM_FIELDS = "NOF";
	/** "NOC" */
	public final static String NUM_TYPES = "NOC";
	/** "NOP" */
	public final static String NUM_PACKAGES = "NOP";
	/** "NOI" */
	public final static String NUM_INTERFACES = "NOI";
	/** "DIT" */
	public final static String INHERITANCE_DEPTH = "DIT";
	/** "NSC" */
	public final static String SUBCLASSES = "NSC";
	/** "NUC" */
	public final static String SUPERCLASSES = "NUC";
	/** "U" */
	public final static String REUSE_RATIO = "U";
	/** "SIX" */
	public final static String SPECIALIZATION_IN = "SIX";
	/** "NORM" */
	public final static String NORM = "NORM";
	/** "WMC" */
	public final static String WMC = "WMC";

    /* Cohesion Constants */

    /** "DCD" - Degree of Cohesion (Direct)*/
    public final static String DCD = "DCD";
    /** "DCI" - Degree of Cohesion (Indirect)*/
    public final static String DCI = "DCI";
    /** "JAC" - Java Aware Cohesion */
    public final static String JAC = "JAC";
    /** "LCC" - Loose Class Cohesion */
    public final static String LCC = "LCC";
    /** "LCOM" - Lack Of Cohesion of Methods */
	public final static String LCOM = "LCOM";
    /** "LCOMHS" - Lack Of Cohesion of Methods - Henderson-Sellers */
    public static final String LCOMHS = "LCOMHS";
    /** "LCOMCK" - Lack Of Cohesion of Methods - Chidamber & Kemmerer */
    public static final String LCOMCK = "LCOMCK";
    /** "TCC" - Tight Class Cohesion */
    public final static String TCC = "TCC";
    
    /* Coupling Constants */

    /** Coupling Between Objects (CBO) */
    public final static String CBO = "CBO";
	/** "RMC" */
	public final static String RMC = "RMC";
    /** Coupling - Afferent (CA) */
	public final static String CA = "CA";
    /** Coupling - Efferent (CE) */
	public final static String CE = "CE";

	/** "RMI" */
	public final static String RMI = "RMI";
	/** "RMA" */
	public final static String RMA = "RMA";
	/** "RMD" */
	public final static String RMD = "RMD";
	/** "MLOC" */
	public final static String MLOC = "MLOC";
	/** "TLOC" */
	public final static String TLOC = "TLOC";

	// scopes for averages and maxima
	/** "method" */
	public final static String PER_METHOD = "method";
	/** "type" */
	public final static String PER_CLASS = "type";
	/** "packageFragment" */
	public final static String PER_PACKAGE = "packageFragment";

    public static final String[] PER_ARRAY = new String[] { PER_PACKAGE,
	    PER_CLASS, PER_METHOD };

	// namespaces for persistent storage and in-memory caching
	public final static String PLUGIN_ID = "net.sourceforge.metrics";

	public final static int FRACTION_DIGITS = 3;

}
