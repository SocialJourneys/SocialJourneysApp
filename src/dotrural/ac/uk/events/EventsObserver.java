package dotrural.ac.uk.events;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.ClientProtocolException;
import org.apache.jena.atlas.web.HttpException;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;

import dotrural.ac.uk.constants.PredefinedConstants;
import dotrural.ac.uk.journeys.Journey;
import dotrural.ac.uk.journeys.JourneyObserver;
import dotrural.ac.uk.nlg.NLG_Factory;
import dotrural.ac.uk.store.JenaStore;

public class EventsObserver extends Thread {

	private JourneyObserver journeyObs;
	private JenaStore store;
	final static Logger logger = Logger.getLogger(EventsObserver.class);

	public EventsObserver(JourneyObserver journeyObs, JenaStore store) {
		this.journeyObs = journeyObs;
		this.store = store;
		HashMap userKnowsAbout = new HashMap();
	}

	public EventsObserver(JourneyObserver journeyObs) {
		this.journeyObs = journeyObs;

		HashMap userKnowsAbout = new HashMap();
	}

	public void run() {

		HashMap sentMessages = new HashMap<String, ArrayList<String>>();
		ArrayList moreThan5MinsPrejourney = new ArrayList();
		ArrayList lessThan5MinsPrejourney = new ArrayList();

		if (logger.isInfoEnabled()) {
			logger.info("Starting events observer thread.");
		}

		while (true) {
			// System.out.println("more than 5 min array"
			// +moreThan5MinsPrejourney + "less than 5 min array" +
			// lessThan5MinsPrejourney + "\n");
			// decide if delays affecting services that current journeys need
			// exist
			// THIS NEEDS to BE TWEAKED !!!!!!
			ArrayList<Journey> ongoingJourneys = (ArrayList<Journey>) ((ArrayList<Journey>) journeyObs
					.getSortedJourneys().get("ongoing")).clone();

//			if (ongoingJourneys.size() > 0)
//				logger.info("ongoing journeys " + ongoingJourneys.size());

			// System.out.println("Active Journeys " +ongoingJourneys.size());

			// see if journey is in pre-journeystage and send relevant messages
			for (int i = 0; i < ongoingJourneys.size(); i++) {

				Journey journey = ongoingJourneys.get(i);
				int currentMinutes = getCurrentMinutes();

				/*
				 * System.out.println("looking into pre-journey stage of journey "
				 * + ongoingJourneys.get(i).getID() + " owned by " +
				 * ongoingJourneys.get(i).getTraveller() + " current mins are "
				 * + currentMinutes + " journey mins are " +
				 * journey.getStartMinutes() + "more than 5 min array"
				 * +moreThan5MinsPrejourney + "less than 5 min array" +
				 * lessThan5MinsPrejourney + "\n");
				 */
				System.out.println(journey.getStartMinutes() + " "
						+ currentMinutes);

				if ((journey.getStartMinutes() > currentMinutes)
						&& (!(moreThan5MinsPrejourney.contains(journey.getID())))) {
					/*
					 * System.out .println("Journey " +
					 * ongoingJourneys.get(i).getID() +
					 * " in pre-journey stage. Minutes untill departure: " +
					 * (journey.getStartMinutes() - currentMinutes));
					 */
					if ((journey.getStartMinutes() - currentMinutes) > 5) {
						try {
							long t = System.currentTimeMillis();
							MessageHandler
									.handlePrejourneyMessageMoreThan5MinutesBeforeTheJourney(
											sentMessages,
											ongoingJourneys.get(i));
							logger.trace("send >5 msg,"
									+ (System.currentTimeMillis() - t) + ","
									+ journey.getID() + ",");
							moreThan5MinsPrejourney.add(journey.getID());
						} catch (ClientProtocolException e) {
							logger.debug("Exception sending >5 msg "
									+ e.getLocalizedMessage());
						} catch (IOException e) {
							logger.debug("Exception sending >5 msg "
									+ e.getLocalizedMessage());
						} catch (IllegalArgumentException e) {
							logger.debug("Parsing exception sending >5 msg related to NextBus API "
									+ e.getLocalizedMessage());
						}
					}
				} else if (((journey.getStartMinutes() - currentMinutes) <= 5)
						&& ((journey.getStartMinutes() - currentMinutes) > 0)
						&& (!(lessThan5MinsPrejourney.contains(journey.getID())))) {
					moreThan5MinsPrejourney.remove(journey.getID());
					// System.out
					// .println("HEY I AM TRYING to figure out th e5 mins before
					// message");
					try {
						long t = System.currentTimeMillis();
						MessageHandler
								.handlePrejourneyMessage5MinutesBeforeTheJourney(
										sentMessages, ongoingJourneys.get(i));
						logger.trace("send 5 msg,"
								+ (System.currentTimeMillis() - t) + ","
								+ journey.getID() + ",");
						lessThan5MinsPrejourney.add(journey.getID());
					} catch (ClientProtocolException e) {
						logger.debug("Exception sending >5 msg "
								+ e.getLocalizedMessage());
					} catch (IOException e) {
						logger.debug("Exception sending >5 msg "
								+ e.getLocalizedMessage());
					} catch (IllegalArgumentException e) {
						logger.debug("Parsing exception sending >5 msg related to NextBus API "
								+ e.getLocalizedMessage());
					}

				} else if ((journey.getStartMinutes() - currentMinutes) <= 0) {
					if (lessThan5MinsPrejourney.contains(journey.getID())) {
						lessThan5MinsPrejourney.remove(journey.getID());
					}
				}
			}

			// see if there is a disruption affecting individual journeys and
			// send relevant messages
			for (int i = 0; i < ongoingJourneys.size(); i++) {
				// System.out.println("looking into journey owned by "
				// +ongoingJourneys.get(i).getTraveller());
				HashMap result = checkJourneyForEvents(ongoingJourneys.get(i));
				long t = System.currentTimeMillis();
				MessageHandler.handleDisruptionEventMessage(result,
						sentMessages, ongoingJourneys.get(i));
				logger.trace("sent disruption msg,"
						+ (System.currentTimeMillis() - t) + ","
						+ ongoingJourneys.get(i).getID() + ",");

			}

			try {
				TimeUnit.SECONDS.sleep(20);
			} catch (InterruptedException e) {
				logger.debug("Exception pausing " + e.getLocalizedMessage());
			}
		}
	}

