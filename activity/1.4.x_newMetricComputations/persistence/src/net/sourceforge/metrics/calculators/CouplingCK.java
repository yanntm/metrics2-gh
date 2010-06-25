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
package net.sourceforge.metrics.calculators;

import java.util.HashSet;
import java.util.Set;

import net.sourceforge.metrics.core.Log;
import net.sourceforge.metrics.core.Metric;
import net.sourceforge.metrics.core.sources.AbstractMetricSource;
import net.sourceforge.metrics.core.sources.TypeMetrics;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

/**
 * Calculates coupling between objects (CBO). "CBO for a class is a count of the
 * number of noninheritance related couples with other classes." Two classes are
 * considered to be coupled when "methods of one use methods or instance
 * variables of another."  This class makes the assumption that CBO will be
 * calculated only between classes in the same project.
 * 
 * @see CHIDAMBER, S., AND KEMERER, C. Towards a metrics suite for object
 *      oriented design. Proceedings OOPSLA '91 (1991).
 * @author Keith Cassell
 */
public class CouplingCK extends Calculator {
	// TODO have user preference about which coupled objects should
	// be counted

	/**
	 * @param name
	 */
	public CouplingCK() {
		super(CBO);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.sourceforge.metrics.core.ICalculator#calculate(net.sourceforge.metrics
	 * .core.sources.AbstractMetricSource)
	 */
	@Override
	public void calculate(AbstractMetricSource source)
			throws InvalidSourceException {
		if (source.getJavaElement().getElementType() != TYPE) {
			throw new InvalidSourceException("CBO only works on Types/Classes");
		}
		TypeMetrics metrics = (TypeMetrics) source;
		Set<String> clients = calculateClients(metrics);
		Set<String> servers = calculateServers(metrics);
		
		// Clients now becomes all directly associated classes, excluding
		// the class itself
		clients.addAll(servers);
		clients.remove(source.getJavaElement().getHandleIdentifier());
		Metric metric = new Metric(CBO, clients.size());
		source.setValue(metric);
	}

	/**
	 * Find all classes that access methods or fields in this class.
	 * @param source
	 * @return the classes that have methods that reference methods or
	 * fields in this class
	 */
	private Set<String> calculateClients(TypeMetrics source) {
		Set<String> clients = null;
		IType aType = (IType) source.getJavaElement();
		try {
			SearchPattern pattern = SearchPattern.createPattern(aType,
					IJavaSearchConstants.REFERENCES);
			IJavaSearchScope scope = createProjectSearchScope(aType);
			SearchEngine searchEngine = new SearchEngine();
			ClientCollector collector = new ClientCollector(source);
			SearchParticipant[] participants = new SearchParticipant[] {
					SearchEngine.getDefaultSearchParticipant() };
			searchEngine.search(pattern, participants, scope, collector, null);
			clients = collector.getResult();
		} catch (CoreException e) {
			Log.logError("Error calculating clients for CBO: ", e);
		}
		return clients;
	}

	/**
	 * Find all classes that this class accesses.
	 * @param source
	 * @return the classes that have methods or
	 * fields accessed by this class
	 */
	private Set<String> calculateServers(TypeMetrics source) {
		Set<String> servers = null;
		IJavaElement callerElement = source.getJavaElement();

		try {
			SearchEngine searchEngine = new SearchEngine();
			ServerCollector collector = new ServerCollector(callerElement);
			searchEngine.searchDeclarationsOfReferencedTypes(callerElement, collector, null);
			servers = collector.getResult();
		} catch (CoreException e) {
			Log.logError("Error calculating servers for CBO: ", e);
		}
		return servers;
	}
	
	/**
	 * Create a search scope consisting of this element's project.
	 * @param element
	 * @return the scope
	 */
	private IJavaSearchScope createProjectSearchScope(IJavaElement element)
			throws JavaModelException {
		IJavaProject project =
			(IJavaProject) element.getAncestor(IJavaElement.JAVA_PROJECT);
	    IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] {project});
		return scope;
	}

	/**
	 * Uses the jdt searchengine to collect all classes outside this package
	 * that depend on things inside this package'
	 */
	public static class ClientCollector extends SearchRequestor {

		/** The set of handles of ITypes (the calling classes). */
		private Set<String> results = null;

		public ClientCollector(TypeMetrics source) {
		}

		/** @return The set of handles of ITypes (the calling classes). */
		public Set<String> getResult() {
			return results;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jdt.core.search.SearchRequestor#beginReporting()
		 */
		@Override
		public void beginReporting() {
			results = new HashSet<String>();
		}

		/**
		 * Adds the handle of the IType that contains this element to the results.
		 * org.eclipse.jdt.core.search.SearchRequestor#acceptSearchMatch(org
		 * .eclipse.jdt.core.search.SearchMatch)
		 */
		@Override
		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			IJavaElement enclosingElement = (IJavaElement) match.getElement();
			if (enclosingElement != null) {
				IJavaElement element = enclosingElement.getAncestor(IJavaElement.TYPE);
				results.add(element.getHandleIdentifier());
				// TODO inheritance check
			}
		}

	}	// class ClientCollector
	

	public static class ServerCollector extends SearchRequestor
	// implements IJavaSearchResultCollector
	{
		/** The element whose called classes are to be found. */
		IJavaElement callerElement = null;
		
		/** The type element whose called classes are to be found. */
		IType callerType = null;
		
		/** The handles of the classes that are depended on. */
		Set<String> results = null;

		public ServerCollector(IJavaElement callerElement) {
			this.callerElement = callerElement;
			callerType = (IType) callerElement.getAncestor(IJavaElement.TYPE);
		}

		/**
		 * @return
		 */
		public Set<String> getResult() {
			return results;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jdt.core.search.SearchRequestor#beginReporting()
		 */
		@Override
		public void beginReporting() {
			results = new HashSet<String>();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jdt.core.search.SearchRequestor#acceptSearchMatch(org
		 * .eclipse.jdt.core.search.SearchMatch)
		 */
		@Override
		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			IJavaElement enclosingElement = (IJavaElement) match.getElement();
			if (enclosingElement != null) {
				IJavaElement typeElement =
					enclosingElement.getAncestor(IJavaElement.TYPE);

				if (typeElement != null) {
					IJavaProject callerProject = callerElement.getJavaProject();
					IJavaProject calleeProject = typeElement.getJavaProject();

					if (callerProject != null
							&& callerProject.equals(calleeProject)) {
//TODO					> > I want to find an IType element that represents the superclass of some
//						> > IType element.
//
//						> Try IType#newSupertypeHierarchy().
						results.add(typeElement.getHandleIdentifier());
					}
				}
			}
		}

	}	// class ServerCollector

}
