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
	private String frame;  // name frame rdf 
	private Map<String, List<NLPnode> >  nodes_wsd; // ctx_xx, sentence analized 
	
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
		this.nodes_wsd = new HashMap<>();
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
    
    public void addTokens(String ctx, NLPnode nod) {
		if (!this.nodes_wsd.containsKey(ctx)) {
			this.nodes_wsd.put(ctx, new ArrayList<>());
		}
		//System.out.println(" addTokens " + this.content+" ==> ctx "+ctx);
		this.nodes_wsd.get(ctx).add(nod);
    }
   
    public boolean containsCtx(String ctx) {
        return this.nodes_wsd.containsKey(ctx);
    }
    
    public void addSenseScore(String ctx, Integer id, String sen, Double score) {
    	if (this.nodes_wsd.containsKey(ctx)) {
    	  this.nodes_wsd.get(ctx).get(id).setSense(sen,score); 
    	}
    }
    
    public Map<String, List<NLPnode>> getDenotedSenses() {
    	return this.nodes_wsd;
    }
}
