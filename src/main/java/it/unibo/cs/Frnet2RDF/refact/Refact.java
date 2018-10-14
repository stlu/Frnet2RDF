package it.unibo.cs.Frnet2RDF.refact;

import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Refact {

	private static Logger logger = LoggerFactory.getLogger(Refact.class);

	public static void main(String[] args) {
		
		logger.info("Refact");

		DataRefactor dr = new DataRefactor(RefactConfiguration.getInstance(args[0]));
		try {
			dr.refactorize();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}
}
