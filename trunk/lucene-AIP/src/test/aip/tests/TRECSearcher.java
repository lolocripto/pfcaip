package aip.tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.ninit.models.bm25.BM25BooleanQuery;
import org.ninit.models.bm25.BM25Parameters;
import org.ninit.models.lmkcd.LMKCDBooleanQuery;
import org.ninit.models.lmql.LMQLBooleanQuery;

/**
 * Programa de busquedas para pruebas
 */
public class TRECSearcher {

	private boolean DEBUG = true;

	enum searchType {
		STANDARD, BM25, LMQL, LMKCD
	}

	public static void main(String[] args) throws Exception {

		String indexDir = AIPTestUtils.INDEX_DIR_TREC_VERYSHORT;

		TRECSearcher searcher = new TRECSearcher();

		searcher.search(indexDir);
	}

	public void search(String indexDir) throws Exception {
		BufferedReader in = new BufferedReader(new FileReader(AIPTestUtils.SEARCH_CONTENT_FILE_SHORT));
		TrecTopicsReader trecTopics = new TrecTopicsReader();
		QualityQuery[] qQueries = trecTopics.readQueries(in);
		QualityQuery qQuery;
		String field = "text";
		Directory dir = new SimpleFSDirectory(new File(indexDir), null);
		IndexSearcher is = new IndexSearcher(dir, true);

//		File fResult = new File(AIPTestUtils.SEARCH_RESULT_FILE);
//		String head = "(num consulta)  (id Lucene)       (DOCNO)      (Posición ranking) (Score)  (Una etiqueta)";
		String head = AIPTestUtils.fitString("(num consulta)", 17) + 
						  AIPTestUtils.fitString("(id Lucene)", 15) + 
						  AIPTestUtils.fitString("(DOCNO)", 15) + 
						  AIPTestUtils.fitString("(Posición ranking)", 19) + 
						  AIPTestUtils.fitString("(Score)", 15) + 
						  AIPTestUtils.fitString("(Una etiqueta)", 15);
		ArrayList<String> linesResult_standard = new ArrayList<String>();
		linesResult_standard.add(head);
		ArrayList<String> linesResult_bm25 = new ArrayList<String>();
		linesResult_bm25.add(head);
		ArrayList<String> linesResult_lmql = new ArrayList<String>();
		linesResult_lmql.add(head);
		ArrayList<String> linesResult_lmkcd = new ArrayList<String>();
		linesResult_lmkcd.add(head);

		String textSearch;
		String finalText;
		for (int i = 0; i < qQueries.length; i++) {
			qQuery = qQueries[i];
			textSearch = qQuery.getValue("title");
			finalText = convertToSearcheable(textSearch);
			if (!textSearch.isEmpty()) {
				debug("IDQuery[" + qQuery.getQueryID() + "] title[" + textSearch + "]");

				procesSearch(is, field, qQuery, finalText, searchType.STANDARD, linesResult_standard);
				procesSearch(is, field, qQuery, finalText, searchType.BM25, linesResult_bm25);
				procesSearch(is, field, qQuery, finalText, searchType.LMQL, linesResult_lmql);
				procesSearch(is, field, qQuery, finalText, searchType.LMKCD, linesResult_lmkcd);
			}
		}
		is.close();
	}

	public void procesSearch(IndexSearcher is, String field, QualityQuery qQuery, String queryText, searchType type, ArrayList<String> linesResult) throws Exception {

		File stopWordList = new File(AIPTestUtils.STOP_WORD_LIST);
		TopDocs hits = null;
		long start = System.currentTimeMillis();
		String fileName = null;
		String model = null;
		switch (type) {
		case STANDARD:
			debug("Busqueda STANDARD");
			fileName = AIPTestUtils.SEARCH_RESULT_FILE_STANDARD;

			QueryParser parser = new QueryParser(Version.LUCENE_30, field, new StandardAnalyzer(Version.LUCENE_30, stopWordList));
			Query query = parser.parse(queryText);
			hits = is.search(query, 1000);
			model = "lucene_standard";

			break;
		case BM25:
			fileName = AIPTestUtils.SEARCH_RESULT_FILE_BM25;
			debug("Busqueda BM25");
			BM25BooleanQuery queryBM25 = new BM25BooleanQuery(queryText, field, new StandardAnalyzer(Version.LUCENE_30, stopWordList));
			hits = is.search(queryBM25, 1000);
			model = "BM25_k1_" + BM25Parameters.getK1() + "_b_" + BM25Parameters.getB();
			break;

		case LMQL:
			fileName = AIPTestUtils.SEARCH_RESULT_FILE_LMQL;
			debug("Busqueda LMQL");
			LMQLBooleanQuery queryLMQL = new LMQLBooleanQuery(queryText, field, new StandardAnalyzer(Version.LUCENE_30, stopWordList));
			hits = is.search(queryLMQL, 1000);
			model = "LMQL_lambda_.5f";
			break;

		case LMKCD:
			fileName = AIPTestUtils.SEARCH_RESULT_FILE_LMKCD;
			debug("Busqueda LMKCD");
			LMKCDBooleanQuery queryLMKCD = new LMKCDBooleanQuery(queryText, field, new StandardAnalyzer(Version.LUCENE_30, stopWordList));
			hits = is.search(queryLMKCD, 1000);
			model = "LMKCD";
			break;

		default:
			break;
		}
		long end = System.currentTimeMillis();

		File fResult = new File(fileName);

		String formattedLine;
		System.out.println("Found " + hits.totalHits + " document(s) (in " + (end - start) + " milliseconds) that matched query '" + queryText + "':");
		for (int i = 0; i < hits.scoreDocs.length; i++) {
			ScoreDoc scoreDoc = hits.scoreDocs[i];
			Document doc = is.doc(scoreDoc.doc);
//			formattedLine = qQuery.getQueryID() + "                " + scoreDoc.doc + "         " + doc.get("docNo") + "        " 
//												+ i + "                " 
//												+ scoreDoc.score + "    " + model;
			formattedLine = "    " +
							AIPTestUtils.fitString(qQuery.getQueryID(), 17) + 
							AIPTestUtils.fitString(""+scoreDoc.doc, 9) +
							AIPTestUtils.fitString(doc.get("docNo"), 23) +
							AIPTestUtils.fitString(""+i, 12) +
							AIPTestUtils.fitString(""+scoreDoc.score, 15) +
							AIPTestUtils.fitString(model, 15);
							
//			System.out.println(formattedLine);
			linesResult.add(formattedLine);
		}

		FileUtils.writeLines(fResult, linesResult);
	}

	private String convertToSearcheable(String text) {
		String[] tokens = text.split("\\s");
		String result = "";
//		for (int i = 0; i < tokens.length; i++) {
//			result += "+" + tokens[i] + " ";
//		}
		if (tokens.length == 1)
			return "+" + text;
		else
			return text;
	}

	private void debug(String text) {
		if (this.DEBUG) {
			System.out.println("[Debug trace][" + text + "]");
		}
	}

}