package dotrural.ac.uk.events;

import java.util.HashMap;

import twitter4j.DirectMessage;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

public class MessageForJourneyOwner {

	String message = "Default message";

	long timeStamp;

	String journeyID;

	HashMap parameters;

	public MessageForJourneyOwner() {

		// / handle generation of the message using NLG
		parameters = new HashMap();
	}

	public void setParametersUsed(HashMap parameters) {

		this.parameters = parameters;
	}

	public HashMap getParametersUsed(boolean removeReportTime) {
		HashMap map = (HashMap) parameters.clone();
		System.out.println("Param map " + parameters.toString());
		if (removeReportTime) {
			map.remove("reportTime");
			System.out.println("Returning map " + map.toString());
		}
		return map;
	}

	public void setMessage(String message) {

		this.message = message;
	}

	public String getMessage() {

		return message;
	}

	public void sentMessage(String userScreenName) {
		Twitter twitter = new TwitterFactory().getInstance();
		try {
			System.out.println("sending " + message + " to " + userScreenName);
			DirectMessage directMessage = twitter.sendDirectMessage(
					userScreenName, message);
			System.out.println("Direct message successfully sent to "
					+ directMessage.getRecipientScreenName());
		} catch (TwitterException te) {
			te.printStackTrace();
			System.out.println("Failed to send a direct message: "
					+ te.getMessage());

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
