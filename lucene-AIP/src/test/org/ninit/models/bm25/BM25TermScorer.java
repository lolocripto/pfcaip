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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Constants;
import org.ninit.models.lmkcd.LMKCDTermScorer;

import aip.tests.AIPTestUtils;

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
	
	private static final boolean DEBUG = true;
	
	Logger logger =Logger.getLogger(BM25TermScorer.class);	

	private TermQuery term;
	private IndexReader reader;
	private TermDocs termDocs;
	private float idf;
	private float av_length;
	private int[] sizes;
	private float b;
	private float k1;
	private long docSizes;     //tamaÒo total de la coleccion

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
		this.docSizes = reader.docSizes();
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
		try {
			length = (float) this.sizes[this.docID()];
		} catch (Exception e) {
			System.out.println("PETE TOTAL en " + this.docID());
			length = this.av_length;
		}

		String docName = this.reader.document(docID()).get("docNo");
	    String docDebug = "FR941006-2-00169";
		if (docDebug.contains(docName)) 
			AIPTestUtils.global_debug = true;
		else
			AIPTestUtils.global_debug = false;

		float result = this.b * (length / this.av_length);
		result = result + 1 - this.b;

		result = (this.term.getBoost() * this.termDocs.freq()) / result;
		// FREQ SATURATION
		result = result / (result + this.k1);

		// return result * this.idf * this.term.getBoost();//AIP: lo
		// multiplicamos otra vez por el boost???
		result = result * this.idf;
		
//		debug("BM25 score() --> doc["+this.reader.document(this.docID()).get("docNo")+"] size["+length+"] av_length["+this.av_length+"] freq["+this.termDocs.freq()+"] result[" + result + "]");
		
		debug("BM25 Score() DATA --------> docID[" + this.docID() + "] docName[" + this.reader.document(this.docID()).get("docNo") + "] term[" + this.term.getTerm().text()+ "]  L(D)[" + length  
				+ "] av_length["+this.av_length + "] freq(t,d)[" + this.termDocs.freq() + "]");
		debug("             COMPUTE -----> [" + result + "]");

		return result;

	}

	public int advance(int target) throws IOException {
		while (this.docID() < target) {
			this.nextDoc();
		}
		return this.docID();
	}
	
	private void debug(String text){
		if (DEBUG && AIPTestUtils.global_debug){
//			System.out.println("[BM25TermScorer]["+text+"]");
			logger.debug(text);
		}
	}

}
