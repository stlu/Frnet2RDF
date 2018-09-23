package it.unibo.cs.Frnet2RDF;

import java.util.HashMap;
import java.util.Map;

/* 
 * Class to contain results after a Standford annotations process
 */
public class NLPnode {
	private String token;                // word
	private String lemma;                // lemma
	private String pos;                  // part of speech Stanford
	private Map<String, Double> senses;  // sense with relative score
	
	public NLPnode(String tk, String lm, String p) {
		this.token = tk;
		this.lemma = lm;
		this.pos = p;
		this.senses = new HashMap<>();
	}
	
	public String toString() {
        return "NLPnode [ token=>" + token + " lemma=>"+ lemma + " pos=>" + pos + " ]";
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
    
    public Map<String, Double> getSense() {
        return this.senses;
    }
    
    public void setSense(String sen, Double score) {
    	//System.out.println("setSense "+this.lemma+" sen "+sen);
    	this.senses.put(sen, score);
    }
}
