package it.unibo.cs.Frnet2RDF;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/*
 *  Contains all examples to disambiguate
 */
public class ExampleFrame {
	
	private String name;    //name example rdf
	private String content;	//sentences
	private String frame;   // name frame rdf 
	private String ctx;     // string context for ukb
	private List<NLPnode>  nodes_wsd; // sentence analized 
	private List<Integer>  synsets;   // index of nodes_wsds that contain verbs
	
	public int hashCode() {
	   return Objects.hashCode(name);
	}

    public boolean equals(Object obj) {
       if(obj == null) return false;
       if(!(obj instanceof ExampleFrame)) return false;
       ExampleFrame n = (ExampleFrame)obj;
       return (n.name.equals(this.name));
    }
    
    // Constructor
	public ExampleFrame(String n, String l, String f) {
		this.name = n;
		this.content = l;
		this.frame = f;
		this.nodes_wsd = new ArrayList<NLPnode>();
		this.synsets = new ArrayList<Integer>();
	}
	
	public String getName() {
        return this.name;
    }
	
    public String getContent() {
        return this.content;
    }
    
    public String getFrame() {
        return this.frame;
    }
    
    public String getCtx() {
        return this.ctx;
    }
    
    public void setCtx(String ctNum) {
        this.ctx = ctNum;
    }
    
    public void addTokens(NLPnode nod) {
		//System.out.println(" addTokens  ==> "+this.ctx +" nod "+nod.toString());
		this.nodes_wsd.add(nod);
		this.setPrincipalSynsets();
    }
   
    public boolean containsCtx(String ctNum) {
        return this.ctx.equals(ctNum);
    }
    
    public void addSenseScore(Integer id, String sen, Double score) {
    	this.nodes_wsd.get(id).setSense(sen,score); 
    }
    
    public List<NLPnode> getDenotedSenses() {
    	return this.nodes_wsd;
    }
    
    /**
     * Analyse all synsets to clean auxiliary verbs
     */
    public List<Integer> getSynsets() {
    	
    	List<Integer> newval = new ArrayList<Integer>();
    	
    	// if multiple verbs check if are auxiliary forms
    	if (this.synsets.size() > 1) {
	    	for (int j = 0; j < this.synsets.size(); j++) {
	    		String speach = this.nodes_wsd.get(this.synsets.get(j)).getPos();
	    		String lemma = this.nodes_wsd.get(this.synsets.get(j)).getLemma();
	    		String token = this.nodes_wsd.get(this.synsets.get(j)).getToken();
	    		//System.out.println(" speach "+speach+" token "+token+" lemma "+lemma+" j "+j);
	    		if (lemma.equalsIgnoreCase("have") || 
	    			lemma.equalsIgnoreCase("do")   ||
	    			lemma.equalsIgnoreCase("be")   ||
	    			speach.equalsIgnoreCase("MD")
	    			) continue ;
	    		newval.add(this.synsets.get(j));
	    	}
	    	return newval;
    	} else {
    		return this.synsets;
    	}
    }
    
    public NLPnode getNodeNum(Integer id) {
    	return this.nodes_wsd.get(id);
    }
    /**
     * Add only verbs in principals synset 
     */
    private void setPrincipalSynsets() {
    	
    	for(int i = 0; i < this.nodes_wsd.size(); i++) {
    		String speach = this.nodes_wsd.get(i).getPos();
    	
			switch (speach) {
				case "VB" : // Verb, base form
				case "VBD" : // Verb, past tense
				case "VBG" : // Verb, gerund or present participle
				case "VBN" : // Verb, past participle
				case "VBP" : // Verb, non­3rd person singular present
				case "VBZ" : // Verb, 3rd person singular present
				case "MD"  : // Modal
					if (!this.synsets.contains(i)) {
						this.synsets.add(i);
					}
					break;
				default:
					break;
			}
    	}
    }
}