	private ResultSet getEventsForService(String serviceName) {

		QueryExecution queryExecution;

		// HashMap res = new HashMap();

		/*
		 * String queryString = "Select ?source " + "WHERE {" +
		 * "?delay <http://vocab.org/transit/terms/service> <http://sj.abdn.ac.uk/resource/basemap/busLines/ABDN_"
		 * +serviceName+">. " +
		 * "?delay <http://www.w3.org/ns/prov#wasDerivedFrom> ?source " + "}";
		 */
		/*
		 * String queryString =
		 * "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
		 * "PREFIX event: <http://purl.org/NET/c4dm/event.owl#>\n" +
		 * "PREFIX timeline: <http://purl.org/NET/c4dm/timeline.owl#>\n" +
		 * "PREFIX transit: <http://vocab.org/transit/terms/>\n" +
		 * "PREFIX td: <http://purl.org/td/transportdisruption#>\n" +
		 * "SELECT *\n" + "WHERE {\n" +
		 * " ?e a ?eventType; ?p ?o. ?o rdfs:label ?olabel." +
		 * "?e <http://vocab.org/transit/terms/service> <http://sj.abdn.ac.uk/resource/basemap/busLines/ABDN_"
		 * +serviceName+">. " +
		 * "filter (?eventType = td:PublicTransportDelay || ?eventType = td:PublicTransportDiversion ).\n"
		 * + " optional {\n" +
		 * "   ?e event:time/timeline:beginsAtDateTime ?startdatetime.\n" +
		 * " } optional {\n" +
		 * "   ?e event:time/timeline:endsAtDateTime ?enddatetime.\n" + "}  \n"
		 * 
		 * 
		 * + "}";
		 */
		String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
				+ "PREFIX event: <http://purl.org/NET/c4dm/event.owl#>\n"
				+ "PREFIX timeline: <http://purl.org/NET/c4dm/timeline.owl#>\n"
				+ "PREFIX transit: <http://vocab.org/transit/terms/>\n"
				+ "PREFIX td: <http://purl.org/td/transportdisruption#>\n"
				+ "SELECT *\n"
				+ "WHERE {\n"
				+ " ?e a ?eventType. ?e rdfs:label ?elabel."
				+ "?e <http://vocab.org/transit/terms/service> <http://sj.abdn.ac.uk/resource/basemap/busLines/ABDN_"
				+ serviceName
				+ ">. "
				+ "?eventType rdfs:subClassOf event:Event.\n"
				+ "   ?e event:time/timeline:beginsAtDateTime ?startdatetime.\n"
				+ "bind (now() as ?now)"
				+ "filter (?now >= ?startdatetime)"
				+ "   ?e event:time/timeline:endsAtDateTime ?enddatetime.\n"
				+ "filter (?now <= ?enddatetime)"
				+ ""
				+ "optional {?e <http://purl.org/td/transportdisruptionprops/delayLength>/rdfs:label ?delayLength.} "
				+ "optional {?e <http://purl.org/td/transportdisruptionprops#primaryLocation>/rdfs:label ?primaryLocation.} "
				+ "?e <http://www.w3.org/ns/prov#wasDerivedFrom> ?instance. "
				+ "service <http://sj.abdn.ac.uk/ozStudyD2R/sparql> { "
				+ "?instance  <http://www.dotrural.ac.uk/irp/uploads/ontologies/bottari#messageTimeStamp> ?reportTime. "
				+ "?instance  a ?sourceType.}" + "}  \n";
		ResultSet results = null;
		try {
			long t1 = System.currentTimeMillis();
			DatasetAccessor da = DatasetAccessorFactory
					.createHTTP(PredefinedConstants.FUSEKI_URI);
			logger.trace("Init event dataset,"
					+ (System.currentTimeMillis() - t1) + ",service "
					+ serviceName);
			t1 = System.currentTimeMillis();

			queryExecution = QueryExecutionFactory.create(queryString,
					da.getModel());
			logger.trace("Init event query exec,"
					+ (System.currentTimeMillis() - t1) + ",service "
					+ serviceName);

			t1 = System.currentTimeMillis();
			results = queryExecution.execSelect();
			logger.trace("Perform event query,"
					+ (System.currentTimeMillis() - t1) + ",service "
					+ serviceName);

		} catch (HttpException except) {
			logger.error("Unable to connect to event fuseki - "
					+ except.getMessage());
		}

		return results;
	}

