package it.unibo.cs.Frnet2RDF;

import it.unibo.cs.Frnet2RDF.utils.*;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.RDFDataMgr;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//public class UKB implements Disambiguator {
public class UKB  {
	
	// example string 
	// ctx_3 w21  02700029-a/0.617999 09937688-n/0.201255 02700199-a/0.093806 02177397-a/0.0869394 !! colonial
	private Pattern resultPattern = Pattern.compile(" (\\S+)/(\\S+) "); // 
	
	private String flags = " --ppr --nopos --allranks";
	private String tempFile;
	private String defaultNamespacePrefixData;
	private boolean wn30 = false, wn31 = false;
	
	private ReadConfiguration conf;
	private static Logger logger = LoggerFactory.getLogger(UKB.class);

	private static UKB instance;

	private UKB(String cfile) {
		try {
			conf = new ReadConfiguration(cfile);
			
			defaultNamespacePrefixData = conf.getNamespacePrefixData();
			tempFile = conf.getWorkFile();
	
			if (conf.getWordnetVersion().equalsIgnoreCase("wn30")) {
				wn30 = true;
			} else if (conf.getWordnetVersion().equalsIgnoreCase("wn31")) {
				wn31 = true;
			}
		} catch (ConfigurationException e) {
			e.printStackTrace();	
		}
	}

	public static UKB getInstance(String configFile) {
		if (instance == null) {
			instance = new UKB(configFile);
		}
		return instance;
	}

