package dotrural.ac.uk.store;


import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.util.FileManager;

import dotrural.ac.uk.constants.PredefinedConstants;

public class JenaStore {
	OntModel store;
	Dataset dataset;

	
	ArrayList transactionQueueWrite  =new ArrayList ();
	ArrayList transactionQueueRead  =new ArrayList ();
	
	
	public JenaStore () {
		//String fullPath = "./Store/sjStore.ttl";
		
		 
		dataset = TDBFactory.createDataset(PredefinedConstants.TDB_DIRECTORY);
		
		//store = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		//store.add( FileManager.get().loadModel( fullPath ));
	}
	
	public OntModel getStore () {
		return store;
	}
	
	public Dataset getTDBdataset() {
		// TODO Auto-generated method stub
		return dataset;
	}

	public void closeWritingSession() {
		dataset.commit();
        dataset.end();
        transactionQueueWrite.remove("Placeholder");
        System.out.println ("Writing Session Freed");
        System.out.println (transactionQueueWrite.size());
	}

	public void closeReadingSession() {
        dataset.end();
        transactionQueueRead.remove("Placeholder");
	}

	
	public void startWritingSession(String location) throws InterruptedException {
		System.out.println ("Writing Session Request "+ location);
		while (!transactionQueueWrite.isEmpty()) {
			TimeUnit.SECONDS.sleep(1);
		}
		System.out.println ("Writing Session Granted " + location);
		transactionQueueWrite.add("Placeholder");
		
		dataset.begin(ReadWrite.WRITE) ;
		Model model = dataset.getDefaultModel();
		store = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM,model);
		//Set prefixes
	}
	
	public void startReadingSession() throws InterruptedException {
		
		while (!transactionQueueRead.isEmpty()) {
			TimeUnit.SECONDS.sleep(1);
		}
		
		dataset.begin(ReadWrite.READ) ;
		Model model = dataset.getDefaultModel();
		store = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM,model);
		//Set prefixes
	}
	
}
