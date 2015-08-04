package dotrural.ac.uk.twitter_manager;

import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import dotrural.ac.uk.constants.PredefinedConstants;

public class EventsEffectsOnBusServicesChecker {

	public void checkAnnotationsAndInfer(DatasetAccessor mainEventsStorage, Model messageAnnotationsReturnedByKIMandInferences) {
		
       
		DatasetAccessor busServicesStoreAccessor = DatasetAccessorFactory.createHTTP(PredefinedConstants.FUSEKI_BUS_SERVICES_URI); 
		
		Property p = ResourceFactory.createProperty("http://vocab.org/transit/terms/service");
		
		boolean inferenecesBetweenBusServicesAndEventsExist = messageAnnotationsReturnedByKIMandInferences.containsLiteral(null, p, null);
		
		//if not inferences between bus services and events exist, try use the street names and bus stops
		if (!inferenecesBetweenBusServicesAndEventsExist) {
		
		/// the code for handling the relations beteen busroutes and tweets goes here 
		
		Model newTriples =  ModelFactory.createDefaultModel();
		 
		mainEventsStorage.add(newTriples); 
		}
		
	}

}
