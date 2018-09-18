package it.unibo.cs.Frnet2RDF;

import java.util.Collection;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			RDFDataMgr.read(in, conf.getInput());  // assumed to be Turtle
			// TODO
		}
		
	}
	
	
}
