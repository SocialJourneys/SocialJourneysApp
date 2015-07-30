package dotrural.ac.uk.realTime;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;

public class RealTimeInfoGetter {

	
	RealTimeInfoGetter (){
		
	}

	
	public String getTimeForServiceAndBusStop (String busStopId, String serviceID) throws ClientProtocolException, IOException {
		
		String res = "error";
		NextBusAdaptor nextBusAdaptor = new NextBusAdaptor ();
		String xml = nextBusAdaptor.getLiveDepartures (busStopId);
		
		
		
		
		return res;
	}

    private String parseFroService (String xml , String service) {
    	String response ="";
    	
    	
    	return response;
    }

}
