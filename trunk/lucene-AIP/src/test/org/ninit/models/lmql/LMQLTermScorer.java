package org.ninit.models.lmql;

/**
 * @author Antonio Iglesias Parma
 */

import java.io.IOException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Constants;

public class LMQLTermScorer extends Scorer {

	private static final boolean DEBUG = false;
	public static final int NO_MORE_DOCS = Integer.MAX_VALUE;
	
	private TermQuery term;
	private IndexReader reader;
	private TermDocs termDocs;
	private int[] sizes;
	private float lambda = .9f;
	private long docSizes; //tamaño total de la coleccion

	public LMQLTermScorer(IndexReader reader, TermQuery term, Similarity similarity)
			throws IOException {
		super(similarity);
		this.reader = reader;
		this.term = term;
		this.sizes = this.reader.sizes(Constants.CATCHALL_FIELD);
		this.termDocs = this.reader.termDocs(this.term.getTerm());
		docSizes = reader.docSizes();
	}

	@Override
	public int docID() {
		return this.termDocs.doc();
	}

	@Override
	public int nextDoc() throws IOException {
		boolean result = this.termDocs.next();
		if (!result)
			this.termDocs.close();
		
		int next = result? this.docID():NO_MORE_DOCS;
//		debug("nextDoc(): " + next);
		return next;
	}

	/**
	 * Implementacion del score del modelo LMQL
	 */
	@Override
	public float score() throws IOException {
	    int docID = this.docID();
		float length = this.sizes[docID];
		float freq = this.termDocs.freq();
		float colDocFreq = this.reader.colDocFreq(this.term.getTerm());
		
		debug("LMQL Score() DATA --------> docID["+docID+"] docName[" + this.reader.document(docID).get("docNo") + 
				"]  L(D)[" + length + "] term[" + this.term.getTerm().text() + "] freq(t,C)[" +
				colDocFreq + "] freq(t,d)["+freq+"] lambda["+this.lambda+"]  L(C)["+docSizes+"]"); 
		
		float part1 = this.lambda * (freq / length);

		float part2 = (1 - this.lambda) * (colDocFreq / docSizes);

		float result = part1 + part2;
		
		debug("             COMPUTE -----> lambda * (freq(t,d) / L(D))=["+part1+"] + (1-lambda) * (freq(t,C) / L(C))=["+part2+"] = [" + result + "]");
		return result;
	}

	 public int advance(int target) throws IOException{
		while (this.docID() < target) {
			 this.nextDoc();
		}

			return this.docID();
	 }
	 
	 private void debug(String text){
		 if (this.DEBUG)
			 System.out.println("[LMQLTermScorer]["+text+"]");
	 }
}
