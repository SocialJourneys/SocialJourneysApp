package dotrural.ac.uk.twitter_manager;

import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import dotrural.ac.uk.constants.PredefinedConstants;

public class EventsEffectsOnBusServicesChecker {

	public void checkAnnotationsAndInfer(DatasetAccessor mainEventsStorage, Model messageAnnotationsReturnedByKIMandInferences) {
		
       
		DatasetAccessor busServicesStoreAccessor = DatasetAccessorFactory.createHTTP(PredefinedConstants.FUSEKI_BUS_SERVICES_URI); 
		
		
		/// the code for handling the relations beteen busroutes and tweets goes here 
		
		 Model newTriples =  ModelFactory.createDefaultModel();
		 
		 
		
		
	}

}
