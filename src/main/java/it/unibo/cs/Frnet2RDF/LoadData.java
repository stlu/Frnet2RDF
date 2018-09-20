package it.unibo.cs.Frnet2RDF;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.graph.*;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.lang.PipedRDFIterator;
import org.apache.jena.riot.lang.PipedRDFStream;
import org.apache.jena.riot.lang.PipedTriplesStream;
import org.apache.jena.riot.out.SinkTripleOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jsonldjava.core.RDFParser;

// Read and load example sentences 
public class LoadData {
	
	private ReadConfiguration conf;
	private static Logger logger = LoggerFactory.getLogger(UKB.class);
	
	public LoadData(ReadConfiguration c) {
		conf = c;
	}
	/* 
	 * Find example sentences 
	 */
	public void loadSentences(Collection<ExampleFrame> prc) {
		
		Model in = ModelFactory.createDefaultModel();
		Model out = ModelFactory.createDefaultModel();
		
		String queryString = "prefix earmark: <http://www.essepuntato.it/2008/12/earmark#> \n" +
		        "prefix example: <https://w3id.org/framester/framenet/abox/example/> \n"+
				"prefix ontology: <http://ontologydesignpatterns.org/cp/owl/semiotics.owl#> \n"+
	            "SELECT DISTINCT ?class ?label ?frame \n" + 
				"WHERE { ?class a earmark:StringDocuverse ." +
	            "  ?feSub a earmark:PointerRange ." + 
	            "  ?feSub earmark:refersTo ?class ." +
				"  ?class earmark:hasContent ?label ." +
	            "  ?feSub ontology:denotes ?frame . }   LIMIT 3" ; 

		if (conf.getInputType().equals(ReadConfiguration.InputType.SPARQL_ENDPOINT)) {
			logger.info("Applaying before rule {}", queryString);
			Query query = QueryFactory.create(queryString);
			QueryExecution qexec = QueryExecutionFactory.sparqlService(conf.getInput(), query);
			
			try {
			    ResultSet results=qexec.execSelect();
			    // For each solution in the result set
			    while (results.hasNext()) {
			      QuerySolution soln=results.nextSolution();
			      RDFNode labelPropertyNode=soln.get("label");
			      RDFNode classPropertyNode=soln.get("class");
			      RDFNode framePropertyNode=soln.get("frame");
			      
			      // set data to elaborate
			      String modifiedlabel = labelPropertyNode.toString().replaceAll("@en", "");
			      prc.add(new ExampleFrame(classPropertyNode.toString(), modifiedlabel, framePropertyNode.toString()));
			    }
			} finally {
			    qexec.close();
			}

		} else {
			// Load data
			RDFDataMgr.read(in, conf.getInput());  // assumed to be Turtle
			logger.info("Applaying before rule {}", queryString);
			Query query = QueryFactory.create(queryString);
			QueryExecution qexec = QueryExecutionFactory.create(query, in);
			
			try {
			    ResultSet results=qexec.execSelect();
			    // For each solution in the result set
			    while (results.hasNext()) {
			      QuerySolution soln=results.nextSolution();
			      RDFNode labelPropertyNode=soln.get("label");
			      RDFNode classPropertyNode=soln.get("class");
			      RDFNode framePropertyNode=soln.get("frame");
			      
			      // set data to elaborate
			      String modifiedlabel = labelPropertyNode.toString().replaceAll("@en", "");
			      prc.add(new ExampleFrame(classPropertyNode.toString(), modifiedlabel, framePropertyNode.toString()));
			    }
			} finally {
			    qexec.close();
			}
		}
		
	}

}
