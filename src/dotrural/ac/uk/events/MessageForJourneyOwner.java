package dotrural.ac.uk.events;

import java.util.HashMap;

import org.apache.log4j.Logger;

import twitter4j.DirectMessage;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

public class MessageForJourneyOwner {

	String message = "Default message";

	long timeStamp;

	String journeyID;

	HashMap parameters;

	private static Logger logger = Logger
			.getLogger(MessageForJourneyOwner.class);

	public MessageForJourneyOwner() {

		// / handle generation of the message using NLG
		parameters = new HashMap();
	}

	public void setParametersUsed(HashMap parameters) {

		this.parameters = parameters;
	}

	public HashMap getParametersUsed(boolean removeReportTime) {
		HashMap map = (HashMap) parameters.clone();
		// System.out.println("Param map " + parameters.toString());
		if (removeReportTime) {
			map.remove("reportTime");
			// System.out.println("Returning map " + map.toString());
		}
		return map;
	}

	public void setMessage(String message) {

		this.message = message;
	}

	public String getMessage() {

		return message;
	}

	public void sentMessage(String messageType, String journeyId,
			String userScreenName) {
		Twitter twitter = new TwitterFactory().getInstance();
		long t = System.currentTimeMillis();
		try {
			logger.info("sending " + message + " to " + userScreenName);
			DirectMessage directMessage = twitter.sendDirectMessage(
					userScreenName, message);
			logger.trace("send dm," + (System.currentTimeMillis() - t) + ","
					+ messageType + "," + directMessage.getId() + ","
					+ userScreenName + "," + journeyId + "" + message);
			logger.info("Direct message successfully sent to "
					+ directMessage.getRecipientScreenName());
		} catch (TwitterException te) {
			logger.error("Failed to send a direct message: " + te.getMessage());
			logger.trace("failed send dm," + (System.currentTimeMillis() - t)
					+ "," + messageType + "," + userScreenName + ","
					+ journeyId + "," + message);

		}
	}

	public void setMessageTime(long timeStamp) {

		this.timeStamp = timeStamp;
	}

	public void setMessageJourneyID(String journeyID) {

		this.journeyID = journeyID;
	}

	public String getMessageJourneyID() {

		return journeyID;
	}

	public long getMessageTime() {

		return timeStamp;
	}

}
