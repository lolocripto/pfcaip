package org.ninit.models.bm25;

/**
 * BM25TermScorer.java
 *
 * Copyright (c) 2008 "Joaqu√≠n P√©rez-Iglesias"
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Constants;

/**
 * Calculate the relevance value of a single term applying BM25 function
 * ranking. The {@link BM25Parameters} k1, and b are used.<BR>
 * 
 * @author "Antonio Iglesias Parma"
 * @see BM25Parameters
 * 
 */
public class BM25TermScorer extends Scorer {

	public static final int NO_MORE_DOCS = Integer.MAX_VALUE;
	
	private static final boolean DEBUG = false;

	private TermQuery term;
	private IndexReader reader;
	private TermDocs termDocs;
	private float idf;
	private float av_length;
	private int[] sizes;
	private float b;
	private float k1;

	int aux1;
	int aux2;

	/**
	 * InicializaciÛn de las variables que ser·n usadas en la fÛrmula del modelo:
	 * 		- idf
	 * 		- tamaÒo del documento
	 * 		- tamaÒo medio de la colecciÛn
	 */
	public BM25TermScorer(IndexReader reader, TermQuery term, Similarity similarity) throws IOException {
		super(similarity);
		this.reader = reader;
		this.term = term;
		aux1 = reader.docFreq(term.getTerm());
		aux2 = reader.numDocs();
		this.idf = this.getSimilarity().idf(aux1, aux2);
		this.sizes = this.reader.sizes(Constants.CATCHALL_FIELD);
		this.av_length = this.reader.avgDocSize();

		this.b = BM25Parameters.getB();
		this.k1 = BM25Parameters.getK1();
		this.termDocs = this.reader.termDocs(this.term.getTerm());
	}

	@Override
	public int docID() {
		return this.termDocs.doc();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.search.Scorer#explain(int)
	 */
	// @Override
	public Explanation explain(int doc) throws IOException {
		// Init termDocs
		if (this.termDocs != null)
			this.termDocs.close();
		this.termDocs = this.reader.termDocs(this.term.getTerm());
		// skipTo doc

		//
		if (this.advance(doc) == NO_MORE_DOCS)
			return null;
		float length = 0f;
		byte[] norm = this.reader.norms(this.term.getTerm().field());

		float av_length = BM25Parameters.getAverageLength(this.term.getTerm().field());
		length = 1 / ((Similarity.decodeNorm(norm[this.docID()])) * (Similarity.decodeNorm(norm[this.docID()])));

		float tf = this.termDocs.freq();

		float result = BM25Parameters.getB() * (length / av_length);
		result = result + 1 - BM25Parameters.getB();
		result = tf / result;
		// FREQ SATURATION
		result = result / (result + BM25Parameters.getK1());

		Explanation idfE = new Explanation(this.idf, " idf (docFreq:" + this.reader.docFreq(this.term.getTerm()) + ",numDocs:" + this.reader.numDocs() + ")");
		Explanation bE = new Explanation(result, "B:" + BM25Parameters.getB() + ",Length:" + length + ",AvgLength:" + av_length + ",Freq:" + tf + ",K1:" + BM25Parameters.getK1());

		Explanation resultE = new Explanation(this.idf * result, "BM25(" + this.term.getTerm().field() + ":" + this.term.getTerm().text());
		resultE.addDetail(idfE);
		resultE.addDetail(bE);

		return resultE;
	}

	/**
	 * El propio motor de Lucene va llamando iterativamente a este mÈtodo para 
	 * 		obtener el ID del siguiente documento 
	 */
	@Override
	public int nextDoc() throws IOException {

		boolean result = this.termDocs.next();
		if (!result)
			this.termDocs.close();

		return (result ? this.docID() : NO_MORE_DOCS);
	}

	/**
	 * AquÌ es donde realmente implementamos la fÛrmula asociada al modelo BM25
	 */
	@Override
	public float score() throws IOException {

		float length = 0f;
		length = (float) this.sizes[this.docID()];

		float result = this.b * (length / this.av_length);
		result = result + 1 - this.b;

		result = (this.term.getBoost() * this.termDocs.freq()) / result;
		// FREQ SATURATION
		result = result / (result + this.k1);

		// return result * this.idf * this.term.getBoost();//AIP: lo
		// multiplicamos otra vez por el boost???

		debug("BM25 score() --> doc["+this.reader.document(this.docID()).get("docNo")+"] size["+length+"] av_length["+this.av_length+"] freq["+this.termDocs.freq()+"]");
		return result * this.idf;

	}

	public int advance(int target) throws IOException {
		while (this.docID() < target) {
			this.nextDoc();
		}
		return this.docID();
	}
	
	private void debug(String text){
		if (DEBUG){
			System.out.println("[Debug]["+text+"]");
		}
	}

}
