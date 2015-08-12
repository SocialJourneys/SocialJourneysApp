package dotrural.ac.uk.nlg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
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
			//			+ "   ?e a ?event.\n"
			// +
			// "   ?eventType rdfs:subClassOf transportdisruption:PublicTransportEvent.\n"
//			+ " optional{ ?e transit:service ?busservices.\n"
		//	+ " ?service rdfs:label ?serviceLabel.\n"
			+ " optional {\n"
			+ "   ?e event:time/timeline:beginsAtDateTime ?startdatetime.\n"
			+ " } optional {\n"
			+ "   ?e event:time/timeline:endsAtDateTime ?enddatetime.\n"
			+ "}  \n"
			+ "}";
	
	String eventNlgQueryDelay = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
			+ "PREFIX event: <http://purl.org/NET/c4dm/event.owl#>\n"
			+ "PREFIX timeline: <http://purl.org/NET/c4dm/timeline.owl#>\n"
			+ "PREFIX transit: <http://vocab.org/transit/terms/>\n"
			+ "PREFIX td: <http://purl.org/td/transportdisruption#>"
			+ "SELECT *\n"
			+ "WHERE {\n"
			+ " ?e a td:PublicTransportDelay; ?p ?o. td:PublicTransportDelay rdfs:label ?eventLabel. ?o rdfs:label ?olabel" 
			//			+ "   ?e a ?event.\n"
			// +
			// "   ?eventType rdfs:subClassOf transportdisruption:PublicTransportEvent.\n"
//			+ " optional{ ?e transit:service ?busservices.\n"
		//	+ " ?service rdfs:label ?serviceLabel.\n"
			+ " optional {\n"
			+ "   ?e event:time/timeline:beginsAtDateTime ?startdatetime.\n"
			+ " } optional {\n"
			+ "   ?e event:time/timeline:endsAtDateTime ?enddatetime.\n"
			+ "}  \n"
			+ "}";

	
	
	
	public Map<String, Map<String, Set<String>>> extractDetailsForNlg_Generic ( ResultSet results) {
		
		Map<String, Map<String, Set<String>>> bigNlgData = new HashMap<String, Map<String, Set<String>>>();

		if (results == null)
			return bigNlgData;
		
		for (; results.hasNext();) {
		QuerySolution soln = results.next();
		
		 String key = soln.getResource("e").getURI();
		   Map<String, Set<String>> nlgData = bigNlgData.get(key);
		   if (nlgData == null){
		    nlgData = new HashMap<String, Set<String>>();
		   }
		//  System.out.println(soln.getResource("p").getLocalName());
		  if (!(soln.getResource("p").getLocalName().equals("type"))){
			 
		   populateNlgData(nlgData, soln, soln.getResource("p").getLocalName(), "olabel");
		  }
		  else {
			  System.out.println("HEY");
		  }
		//   populateNlgData(nlgData, soln, "event", "eventLabel");
		   populateNlgData(nlgData, soln, "type", "eventType");
		   populateNlgData(nlgData, soln, "startsAtDateTime", "startdatetime");
		   populateNlgData(nlgData, soln, "endsAtDateTime", "endsdatetime");
		   populateNlgData(nlgData, soln, "reportTime", "reportTime");
		   bigNlgData.put(key, nlgData);

		/*populateNlgData(nlgData, soln, soln.getResource("p").getLocalName(), "olabel");
		
		populateNlgData(nlgData, soln, "event", "eventLabel");
		populateNlgData(nlgData, soln, "startsAtDateTime", "startdatetime");
		populateNlgData(nlgData, soln, "endsAtDateTime", "endsdatetime");*/
		}
		
		return bigNlgData;
	}

	private void populateNlgData(Map<String, Set<String>> nlgData,
			QuerySolution soln, String mapKey, String variable) {
		Set<String> values = nlgData.get(mapKey);
		if (values == null)
			values = new HashSet<String>();
		RDFNode object = soln.get(variable);
		if (object != null) {
			if (object.isResource()) {
				values.add(object.asResource().getLocalName());
			} else if (object.isLiteral()) {
				values.add(object.asLiteral().getValue().toString());
			}
		}
		nlgData.put(mapKey, values);
	}

}
