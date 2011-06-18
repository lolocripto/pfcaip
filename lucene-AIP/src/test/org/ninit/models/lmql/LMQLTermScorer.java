package org.ninit.models.lmql;

/**
 * @author Antonio Iglesias Parma
 *
 * Hay un objeto de esta clase por término en la query
 */

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Constants;

import aip.tests.AIPTestUtils;
import aip.tests.TRECSearcher;

public class LMQLTermScorer extends Scorer {

	private static final boolean DEBUG = true;
	public static final int NO_MORE_DOCS = Integer.MAX_VALUE;

//	FileWriter fdebug = new FileWriter(AIPTestUtils.DIR_BASE + "debug/LMQLTermScorer.txt");
//	BufferedWriter out = new BufferedWriter(fdebug);
	Logger logger =Logger.getLogger(LMQLTermScorer.class);

	private TermQuery term;
	private IndexReader reader;
	private TermDocs termDocs;
	private int[] sizes;
	private float lambda = 0.9f;
	private long docSizes; // tamaño total de la coleccion
	
	public float constantPart;
	public String termS; 

	public LMQLTermScorer(IndexReader reader, TermQuery term, Similarity similarity) throws IOException {
		super(similarity);
		this.reader = reader;
		this.term = term;
		termS = term.getTerm().text();
		this.sizes = this.reader.sizes(Constants.CATCHALL_FIELD);
		docSizes = reader.docSizes();
		
		this.termDocs = this.reader.termDocs(this.term.getTerm());
//		this.termDocs = this.reader.termDocs(null);
		
		float colDocFreq = this.reader.colDocFreq( this.term.getTerm());
		
		constantPart = new Float((1 - lambda) * (colDocFreq / docSizes));
		if (constantPart != 0){
			constantPart = (float) Math.log(constantPart);
		}
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

		int next = result ? this.docID() : NO_MORE_DOCS;
		// debug("nextDoc(): " + next);
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
		String docNo = this.reader.document(docID).get("docNo");
		
		String docDebug = "FBIS4-41684,LA112489-0141,FBIS4-19535,LA102989-0026,FT921-4265,FBIS4-63999";
		if (docDebug.contains(docNo)) 
			AIPTestUtils.global_debug = true;
		else
			AIPTestUtils.global_debug = false;

//		float part1 = (1 - this.lambda) * (freq / length);
//		float part2 = (this.lambda) * (colDocFreq / docSizes);
		
		/*
		 * NUEVA Formula
		 * 
		 * log ((lambda)* (freq_t_documento/length(documento)) + (1-lambda)*(freq_t_colección/length(colección)))
		 */

		float part1 = lambda * (freq / length);
		float part2 = (1 - lambda) * (colDocFreq / docSizes);
		
		float result = (float) Math.log(part1 + part2);

		debug("LMQL Score() DATA --------> docID[" + docID + "] docName[" + docNo + "]  L(D)[" + length + "] term[" + termS + "] freq(t,C)[" + colDocFreq
					+ "] freq(t,d)[" + freq + "] lambda[" + this.lambda + "]  L(C)[" + docSizes + "]");

//		debug("             COMPUTE -----> (1 - lambda) * (freq(t,d) / L(D))=[" + part1 + "] + lambda * (freq(t,C) / L(C))=[" + part2 + "] = [" + result + "]");
		debug("             COMPUTE ----->  (lambda * (freq(t,d) / L(D))=[" + part1 + "] +  (1 - lambda) * (freq(t,C) / L(C))=[" + part2 + "] => log(result)=["+result+"]");
		return result;
	}

	public float getDefault(){
			
		debug("             COMPUTE(constant part '"+termS+"' ------>  log ((1 - lambda) * (colDocFreq / docSizes))=["+this.constantPart+"]");
		
		return this.constantPart;
	}
	
	public int advance(int target) throws IOException {
		while (this.docID() < target) {
			this.nextDoc();
		}

		return this.docID();
	}

	private void debug(String text) {
		if (DEBUG && AIPTestUtils.global_debug) {
//			System.out.println("[LMQLTermScorer][" + text + "]");
			logger.debug(text);
//			try {
//				out.write(text);
//				out.newLine();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
		}
	}
}
