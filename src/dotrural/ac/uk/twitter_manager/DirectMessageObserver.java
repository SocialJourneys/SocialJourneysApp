package dotrural.ac.uk.twitter_manager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;

import dotrural.ac.uk.constants.PredefinedConstants;
import dotrural.ac.uk.events.EventsObserver;
import dotrural.ac.uk.store.JenaStore;
import dotrural.ac.uk.utils.DbConnect;
import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

public class DirectMessageObserver extends Thread {

	private final String USER_AGENT = "Mozilla/5.0";

	List<DirectMessage> lastDirectMessagesResult;
	List<DirectMessage> previousDirectMessagesResult;
	PreparedStatement pst = null;
	ResultSet rs = null;
	JenaStore store;

	final static Logger logger = Logger.getLogger(DirectMessageObserver.class);

	public DirectMessageObserver(JenaStore store) throws SQLException {
		previousDirectMessagesResult = new ArrayList<DirectMessage>();
		this.store = store;
	}

	public DirectMessageObserver() throws SQLException {
		previousDirectMessagesResult = new ArrayList<DirectMessage>();

	}

	public void run() {
		while (true) {
			getLatestDirectMessages();
			List<DirectMessage> toBeAnnotated = new ArrayList<DirectMessage>();
			for (DirectMessage dm : lastDirectMessagesResult) {
				if (!previousDirectMessagesResult.contains(dm)) {
					previousDirectMessagesResult.add(dm);
					toBeAnnotated.add(dm);
				}
			}
			// if
			// (!lastDirectMessagesResult.equals(previousDirectMessagesResult))
			// {
			System.out.println("New direct messages discovered!");
			try {

				try {
					requestAnnotations(toBeAnnotated);// selectMessagesForAnnotation());
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				/*
				 * BufferedWriter bufferWritter = new BufferedWriter(new
				 * FileWriter("./Store/sjStore.ttl")); store.getStore().write
				 * (bufferWritter,"TTL"); bufferWritter.close();
				 */

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// }

			// previousDirectMessagesResult = lastDirectMessagesResult;

			try {
				TimeUnit.SECONDS.sleep(180);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void pushNewMessagesToDatabase(DirectMessage msg)
			throws SQLException {

		long t = System.currentTimeMillis();
		DbConnect connectionObject = new DbConnect();

		// Check if already in the database
		pst = connectionObject.getDbConnect().prepareStatement(
				"SELECT msg_id from direct_messages WHERE msg_id = "
						+ msg.getId() + ";");

		rs = pst.executeQuery();

		if (!rs.next()) {

			pst = connectionObject
					.getDbConnect()
					.prepareStatement(
							"INSERT INTO direct_messages (msg_id,recipient_screen_name,text,created_at,sender_screen_name) VALUES (?,?,?,?,?);");

			pst.setLong(1, msg.getId());
			pst.setString(2, msg.getRecipientScreenName());
			pst.setString(3, msg.getText());

			long time = (msg.getCreatedAt()).getTime();
			pst.setTimestamp(4, new Timestamp(time));

			pst.setString(5, msg.getSenderScreenName());

			pst.executeUpdate();

			connectionObject.closeDbConnect();
			// System.out.println("Direct message sent to the database");
		}
		pst.close();
		logger.trace("store dm," + (System.currentTimeMillis() - t) + ","
				+ msg.getId() + ",");
	}

	private void getLatestDirectMessages() {
		long t = System.currentTimeMillis();
		Twitter twitter = new TwitterFactory().getInstance();
		try {
			Paging pg = new Paging();
			pg.setCount(50);
			// id of the last direct message from the previous study
			long lng = Long
					.parseLong(PredefinedConstants.OLDEST_DIRECT_MESSAGE_ID_TO_LOOK_UP);
			pg.setSinceId(lng);

			/*
			 * Cap on 1 request per minute
			 * https://dev.twitter.com/rest/reference/get/direct_messages
			 */

			lastDirectMessagesResult = twitter.getDirectMessages(pg);
			logger.info("request for direct message sent at " + new Date()
					+ " received " + lastDirectMessagesResult.size());
			// System.out.println("Direct messages " + directMessage);
			// System.out.println("Direct messages retrieved" +
			// lastDirectMessagesResult.size());
			for (int i = 0; i < lastDirectMessagesResult.size(); i++) {
				DirectMessage message = lastDirectMessagesResult.get(i);
				// lastDirectMessagesResult.add(message);
				// System.out.println("Direct messages " + message.getId());
				// System.out.println("Direct message " +
				// message.getSenderScreenName());
				// System.out.println("Direct messages " + message.getText());
			}
		} catch (TwitterException te) {
			logger.error("Error getting DMs " + te.getLocalizedMessage(), te);
		}
		logger.trace("get dms from twitter," + (System.currentTimeMillis() - t)
				+ "," + lastDirectMessagesResult.size() + ",");

	}

	private ArrayList selectMessagesForAnnotation() throws IOException,
			SQLException {
		ArrayList arrayTweetsForAnnotation = new ArrayList();

		for (int i = 0; i < lastDirectMessagesResult.size(); i++) {
			if (!previousDirectMessagesResult.contains(lastDirectMessagesResult
					.get(i))) {
				arrayTweetsForAnnotation.add(lastDirectMessagesResult.get(i));
			}

		}

		return arrayTweetsForAnnotation;
	}

	private void requestAnnotations(List<DirectMessage> temp)
			throws IOException, SQLException, InterruptedException {

		for (int i = 0; i < temp.size(); i++) {

			// System.out.println (temp.get(i).getId());
			Resource r = ResourceFactory
					.createResource("http://sj.abdn.ac.uk/ozStudyD2R/resource/ozstudy/twitter/directMessage/"
							+ temp.get(i).getId());

			// System.out.println("New direct message ID!" +
			// temp.get(i).getId());
			// System.out.println(temp.get(i)+" - Trying to decide");
			/*
			 * store.startReadingSession(); boolean answer =
			 * store.getStore().containsResource(r);
			 * store.closeReadingSession();
			 */

			// conntact fuseki
			DatasetAccessor da = DatasetAccessorFactory
					.createHTTP(PredefinedConstants.FUSEKI_URI);
			boolean answer = da.getModel().containsResource(r);
			// System.out.println(answer);
			if (answer) {
				System.out.println(temp.get(i).getId()
						+ " - Already in the model !");
			}

			else {
				logger.info("Requesting annotations for direct message : http://sj.abdn.ac.uk/ozStudyD2R/resource/ozstudy/twitter/directMessage/"
						+ temp.get(i).getId());

				pushNewMessagesToDatabase(lastDirectMessagesResult.get(i));

				// test annotation service
				String rawData = "uri=http://sj.abdn.ac.uk/ozStudyD2R/resource/ozstudy/twitter/directMessage/"
						+ temp.get(i).getId()
						+ "&sparqEndpoint="
						+ PredefinedConstants.REPOSITORY_SPARQL_ENDPOINT_URL
						+ "&includeInference=on";
				// System.out.println(HttpRequests.sendPostRequest(rawData));
				long t = System.currentTimeMillis();
				String response = sendPostRequest(
						PredefinedConstants.ANNOTATION_DIRECT_MESSAGE_SERVICE_URL,
						rawData);
				logger.trace("request annotations for DM,"
						+ (System.currentTimeMillis() - t) + ","
						+ lastDirectMessagesResult.get(i).getId() + ",");
				// request annotate
				if (response != null) {
					/*
					 * store.startWritingSession("direct message");
					 * store.getStore().read(new
					 * ByteArrayInputStream(response.getBytes()), null);
					 * store.closeWritingSession();
					 */
					// DatasetAccessor da =
					// DatasetAccessorFactory.createHTTP(PredefinedConstants.FUSEKI_URI);

					Model m = ModelFactory
							.createDefaultModel()
							.read(new ByteArrayInputStream(response.getBytes()),
									null);
					Resource eventType = ResourceFactory
							.createResource("http://purl.org/NET/c4dm/event.owl#Event");

					boolean eventAnnotationsPresent = da.getModel().contains(
							null, RDF.type, eventType);

					if (eventAnnotationsPresent) {
						logger.info("Annotations present");
						Property serviceProperty = ResourceFactory
								.createProperty("http://vocab.org/transit/terms/service");

						boolean inferenecesBetweenBusServicesAndEventsExist = da
								.getModel().contains(null, serviceProperty);
						m.write(System.out);

						// if (inferenecesBetweenBusServicesAndEventsExist) {
						// add to fuseki store

						t = System.currentTimeMillis();
						da.add(m);
						logger.trace("storing event and annotation model for DM,"
								+ (System.currentTimeMillis() - t)
								+ ","
								+ lastDirectMessagesResult.get(i).getId() + ",");

						// }

					}

				} else {
					// System.out.println("problem with the request for resource
					// :
					// http://sj.abdn.ac.uk/ozStudyD2R/resource/ozstudy/twitter/direcMessage/"+temp.get(i));

					if (logger.isInfoEnabled()) {
						logger.info("Tweet annotation service probably not working. Check resource : http://sj.abdn.ac.uk/ozStudyD2R/resource/ozstudy/twitter/direcMessage/"
								+ temp.get(i));

					}

				}
				TimeUnit.SECONDS.sleep(2);

			}

		}

	}

	public String sendPostRequest(String url, String urlParameters)
			throws IOException {

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

		System.out.println("sending request to " + url);
		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();
		System.out.println("response code " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(
				con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine + "\n");
		}
		in.close();

		// print result
		String res = null;
		if (response.toString() != null) {
			res = response.toString();
		}
		return res;
	}

}
