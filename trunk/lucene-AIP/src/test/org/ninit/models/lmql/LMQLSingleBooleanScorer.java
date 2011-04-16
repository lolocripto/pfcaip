package org.ninit.models.lmql;
import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Similarity;
import org.ninit.models.bool.AbstractBooleanScorer;
import org.ninit.models.bool.MatchAllBooleanScorer;
import org.ninit.models.bool.MustBooleanScorer;
import org.ninit.models.bool.NotBooleanScorer;
import org.ninit.models.bool.ShouldBooleanScorer;
import org.ninit.models.lmql.LMQLBooleanQuery.BooleanTermQuery;

/**
 * BM25SingleBooleanScorer, calculates the total relevance value based boolean
 * expression, that has just one common operator (AND, OR, NOT) for all terms.<BR>
 * 
 * @author "Joaquin Perez-Iglesias"
 * 
 */
public class LMQLSingleBooleanScorer extends Scorer {

	private AbstractBooleanScorer booleanScorer = null;

	public LMQLSingleBooleanScorer(IndexReader reader,
			BooleanTermQuery[] termQuery, Similarity similarity)
			throws IOException {

	    super(similarity);

		Scorer[] scorer = new Scorer[termQuery.length];
		for (int i = 0; i < scorer.length; i++) {
			scorer[i] = new LMQLTermScorer(reader, termQuery[i].termQuery,
					similarity);
		}
		 
		if (termQuery[0].occur == BooleanClause.Occur.MUST)
			this.booleanScorer = new MustBooleanScorer(similarity, scorer);
		else if (termQuery[0].occur == BooleanClause.Occur.SHOULD)
			this.booleanScorer = new ShouldBooleanScorer(similarity, scorer);
		else
			this.booleanScorer = new NotBooleanScorer(similarity, scorer,
					reader.numDocs());
		

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.search.Scorer#doc()
	 */
	@Override
	public int docID() {
		return booleanScorer.docID();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.search.Scorer#explain(int)
	 *//*
	@Override
	public Explanation explain(int doc) throws IOException {
		Explanation result = new Explanation();
		result.setDescription("Total");
		Explanation detail = this.booleanScorer.explain(doc);
		result.addDetail(detail);
		result.setValue(detail.getValue());
		return result;
	}*/

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.search.Scorer#next()
	 */
	@Override
	public int nextDoc() throws IOException {
	    	int result = booleanScorer.nextDoc(); 
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.search.Scorer#score()
	 */
	@Override
	public float score() throws IOException {
	    float result = booleanScorer.score();
		return result;

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
