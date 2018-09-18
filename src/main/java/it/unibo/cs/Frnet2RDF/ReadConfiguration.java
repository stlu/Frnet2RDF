package it.unibo.cs.Frnet2RDF;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadConfiguration {

	private static String CONFIG_FILE = "config.properties";
	private static Logger logger = LoggerFactory.getLogger(UKB.class);
	
	public enum InputType {
		SPARQL_ENDPOINT, TTL_FILE, NT_FILE
	}

	private String input, outputFormat;
	private String ukb_wsd = "bin/ukb_wsd";
	private String lkb_graph_bin_filepath = "lkb/wn30.bin";
	private String lkb_dict_filepath = "lkb/wnet30_dict.txt";
	private String wordnet_version = "wn30";
	private String namespacePrefixData = "";
	private String tempFile = "tmp.txt";

	public InputType inputType;

	public ReadConfiguration(String configFile) throws ConfigurationException {
		
		if (configFile != null) {
			CONFIG_FILE = configFile;
		}
		
		//try {
		    Configurations configs = new Configurations();
			Configuration config = configs.properties(configFile);

			input = config.getString("input");
			outputFormat = config.getString("outputFormat");
			
			String inputT = config.getString("inputType");
			if (inputT.equalsIgnoreCase("SPARQL")) {
				inputType = InputType.SPARQL_ENDPOINT;
			} else if (inputT.equalsIgnoreCase("TTL")) {
				inputType = InputType.TTL_FILE;
			} else if (inputT.equalsIgnoreCase("NT")) {
				inputType = InputType.NT_FILE;
			}
			
			ukb_wsd = config.getString("textdisambiguation.ukb_wsd");
			lkb_graph_bin_filepath = config
					.getString("textdisambiguation.lkb_graph_bin_filepath");
			lkb_dict_filepath = config
					.getString("textdisambiguation.lkb_dict_filepath");
			wordnet_version = config
					.getString("textdisambiguation.wordnet_version");
			
			logger.trace("Configuration: ukb_wsd:" + ukb_wsd
					+ ", lkb_graph_bin_filepath:" + lkb_graph_bin_filepath
					+ ", lkb_dict_filepath:" + lkb_dict_filepath
					+ ", wordnet_version:" + wordnet_version);
			
			namespacePrefixData = config.getString("prefixdata");
			tempFile = config.getString("textdisambiguation.file");
			
		//} catch (ConfigurationException e) {
		//	e.printStackTrace();
		//}

	}

	public  String getCONFIG_FILE() {
		return CONFIG_FILE;
	}

	public String getInput() {
		return input;
	}

	public InputType getInputType() {
		return inputType;
	}

	public String getUkbWsd() {
		return ukb_wsd;
	}
	
	public String getKbGraphBinFilepath() {
		return lkb_graph_bin_filepath;
	}
	
	public String getLkbDictFilepath() {
		return lkb_dict_filepath;
	}
	
	public String getWordnetVersion() {
		return wordnet_version;
	}
	
	public String getNamespacePrefixData() {
		return namespacePrefixData;
	}
	
	public String getWorkFile() {
		return tempFile;
	}
	
	public String getOutputFormat() {
		return outputFormat;
	}

}
