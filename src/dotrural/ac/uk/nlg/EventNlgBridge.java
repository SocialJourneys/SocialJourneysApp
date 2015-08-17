package dotrural.ac.uk.nlg;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class EventNlgBridge {

	String eventNlgQueryDiversion = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
			+ "PREFIX event: <http://purl.org/NET/c4dm/event.owl#>\n"
			+ "PREFIX timeline: <http://purl.org/NET/c4dm/timeline.owl#>\n"
			+ "PREFIX transit: <http://vocab.org/transit/terms/>\n"
			+ "PREFIX td: <http://purl.org/td/transportdisruption#>"
			+ "SELECT *\n"
			+ "WHERE {\n"
			+ " ?e a td:PublicTransportDiversion; ?p ?o. td:PublicTransportDiversion rdfs:label ?eventLabel. ?o rdfs:label ?olabel"
			// + "   ?e a ?event.\n"
			// +
			// "   ?eventType rdfs:subClassOf transportdisruption:PublicTransportEvent.\n"
			// + " optional{ ?e transit:service ?busservices.\n"
			// + " ?service rdfs:label ?serviceLabel.\n"
			+ " optional {\n"
			+ "   ?e event:time/timeline:beginsAtDateTime ?startdatetime.\n"
			+ " } optional {\n"
			+ "   ?e event:time/timeline:endsAtDateTime ?enddatetime.\n"
			+ "}  \n" + "}";

	String eventNlgQueryDelay = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
			+ "PREFIX event: <http://purl.org/NET/c4dm/event.owl#>\n"
			+ "PREFIX timeline: <http://purl.org/NET/c4dm/timeline.owl#>\n"
			+ "PREFIX transit: <http://vocab.org/transit/terms/>\n"
			+ "PREFIX td: <http://purl.org/td/transportdisruption#>"
			+ "SELECT *\n"
			+ "WHERE {\n"
			+ " ?e a td:PublicTransportDelay; ?p ?o. td:PublicTransportDelay rdfs:label ?eventLabel. ?o rdfs:label ?olabel"
			// + "   ?e a ?event.\n"
			// +
			// "   ?eventType rdfs:subClassOf transportdisruption:PublicTransportEvent.\n"
			// + " optional{ ?e transit:service ?busservices.\n"
			// + " ?service rdfs:label ?serviceLabel.\n"
			+ " optional {\n"
			+ "   ?e event:time/timeline:beginsAtDateTime ?startdatetime.\n"
			+ " } optional {\n"
			+ "   ?e event:time/timeline:endsAtDateTime ?enddatetime.\n"
			+ "}  \n" + "}";

	public Map<String, Map<String, Set<String>>> extractDetailsForNlg_Generic(
			ResultSet results) {

		Map<String, Map<String, Set<String>>> bigNlgData = new HashMap<String, Map<String, Set<String>>>();

		if (results == null)
			return bigNlgData;

		for (; results.hasNext();) {
			QuerySolution soln = results.next();

			String key = soln.getResource("e").getURI();
			Map<String, Set<String>> nlgData = bigNlgData.get(key);
			if (nlgData == null) {
				nlgData = new HashMap<String, Set<String>>();
			}
			// System.out.println(soln.getResource("p").getLocalName());
			/*
			 * if (!(soln.getResource("p").getLocalName().equals("type"))){
			 * 
			 * populateNlgData(nlgData, soln,
			 * soln.getResource("p").getLocalName(), "olabel"); }
			 */

			// populateNlgData(nlgData, soln, "event", "eventLabel");
			// populateNlgData(nlgData, soln, "type", "eventType");
			// Set<String> typeSet = new HashSet<String>();
			// typeSet.add("GeneralDisruption");
			// nlgData.put("type", typeSet);
			Set<String> cSet = new HashSet<String>();
			cSet.add("0.5");
			nlgData.put("certainty", cSet);
			populateNlgData(nlgData, soln, "startsAtDateTime", "startdatetime");
			populateNlgData(nlgData, soln, "endsAtDateTime", "enddatetime");

			String typeValue = getValue(soln, "sourceType");
			System.out.println(typeValue);
			if ("DirectMessage"
					.equals(typeValue))
				populateNlgData(nlgData, soln, "reportedAt", "reportTime");
			else if ("Tweet"
					.equals(typeValue)) {
				String value = getValue(soln, "reportTime");
				SimpleDateFormat format = new SimpleDateFormat(
						"yyyy-MM-dd'T'HH:mm:ss");
				Date d;
				try {
					d = format.parse(value);
					Calendar c = Calendar.getInstance();
					c.setTime(d);
					c.add(Calendar.HOUR_OF_DAY, 1);
					populateNlgDataValue(nlgData, "reportedAt", format.format(c.getTime()));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace(System.err);
				}
			}

			String type = populateNlgData(nlgData, soln, "type", "elabel")
					.toLowerCase();
			// this is some of the worst hacking code ever, but if it works....
			if ("running late delay delays diverted diversions diversions"
					.indexOf(type) < 0) {
				populateNlgData(nlgData, "hasFactor", type);
			}
			// populateNlgData(nlgData, soln, "hasFactor", "elabel");
			populateNlgData(nlgData, soln, "primaryLocation", "primaryLocation");
			populateNlgData(nlgData, soln, "delayLength", "delayLength");
			bigNlgData.put(key, nlgData);

			/*
			 * populateNlgData(nlgData, soln,
			 * soln.getResource("p").getLocalName(), "olabel");
			 * 
			 * populateNlgData(nlgData, soln, "event", "eventLabel");
			 * populateNlgData(nlgData, soln, "startsAtDateTime",
			 * "startdatetime"); populateNlgData(nlgData, soln,
			 * "endsAtDateTime", "endsdatetime");
			 */
		}

		return bigNlgData;
	}

	private void populateNlgData(Map<String, Set<String>> nlgData, String key,
			String value) {
		Set<String> values = nlgData.get(key);
		if (values == null)
			values = new HashSet<String>();
		values.add(value);
		nlgData.put(key, values);
	}

	private String populateNlgData(Map<String, Set<String>> nlgData,
			QuerySolution soln, String mapKey, String variable) {

		String value = getValue(soln, variable);
		return populateNlgDataValue(nlgData, mapKey, value);
	}

	private String populateNlgDataValue(Map<String, Set<String>> nlgData,
			String variable, String value) {
		if (value != null) {
			Set<String> values = nlgData.get(variable);
			if (values == null)
				values = new HashSet<String>();

			values.add(value);
			nlgData.put(variable, values);
		}
		return value;
	}

	private String getValue(QuerySolution soln, String variable) {
		String value = null;
		RDFNode object = soln.get(variable);
		if (object != null) {
			if (object.isResource()) {
				value = object.asResource().getLocalName();
			} else if (object.isLiteral()) {
				value = object.asLiteral().getValue().toString();
			}
		}
		return value;
	}

}
