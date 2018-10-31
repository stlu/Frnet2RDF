package it.unibo.cs.Frnet2RDF;

import it.unibo.cs.Frnet2RDF.utils.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.*;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.lang.PipedRDFIterator;
import org.apache.jena.riot.lang.PipedRDFStream;
import org.apache.jena.riot.lang.PipedTriplesStream;
import org.apache.jena.riot.out.SinkTripleOutput;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jsonldjava.core.RDFParser;

// Read and load example sentences 
public class LoadData {
	
	private ReadConfiguration conf;
	private static Logger logger = LoggerFactory.getLogger(UKB.class);
	private Model in;
	private Model out;
	
	public LoadData(ReadConfiguration c) {
		conf = c;
		in = ModelFactory.createDefaultModel();
		out = ModelFactory.createDefaultModel();
	}
	/* 
	 * Find and load example sentences 
	 */
	public void loadSentences(Collection<ExampleFrame> prc) {
			
		String queryString = "prefix earmark: <http://www.essepuntato.it/2008/12/earmark#> \n" +
		        "prefix example: <https://w3id.org/framester/framenet/abox/example/> \n"+
				"prefix ontology: <http://ontologydesignpatterns.org/cp/owl/semiotics.owl#> \n"+
	            "SELECT DISTINCT ?class ?label ?frame \n" + 
				"WHERE { ?class a earmark:StringDocuverse ." +
	            "  ?feSub a earmark:PointerRange ." + 
	            "  ?feSub earmark:refersTo ?class ." +
				"  ?class earmark:hasContent ?label ." +
		    "  ?feSub ontology:denotes ?frame . } ";  //LIMIT 15" ;

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
			} catch (Exception e) {
				e.printStackTrace();
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
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
			    qexec.close();
			}
		}
		
	}
	
	/**
	 * Save ouput generated
	 */
	public void saveSentences(Collection<ExampleFrame> exfr) throws FileNotFoundException {
		
		String prefix = conf.getNamespacePrefixData();
		out.setNsPrefix("", prefix);
		out.setNsPrefix("framester", "https://w3id.org/framester/framenet/abox#");
		out.setNsPrefix("frame", "https://w3id.org/framester/framenet/abox/frame/");
		out.setNsPrefix("example", "https://w3id.org/framester/framenet/abox/example/");
		out.setNsPrefix("eamark", "http://www.essepuntato.it/2008/12/earmark#");
		out.setNsPrefix("rdfs","http://www.w3.org/2000/01/rdf-schema#");
		out.setNsPrefix("ontology","http://ontologydesignpatterns.org/cp/owl/semiotics.owl#");
		out.setNsPrefix("wn","http://wordnet-rdf.princeton.edu/ontology#");
		out.setNsPrefix("xsd","http://www.w3.org/2001/XMLSchema#");
		//pwnlemma: <http://wordnet-rdf.princeton.edu/rdf/lemma/> .
		//pwnid: <http://wordnet-rdf.princeton.edu/id/> .

		Property propDenotes = out.createProperty("http://ontologydesignpatterns.org/cp/owl/semiotics.owl#denotes");
		Property propBegin = out.createProperty("http://www.essepuntato.it/2008/12/earmark#begins");
		Property propEnd = out.createProperty("http://www.essepuntato.it/2008/12/earmark#ends");
		Property propPosST = out.createProperty("https://w3id.org/framester/framenet/abox#posStanford");
		Property propPosUKB = out.createProperty("https://w3id.org/framester/framenet/abox#pos");
		Property resRefer = out.createProperty("http://www.essepuntato.it/2008/12/earmark#refersTo");
		Property propWeight = out.createProperty("https://w3id.org/framester/framenet/abox#weight");
		Resource ptrRange=out.createResource("http://www.essepuntato.it/2008/12/earmark#PointerRange");
		Resource syn=out.createResource("https://w3id.org/framester/wn/wn30/schema/Synset");
		
		int ntot_synsets = 0;
		
		for (ExampleFrame frame : exfr) {
			System.out.println(">>>>>>>>>>>\n"+frame.getContent());
			String last = frame.getName();
			System.out.println("\t"+last+"\n");
			
			ntot_synsets += frame.getSynsets().size();
			
			int[] ii = {1};
			
			frame.getDenotedSenses().forEach( nods -> {
				logger.debug("\t"+nods.getLemma()+"\t"+nods.getSense());
			});
		    
			// Check  synset 
			frame.getSynsets().forEach(  ind -> {
				//System.out.println("\t"+frame.getNodeNum(ind).toString());
				
			    String label = frame.getNodeNum(ind).getToken();
			    String posST = frame.getNodeNum(ind).getPos();
			    String posUKB = frame.getNodeNum(ind).getPosUkb(); //v,n
			    //System.out.println(" saveSentences  ==> nod "+frame.getNodeNum(ind).toString());
				
				// find index token on original example
			    int ini = frame.getContent().indexOf(label, 0);
				int end = ini + label.length();
				//System.out.println("\""+frame.getContent().substring(ini,end)+"\"");
				Literal lini = out.createTypedLiteral(ini);
				Literal lend = out.createTypedLiteral(end);
				
				Map<String, Double> sortvalues = frame.getNodeNum(ind).getSenseOrder();
				sortvalues.forEach( (wn,dval) -> {
					
					Resource resource=out.createResource(last +"-"+label+"-"+ii[0]);
                    
					resource.addProperty(RDF.type, ptrRange);
					resource.addProperty(RDFS.label, label, "en");
					
					Resource rl = out.createProperty(last);
					resource.addProperty(resRefer, rl);
					
					resource.addProperty(propBegin, lini); 
					resource.addProperty(propEnd, lend); 
					resource.addProperty(propPosUKB, posUKB); 
					resource.addProperty(propPosST, posST);
					
					Literal dv = out.createTypedLiteral(dval.doubleValue());
					resource.addProperty(propWeight,dv) ;
					
					Resource ww = out.createProperty(wn);
					resource.addProperty(propDenotes,ww);
					
					resource.inModel(out);
				    ii[0]++;
				    
				    Resource res2=out.createResource(wn);
				    res2.addProperty(RDF.type, syn);
				    res2.inModel(out);
				    ii[0]++;
				});
				
			});
		}
		System.out.println("##### TOT synsets found UKB "+ntot_synsets);
		
		out.write(new FileOutputStream(new File(conf.getOutputFile())), conf.getOutputFormat());
	}
}
