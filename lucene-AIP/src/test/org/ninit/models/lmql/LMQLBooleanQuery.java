package org.ninit.models.lmql;

import java.io.IOException;



import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.Version;
import org.ninit.models.bool.AbstractBooleanScorer;

/**
 * Esta clase divide la boolean query en terminos y los introduce en las colecciones dependiendo
 * 	de si son tipo AND o tipo OR
 * @author antonio
 *
 */
@SuppressWarnings("serial")
public class LMQLBooleanQuery extends Query {

    private List<BooleanTermQuery> mustBoolTermQueries = new ArrayList<BooleanTermQuery>();
    private List<BooleanTermQuery> shouldBoolTermQueries = new ArrayList<BooleanTermQuery>();
    private List<BooleanTermQuery> notBoolTermQueries = new ArrayList<BooleanTermQuery>();
    private String[] fields = null;
    private float[] boosts;
    private float[] bParams;

    @SuppressWarnings("unchecked")
	public LMQLBooleanQuery(String query, String field, Analyzer analyzer)
	    throws ParseException, IOException {
	QueryParser qp = new QueryParser(Version.LUCENE_30, field, analyzer);
	Query q = qp.parse(query);

	List<BooleanClause> clauses = ((BooleanQuery) q).clauses();
	// Para cada termino de la query
	for (int i = 0; i < clauses.size(); i++) {
	    Set<Term> terms = new HashSet<Term>();
	    clauses.get(i).getQuery().extractTerms(terms);
	    Iterator<Term> iter = terms.iterator();
	    while (iter.hasNext()) {
		BooleanTermQuery boolTerm = new BooleanTermQuery(new TermQuery(
			new Term(field, iter.next().text())), clauses.get(i)
			.getQuery().getBoost(), clauses.get(i).getOccur());
		this.addClause(boolTerm);
	    }
	}
    }

    public LMQLBooleanQuery(String query, String[] fields, Analyzer analyzer,
	    AbstractBooleanScorer.searchModel model) throws ParseException,
	    IOException {
	this(query, "ALL_FIELDS", analyzer);
	this.fields = fields;
	this.boosts = new float[this.fields.length];
	this.bParams = new float[this.fields.length];
	for (int i = 0; i < this.fields.length; i++) {
	    this.boosts[i] = 1;
	    this.bParams[i] = 0.75f;
	}

    }

    public LMQLBooleanQuery(String query, String[] fields, Analyzer analyzer)
	    throws ParseException, IOException {
	this(query, "ALL_FIELDS", analyzer);
	this.fields = fields;
	this.boosts = boosts;
	this.bParams = bParams;

    }

    @Override
    public Weight weight(Searcher searcher) throws IOException {

	if (this.fields == null)
	    return new LMQLBooleanWeight(
		    this.shouldBoolTermQueries.toArray(new BooleanTermQuery[this.shouldBoolTermQueries.size()]),
		    this.mustBoolTermQueries.toArray(new BooleanTermQuery[this.mustBoolTermQueries.size()]),
		    this.notBoolTermQueries.toArray(new BooleanTermQuery[this.notBoolTermQueries.size()])
		    );
	else
	    return new LMQLBooleanWeight(
		    this.shouldBoolTermQueries.toArray(new BooleanTermQuery[this.shouldBoolTermQueries.size()]),
		    this.mustBoolTermQueries.toArray(new BooleanTermQuery[this.mustBoolTermQueries.size()]),
		    this.notBoolTermQueries.toArray(new BooleanTermQuery[this.notBoolTermQueries.size()]), 
		    this.fields, this.boosts,this.bParams);
    }

    private void addClause(BooleanTermQuery boolTerm) {
	if (boolTerm.occur == BooleanClause.Occur.MUST)
	    this.mustBoolTermQueries.add(boolTerm);
	else if (boolTerm.occur == BooleanClause.Occur.SHOULD)
	    this.shouldBoolTermQueries.add(boolTerm);
	else
	    this.notBoolTermQueries.add(boolTerm);
    }

    public String toString() {
	StringBuilder buffer = new StringBuilder();
	for (BooleanTermQuery btq : this.mustBoolTermQueries) {
	    buffer.append(btq.toString());
	    buffer.append(" ");
	}
	for (BooleanTermQuery btq : this.shouldBoolTermQueries) {
	    buffer.append(btq.toString());
	    buffer.append(" ");
	}
	for (BooleanTermQuery btq : this.notBoolTermQueries) {
	    buffer.append(btq.toString());
	}
	return buffer.toString();
    }

    @Override
    public String toString(String field) {
	return this.toString();
    }

    public class BooleanTermQuery {

	TermQuery termQuery;
	BooleanClause.Occur occur;

	public BooleanTermQuery(TermQuery termQuery, BooleanClause.Occur occur) {
	    this.termQuery = termQuery;
	    this.occur = occur;
	}

	public BooleanTermQuery(TermQuery termQuery, float boost,
		BooleanClause.Occur occur) {
	    this(termQuery, occur);
	    this.termQuery.setBoost(boost);
	}

	public TermQuery getTermQuery() {
	    return termQuery;
	}

	public float getBoost() {
	    return this.termQuery.getBoost();
	}

	public void setTermQuery(TermQuery termQuery) {
	    this.termQuery = termQuery;
	}

	public BooleanClause.Occur getOccur() {
	    return occur;
	}

	public void setOccur(BooleanClause.Occur occur) {
	    this.occur = occur;
	}

	public String toString() {
	    String result = "";
	    result = "(" + this.occur + "(" + this.getTermQuery().getTerm()
		    + "^" + this.getBoost() + "))";
	    return result;
	}
    }

    public static void main(String args[]) throws ParseException,
	    CorruptIndexException, IOException {
	LMQLBooleanQuery q = new LMQLBooleanQuery(
		"(+product +faroe +islands +exported)", "CONTENT",
		new StandardAnalyzer(Version.LUCENE_30));
	System.out.println(q);
    }

}
