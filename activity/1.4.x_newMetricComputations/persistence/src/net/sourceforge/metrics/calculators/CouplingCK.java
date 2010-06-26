/*
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
 * number of other classes to which it is coupled."  "...two classes are coupled
 * when methods declared in one class use methods or instance variables defined
 * by the other class."  This implementation of CBO considers inheritance
 * to be a form of coupling.
 * It is not possible to know all possible externally defined clients of a
 * class, so this implementation of CBO only considers those clients of a class
 * that exist within the same project.
 * 
 * @see CHIDAMBER, S., AND KEMERER, C.,  "A metrics suite for object
 *      oriented design",  IEEE Transactions on Software Engineering
 *      Vol 20 #6, 1994.
 * @author Keith Cassell
 */
public class CouplingCK extends Calculator {
	// TODO have user preference about which coupled objects should
	// be counted, e.g. inheritance or not, external clients or not

	public CouplingCK() {
		super(CBO);
	}

	/**
	 * Calculates coupling between objects (CBO). "CBO for a class is a count of
	 * the number of other classes to which it is coupled."  "...two classes are
	 * coupled when methods declared in one class use methods or instance
	 * variables defined by the other class." This implementation of CBO
	 * considers inheritance to be a form of coupling. It is not possible to
	 * know all possible externally defined clients of a class, so this
	 * implementation of CBO only considers those clients of a class that exist
	 * within the same project.
	 * @param source
	 * @see net.sourceforge.metrics.core.ICalculator#calculate(net.sourceforge.metrics
	 *      .core.sources.AbstractMetricSource)
	 */
	@Override
	public void calculate(AbstractMetricSource source)
			throws InvalidSourceException {
		if (source.getJavaElement().getElementType() != TYPE) {
			throw new InvalidSourceException("CBO only works on Types/Classes");
		}
		TypeMetrics metrics = (TypeMetrics) source;
		Set<String> coupledClasses = calculateClients(metrics);
		Set<String> servers = calculateServers(metrics);
		
		// CoupledClasses now becomes all directly associated classes, excluding
		// the class itself
		coupledClasses.addAll(servers);
		coupledClasses.remove(source.getJavaElement().getHandleIdentifier());
		Metric metric = new Metric(CBO, coupledClasses.size());
		source.setValue(metric);
	}

	/**
	 * Find all classes that access methods or fields in this class
	 * from within the same project.
	 * @param source metrics for a particular IType
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
	 * @param source metrics for a particular IType
	 * @return the classes that have methods or
	 * fields accessed by this class
	 */
	private Set<String> calculateServers(TypeMetrics source) {
		Set<String> servers = null;
		IJavaElement callerElement = source.getJavaElement();

		try {
			SearchEngine searchEngine = new SearchEngine();
			ServerCollector collector = new ServerCollector(callerElement);
			searchEngine.searchDeclarationsOfReferencedTypes(
					callerElement, collector, null);
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
	    IJavaSearchScope scope =
	    	SearchEngine.createJavaSearchScope(new IJavaElement[] {project});
		return scope;
	}

	/**
	 * Uses the JDT SearchEngine to collect all ITypes
	 * that directly depend on members in the specified IType
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

		/**
		 * Sets up an empty collection to contain the results
		 * @see org.eclipse.jdt.core.search.SearchRequestor#beginReporting()
		 */
		@Override
		public void beginReporting() {
			results = new HashSet<String>();
		}

		/**
		 * Adds the handle of the IType that contains this element to the results.
		 * @see org.eclipse.jdt.core.search.SearchRequestor#acceptSearchMatch(org
		 * .eclipse.jdt.core.search.SearchMatch)
		 */
		@Override
		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			IJavaElement enclosingElement = (IJavaElement) match.getElement();
			if (enclosingElement != null) {
				IJavaElement element =
					enclosingElement.getAncestor(IJavaElement.TYPE);
				if (element != null) {
					results.add(element.getHandleIdentifier());
				}
			}
		}

	}	// class ClientCollector
	

	/**
	 * Collects all ITypes that the specified element directly depends on.
	 */
	public static class ServerCollector extends SearchRequestor
	// implements IJavaSearchResultCollector
	{
		/** The element whose called classes are to be found. */
		IJavaElement callerElement = null;
		
		/** The type element whose called classes are to be found. */
		IType callerType = null;
		
		/** The project of the IType. */
		IJavaProject callerProject = null;

		/** The handles of the classes that are depended on. */
		Set<String> results = null;

		public ServerCollector(IJavaElement callerElement) {
			this.callerElement = callerElement;
			callerType = (IType) callerElement.getAncestor(IJavaElement.TYPE);
			callerProject = callerElement.getJavaProject();
		}

		/** @return The set of handles of ITypes (the called classes). */
		public Set<String> getResult() {
			return results;
		}

		/**
		 * Sets up an empty collection to contain the results
		 * @see org.eclipse.jdt.core.search.SearchRequestor#beginReporting()
		 */
		@Override
		public void beginReporting() {
			results = new HashSet<String>();
		}

		/**
		 * Adds the handle of the IType that contains this element to the results.
		 * @see org.eclipse.jdt.core.search.SearchRequestor#acceptSearchMatch(org
		 * .eclipse.jdt.core.search.SearchMatch)
		 */
		@Override
		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			IJavaElement enclosingElement = (IJavaElement) match.getElement();
			if (enclosingElement != null) {
				IJavaElement typeElement =
					enclosingElement.getAncestor(IJavaElement.TYPE);

				if (typeElement != null) {
					IJavaProject calleeProject = typeElement.getJavaProject();

					if (callerProject != null
							&& callerProject.equals(calleeProject)) {
//TODO to find an IType element that is a superclass of some
// IType element, use IType#newSupertypeHierarchy().
						results.add(typeElement.getHandleIdentifier());
					}
				}
			}
		}

	}	// class ServerCollector

}
