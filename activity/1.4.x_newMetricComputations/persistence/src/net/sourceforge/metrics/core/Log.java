/*
 * Created on May 13, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package net.sourceforge.metrics.core;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Separating this from the MetricsPlugin class breaks numerous cyclic dependencies
 * 
 * @author Frank Sauer
 */
public class Log {

	public final static String PLUGIN_ID = "net.sourceforge.metrics";

	public static void logError(String message, Throwable t) {
		ILog logger = getLog();
		if (logger == null) {
			System.err.println(message);
		} else {
			logger.log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, message, t));
		}
	}

	public static void logWarning(String message, Throwable t) {
		ILog logger = getLog();
		if (logger == null) {
			System.err.println(message);
		} else {
			logger.log(new Status(IStatus.WARNING, PLUGIN_ID, IStatus.WARNING,
					message, t));
		}
	}

	public static void logMessage(String message) {
		ILog logger = getLog();
		if (logger == null) {
			System.out.println(message);
		} else {
			logger.log(new Status(IStatus.INFO, PLUGIN_ID, IStatus.INFO,
					message, null));
		}
	}

	private static ILog getLog() {
		ILog logger = null;
		MetricsPlugin plugin = MetricsPlugin.getDefault();
		if (plugin != null) {
			logger = plugin.getLog();
		}
		return logger;
	}

}