	public void disambiguate(Collection<ExampleFrame> prc, boolean allRanks) {

		// parse sentence to detect tokens, pos, and word's lemma
		String inputText = tokenToUKBContext(prc);
		
		// lower case all strings 
		inputText = inputText.toLowerCase();

		try {

			FileUtils.toTextFile(inputText, tempFile);
			
			String commandToExecute;
			// run ukb executions
			commandToExecute = conf.getUkbWsd().concat(flags).concat(" -K ")
					.concat(conf.getKbGraphBinFilepath()).concat(" -D ")
					.concat(conf.getLkbDictFilepath()).concat(" " + tempFile);

			logger.info(commandToExecute);
			Process p = Runtime.getRuntime().exec(commandToExecute);  
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			
			String line = "";
			
			
			while ((line = reader.readLine()) != null) {
				logger.trace(line);

				// The first line prints the command
				if (line.startsWith("!!")) {
					continue;
				}

				Matcher m = resultPattern.matcher(line);
				while (m.find()) {
                    
					String synId = line.substring(m.start(1), m.end(1));
					String scoreString = line.substring(m.start(2), m.end(2));
					
					logger.debug(line + " ===> " + scoreString);
					
					Double score = Double.parseDouble(scoreString);

					String iri_denotes = wn30 ? SynsetIDtoURI
							.synsetId30toIRI(synId) : wn31 ? SynsetIDtoURI
							.synsetId31toIRI(synId) : null;
							//System.out.println("iri_denotes "+iri_denotes);
					
							String ctx = getWordCtx(line);
							Integer id = getWordId(line);
					
					// add Sense on token disambiguated
					prc.forEach( fr -> {
						  fr.addSenseScore(ctx, id, iri_denotes, score);
					});		
		/**			prl.get(getWordId(line)).addDenotedSense(
							defaultNamespacePrefixData + IdFactory.getNewId(),
							iri_denotes,
							synId,
							score,
							wn30 ? SenseInventory.WN30 : wn31
									? SenseInventory.WN31
									: null); **/
       
					if (!allRanks)
						break;

				} // end while m.find

			} // end while reader
			
			// see https://www.javaworld.com/article/2071275/core-java/when-runtime-exec---won-t.html
			int exitVal = p.waitFor(); 
			logger.info("Exited with error code " + exitVal);
	       
			reader.close();
			//###### UNDO #### FileUtils.deleteFile(tempFile); 
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static int getWordId(String line) {
		String[] fs = line.split(" ");
		String stringNumber = fs[1].replace('w', '0');
		return (Integer.parseInt(stringNumber) - 1);  // start index = 0
	}
	
	private static String getWordCtx(String line) {
		String[] fs = line.split(" ");
		return fs[0];
	}

	/*
	 * Parse senteces to detect token, pos, end word's lemma
	 */
	private static String tokenToUKBContext(Collection<ExampleFrame> prl) {
		
        StfdLemmatizer slem = new StfdLemmatizer();
        List<List<NLPnode>> nlpnodes; 
        StringBuilder sb = new StringBuilder();
        int n_sent = 0;
        
        // lemmatize every frame
        for (ExampleFrame fr : prl) {
        	logger.info(fr.getContent());
        	
        	nlpnodes = slem.lemmatize(fr.getContent());
        	
        	String ctxStr = "ctx_"+ ++n_sent;
        	sb.append(ctxStr+"\n");
        	
        	for(int jk=0; jk < nlpnodes.size(); jk++) {
        		
        		int i = 1;
        	    // Sentences
        	    for (NLPnode nod : nlpnodes.get(jk)) {
        	    	String pos = nod.getPos();
        	    	
        	    	// Convert and skip some kind of token
        	    	String upos = convPOStoUKB(pos);
        	    	if (upos.equals("SKIP")) continue;
        	    	
        	    	// Save only node to wsd
        	    	fr.addTokens(ctxStr, nod );
        	    	//System.out.println("POS "+pos);
        	    	
            		String word = nod.getLemma();
            		sb.append(word.replace(' ', '_'));
         			sb.append("#");         // POS
         			sb.append("#w" + i++);  // word identifier
         			sb.append("#1 ");       // 1 means disambiguate the word 	 
            	}
        	}
        	// reset nlpnodes
        	nlpnodes.clear(); 
        	sb.append("\n");
        }
        	
  /**      

		if (context != null) {
			List<PointerRange> contextTokens = Parser.tokenize(context);
			for (PointerRange tw : contextTokens) {
				String word = tw.getContent();
				word = l.getLemma(word);
				sb.append(word.replace(' ', '_'));
				sb.append("#"); // no pos
				sb.append("#c" + i++); // word identifier
				sb.append("#0 "); // 0 means do not disambiguate the word
			}
		} **/
        System.out.println(sb.toString());
		return sb.toString();
	}
	
	/* 
	 * Convert Stanford's pos into UKB pos
	 */
	 private static String convPOStoUKB(String in_pos) {
		
		String posUKB = "SKIP";
        /*  vedi https://stackoverflow.com/questions/1833252/java-stanford-nlp-part-of-speech-labels
            CC Coordinating conjunction
		    CD Cardinal number
		    DT Determiner
		    EX Existential there
		    FW Foreign word
		    IN Preposition or subordinating conjunction
		    
		    LS List item marker
		    
		    PDT Predeterminer
		    POS Possessive ending
		    PRP Personal pronoun
		    PRP$ Possessive pronoun
		    
		    RP Particle
		    SYM Symbol
		    TO to
		    UH Interjection
		    
		    WDT Whdeterminer
		    WP Whpronoun
		    WP$ Possessive whpronoun

       **/
		
		 switch (in_pos) {
            case "NN" :  // Noun, singular or mass
            case "NNS" : // Noun, plural
            case "NNP" : // Proper noun, singular
            case "NNPS" : // Proper noun, plural
            	posUKB = "n";
       			break;
            case "VB" : // Verb, base form
            case "VBD" : // Verb, past tense
            case "VBG" : // Verb, gerund or present participle
            case "VBN" : // Verb, past participle
            case "VBP" : // Verb, non­3rd person singular present
            case "VBZ" : // Verb, 3rd person singular present
            case "MD"  : // Modal
            	posUKB = "v";
       			break;
            case "RB"  : // Adverb
            case "RBR" : // Adverb, comparative
            case "RBS" : // Adverb, superlative
            case "WRB" : // Whadverb" 
            	posUKB = "r";
       			break;
            case "JJ" :  // Adjective
            case "JJR" : // Adjective, comparative
            case "JJS" : // Adjective, superlative
       			posUKB = "a";
       			break;
       			
            case "," : // skip punctuation
            case "." :
            case ";" :
            case ":" :
            case "!" :
            case "?" :
            case "-LRB-" : // ( [ {
            case "-RRB-" : // ( [ {
            	posUKB = "SKIP";
            	break;
       		default :
       			posUKB = "";
       			break;
        } 
        return posUKB;
	}
	
	public static void main(String[] args) {
		//Disambiguator ukb = UKB.getInstance(args[0]);
		UKB ukb = UKB.getInstance(args[0]);
		Collection<ExampleFrame> exfr = new HashSet<ExampleFrame>();
		
		// Load data to disambiguate
		LoadData ldt = new LoadData(ukb.conf);
		ldt.loadSentences(exfr);
		
		// disambiguate all data 
		ukb.disambiguate(exfr, true);  // true examine all rank, false only the biggest
		
		for (ExampleFrame frame : exfr) {
			System.out.println(frame.getContent());
			
			Set<String> ctxs = frame.getDenotedSenses().keySet();
			Collection<List <NLPnode>> nn = frame.getDenotedSenses().values();
			frame.getDenotedSenses().forEach( (ctx, nods) -> {
				nods.forEach( nd -> { 
					System.out.println("\t"+nd.getLemma()+"\t"+nd.getSense());
				});
			});
		}
	}

}
