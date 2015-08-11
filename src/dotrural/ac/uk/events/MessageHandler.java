package dotrural.ac.uk.events;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.bind.DatatypeConverter;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;

import dotrural.ac.uk.journeys.Journey;
import dotrural.ac.uk.nlg.NLG_Factory;
import dotrural.ac.uk.realTime.NextBusAdaptor;
import dotrural.ac.uk.realTime.Parser;

public class MessageHandler {

	final static Logger logger = Logger.getLogger(MessageHandler.class);

	public static void handleDisruptionEventMessage(HashMap result,
			HashMap sentMessages, Journey ongoingJourney) {

		int count = 0;
		Iterator it = result.keySet().iterator();
		while (it.hasNext()) {

			String service = (String) it.next();
			System.out.println(service);
			HashMap delaysReported = (HashMap) result.get(service);

			Iterator eventsIt = delaysReported.keySet().iterator();

			while (eventsIt.hasNext()) {

				String eventId = (String) eventsIt.next();

				if (!sentMessages.keySet().contains(
						ongoingJourney.getTraveller())) {
					HashMap messages = new HashMap<String, String>();
					sentMessages.put(ongoingJourney.getTraveller(), messages);
				}

				if (!(((HashMap) sentMessages
						.get(ongoingJourney.getTraveller())))
						.containsKey("Messages")) {
					(((HashMap) sentMessages.get(ongoingJourney.getTraveller())))
							.put("Messages", new ArrayList());
				}

				HashMap parameters = (HashMap) delaysReported.get(eventId);
				NLG_Factory nlg = new NLG_Factory();
				String msg = nlg.getGeneratedMessage(parameters);

				MessageForJourneyOwner newMessage = new MessageForJourneyOwner();

				newMessage.setParametersUsed(parameters);

				// String msg ="nlg service not working";
				newMessage.setMessage(msg);

				Date dt = new Date();
				long msgTime = dt.getTime();
				newMessage.setMessageTime(msgTime);

				String journeyID = ongoingJourney.getID();
				newMessage.setMessageJourneyID(journeyID);

				ArrayList alreadySentMessages = (ArrayList) (((HashMap) sentMessages
						.get(ongoingJourney.getTraveller()))).get("Messages");

				// send if not already sent once
				boolean dispatchDecision = checkDispatchStatus(newMessage,
						alreadySentMessages);

				if (dispatchDecision) {
					// System.out.println (msg);
					if (logger.isInfoEnabled()) {
						logger.info("Sending doruption message to user: "
								+ ongoingJourney.getTraveller());
						logger.info("Message: " + newMessage.getMessage());
						logger.info("Journey ID: " + ongoingJourney.getID());
						logger.info("Event ID: " + eventId);
						logger.info("Parameters: "
								+ newMessage.getParametersUsed());
					}

					newMessage.sentMessage(ongoingJourney.getTraveller());

					((ArrayList) (((HashMap) sentMessages.get(ongoingJourney
							.getTraveller()))).get("Messages")).add(newMessage);
				}

			}
		}
	}

