package it.unibo.cs.Frnet2RDF.utils;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;

import org.apache.jena.system.Txn;

import java.net.URL;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

/*
 * Load an input file in Turtle format into a sparql 
 */
public class LoadIntoSparq {

    private static String file;
    private static String sparql;
    private static String graphName;
    
    public static void main(String[] args)  {
        
        // create the command line parser
        CommandLineParser parser = new DefaultParser();
        
        // create the Options
        Options options = new Options();
        Option option_f = Option.builder("f")
        		.hasArg()
                .required(true)
                .desc("Input file to load")
                .longOpt("file")
                .build();
        Option option_s = Option.builder("s")
            	.hasArg()
                .required(true)
                .desc("Sparl Endpoing Http")
                .longOpt("sparql")
                .build();
        Option option_d = Option.builder("d")
                    .desc("Delete Sparql data")
                    .longOpt("delete")
                    .build();
        Option option_g = Option.builder("g")
            	.hasArg()
                .desc("URI Graph Name")
                .longOpt("graph")
                .build();
        options.addOption(option_f);
        options.addOption(option_s);
        options.addOption(option_d);
        options.addOption(option_g);
        
        try {
        	// parse the command line arguments
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("f")) {
            	// initialise the member variable
                file = cmd.getOptionValue("f");
            }
            if (cmd.hasOption("s")) {
                sparql = cmd.getOptionValue("s");
            }
            if (cmd.hasOption("g")) {
            	// initialise the member variable
                graphName = cmd.getOptionValue("g");
            }
           
            //System.out.println(" sparql "+SPK.sparql);
            //Query query = QueryFactory.create("SELECT * { {?s ?p ?o }  } ");
            
            try ( RDFConnection conn = RDFConnectionFactory.connect(sparql) ) {
            	// https://jena.apache.org/documentation/txn/txn.html
            	Txn.executeWrite(conn, ()->{
            		if (cmd.hasOption("d")) {
            			if (cmd.hasOption("g")) {
            				try {
            					conn.delete(sparql+"data/"+graphName);
            				} catch (Exception exp) {
            					System.out.println("Delete failed on graph "+graphName+ " error "+exp.getMessage());
            				}
            			} else {
            				conn.update("DELETE { ?s ?p ?o } WHERE { ?s ?p ?o }");
            			}
            		}
                    conn.load(graphName, file);
                    System.out.println("Succesfully load "+file);
                });
            	//conn.queryResultSet(query, ResultSetFormatter::out);
       
            } catch (Exception exp) {
            	 System.out.println("Connection failed on "+sparql+ " error "+exp.getMessage());
            	 exp.printStackTrace();
  	        }

        } catch (ParseException e) {
            System.out.println( "Parsing failed.  Reason: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
