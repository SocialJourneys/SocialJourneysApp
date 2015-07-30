package dotrural.ac.uk.events;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import dotrural.ac.uk.constants.PredefinedConstants;
import dotrural.ac.uk.journeys.Journey;
import dotrural.ac.uk.journeys.JourneyObserver;
import dotrural.ac.uk.main.RunSocialJourneys;
import dotrural.ac.uk.nlg.NLG_Factory;
import dotrural.ac.uk.store.JenaStore;
import dotrural.ac.uk.utils.HttpRequests;

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
			logger.info("Starting events observer thread. ");
		}

		while (true) {
	//		System.out.println("more than 5 min array" +moreThan5MinsPrejourney + "less than 5 min array" + lessThan5MinsPrejourney + "\n");
			// decide if delays affecting services that current journeys need
			// exist
			// THIS NEEDS to BE TWEAKED !!!!!!
			ArrayList<Journey> ongoingJourneys = (ArrayList<Journey>) ((ArrayList<Journey>) journeyObs
					.getSortedJourneys().get("ongoing")).clone();

			// System.out.println("Active Journeys " +ongoingJourneys.size());

			// see if journey is in pre-journeystage and send relevant messages
			for (int i = 0; i < ongoingJourneys.size(); i++) {
				
				Journey journey = ongoingJourneys.get(i);
				int currentMinutes = getCurrentMinutes();

			/*	System.out.println("looking into pre-journey stage of journey "
						+ ongoingJourneys.get(i).getID() + " owned by "
						+ ongoingJourneys.get(i).getTraveller() + " current mins are " + currentMinutes + " journey mins are " + journey.getStartMinutes()
						+ "more than 5 min array" +moreThan5MinsPrejourney + "less than 5 min array" + lessThan5MinsPrejourney + "\n");
*/
				
				if ((journey.getStartMinutes() > currentMinutes)
						&& (!(moreThan5MinsPrejourney.contains(journey.getID())))) {
		/*			System.out
							.println("Journey "
									+ ongoingJourneys.get(i).getID()
									+ " in pre-journey stage. Minutes untill departure: "
									+ (journey.getStartMinutes() - currentMinutes));
*/
					if ((journey.getStartMinutes() - currentMinutes) > 5) {
						try {
							MessageHandler
									.handlePrejourneyMessageMoreThan5MinutesBeforeTheJourney(
											sentMessages,
											ongoingJourneys.get(i));
							moreThan5MinsPrejourney.add(journey.getID());
						} catch (ClientProtocolException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalArgumentException e) {
							System.out.println("First API issue");
							e.printStackTrace(System.err);
						}
					}
				} else if (((journey.getStartMinutes() - currentMinutes) <= 5)
						&& ((journey.getStartMinutes() - currentMinutes) > 0)
						&& (!(lessThan5MinsPrejourney.contains(journey.getID())))) {
					moreThan5MinsPrejourney.remove(journey.getID());
					//System.out
					//		.println("HEY I AM TRYING to figure out th e5 mins before message");
					try {
						MessageHandler
								.handlePrejourneyMessage5MinutesBeforeTheJourney(
										sentMessages, ongoingJourneys.get(i));
						lessThan5MinsPrejourney.add(journey.getID());
					} catch (ClientProtocolException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if ((journey.getStartMinutes() - currentMinutes) <= 0){
					if (lessThan5MinsPrejourney.contains(journey.getID())){
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
				MessageHandler.handleDisruptionEventMessage(result,
						sentMessages, ongoingJourneys.get(i));
			}

			try {
				TimeUnit.SECONDS.sleep(20);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
				+ " ?e a ?eventType; ?p ?o. ?o rdfs:label ?olabel."
				+ "?e <http://vocab.org/transit/terms/service> <http://sj.abdn.ac.uk/resource/basemap/busLines/ABDN_"
				+ serviceName
				+ ">. "
				+ "filter (?eventType = td:PublicTransportDelay || ?eventType = td:PublicTransportDiversion ).\n"
				+ "   ?e event:time/timeline:beginsAtDateTime ?startdatetime.\n"
				+ "bind (now() as ?now)"
				+ "filter (?now >= ?startdatetime)"
				+ "   ?e event:time/timeline:endsAtDateTime ?enddatetime.\n"
				+ "filter (?now <= ?enddatetime)" + "}  \n";

		DatasetAccessor da = DatasetAccessorFactory
				.createHTTP(PredefinedConstants.FUSEKI_URI);
		queryExecution = QueryExecutionFactory.create(queryString,
				da.getModel());

		ResultSet results = queryExecution.execSelect();

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

			// System.out.println (map);

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
