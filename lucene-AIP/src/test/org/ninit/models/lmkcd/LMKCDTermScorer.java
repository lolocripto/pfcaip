package org.ninit.models.lmkcd;

/**
 * @author Antonio Iglesias Parma
 */

import java.io.IOException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.TermQuery;

public class LMKCDTermScorer extends Scorer {

	public static final int NO_MORE_DOCS = Integer.MAX_VALUE;
	private static final boolean DEBUG = false;
	
	private TermQuery term;
	private IndexReader reader;
	private TermDocs termDocs;
	private long docSizes;     //tamaño total de la coleccion
	private int freqTermQuery; //frecuencia del termino en la consulta
	private int queryLength;   //tamaño de la consulta
	float colDocFreq;

	/**
	 * Inicialización de las variables que serán usadas en la fórmula del modelo:
	 * 		- tamaño de la colección
	 * 		- tamaño de la consulta
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
	}

	@Override
	public int docID() {
		return this.termDocs.doc();
	}

	/**
	 * El propio motor de Lucene va llamando iterativamente a este método para 
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

	    String docName = this.reader.document(this.docID()).get("docNo");
		debug("LMKCD Score() DATA -----------> docName[" + docName + 
				"] term[" + this.term.getTerm().text() + "] frec(t,Q)[" + this.freqTermQuery + 
				"] L(Q)[" + this.queryLength +  "] freq(t,C)[" + this.colDocFreq 
				+ "] L(C)["+docSizes+"]"); 

		
		float a = (float) this.freqTermQuery / this.queryLength;
	    float b = (float) this.colDocFreq / this.docSizes;
	    
	    if (b==0) return 0f; //AIP si no controlamos este error daria infinito
	    
	
	    double result = a / b;
//	    System.out.println("a/b="+result);
	    result = Math.log10(result);
//	    System.out.println("log(a/b)["+result+"]");
	    result = result * a;
//	    System.out.println("*a="+result);
	    result = result * -1;
//	    System.out.println("*-1="+result);

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
		 if (DEBUG)
			 System.out.println("[LMKCDTermScorer]["+text+"]");
	 }
}