	public static void handlePrejourneyMessageMoreThan5MinutesBeforeTheJourney(
			HashMap sentMessages, Journey ongoingJourney)
			throws ClientProtocolException, IOException {

		logger.info("handling pre 5 messages");
		
		if (!sentMessages.keySet().contains(ongoingJourney.getTraveller())) {
			HashMap messages = new HashMap<String, String>();
			sentMessages.put(ongoingJourney.getTraveller(), messages);
		}

		if (!(((HashMap) sentMessages.get(ongoingJourney.getTraveller())))
				.containsKey("Messages")) {
			(((HashMap) sentMessages.get(ongoingJourney.getTraveller()))).put(
					"Messages", new ArrayList());
		}

		NextBusAdaptor nxt = new NextBusAdaptor();

		String xml = nxt.getLiveDepartures(ongoingJourney.getBusStopCode());
		// System.out.println(xml);

		for (int i = 0; i < ongoingJourney.getBusRoutes().size(); i++) {
			String closesToTheJourneyStart = "";
			long busTime = -1;
			int tempMinutes = 10;

			Parser pars = new Parser(xml, (String) ongoingJourney
					.getBusRoutes().get(i));
			pars.runParsing();
			ArrayList<String> times = pars.getParsingResult();
			logger.info("Retrieved times for service "
					+ ongoingJourney.getBusRoutes().get(i) + ": "
					+ times.toString());

			if (times.isEmpty()) {
				logger.info("Error with response from NextBus API so not sending any real time message for journey "
						+ ongoingJourney.getID()
						+ " route "
						+ ongoingJourney.getBusRoutes().get(i));
				continue;
			}

			if (!times.isEmpty()) {

				for (int j = 0; j < times.size(); j++) {
					try {
						Calendar dt = DatatypeConverter.parseDateTime(times
								.get(j));
						// System.out.println
						// (ongoingJourney.getStartMinutes());

						int journeyStart = ongoingJourney.getStartMinutes();
						int minutes = dt.get(Calendar.MINUTE)
								+ (dt.get(Calendar.HOUR_OF_DAY) * 60);

						if (tempMinutes >= Math.abs((journeyStart - minutes))) {
							tempMinutes = Math.abs((journeyStart - minutes));
							SimpleDateFormat format1 = new SimpleDateFormat(
									"HH:mm");
							String formatted = format1.format(dt.getTime());
							busTime = (dt.getTime()).getTime();
							closesToTheJourneyStart = formatted;
						}
					} catch (IllegalArgumentException iae) {
						logger.error("Error parsing DateTime from NextBus API - value ("
								+ times.get(j) + ") error: " + iae.getMessage());
					}

				}

				String msg;

				// System.out.println("got times");
				// System.out.println(closesToTheJourneyStart);
				if (!closesToTheJourneyStart.equals("") && busTime != -1) {

					// HashMap parameters = (HashMap)
					// delaysReported.get(eventId);
					NLG_Factory nlg = new NLG_Factory();

					Date dt2 = new Date();

					long currentTime = dt2.getTime();

					long diff = busTime - currentTime;
					long diffMinutes = diff / (60 * 1000) % 60;

					String data = "{\"type\":\"RealTime\",\"service\":\"service "
							+ ongoingJourney.getBusRoutes().get(i)
							+ "\",\"duration\":\""
							+ diffMinutes
							+ "mins\",\"serviceTime\":\""
							+ closesToTheJourneyStart
							+ "\",\"recipient\":\""
							+ ongoingJourney.getTraveller() + "\" }";
					HashMap parameters = new HashMap();
					parameters.put("type", "RealTime");
					parameters.put("service", ongoingJourney.getBusRoutes()
							.get(i));
					parameters.put("serviceTime", closesToTheJourneyStart);

					msg = nlg.getGeneratedMessage(data);
					logger.info("Received message to send " + msg);
					if (!msg.equals("error")) {

						// msg = "Service "+ongoingJourney.getBusRoutes().get(i)
						// + " due at : "+closesToTheJourneyStart;

						MessageForJourneyOwner newMessage = new MessageForJourneyOwner();

						newMessage.setParametersUsed(parameters);
						newMessage.setMessage(msg);

						Date dt = new Date();
						long msgTime = dt.getTime();
						newMessage.setMessageTime(msgTime);

						String journeyID = ongoingJourney.getID();
						newMessage.setMessageJourneyID(journeyID);

						ArrayList alreadySentMessages = (ArrayList) (((HashMap) sentMessages
								.get(ongoingJourney.getTraveller())))
								.get("Messages");

						// send if not already sent once
						boolean dispatchDecision = checkDispatchStatus(
								newMessage, alreadySentMessages);

						if (dispatchDecision) {
							if (logger.isInfoEnabled()) {
								logger.info("Sending realTime message more than 5 minutes before the journey to user: "
										+ ongoingJourney.getTraveller());
								logger.info("Message: "
										+ newMessage.getMessage());
								logger.info("Journey ID: "
										+ ongoingJourney.getID());
								logger.info("Parameters: "
										+ newMessage.getParametersUsed());
							}

							newMessage.sentMessage(ongoingJourney
									.getTraveller());

							((ArrayList) (((HashMap) sentMessages
									.get(ongoingJourney.getTraveller())))
									.get("Messages")).add(newMessage);
						}
					}
				}
			}

		}

	}

