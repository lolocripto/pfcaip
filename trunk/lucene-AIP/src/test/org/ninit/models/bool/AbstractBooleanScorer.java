package org.ninit.models.bool;

import java.io.IOException;

import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Similarity;

public abstract class AbstractBooleanScorer extends Scorer {

    public static enum searchModel {
	AND_MODEL, OR_MODEL
    };

    protected Scorer[] subScorer;
    protected boolean subScorerNext[];

    protected AbstractBooleanScorer(Similarity similarity, Scorer scorer[])
	    throws IOException {
	super(similarity);
	this.subScorer = scorer;
	if (scorer != null && scorer.length > 0)
	    this.subScorerNext = new boolean[this.subScorer.length];
    }
}
