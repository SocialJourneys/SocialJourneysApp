package dotrural.ac.uk.journeys;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import dotrural.ac.uk.utils.DbConnect;

public class JourneyObserver extends Thread {

	private HashMap<String, ArrayList<Journey>> sortedJourneys;
	PreparedStatement pst = null;
	static private Logger logger = Logger.getLogger(JourneyObserver.class);

	ResultSet rs = null;

	public JourneyObserver() {

		this.sortedJourneys = new HashMap<String, ArrayList<Journey>>();

		sortedJourneys.put("completed", new ArrayList<Journey>());
		sortedJourneys.put("ongoing", new ArrayList<Journey>());
		sortedJourneys.put("upcoming", new ArrayList<Journey>());

		// databaseConnection = (new DbConnect ()).getConnection();

	}


	public void run() {

		while (true) {
			HashMap<String, ArrayList<Journey>> tempSortedJourneys = new HashMap<String, ArrayList<Journey>>();

			tempSortedJourneys.put("completed", new ArrayList<Journey>());
			tempSortedJourneys.put("ongoing", new ArrayList<Journey>());
			tempSortedJourneys.put("upcoming", new ArrayList<Journey>());

			long t = System.currentTimeMillis();
			try {
				// databaseConnection = DriverManager.getConnection(url, user,
				// password);

				
				
				DbConnect connectionObject = new DbConnect();
				pst = connectionObject.getDbConnect().prepareStatement(
						"Select participant.twitter_handle,journey.id,journey.name,journey.bus_stop_code, journey.origin_master, journey.destination_master, journey.days_travelling,journey.alert_time, journey.time_of_departure, journey.time_of_arrival, journey.stages from participant inner join journey on journey.id = ANY (participant.journeys) where  ('"
								+ getCurrentDay()
								+ "' = any(journey.days_travelling) AND (journey.status = 'TRUE')) Order by journey.time_of_departure asc;");
				rs = pst.executeQuery();
				// System.out.println("checking for journeys");
				connectionObject.closeDbConnect();

				while (rs.next()) {
					Journey newJourney = new Journey();
					newJourney.setID(rs.getString("id"));
					if (rs.getString("bus_stop_code") != null) {
						newJourney.setBusStopCode(rs.getString("bus_stop_code"));
					}
					newJourney.setTraveler(rs.getString("twitter_handle"));
					newJourney.setStartMinutes(parseTime(Integer.parseInt(rs.getString("time_of_departure"))));
					newJourney.setEndsMinutes(parseTime(Integer.parseInt(rs.getString("time_of_arrival"))));
					newJourney.setDelay(Integer.parseInt(rs.getString("alert_time")));

					// get serivces
					// System.out.println ( "Stages" + rs.getString("stages"));

					connectionObject = new DbConnect();
					pst = connectionObject.getDbConnect().prepareStatement(
							"Select bus_routes from journey_stage where id = ANY ('" + rs.getString("stages") + "');");
					ResultSet rs2 = pst.executeQuery();

					connectionObject.closeDbConnect();

					ArrayList busRoutes = new ArrayList();
					// add sevrices to journey object
					while (rs2.next()) {
						String my_new_str = rs2.getString("bus_routes").replaceAll("\\{", "");
						my_new_str = my_new_str.replaceAll("\\}", "");
						String[] journeyStage = my_new_str.split(",");

						for (int j = 0; j < journeyStage.length; j++) {
							busRoutes.add(journeyStage[j]);
						}

					}

					newJourney.setBusRoutes(busRoutes);

					// find out services for the first stages
					connectionObject = new DbConnect();
					pst = connectionObject.getDbConnect().prepareStatement(
							"Select journey_stage.bus_routes from journey inner join journey_stage on journey_stage.id =ANY (journey.stages) WHERE journey_stage.id = ANY ('"
									+ rs.getString("stages")
									+ "') AND journey.origin_master=journey_stage.origin_bus_stop;");
					ResultSet rs3 = pst.executeQuery();

					connectionObject.closeDbConnect();

					ArrayList busRoutesInitialStage = new ArrayList();
					// add sevrices to journey object
					while (rs3.next()) {
						String my_new_str = rs3.getString("bus_routes").replaceAll("\\{", "");
						my_new_str = my_new_str.replaceAll("\\}", "");
						String[] journeyStage = my_new_str.split(",");

						for (int j = 0; j < journeyStage.length; j++) {
							busRoutesInitialStage.add(journeyStage[j]);
						}

					}

					newJourney.setBusRoutesInitialStage(busRoutesInitialStage);

					tempSortedJourneys = sortJourney(newJourney, tempSortedJourneys);

					// System.out.println (rs.getString("twitter_handle") + " "
					// +getCurrentMinutes() + " " + newJourney.getStartMinutes()
					// +" "+ newJourney.getEndsMinutes() +" "
					// +newJourney.getDelay()+" "
					// +newJourney.getBusRoutes().get(0));

				}

				rs.close();
				pst.close();

			} catch (SQLException e1) {
				logger.error("SQL Error getting journeys ", e1);
			}

			sortedJourneys = tempSortedJourneys;

			logger.trace("getting journeys,"+(System.currentTimeMillis()-t)+"," + sortedJourneys.get("ongoing").size());
			
			try {
				TimeUnit.SECONDS.sleep(60);
			} catch (InterruptedException e) {
				logger.error("Error sleeping ", e);
			}
		}
	}

	private boolean isOngoing(Journey journey) {

		boolean response = false;
		int currentMinutes = getCurrentMinutes();

		if (((journey.getStartMinutes() - journey.getDelay()) <= currentMinutes)
				&& (currentMinutes <= journey.getEndsMinutes())) {
			response = true;
		}

		return response;
	}

	private boolean isCompleted(Journey journey) {

		boolean response = false;
		int currentMinutes = getCurrentMinutes();

		if (((journey.getStartMinutes() - journey.getDelay()) < currentMinutes)
				&& (getCurrentMinutes() > journey.getEndsMinutes())) {
			response = true;
		}
		return response;
	}

	private int getCurrentMinutes() {
		int currentMinutes = 0;

		Calendar calendar = GregorianCalendar.getInstance();

		currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

		return currentMinutes;
	}

	private String getCurrentDay() {
		String day = "";
		Calendar calendar = GregorianCalendar.getInstance();

		switch (calendar.get(Calendar.DAY_OF_WEEK)) {
		case 1:
			day = "sunday";
			break;
		case 2:
			day = "monday";
			break;
		case 3:
			day = "tuesday";
			break;
		case 4:
			day = "wednesday";
			break;
		case 5:
			day = "thursday";
			break;
		case 6:
			day = "friday";
			break;
		case 7:
			day = "saturday";
			break;
		}

		return day;
	}


	private HashMap<String, ArrayList<Journey>> sortJourney(Journey journey,
			HashMap<String, ArrayList<Journey>> sortedJourneys) {

		if (isCompleted(journey)) {
			sortedJourneys.get("completed").add(journey);
		}

		else if (isOngoing(journey)) {
			sortedJourneys.get("ongoing").add(journey);
		}

		else {
			sortedJourneys.get("upcoming").add(journey);
		}

		return sortedJourneys;
	}

	private int parseTime(int time) {
		int resultMinutes;

		int hours = time / 100;
		int minutes = time % 100;
		resultMinutes = (hours * 60) + minutes;

		return resultMinutes;
	}

	public HashMap getSortedJourneys() {
		return sortedJourneys;
	}

}
