package org.ninit.models.lmql;

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
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Constants;

/**
 * Calculate the relevance value of a single term applying BM25 function
 * ranking. The {@link LMQLParameters} k1, and b are used.<BR>
 * 
 * @author "Joaquin Perez-Iglesias"
 * @see LMQLParameters
 * 
 */
public class LMQLTermScorer extends Scorer {

	public static final int NO_MORE_DOCS = Integer.MAX_VALUE;
	
	private TermQuery term;
	private IndexReader reader;
	private TermDocs termDocs;
	private int[] sizes;
	private float lambda = .5f;
	private long docSizes; //tama�o total de la coleccion
	
	int aux1;
	int aux2;

	public LMQLTermScorer(IndexReader reader, TermQuery term, Similarity similarity)
			throws IOException {
		super(similarity);
		this.reader = reader;
		this.term = term;
		aux1 = reader.docFreq(term.getTerm());
		aux2 = reader.numDocs();		
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
		
		return (result? this.docID():NO_MORE_DOCS);
	}

	/**
	 * Implementacion del score del modelo LMQL
	 */
	@Override
	public float score() throws IOException {
	    
		float length = this.sizes[this.docID()];
		float result = this.termDocs.freq();
		result = result / length;
		
		result = result * this.lambda;

		float aux = this.reader.colDocFreq(this.term.getTerm());
		aux = aux / docSizes;
		aux = aux * (1 - this.lambda);

		result = result + aux;
		return result;
	}

	 public int advance(int target) throws IOException{
		while (this.docID() < target) {
			 this.nextDoc();
		}

			return this.docID();
	 }
}
