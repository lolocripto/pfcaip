package org.ninit.models.bool;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Similarity;
import org.ninit.models.lmql.LMQLTermScorer;

import aip.tests.AIPTestUtils;

/**
 * Boolean Scorer that matches all documents that contains at least one term (OR
 * operator).<BR>
 * 
 * @author "Joaquin Perez-Iglesias" & "Antonio Iglesias Parma"
 * 
 */
public class ShouldBooleanScorer extends AbstractBooleanScorer {

	private static final boolean DEBUG = true;
	public static final int NO_MORE_DOCS = Integer.MAX_VALUE;

	private boolean initializated = false;
	private int doc = NO_MORE_DOCS;
	
//	FileWriter fdebug = new FileWriter(AIPTestUtils.DIR_BASE + "debug/ShouldBooleanScorer.txt");
//	BufferedWriter out = new BufferedWriter(fdebug);
	Logger logger =Logger.getLogger(ShouldBooleanScorer.class);

	public ShouldBooleanScorer(Similarity similarity, Scorer[] scorer)
			throws IOException {
		super(similarity, scorer);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.search.Scorer#doc()
	 */
	@Override
	public int docID() {
		return this.doc;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.search.Scorer#explain(int)
	 *//*
	@Override
	public Explanation explain(int doc) throws IOException {
		if (this.advance(doc) == NO_MORE_DOCS)
			return null;
		Explanation result = new Explanation();
		Explanation detail;
		result.setDescription("OR");
		float value = 0f;
		for (int i = 0; i < this.subScorer.length; i++) {
			if (this.subScorer[i].docID() == doc) {
				detail = this.subScorer[i].explain(doc);
				result.addDetail(detail);
				value += detail.getValue();
			}
		}
		result.setValue(value);
		return result;
	}*/

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.search.Scorer#next()
	 */
	@Override
	public int nextDoc() throws IOException {
		if (!this.initializated) {
			this.initializated = true;
			return this.init();
		}
		int min = NO_MORE_DOCS;
		// Avanzo los terminos con menor id
		for (int i = 0; i < this.subScorer.length; i++) {
			if (this.subScorerNext[i] && this.subScorer[i].docID() == this.doc) {
				this.subScorerNext[i] = (this.subScorer[i].nextDoc() != NO_MORE_DOCS);
			}
			if (this.subScorerNext[i] && this.subScorer[i].docID() < min)
				min = this.subScorer[i].docID();
		}

		this.doc = min;
		
//		debug("nextDoc() --> this.doc="+this.doc);
		
		return (this.doc == NO_MORE_DOCS)? NO_MORE_DOCS : this.doc;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.search.Scorer#skipTo(int)
	 */
	@Override
	public int advance(int target) throws IOException {
		while (this.docID() < target) {
		    this.nextDoc();
		}

		return this.docID();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * Aqui es donde se calcula el sumatorio de los scores de cada uno de los terminos
	 */
	@Override
	public float score() throws IOException {
		double result = 0f;
		int docID;
		for (int i = 0; i < this.subScorer.length; i++) {
			docID = this.subScorer[i].docID();
			
			if (docID == this.doc){
				result += this.subScorer[i].score();
			}else{
				if (this.subScorer[i] instanceof LMQLTermScorer){
					result += ((LMQLTermScorer)this.subScorer[i]).getDefault();	
				}else{
					debug("El termino no está y el subScrorer No es LMQLTermScorer con lo que no hace falta calcular la parte constante");
				}
				
			}
		}
		debug("---> TOTAL sumando scores para el doc["+this.docID()+"] es["+result+"]");
		return (float) result;
	}
	
	private int init() throws IOException {
		int result = NO_MORE_DOCS;
		for (int i = 0; i < this.subScorer.length; i++) {
		    int aux = this.subScorer[i].nextDoc();
			this.subScorerNext[i] = (aux != NO_MORE_DOCS);
			if (this.subScorerNext[i] && this.subScorer[i].docID() < this.doc) {
				this.doc = this.subScorer[i].docID();
				result = this.docID();
			}
		}
		return result;
	}

	/** 
	 * Init original
	private boolean init() throws IOException {
		boolean result = false;
		for (int i = 0; i < this.subScorer.length; i++) {
			this.subScorerNext[i] = this.subScorer[i].next();
			if (this.subScorerNext[i] && this.subScorer[i].doc() < this.doc) {
				this.doc = this.subScorer[i].doc();
				result = true;
			}
		}
		return result;
	}
	 */
	
	
	private void debug(String text){
		if (DEBUG && AIPTestUtils.global_debug)
			logger.debug(text);
	}
}
