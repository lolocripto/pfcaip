package org.ninit.models.bool;

/**
 * MatchAllBooleanScorer.java
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

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Similarity;

/**
 * Boolean Scorer that matches all documents.<BR>
 * 
 * @author "Joaquin Perez-Iglesias"
 * 
 */
public class MatchAllBooleanScorer extends AbstractBooleanScorer {

	public static final int NO_MORE_DOCS = Integer.MAX_VALUE;

	private int doc = -1;
	private int ndocs;

	public MatchAllBooleanScorer(Similarity similarity, int numDocs)
			throws IOException {
		super(similarity, null);
		this.ndocs = numDocs;
	}

	public MatchAllBooleanScorer(Similarity similarity, Scorer[] scorer, int numDocs)
			throws IOException {
	    	super(similarity, scorer);
	    	this.ndocs = numDocs;
	}	
	

	private int init() throws IOException {
		for (int i = 0; i < this.subScorer.length; i++) {
		    int aux = this.subScorer[i].nextDoc();
			this.subScorerNext[i] = (aux != NO_MORE_DOCS);
			if (this.subScorerNext[i] && this.subScorer[i].docID() > this.doc) {
				this.doc = this.subScorer[i].docID();
			}
		}
		return this.doc;
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

	/**
	 * <B>Return null</B><BR>
	 * 
	 * @see org.apache.lucene.search.Scorer#explain(int)
	 */
	/*
	@Override
	public Explanation explain(int doc) throws IOException {
		return null;
	}
	*/
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.search.Scorer#next()
	 */
	@Override
	public int nextDoc() throws IOException {
		this.doc++;
		return (this.doc < this.ndocs)? this.doc : NO_MORE_DOCS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.search.Scorer#score()
	 */
	@Override
	public float score() throws IOException {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.search.Scorer#skipTo(int)
	 */
	@Override
	public int advance(int target) throws IOException {
		return (target < this.ndocs)?target:this.ndocs;
	}

}
