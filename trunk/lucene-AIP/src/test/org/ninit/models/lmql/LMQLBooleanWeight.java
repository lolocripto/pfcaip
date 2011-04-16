package org.ninit.models.lmql;



import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.ninit.models.lmql.LMQLBooleanQuery.BooleanTermQuery;

/**
 * Weight BM25 class, implements <I>public Scorer scorer(IndexReader reader)
 * throws IOException</I> <BR>
 * and <I>public Explanation explain(IndexReader reader, int doc) throws
 * IOException </I><BR>
 * Query weight is not used in this BM25 implementation.
 * 
 * @author "Joaquin Perez-Iglesias"
 * 
 */
@SuppressWarnings("serial")
public class LMQLBooleanWeight extends Weight {

	private BooleanTermQuery[] should;
	private BooleanTermQuery[] must;
	private BooleanTermQuery[] not;
	private BooleanTermQuery[] unique = null;
	private String[] fields = null;
	private float[] boosts;
	private float[] bParams;
	private int howMany = 0;

    public LMQLBooleanWeight(BooleanTermQuery[] should,
			BooleanTermQuery[] must, BooleanTermQuery[] not) {
		if (should.length > 0) {
			this.should = should;
			this.unique = this.should;
			howMany++;
		}
		if (must.length > 0) {
			this.must = must;
			this.unique = this.must;
			howMany++;
		}
		if (not.length > 0) {
			this.not = not;
			this.unique = this.not;
			howMany++;
		}
	}

	public LMQLBooleanWeight(BooleanTermQuery[] should,
			BooleanTermQuery[] must, BooleanTermQuery[] not, String fields[],
			float[] boosts, float[] bParams) {
		this(should, must, not);
		this.fields = fields;
		this.boosts = boosts;
		this.bParams = bParams;
	}

	/**
	 * Return null
	 * 
	 * @see org.apache.lucene.search.Weight#explain(org.apache.lucene.index.IndexReader,
	 *      int)
	 */
	@Override
	public Explanation explain(IndexReader reader, int doc) throws IOException {
	    /*
		if (this.fields == null)
			return new BM25BooleanScorer(reader, this.should, this.must,
					this.not, new BM25Similarity()).explain(doc);
		else
			return new BM25BooleanScorer(reader, this.should, this.must,
					this.not, new BM25Similarity(), this.fields, this.boosts,
					this.bParams).explain(doc);
					*/
	    return null;
	}

	/*
	 * Return null
	 * 
	 * @see org.apache.lucene.search.Weight#getQuery()
	 */
	@Override
	public Query getQuery() {
		return null;
	}

	/**
	 * Return 0
	 * 
	 * @see org.apache.lucene.search.Weight#getValue()
	 */
	@Override
	public float getValue() {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.search.Weight#normalize(float)
	 */
	@Override
	public void normalize(float norm) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.lucene.search.Weight#scorer(org.apache.lucene.index.IndexReader
	 * )
	 */
//	@Override
	public Scorer scorer(IndexReader reader) throws IOException {
		return new LMQLSingleBooleanScorer(reader, this.unique,	new LMQLSimilarity());
	}

	/**
	 * AIP Change code: added this new method to update this code that was made for 
	 * 	2.4.1 version of Lucene and we are using 3.0.3
	 */
	public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
		      boolean topScorer) throws IOException{
	    
	    return scorer(reader);
	}
	
	/**
	 * Return 0.
	 * 
	 * @see org.apache.lucene.search.Weight#sumOfSquaredWeights()
	 */
	@Override
	public float sumOfSquaredWeights() throws IOException {
		return 0;
	}

}
