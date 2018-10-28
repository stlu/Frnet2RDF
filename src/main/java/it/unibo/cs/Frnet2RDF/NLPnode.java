package it.unibo.cs.Frnet2RDF;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/* 
 * Class to contain results after a Standford annotations process
 */
public class NLPnode {
	private String token;                // word
	private String lemma;                // lemma
	private String pos;                  // part of speech Stanford
	private String pos_u;                // part of speech UKB
	private Map<String, Double> senses;  // sense with relative score
	
	public NLPnode(String tk, String lm, String p, String u) {
		this.token = tk;
		this.lemma = lm;
		this.pos = p;
		this.pos_u = u;
		this.senses = new HashMap<>();
	}
	
	public String toString() {
        return "NLPnode [ token=>" + token + " lemma=>"+ lemma + " pos=>" + pos + " pos_u=>" + pos_u + " ]";
    }
	
	public String getToken() {
        return this.token;
    }
    public String getLemma() {
        return this.lemma;
    }
    public String getPos() {
        return this.pos;
    }
    public String getPosUkb() {
        return this.pos_u;
    }
    
    public Map<String, Double> getSense() {
        return this.senses;
    }
    
    public Map<String, Double> getSenseOrder() {
        return NLPSortByValue(this.senses);
    }
    
    public void setSense(String sen, Double score) {
    	//System.out.println("setSense "+this.lemma+" sen "+sen);
    	this.senses.put(sen, score);
    }
    
	// Sort HashMap by Value
    // see https://crunchify.com/how-to-sort-hashmap-by-key-and-value-in-java8-complete-tutorial/
	private static <K, V extends Comparable<? super V>> Map<K, V> NLPSortByValue(Map<K, V> nlpMap) {
 
		Map<K, V> nlpResult = new LinkedHashMap<>();
		// get a list from nlMap
		Stream<Map.Entry<K, V>> sequentialStream = nlpMap.entrySet().stream();
 
		// comparingByValue() returns a comparator that compares Map.Entry in reverse order on value.
		sequentialStream.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).forEachOrdered(c -> nlpResult.put(c.getKey(), c.getValue()));
		return nlpResult;
	}
}
