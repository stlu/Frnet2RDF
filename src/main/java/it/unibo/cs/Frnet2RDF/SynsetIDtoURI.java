package it.unibo.cs.Frnet2RDF;

class SynsetIDtoURI {

	static String synsetId30toIRI(String id) {
		return "http://wordnet-rdf.princeton.edu/wn30/" + id;
	}

	static String synsetId31toIRI(String id) {
		char pos = id.charAt(id.length() - 1);
		int poscode = posToInt(pos);
		return "http://wordnet-rdf.princeton.edu/wn31/" + poscode + id;
	}

	private static boolean synsetId31toURI_test() {

		String test1 = "06371064-n";
		String result1 = synsetId31toIRI(test1);
		String result1T = "http://wordnet-rdf.princeton.edu/wn31/106371064-n";

		return result1T.equals(result1);
	}

	/*
	 * Noun n 1 Verb v 2 Adjective a 3 Adverb r 4 Adjective Satellite s 3 Phrase
	 * p 4
	 */

	private static int posToInt(char pos) {

		switch (pos) {
		case 'n':
			return 1;
		case 'v':
			return 2;
		case 'a':
			return 3;
		case 'r':
			return 4;
		case 's':
			return 3;
		case 'p':
			return 4;
		default:
			throw new RuntimeException();
		}
	}

	public static void main(String[] args) {
		System.out.println(synsetId31toURI_test());
	}
}
