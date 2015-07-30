package dotrural.ac.uk.main;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;

import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import dotrural.ac.uk.constants.PredefinedConstants;
import dotrural.ac.uk.events.EventsObserver;
import dotrural.ac.uk.events.MessageForJourneyOwner;
import dotrural.ac.uk.journeys.JourneyObserver;
import dotrural.ac.uk.realTime.NextBusAdaptor;
import dotrural.ac.uk.store.JenaStore;
import dotrural.ac.uk.twitter_manager.DirectMessageObserver;
import dotrural.ac.uk.twitter_manager.TweetObserver;
import dotrural.ac.uk.utils.DbConnect;
import dotrural.ac.uk.utils.HttpRequests;

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
         // turn this back on  	 tweetObserver.start();
		 
	
		if(logger.isInfoEnabled()){
			logger.info("Initiating the direct message observer. ");
		} 
	Thread directMsgObserver  = new DirectMessageObserver ();
   // turn this back on  directMsgObserver.start();
	
		if(logger.isInfoEnabled()){
			logger.info("Initiating the events observer. ");
		} 
	//start disruption observer that checks for disruptions and sends messages
		        Thread eventsObserver = new EventsObserver (journeyObs);
		        eventsObserver.start();
				
	    
	
	
		
	}

}
