package dotrural.ac.uk.main;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import dotrural.ac.uk.constants.PredefinedConstants;
import dotrural.ac.uk.events.EventsObserver;
import dotrural.ac.uk.journeys.JourneyObserver;
import dotrural.ac.uk.twitter_manager.DirectMessageObserver;
import dotrural.ac.uk.twitter_manager.TweetObserver;

public class RunSocialJourneys {

	final static Logger logger = Logger.getLogger(RunSocialJourneys.class);
	
	public static void main(String[] args) throws SQLException {
		
		 System.getProperties().put("proxySet", "true");
		 System.getProperties().put("http.proxyHost", PredefinedConstants.ProxyHost);
		 System.getProperties().put("http.proxyPort", PredefinedConstants.ProxyPort);
		
		if(logger.isInfoEnabled()){
			logger.info("Initiating the app. ");
		}
		
		/*if(logger.isInfoEnabled()){
			logger.info("Initiating the persistence storage. ");
		}*/
		//initiate store 
		//JenaStore store = new JenaStore();
		
		
		if(logger.isInfoEnabled()){
			logger.info("Initiating the journey observer. ");
		}
		//start observing today's journeys
		//every 60 seconds the journeys are retrieved again and sorted use JourneyObserver.getSortedJourneys() to retrieve the hash map with journey objects in arrays ongoing , upcoming, and completed
		JourneyObserver journeyObs = new JourneyObserver ();
		journeyObs.start ();
	    
		
		if(logger.isInfoEnabled()){
			logger.info("Initiating the tweet observer. ");
		}
		 //start observing new tweets in the database and trigger annotation generation
         Thread tweetObserver = new TweetObserver ();

         tweetObserver.start();

	
		if(logger.isInfoEnabled()){
			logger.info("Initiating the direct message observer. ");
		} 

		Thread directMsgObserver  = new DirectMessageObserver ();
		directMsgObserver.start();

	
		if(logger.isInfoEnabled()){
			logger.info("Initiating the events observer. ");
		} 
	//start disruption observer that checks for disruptions and sends messages
		        Thread eventsObserver = new EventsObserver (journeyObs);
		        eventsObserver.start();
				
	    
	
	
		
	}

}
