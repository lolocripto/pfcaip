package org.ninit.models.bm25;

/**
 * BM25TermScorer.java
 *
 * Copyright (c) 2008 "Joaquín Pérez-Iglesias"
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
 * @author "Joaquin Perez-Iglesias"
 * @see BM25Parameters
 * 
 */
public class BM25TermScorer extends Scorer {

	public static final int NO_MORE_DOCS = Integer.MAX_VALUE;
	
	private TermQuery term;
	private IndexReader reader;
	private TermDocs termDocs;
	private float idf;
	private float av_length;
//	private byte[] norm;
	private int[] sizes;
	private float b;
	private float k1;
	
	int aux1;
	int aux2;

	public BM25TermScorer(IndexReader reader, TermQuery term, Similarity similarity)
			throws IOException {
		super(similarity);
		this.reader = reader;
		this.term = term;
		aux1 = reader.docFreq(term.getTerm());
		aux2 = reader.numDocs();		
		this.idf = this.getSimilarity().idf(aux1, aux2);
//		this.norm = this.reader.norms(this.term.getTerm().field());
		this.sizes = this.reader.sizes(Constants.CATCHALL_FIELD);
//		this.av_length = BM25Parameters.getAverageLength(this.term.getTerm().field());
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
//	@Override
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
		length = 1 / ((Similarity.decodeNorm(norm[this.docID()])) * (Similarity.decodeNorm(norm[this
				.docID()])));

		float tf = this.termDocs.freq();

		float result = BM25Parameters.getB() * (length / av_length);
		result = result + 1 - BM25Parameters.getB();
		result = tf / result;
		// FREQ SATURATION
		result = result / (result + BM25Parameters.getK1());

		Explanation idfE = new Explanation(this.idf, " idf (docFreq:"
				+ this.reader.docFreq(this.term.getTerm()) + ",numDocs:" + this.reader.numDocs()
				+ ")");
		Explanation bE = new Explanation(result, "B:" + BM25Parameters.getB() + ",Length:" + length
				+ ",AvgLength:" + av_length + ",Freq:" + tf + ",K1:" + BM25Parameters.getK1());

		Explanation resultE = new Explanation(this.idf * result, "BM25("
				+ this.term.getTerm().field() + ":" + this.term.getTerm().text());
		resultE.addDetail(idfE);
		resultE.addDetail(bE);

		return resultE;
	}
	
	@Override
	public int nextDoc() throws IOException {

		boolean result = this.termDocs.next();
		if (!result)
			this.termDocs.close();
		
//		return result;
		return (result? this.docID():NO_MORE_DOCS);
	}

	@Override
	public float score() throws IOException {
		float length = 0f;
//		float norm = Similarity.decodeNorm(this.norm[this.doc()]);
//		length = 1 / (norm * norm);
		length = (float) this.sizes[this.docID()];

		// length = Similarity.decodeNorm(this.norm[this.doc()]);

		// LENGTH NORMALIZATION

		float result = this.b * (length / this.av_length);
		result = result + 1 - this.b;

		result = (this.term.getBoost() * this.termDocs.freq()) / result;
		// FREQ SATURATION
		result = result / (result + this.k1);

		return result * this.idf * this.term.getBoost();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.search.Scorer#skipTo(int)
	 */
	/*
	@Override
	public boolean skipTo(int target) throws IOException {
		while (this.next() && this.doc() < target) {
		}

		return this.doc() == target;
	}
	*/
	 public int advance(int target) throws IOException{
		while (this.docID() < target) {
			 this.nextDoc();
		}

			return this.docID();
	 }
}
