package dotrural.ac.uk.nlg;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.jena.atlas.json.JSON;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.ResultSet;

import dotrural.ac.uk.constants.PredefinedConstants;
import dotrural.ac.uk.utils.HttpRequests;

public class NLG_Factory {
	
	static private Logger logger = Logger.getLogger(NLG_Factory.class);

	
	
	public NLG_Factory () {	
		/*for (String key : nlgData.keySet()) {
			String s = "";
			for (Map<String, Set<String>>> value : nlgData.get(key)) {
				s += " " + value;
			}
			dataForNlg.put(key, s);
			System.out.println(key + ": " +s);
		}*/
	}
	
	public Map<String, Map<String, Set<String>>>   getParametersForNLG (ResultSet result) {
		
		Map<String, Map<String, Set<String>>> nlgData;
		
		EventNlgBridge bridge = new EventNlgBridge();
		
		 nlgData = bridge
				.extractDetailsForNlg_Generic( result );
		//System.out.println ("printing " +nlgData);
		
		return nlgData;
		
	}
	
	
	public String getGeneratedMessage (HashMap<String, Set<String>> nlgData) {
		 
String data = "{"; 
		
		Iterator<String> it = nlgData.keySet().iterator();
		
		while (it.hasNext()) {
			
			String key = it.next();
			data = data +"\""+key+"\":";
			
			Set set = nlgData.get(key);
			
			
			Iterator<String> it2 = set.iterator();
			
			if (!it2.hasNext()) {
				data = data+ "\"\"";
			}
			boolean first = true;
			
			while (it2.hasNext()) {
				String value = it2.next();
				
				
				if (it2.hasNext()&&!first) {
					
					data = data +value+",";
				}
				if (!it2.hasNext()&&!first) {
					
					data = data +value+"\"";
				}
				if (it2.hasNext()&&first){
					
				data = data +"\""+value+",";
				first = false;
				}
				
				if (!it2.hasNext()&&first) 
				{
					data = data +"\""+value+"\"";
				}
			}
			
			
			
			if (it.hasNext()){
			data = data+ ",";
			}
		}
		
		data = data+ "}";
		
		
		/*	String data = "{"; 
		
		Iterator<String> it = nlgData.keySet().iterator();
		
		while (it.hasNext()) {
			
			String key = it.next();
			data = data +"\""+key+"\":{";
			
			Set set = nlgData.get(key);
			
			
			Iterator<String> it2 = set.iterator();
			
			int count = 0;
			
			while (it2.hasNext()) {
				String value = it2.next();
				
				data = data + "\"value_"+count+"\":";
				count++;
				
				if (it2.hasNext()){
				data = data +"\""+value+"\",";
				}
				else {
					data = data +"\""+value+"\"";
				}
			}
			
			data = data+"}";
			
			if (it.hasNext()){
			data = data+ ",";
			}
		}
		
		data = data+ "}"; */
		
		String msg ="error";
//		try {
			//data = "{\"startsAtDateTime\":\"2015-05-27T17:45:38\",\"service\":\"Service 1\",\"primaryLocation\":\"Faulds Gate\",\"place\":\"Gardner Drive\",\"type\":\"PublicTransportDelay\",\"delayLength\":\"0 mins\"}";
			//data = "{\"type\":\"PublicTransportDelay\",\"service\":\"service 1\",\"hasFactor\":\"road works\",\"primaryLocation\":\"st machar dr\",\"place\":\"road 1, road2\",\"delayLength\":\"0 mins\",\"startsAtDateTime\":\"2015-03-08T19:59:59\"}";
			logger.info("Event data to send to nlg: " + data);
			msg = HttpRequests.sendPostRequest(PredefinedConstants.NLG_SERVICE_URI, data);
			if ("".equals(msg))msg="error";
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		return msg;
	}

	public String getGeneratedMessage (String nlgData) {
		String msg ="error";
//		try {
			//data = "{\"startsAtDateTime\":\"2015-05-27T17:45:38\",\"service\":\"Service 1\",\"primaryLocation\":\"Faulds Gate\",\"place\":\"Gardner Drive\",\"type\":\"PublicTransportDelay\",\"delayLength\":\"0 mins\"}";
			//data = "{\"type\":\"PublicTransportDelay\",\"service\":\"service 1\",\"hasFactor\":\"road works\",\"primaryLocation\":\"st machar dr\",\"place\":\"road 1, road2\",\"delayLength\":\"0 mins\",\"startsAtDateTime\":\"2015-03-08T19:59:59\"}";
			logger.info("NLG generated messge data " + nlgData);
			msg = HttpRequests.sendPostRequest(PredefinedConstants.NLG_SERVICE_URI, nlgData);
			if ("".equals(msg))msg="error";
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		return msg;
		
	}

}
