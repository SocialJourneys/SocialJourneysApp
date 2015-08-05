package dotrural.ac.uk.twitter_manager;

import java.util.ArrayList;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import dotrural.ac.uk.constants.PredefinedConstants;

public class EventsEffectsOnBusServicesChecker {

	public void checkAnnotationsAndInfer(DatasetAccessor mainEventsStorage,
			Model messageAnnotationsReturnedByKIMandInferences) {

		DatasetAccessor busServicesStoreAccessor = DatasetAccessorFactory
				.createHTTP(PredefinedConstants.FUSEKI_BUS_SERVICES_URI);

		Property p = ResourceFactory.createProperty("http://vocab.org/transit/terms/service");

		boolean inferenecesBetweenBusServicesAndEventsExist = messageAnnotationsReturnedByKIMandInferences
				.containsLiteral(null, p, null);

		// if not inferences between bus services and events exist, try use the
		// street names and bus stops
		if (!inferenecesBetweenBusServicesAndEventsExist) {

			/// the code for handling the relations beteen busroutes and tweets
			/// goes here

			// query for ways and busstops objects in the

			ArrayList<String> resources = getStreetAndBusStopURIs(messageAnnotationsReturnedByKIMandInferences);

			ArrayList<String> relevantBusroutes = matchBusRouteToStreetOrBusStop(resources,
					busServicesStoreAccessor.getModel(), new ArrayList());

			if (!relevantBusroutes.isEmpty()) {
				createTriples(messageAnnotationsReturnedByKIMandInferences, mainEventsStorage, relevantBusroutes);
			}

		}

	}

	private void createTriples(Model messageAnnotationsReturnedByKIMandInferences, DatasetAccessor mainEventsStorage,
			ArrayList<String> relevantBusroutes) {
		OntModel newTriples = ModelFactory.createOntologyModel();
		QueryExecution queryExecution;
		Query query = new Query();

		String queryString = "Select ?event" + "WHERE {"
				+ "?event <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/NET/c4dm/event.owl#Event>. "
				+ "}";

		queryExecution = QueryExecutionFactory.create(queryString, messageAnnotationsReturnedByKIMandInferences);
		ResultSet results = queryExecution.execSelect();

		while (results.hasNext()) {
			QuerySolution rs = results.next();
			// Iterator <String> it = rs.varNames();
			String varName = "event";
			if (rs.get(varName).isResource()) {

				Individual eventResource = newTriples.createIndividual(rs.get(varName).asResource().getURI(),
						newTriples.createClass("http://purl.org/NET/c4dm/event.owl#Event"));

				for (int i = 0; i < relevantBusroutes.size(); i++) {

					// relate annotation to tweet
					Individual busRouteURI = newTriples.createIndividual(relevantBusroutes.get(i),
							newTriples.createClass("http://sj.abdn.ac.uk/ontology/BusRoute"));

					eventResource.setPropertyValue(newTriples.createProperty("http://vocab.org/transit/terms/service"),
							busRouteURI);

				}
			}

		}

		mainEventsStorage.add(newTriples);
	}

	private ArrayList getStreetAndBusStopURIs(Model model) {

		ArrayList result = new ArrayList();

		QueryExecution queryExecution;
		Query query = new Query();

		String queryString = "Select ?resource" + "WHERE {"
				+ "{?resource <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://sj.abdn.ac.uk/ontology/Highway>.} "
				+ "UNION"
				+ "{?resource <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://transport.data.gov.uk/def/naptan/Stop>.}"
				+ "}";

		queryExecution = QueryExecutionFactory.create(queryString, model);
		ResultSet results = queryExecution.execSelect();

		while (results.hasNext()) {
			QuerySolution rs = results.next();
			// Iterator <String> it = rs.varNames();
			String varName = "resource";
			if (rs.get(varName).isResource()) {

				if (!result.contains(rs.get(varName).asResource().getURI())) {
					result.add(rs.get(varName).asResource().getURI());
				}
			}

		}

		return result;
	}

	private ArrayList matchBusRouteToStreetOrBusStop(ArrayList<String> resourcesURIs, Model model,
			ArrayList relevantBusroutes) {
		QueryExecution queryExecution;
		Query query = new Query();

		for (int i = 0; i < resourcesURIs.size(); i++) {

			String resourceInstanceURI = resourcesURIs.get(i);

			String queryString = "Select ?route ?label " + "WHERE {"
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
						relevantBusroutes.add(rs.get(varName).asResource().getURI());
					}
				}

			}

		}

		return relevantBusroutes;

	}

}