	private HashMap checkJourneyForEvents(Journey journey) {

		HashMap journeyEventsStatus = new HashMap<String, ArrayList<String>>();
		ArrayList journeyBusRoutes = journey.getBusRoutes();
		for (int j = 0; j < journeyBusRoutes.size(); j++) {

			ResultSet temp = getEventsForService((String) journeyBusRoutes
					.get(j));

			NLG_Factory generatedResponses = new NLG_Factory();

			Map map = generatedResponses.getParametersForNLG(temp);

			logger.debug("Map of event nlg responses is " + map);

			for (Object key : map.keySet()) {
				Map value = (Map) map.get(key);
				Set<String> recipientSet = new HashSet<String>();
				recipientSet.add(journey.getTraveller());
				value.put("recipient", recipientSet);
				map.put(key, value);
			}

			if (!map.isEmpty()) {

				ArrayList tempList = new ArrayList();
				tempList.add(map);
				journeyEventsStatus.put((String) journeyBusRoutes.get(j), map);

			}

		}

		return journeyEventsStatus;

	}

	private int getCurrentMinutes() {
		int currentMinutes = 0;

		Calendar calendar = GregorianCalendar.getInstance();

		currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60
				+ calendar.get(Calendar.MINUTE);

		return currentMinutes;
	}

}
