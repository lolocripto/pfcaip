package org.ninit.models.bm25f;

/**
 * BM25FTermScorer.java
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
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Constants;

/**
 * Calculate the relevance value of a term applying BM25F function ranking. The
 * {@link BM25FParameters} k1,b_field, boost_field are used.<BR>
 * 
 * @author "Joaquin Perez-Iglesias"
 * @see BM25FParameters
 * 
 */
public class BM25FTermScorer extends Scorer {

    public static final int NO_MORE_DOCS = Integer.MAX_VALUE;
    
	private TermDocs[] termDocs;
	private TermQuery term;
	private float idf = 0f;
	private IndexReader reader;
	private String[] fields;
	private float[] boost;
	private float[] bParam;
	private boolean[] termDocsNext;
	private int doc = Integer.MAX_VALUE;
	private boolean initializated = false;

	/**
	 * Storing in "termDocs", for each field, an array of the docId that the term "term" appears
	 * @param reader
	 * @param term
	 * @param fields
	 * @param boosts
	 * @param bParams
	 * @param similarity
	 */
	public BM25FTermScorer(IndexReader reader, TermQuery term, String[] fields,
			float[] boosts, float[] bParams, Similarity similarity) {
		super(similarity);

		this.reader = reader;
		this.term = term;
		this.fields = fields;
		this.boost = boosts;
		this.bParam = bParams;
		this.termDocs = new TermDocs[this.fields.length];
		this.termDocsNext = new boolean[this.fields.length];
		try {
			for (int i = 0; i < this.fields.length; i++)
				this.termDocs[i] = reader.termDocs(new Term(this.fields[i],
						term.getTerm().text()));

			//idf value only depends on the term frequency in the doc, this stats is given by Lucene
			//AIP comment: creo que esto hay que cambiarlo!!
			this.idf = this.getSimilarity().idf(
					this.reader.docFreq(new Term(BM25FParameters.getIdfField(),
							term.getTerm().text())), this.reader.numDocs());

		} catch (IOException e) {
			e.printStackTrace();
		}

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
	 */
	/*
	@Override
	public Explanation explain(int doc) throws IOException {
		if (this.advance(doc) == NO_MORE_DOCS)
			return null;
		
		float acum = 0f;
		Explanation result = new Explanation();
		Explanation tf = new Explanation();
		result.setDescription("BM25F (" + this.term.getTerm().text() + ")");

		for (int i = 0; i < this.fields.length; i++) {

			if (this.termDocs[i].doc() == doc) {
				Explanation partial = new Explanation();

				byte[] norm = this.reader.norms(this.fields[i]);

				float av_length = (float) BM25FParameters
						.getAverageLength(this.fields[i]);
				float length = 1 / ((Similarity.decodeNorm(norm[this.doc()])) * (Similarity
						.decodeNorm(norm[this.doc()])));

				float aux = 0f;
				aux = this.bParam[i] * length / av_length;

				aux = aux + 1 - this.bParam[i];
				acum += this.boost[i] * this.termDocs[i].freq() / aux;

				partial = new Explanation(this.boost[i]
						* this.termDocs[i].freq() / aux, "(" + this.fields[i]
						+ ":" + this.term.getTerm().text() + ") B:"
						+ this.bParam[i] + ",Length:" + length + ",AvgLength:"
						+ av_length + ",Freq:" + this.termDocs[i].freq()
						+ ",Boost:" + this.boost[i]);
				tf.addDetail(partial);
			}
		}

		Explanation idfE = new Explanation(this.idf, " idf (docFreq:"
				+ this.reader.docFreq(new Term(BM25FParameters.getIdfField(),
						this.term.getTerm().text())) + ",numDocs:"
				+ this.reader.numDocs() + ")");
		result.addDetail(idfE);

		tf.setDescription("K1: " + acum + "/(" + acum + " + "
				+ BM25FParameters.getK1() + ")");

		acum = acum / (BM25FParameters.getK1() + acum);
		tf.setValue(acum);
		result.addDetail(tf);
		acum = acum * this.idf;
		result.setValue(acum);

		return result;
	}
	*/
	
	/**
	 * termDocsNext is an array that stored, in ascendent order, the docIDs 
	 * @return
	 * @throws IOException
	 */
	private int init() throws IOException {
		for (int i = 0; i < this.fields.length; i++) {
			this.termDocsNext[i] = this.termDocs[i].next();
			if (this.termDocsNext[i] && this.termDocs[i].doc() < this.doc) {
				this.doc = this.termDocs[i].doc();
			}
		}
		return this.doc;
	}

	/*
	 * Returns: doc = next() ? doc() : NO_MORE_DOCS;
	 * 
	 * @see org.apache.lucene.search.Scorer#next()
	 */
	@Override
	public int nextDoc() throws IOException {

		if (!initializated) {
			this.initializated = true;
			return this.init();
		}

		int min = NO_MORE_DOCS;

		for (int i = 0; i < this.fields.length; i++) {
			if (this.termDocsNext[i] && this.termDocs[i].doc() == this.doc) {
				this.termDocsNext[i] = this.termDocs[i].next();
			}
			if (this.termDocsNext[i] && this.termDocs[i].doc() < min)
				min = this.termDocs[i].doc();
		}
		return this.doc;//AIP comment: nose nose

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.search.Scorer#score()
	 */
	@Override
	public float score() throws IOException {
		float acum = 0f;

		for (int i = 0; i < this.fields.length; i++) {

			if (this.termDocs[i].doc() == doc) {
//				byte[] norm = this.reader.norms(this.fields[i]);
				int[] sizes = this.reader.sizes(Constants.CATCHALL_FIELD);

//				float av_length = (float) BM25FParameters.getAverageLength(this.fields[i]);
				float av_length = (float) this.reader.avgDocSize();
				float length = 0f;
//				float normV = Similarity.decodeNorm(norm[this.docID()]);
//				length = 1 / (normV * normV);
				length = sizes[this.docID()];

				float aux = 0f;
				aux = this.bParam[i] * length / av_length;

				aux = aux + 1 - this.bParam[i];
				acum += (this.term.getBoost()*this.boost[i] * this.termDocs[i].freq()) / aux;
			}
		}

		acum = acum / (BM25FParameters.getK1() + acum);
		acum = acum * this.idf;

		return acum;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.search.Scorer#skipTo(int)
	 */
	/* Not needed in this implementation
	@Override
	public boolean skipTo(int target) throws IOException {
		while (this.doc() < target && this.next()) {
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
