package dotrural.ac.uk.realTime;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;

import dotrural.ac.uk.constants.PredefinedConstants;
import dotrural.ac.uk.utils.HttpRequests;

public class NextBusAdaptor {

	private static final String USER_ID = PredefinedConstants.NEXTBUS_USER_ID;
	private static final String PASSWORD = PredefinedConstants.NEXTBUS_PASSWORD;
	private static final String LIVE_BUS_DEPTS_URL = String.format(
			"http://%s:%s@nextbus.mxdata.co.uk/nextbuses/1.0/1", USER_ID,
			PASSWORD);

	private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy=MM-dd'T'HH:mm:ssZ");

	private static final String REQUEST_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<Siri xmlns=\"http://www.siri.org.uk/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.siri.org.uk/../siri.xsd\" version=\"1.0\">\n"
			+ "    <ServiceRequest>\n"
			+ "      <!--======ENDPOINT REFERENCES================================-->\n"
			+ "    <RequestTimestamp>%s</RequestTimestamp>\n"
			+ "  <RequestorRef>%s</RequestorRef>\n"
			+ "<StopMonitoringRequest version=\"1.0\">\n"
			+ "   <RequestTimestamp>%s</RequestTimestamp>\n"
			+ "   <!--=======TOPIC  ===================================== -->\n"
			+ "    <MonitoringRef>%s</MonitoringRef>\n"
			+ "</StopMonitoringRequest>\n" + "</ServiceRequest>\n" + "</Siri>";
	
	
	public NextBusAdaptor () {
		
	}
	

	public String getLiveDepartures(String naptanCode)
			throws ClientProtocolException, IOException {
		String now = now();
		String body = String.format(REQUEST_XML, now, USER_ID, now, naptanCode);
		//System.out.println(body);
		String results = makeRequest(body);
		return results;
	}

	private String now() {
		return DATE_FORMAT.format(new Date(System.currentTimeMillis()));
	}

	private String makeRequest(String body) throws ClientProtocolException,
			IOException {
		
		
		String json = HttpRequests.sendAuthorisedPostRequest(LIVE_BUS_DEPTS_URL,body,USER_ID, PASSWORD);
		
		return json;
	}
	
	public static void main(String[] args) throws ClientProtocolException, IOException {
		Parser pars = new Parser (new NextBusAdaptor().getLiveDepartures("639003662"),"2");
		
		
		pars.runParsing();
		
	}
}
