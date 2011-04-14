package org.ninit.models.bool;

/**
 * MustBooleanScorer.java
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
 * Boolean Scorer that matches all documents that contains all terms (AND
 * operator).<BR>
 * 
 * @author "Joaquin Perez-Iglesias"
 * 
 */
public class MustBooleanScorer extends AbstractBooleanScorer {

	public static final int NO_MORE_DOCS = Integer.MAX_VALUE;

	private boolean initializated = false;
	private int doc = -1;

	public MustBooleanScorer(Similarity similarity, Scorer[] scorer)
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
		result.setDescription("AND");
		float value = 0f;
		for (int i = 0; i < this.subScorer.length; i++) {
			detail = this.subScorer[i].explain(doc);
			result.addDetail(detail);
			value += detail.getValue();
		}
		result.setValue(value);
		return result;
	}*/
	
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
	 * @see org.apache.lucene.search.Scorer#next()
	 */
	@Override
	public int nextDoc() throws IOException {
		// INIT SUBSCORERS
		if (!this.initializated) {
			this.init();
			this.initializated = true;
		} else {
			int max = -1;
			for (int i = 0; i < this.subScorer.length; i++) {
				if (this.subScorerNext[i]
						&& this.subScorer[i].docID() == this.doc) {
				    	int aux = this.subScorer[i].nextDoc();
					this.subScorerNext[i] = (aux != NO_MORE_DOCS);
					if (this.subScorerNext[i] && this.subScorer[i].docID() > max)
						max = this.subScorer[i].docID();
				}
			}
			this.doc = max;
		}
		while (true) {
			int count = 0;
			boolean more = true;
			for (int i = 0; i < this.subScorer.length && more; i++) {
				if (this.subScorerNext[i]) {
					if (this.subScorer[i].docID() == this.doc) {
						count++;
					}
					if (this.subScorer[i].docID() < this.doc) {
					    int aux = this.subScorer[i].nextDoc();
						this.subScorerNext[i] = (aux != NO_MORE_DOCS);
						if (this.subScorerNext[i]
								&& this.subScorer[i].docID() > this.doc) {
							this.doc = this.subScorer[i].docID();
							more = false;
							count = 0;
						}
					}
					if (count == this.subScorer.length)
						return this.doc;
				} else
					return NO_MORE_DOCS;
			}

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.search.Scorer#score()
	 */
	@Override
	public float score() throws IOException {
		double result = 0f;
		for (int i = 0; i < this.subScorer.length; i++) {
			if (this.subScorer[i].docID() == this.doc)
				result = this.subScorer[i].score() + result;

		}
		return (float) result;
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

}
