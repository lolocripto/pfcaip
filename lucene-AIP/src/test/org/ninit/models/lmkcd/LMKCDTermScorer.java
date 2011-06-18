package org.ninit.models.lmkcd;

/**
 * @author Antonio Iglesias Parma
 */

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Constants;
import org.ninit.models.lmql.LMQLTermScorer;

import aip.tests.AIPTestUtils;

public class LMKCDTermScorer extends Scorer {

	public static final int NO_MORE_DOCS = Integer.MAX_VALUE;
	private static final boolean DEBUG = true;
	Logger logger =Logger.getLogger(LMKCDTermScorer.class);
	
	private TermQuery term;
	private IndexReader reader;
	private TermDocs termDocs;
	private long docSizes;     //tama�o total de la coleccion
	private int freqTermQuery; //frecuencia del termino en la consulta
	private int queryLength;   //tama�o de la consulta
	float colDocFreq;
	private int[] sizes;
	
	float a;
	
	/**
	 * Inicializaci�n de las variables que ser�n usadas en la f�rmula del modelo:
	 * 		- tama�o de la colecci�n
	 * 		- tama�o de la consulta
	 * 		- frecuencia del termino en la consulta
	 */
	public LMKCDTermScorer(IndexReader reader, TermQuery term, Similarity similarity,
		int freqTermQuery, int queryLength)
			throws IOException {
		super(similarity);
		this.reader = reader;
		this.term = term;
		this.termDocs = this.reader.termDocs(this.term.getTerm());
		docSizes = reader.docSizes();
		this.freqTermQuery = freqTermQuery;
		this.queryLength = queryLength;
		this.colDocFreq = this.reader.colDocFreq(this.term.getTerm());
		this.sizes = this.reader.sizes(Constants.CATCHALL_FIELD);
		
		a = (float) this.freqTermQuery / this.queryLength;
		
	}

	@Override
	public int docID() {
		return this.termDocs.doc();
	}

	/**
	 * El propio motor de Lucene va llamando iterativamente a este m�todo para 
	 * 		obtener el ID del siguiente documento 
	 */
	@Override
	public int nextDoc() throws IOException {

		boolean result = this.termDocs.next();
		if (!result)
			this.termDocs.close();
		
		return (result? this.docID():NO_MORE_DOCS);
	}

	/**
	 * Implementacion del score del modelo LMKCL
	 */
	@Override
	public float score() throws IOException {
		int docID = this.docID();
		float length = this.sizes[docID];
		float freq = this.termDocs.freq();
		
	    String docName = this.reader.document(docID).get("docNo");
	    String docDebug = "FR941006-2-00169,FBIS4-41684,LA112489-0141,FBIS4-19535,LA102989-0026,FT921-4265,FBIS4-63999"; 
		if (docDebug.contains(docName)) 
			AIPTestUtils.global_debug = true;
		else
			AIPTestUtils.global_debug = false;
		
//	    float b = (float) this.colDocFreq / this.docSizes;
		float b = (float) freq / length;
	    
	    if (b==0) return 0f; //AIP si no controlamos este error daria infinito
	
	    double result = a / b;
//	    System.out.println("a/b="+result);
	    result = Math.log(result);
//	    System.out.println("log(a/b)["+result+"]");
	    result = result * a;
//	    System.out.println("*a="+result);
	    result = result * -1;
//	    System.out.println("*-1="+result);
	    
		debug("LMKCD Score() DATA -----------> docName[" + docName + 
				"] term[" + this.term.getTerm().text() + "] frec(t,Q)[" + this.freqTermQuery + 
				"] L(Q)[" + this.queryLength +  "] freq(t,C)[" + colDocFreq + "] freq(t,d)[" + freq   
				+ "] L(D)[" + length + "] L(C)["+docSizes+"]"); 
	    debug("              COMPUTE --------> a=["+a+"] b=["+b+"] -a*log(a/b)[" + result+"]");

	    return (float) result;
	}
	
	 public int advance(int target) throws IOException{
		while (this.docID() < target) {
			 this.nextDoc();
		}

			return this.docID();
	 }
	 
	 private void debug(String text){
		 if (DEBUG && AIPTestUtils.global_debug)
//			 System.out.println("[LMKCDTermScorer]["+text+"]");
			 logger.debug(text);
	 }
}