	public static void handlePrejourneyMessage5MinutesBeforeTheJourney(
			HashMap sentMessages, Journey ongoingJourney)
			throws ClientProtocolException, IOException {

		if (!sentMessages.keySet().contains(ongoingJourney.getTraveller())) {
			HashMap messages = new HashMap<String, String>();
			sentMessages.put(ongoingJourney.getTraveller(), messages);
		}

		if (!(((HashMap) sentMessages.get(ongoingJourney.getTraveller())))
				.containsKey("Messages")) {
			(((HashMap) sentMessages.get(ongoingJourney.getTraveller()))).put(
					"Messages", new ArrayList());
		}

		// HashMap parameters = (HashMap) delaysReported.get(eventId);
		// NLG_Factory nlg = new NLG_Factory ();
		// String msg = nlg.getGeneratedMessage(parameters);

		NextBusAdaptor nxt = new NextBusAdaptor();

		String xml = nxt.getLiveDepartures(ongoingJourney.getBusStopCode());

		// System.out.println("Got xml");

		for (int i = 0; i < ongoingJourney.getBusRoutes().size(); i++) {
			// System.out.println("parsing");
			Parser pars = new Parser(xml, (String) ongoingJourney
					.getBusRoutes().get(i));
			pars.runParsing();
			ArrayList<String> times = pars.getParsingResult();
			if (times.isEmpty()) {
				logger.error("Error with response from NextBus API so not sending any real time message for journey "
						+ ongoingJourney.getID()
						+ " route "
						+ ongoingJourney.getBusRoutes().get(i));
				continue;
			}

			// System.out.println (times);
			long busTime = -1;
			String timesString = "";

			for (int j = 0; j < times.size(); j++) {

				try {
					Calendar dt = DatatypeConverter.parseDateTime(times.get(j));

					SimpleDateFormat format1 = new SimpleDateFormat("HH:mm");
					String formatted = format1.format(dt.getTime());

					if (j == 0) {
						timesString = formatted;
						busTime = (dt.getTime()).getTime();
					} else if (j < 3) {
						timesString = timesString + "," + formatted;
					}
				} catch (IllegalArgumentException p) {
					logger.error("Error parsing DateTime from NextBus API - value ("
							+ times.get(j) + ") error: " + p.getMessage());
				}

			}
			// System.out.println("got times");
			// System.out.println(times);

			if (!times.isEmpty()) {

				String msg;
				NLG_Factory nlg = new NLG_Factory();

				Date dt2 = new Date();

				long currentTime = dt2.getTime();

				long diff = busTime - currentTime;
				long diffMinutes = diff / (60 * 1000) % 60;

				String data = "{\"type\":\"RealTime\",\"service\":\"service "
						+ ongoingJourney.getBusRoutes().get(i)
						+ "\",\"duration\":\"" + diffMinutes
						+ "mins\",\"serviceTime\":\"" + timesString
						+ "\",\"recipient\":\"" + ongoingJourney.getTraveller()
						+ "\" }";
				HashMap parameters = new HashMap();
				parameters.put("type", "RealTime");
				parameters.put("service", ongoingJourney.getBusRoutes().get(i));
				parameters.put("serviceTime", timesString);

				msg = nlg.getGeneratedMessage(data);

				if (!msg.equals("error")) {

					// msg =
					// "Next departures of Service "+ongoingJourney.getBusRoutes().get(i)+" due at "+
					// timesString ;

					MessageForJourneyOwner newMessage = new MessageForJourneyOwner();

					newMessage.setMessage(msg);
					newMessage.setParametersUsed(parameters);
					Date dt = new Date();
					long msgTime = dt.getTime();
					newMessage.setMessageTime(msgTime);

					String journeyID = ongoingJourney.getID();
					newMessage.setMessageJourneyID(journeyID);

					ArrayList<MessageForJourneyOwner> alreadySentMessages = (ArrayList) (((HashMap) sentMessages
							.get(ongoingJourney.getTraveller())))
							.get("Messages");

					for (int k = 0; k < alreadySentMessages.size(); k++) {

					}

					// send if not already sent once
					boolean dispatchDecision = checkDispatchStatus(newMessage,
							alreadySentMessages);

					if (dispatchDecision) {

						if (logger.isInfoEnabled()) {
							logger.info("Sending realTime message  5 minutes before the journey to user: "
									+ ongoingJourney.getTraveller());
							logger.info("Message: " + newMessage.getMessage());
							logger.info("Journey ID: " + ongoingJourney.getID());
							logger.info("Parameters: "
									+ newMessage.getParametersUsed());
						}

						newMessage.sentMessage(ongoingJourney.getTraveller());

						((ArrayList) (((HashMap) sentMessages
								.get(ongoingJourney.getTraveller())))
								.get("Messages")).add(newMessage);
					}

				}
			}

		}

	}

	private static boolean checkDispatchStatus(
			MessageForJourneyOwner newMsgObject,
			ArrayList<MessageForJourneyOwner> sentMessages) {

		boolean decision = true;

		for (int i = 0; i < sentMessages.size(); i++) {

			String newMessage = newMsgObject.getMessage();
			String sentMessage = sentMessages.get(i).getMessage();

			long newMessageTime = newMsgObject.getMessageTime();
			long sentMessageTime = sentMessages.get(i).getMessageTime();

			String newMessageJourneyID = newMsgObject.getMessageJourneyID();
			String sentMessageJourneyID = sentMessages.get(i)
					.getMessageJourneyID();

			if (newMessage.equals(sentMessage)
					&& (!((newMsgObject.getParametersUsed().equals(sentMessages
							.get(i).getParametersUsed())) && (!sentMessages
							.get(i).getParametersUsed().isEmpty())))) {

				long diff = newMessageTime - sentMessageTime;
				long diffMinutes = diff / (60 * 1000) % 60;

				// System.out.println ("Difference between send time of last "
				// +newMessage+ " and new one is " + diffMinutes+ "minutes");

				if (diffMinutes > 5 && diffMinutes < (60 * 12)) {
					if (newMessageJourneyID.equals(sentMessageJourneyID)) {
						// already sent for this journey
						decision = false;
					}
				} else {
					// found that message was sent less than 5 minutes ago
					return false;
				}
			}

			else if ((newMsgObject.getParametersUsed().equals(sentMessages.get(
					i).getParametersUsed()))
					&& (!sentMessages.get(i).getParametersUsed().isEmpty())) {
				if (newMessageJourneyID.equals(sentMessageJourneyID)) {
					decision = false;
				}
			}

		}
		return decision;
	}

}
