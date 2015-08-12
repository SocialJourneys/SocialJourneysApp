package dotrural.ac.uk.twitter_manager;

import java.util.ArrayList;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import dotrural.ac.uk.constants.PredefinedConstants;

public class EventsEffectsOnBusServicesChecker {

	private Property serviceProperty;

	public void checkAnnotationsAndInfer(DatasetAccessor mainEventsStorage,
			Model messageAnnotationsReturnedByKIMandInferences) {

		DatasetAccessor busServicesStoreAccessor = DatasetAccessorFactory
				.createHTTP(PredefinedConstants.FUSEKI_BUS_SERVICES_URI);

		serviceProperty = ResourceFactory.createProperty("http://vocab.org/transit/terms/service");

		boolean inferenecesBetweenBusServicesAndEventsExist = messageAnnotationsReturnedByKIMandInferences
				.contains(null, serviceProperty);

		// if not inferences between bus services and events exist, try use the
		// street names and bus stops
		if (!inferenecesBetweenBusServicesAndEventsExist) {

			// / the code for handling the relations beteen busroutes and tweets
			// / goes here

			// query for ways and busstops objects in the

			ArrayList<String> resources = getStreetAndBusStopURIs(messageAnnotationsReturnedByKIMandInferences);

			ArrayList<Resource> relevantBusroutes = matchBusRouteToStreetOrBusStop(resources,
					busServicesStoreAccessor.getModel());

			if (!relevantBusroutes.isEmpty()) {
				createTriples(messageAnnotationsReturnedByKIMandInferences, mainEventsStorage, relevantBusroutes);
			}

		}

	}

	private void createTriples(Model messageAnnotationsReturnedByKIMandInferences, DatasetAccessor mainEventsStorage,
			ArrayList<Resource> relevantBusroutes) {
		OntModel newTriples = ModelFactory.createOntologyModel();
		QueryExecution queryExecution;

		messageAnnotationsReturnedByKIMandInferences.write(System.out);
		System.out.println("------------------");
		
		String queryString = "Select ?event WHERE {"
				+ "?event <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/NET/c4dm/event.owl#Event>. "
				+ "}";

		queryExecution = QueryExecutionFactory.create(queryString, messageAnnotationsReturnedByKIMandInferences);
		ResultSet results = queryExecution.execSelect();

		while (results.hasNext()) {
			QuerySolution rs = results.next();
			// Iterator <String> it = rs.varNames();
			String varName = "event";
			if (rs.get(varName).isResource()) {
				Resource r = rs.get(varName).asResource();
				String rUri= r.getURI();
				for (int i = 0; i < relevantBusroutes.size(); i++) {
					newTriples.add(r, serviceProperty, relevantBusroutes.get(i));
				}
				/*
				 * Individual eventResource =
				 * newTriples.createIndividual(rs.get(
				 * varName).asResource().getURI(), newTriples.createClass(
				 * "http://purl.org/NET/c4dm/event.owl#Event" ));
				 * 
				 * for (int i = 0; i < relevantBusroutes.size(); i++) {
				 * 
				 * // relate annotation to tweet Individual busRouteURI =
				 * newTriples.createIndividual(relevantBusroutes.get(i),
				 * newTriples
				 * .createClass("http://sj.abdn.ac.uk/ontology/BusRoute"));
				 * 
				 * eventResource.setPropertyValue(newTriples.createProperty(
				 * "http://vocab.org/transit/terms/service"), busRouteURI);
				 * 
				 * }
				 */
			}

		}

		mainEventsStorage.add(newTriples);
	}

	private ArrayList<String> getStreetAndBusStopURIs(Model model) {

		ArrayList<String> result = new ArrayList<String>();

		QueryExecution queryExecution;

		String queryString = "Select ?resource WHERE {"
				+ "{?resource <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://sj.abdn.ac.uk/ontology/Highway>.} "
				+ "UNION"
				+ "{?resource <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://transport.data.gov.uk/def/naptan/Stop>.}"
				+ "}";

		queryExecution = QueryExecutionFactory.create(queryString, model);
		ResultSet results = queryExecution.execSelect();
		while (results.hasNext()){
			QuerySolution rs = results.next();
			if (rs==null){
				System.err.println("null result set");
				continue;
			}
			// Iterator <String> it = rs.varNames();
			String varName = results.getResultVars().get(0);
			RDFNode resource = rs.get(varName);
			if (resource != null && resource.isResource()) {
				String uri = resource.asResource().getURI();
				if (!result.contains(uri)) {
					result.add(uri);
				}
			}

		}

		return result;

	}

	private ArrayList<Resource> matchBusRouteToStreetOrBusStop(ArrayList<String> resourcesURIs, Model model) {
		ArrayList<Resource> relevantBusroutes = new ArrayList<Resource>();
		QueryExecution queryExecution;

		for (int i = 0; i < resourcesURIs.size(); i++) {

			String resourceInstanceURI = resourcesURIs.get(i);

			String queryString = "Select ?route ?label  WHERE {"
					+ "?route <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://sj.abdn.ac.uk/ontology/BusRoute>. "
					+ "{?route <http://sj.abdn.ac.uk/ontology/BusRoute#includesWay> <" + resourceInstanceURI + ">.} "
					+ "UNION" + "{?route <http://sj.abdn.ac.uk/ontology/BusRoute#includesStop> <" + resourceInstanceURI
					+ ">.}" + "}";

			queryExecution = QueryExecutionFactory.create(queryString, model);
			ResultSet results = queryExecution.execSelect();

			while (results.hasNext()) {
				QuerySolution rs = results.next();
				// Iterator <String> it = rs.varNames();
				String varName = "route";
				if (rs.get(varName).isResource()) {

					if (!relevantBusroutes.contains(rs.get(varName).asResource().getURI())) {
						relevantBusroutes.add(rs.get(varName).asResource());
					}
				}

			}

		}

		return relevantBusroutes;

	}

}
