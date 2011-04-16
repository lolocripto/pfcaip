package org.ninit.models.lmkcd;

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

public class LMKCDTermScorer extends Scorer {

	public static final int NO_MORE_DOCS = Integer.MAX_VALUE;
	
	private TermQuery term;
	private IndexReader reader;
	private TermDocs termDocs;
	private int[] sizes;
	private long docSizes; //tama�o total de la coleccion
	private int freqTermQuery;
	private int queryLength;
	int aux1;
	int aux2;

	public LMKCDTermScorer(IndexReader reader, TermQuery term, Similarity similarity,
		int freqTermQuery, int queryLength)
			throws IOException {
		super(similarity);
		this.reader = reader;
		this.term = term;
		aux1 = reader.docFreq(term.getTerm());
		aux2 = reader.numDocs();		
		this.sizes = this.reader.sizes(Constants.CATCHALL_FIELD);
		this.termDocs = this.reader.termDocs(this.term.getTerm());
		docSizes = reader.docSizes();
		this.freqTermQuery = freqTermQuery;
		this.queryLength = queryLength;
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
	 * Implementacion del score del modelo LMKCL
	 */
	@Override
	public float score() throws IOException {
	    float a = (float) this.freqTermQuery / this.queryLength;
	    float b = this.reader.colDocFreq(this.term.getTerm());
	    b = (float) b / this.docSizes;
	    
	    if (b==0) return 0f; //AIP si no controlamos este error daria infinito
	    
	    double result = a / b;
	    result = Math.log(result);
	    result = result * a;
	    result = result * -1;
	    
	    return (float) result;
	}

	 public int advance(int target) throws IOException{
		while (this.docID() < target) {
			 this.nextDoc();
		}

			return this.docID();
	 }
}
