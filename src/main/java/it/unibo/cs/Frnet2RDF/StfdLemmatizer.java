package it.unibo.cs.Frnet2RDF;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class StfdLemmatizer {
	/** see:
	 *  https://stackoverflow.com/questions/1578062/lemmatization-java 
	 *  http://www.sfs.uni-tuebingen.de/~keberle/NLPTools/presentations/CoreNLP/CoreNLP_introduction.pdf
	 **/
	    private StanfordCoreNLP pipeline;
	    private List<List<NLPnode>> nodes;

	    public StfdLemmatizer() {
	        // Create StanfordCoreNLP object properties, with POS tagging
	        // (required for lemmatization), and lemmatization
	        Properties props = new Properties();
	        // tokenize -> tokenization
	        // ssplit -> sentence-splitting
	        // pos -> POS-tagging
	        // lemma -> lemmatization
	        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");

	        this.pipeline = new StanfordCoreNLP(props);
	        this.nodes = new ArrayList<List<NLPnode>>();
	    }

	    public List<List<NLPnode>> lemmatize(String documentText)
	    {
	        
	        // Create an empty Annotation just with the given text
	        Annotation document = new Annotation(documentText);
	        
	        // run all Annotators on this text
	        this.pipeline.annotate(document);
	        
	        // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
	        // Iterate over all of the sentences found
	        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	   
	        int i=-1;
	        for(CoreMap sentence: sentences) {
	            // Iterate over all tokens in a sentence
	        	
	        	nodes.add(++i, new ArrayList<NLPnode>());
	        	// a CoreLabel is a CoreMap with additional token-specific methods
	            for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	            	// this is the POS tag of the token
	                String pos = token.get(PartOfSpeechAnnotation.class);
	                // this is the lemma of the token
	                String lemma = token.get(LemmaAnnotation.class);
	                // this is the text of the token
	                String word = token.get(TextAnnotation.class);
	                // Add for each word annotations found
	                nodes.get(i).add(new NLPnode(word,lemma,pos));
	                
	                // this is the NER  (Named entity recognition) label of the token
	                //String ne = token.get(NamedEntityTagAnnotation.class);   
	            }
	        }
	        return nodes;
	    }
}
