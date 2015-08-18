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
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

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
import dotrural.ac.uk.events.MessageHandler;
import dotrural.ac.uk.journeys.Journey;
import dotrural.ac.uk.store.JenaStore;
import dotrural.ac.uk.utils.DbConnect;
import dotrural.ac.uk.utils.HttpRequests;

public class TweetObserver extends Thread {
	private final String USER_AGENT = "Mozilla/5.0";

	ArrayList<String> lastTweetResult;
	ArrayList<String> currentTweetResult;
	PreparedStatement pst = null;
	ResultSet rs = null;
	JenaStore store;

	final static Logger logger = Logger.getLogger(TweetObserver.class);

	public TweetObserver(JenaStore store) {
		lastTweetResult = new ArrayList<String>();
		this.store = store;
	}

	public TweetObserver() {
		lastTweetResult = new ArrayList<String>();
	}

	public void run() {
		while (true) {
			DbConnect connectionObject = new DbConnect();
			try {
				pst = connectionObject.getDbConnect().prepareStatement(
						"SELECT original_tweet_id,author,text,time_stamp FROM tweet order by time_stamp desc limit 10");
				rs = pst.executeQuery();

				connectionObject.closeDbConnect();

				currentTweetResult = new ArrayList<String>();

				while (rs.next()) {

					currentTweetResult.add(rs.getString("original_tweet_id"));
				}
				rs.close();
				pst.close();

				if (lastTweetResult.equals(currentTweetResult)) {
					// System.out.println("All good , same old!");
				}

				else {

					ArrayList temp = selectTweetsForAnnotation();

					for (int i = 0; i < temp.size(); i++) {

						Resource r = ResourceFactory.createResource(
								"http://sj.abdn.ac.uk/ozStudyD2R/resource/ozstudy/twitter/tweet/" + temp.get(i));

						// System.out.println("New tweet ID!" + temp.get(i));

						/*
						 * store.startReadingSession(); boolean answer =
						 * store.getStore().containsResource(r);
						 * store.closeReadingSession();
						 */

						// conntact fuseki
						DatasetAccessor da = DatasetAccessorFactory.createHTTP(PredefinedConstants.FUSEKI_URI);
						// da.getModel().write(System.out);
						
						boolean answer = da.getModel().containsResource(r);

						if (answer) {
							System.out.println(temp.get(i) + " - Already in the model !");
						}

						else {

							System.out.println("New tweet ID!" + temp.get(i));

							if (logger.isInfoEnabled()) {
								logger.info(
										" \n Requesting annotations for tweet : http://sj.abdn.ac.uk/ozStudyD2R/resource/ozstudy/twitter/tweet/"
												+ temp.get(i));

							}

							String rawData = "uri=http://sj.abdn.ac.uk/ozStudyD2R/resource/ozstudy/twitter/tweet/"
									+ temp.get(i) + "&sparqEndpoint="
									+ PredefinedConstants.REPOSITORY_SPARQL_ENDPOINT_URL + "&includeInference=on";
							// System.out.println(HttpRequests.sendPostRequest(rawData));
							long t = System.currentTimeMillis();
							String response = sendPostRequest(PredefinedConstants.ANNOTATION_TWEET_SERVICE_URL,
									rawData);
							logger.trace("request annotations for Tweet,"+(System.currentTimeMillis()-t)+","+temp.get(i)+",");
							
							// request annotate
							// System.out.println(response);

							if (response != null) {
								/*
								 * store.startWritingSession("tweet");
								 * store.getStore().read(new
								 * ByteArrayInputStream(response.getBytes()),
								 * null); store.closeWritingSession();
								 */
								Model m = ModelFactory.createDefaultModel()
										.read(new ByteArrayInputStream(response.getBytes()), null);

								Resource eventType = ResourceFactory
										.createResource("http://purl.org/NET/c4dm/event.owl#Event");

								boolean eventAnnotationsPresent = da.getModel().contains(null, RDF.type, eventType);

								if (eventAnnotationsPresent) {

									Property serviceProperty = ResourceFactory
											.createProperty("http://vocab.org/transit/terms/service");

									boolean inferenecesBetweenBusServicesAndEventsExist = da.getModel().contains(null,
											serviceProperty);

									if (inferenecesBetweenBusServicesAndEventsExist) {
										// add to fuseki store
										t = System.currentTimeMillis();
										da.add(m);
										logger.trace("storing event and annotation model for Tweet,"+(System.currentTimeMillis()-t)+","+temp.get(i)+",");
										
									}
								}

							}

							else {
								// System.out.println("problem with the request
								// for resource :
								// http://sj.abdn.ac.uk/ozStudyD2R/resource/ozstudy/twitter/tweet/"+temp.get(i));

								if (logger.isInfoEnabled()) {
									logger.info(
											"Tweet annotation service probably not working. Check resource : http://sj.abdn.ac.uk/ozStudyD2R/resource/ozstudy/twitter/tweet/"
													+ temp.get(i));

								}
							}

						}

					}

					/*
					 * FileWriter fileWritter = new
					 * FileWriter("./Store/sjStore.ttl");
					 * 
					 * BufferedWriter bufferWritter = new
					 * BufferedWriter(fileWritter);
					 * 
					 * store.getStore().write (bufferWritter,"TTL");
					 * bufferWritter.close();
					 */

				}

				lastTweetResult = currentTweetResult;

				TimeUnit.SECONDS.sleep(5);

			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	private ArrayList selectTweetsForAnnotation() throws IOException {
		ArrayList arrayTweetsForAnnotation = new ArrayList();

		for (int i = 0; i < currentTweetResult.size(); i++) {
			if (!lastTweetResult.contains(currentTweetResult.get(i))) {
				arrayTweetsForAnnotation.add(currentTweetResult.get(i));
			}

		}

		return arrayTweetsForAnnotation;
	}

	public String sendPostRequest(String url, String urlParameters) throws IOException {

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
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
