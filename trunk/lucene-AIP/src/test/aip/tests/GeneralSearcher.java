package aip.tests;

import java.io.File;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
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
import org.ninit.models.bm25f.BM25FParameters;
import org.ninit.models.lmkcd.LMKCDBooleanQuery;
import org.ninit.models.lmql.LMQLBooleanQuery;

/**
 * Programa de busquedas para pruebas
 */
public class GeneralSearcher {

	public static void main(String[] args) throws Exception {

		String indexDir = "H:/programacion/java/Lucene/index/opos";

		String query = "concurrencia";
		String field = "content";
		System.out.println("Busqueda con el modelo de Lucene ...");
		search(indexDir, field, query);
	}

	public static void search(String indexDir, String field, String q) throws Exception {
		Directory dir = new SimpleFSDirectory(new File(indexDir), null);
		IndexSearcher is = new IndexSearcher(dir, true);

		QueryParser parser = new QueryParser(Version.LUCENE_30, field, new StandardAnalyzer(Version.LUCENE_30));
		Query query = parser.parse(q);

		long start = System.currentTimeMillis();
		TopDocs hits = is.search(query, 10);
		long end = System.currentTimeMillis();
		System.out.println("Found " + hits.totalHits + " document(s) (in " + (end - start) + " milliseconds) that matched query '" + q + "':");
		for (int i = 0; i < hits.scoreDocs.length; i++) {
			ScoreDoc scoreDoc = hits.scoreDocs[i];
			Document doc = is.doc(scoreDoc.doc);
			System.out.println(doc.get("filename") + " score[" + scoreDoc.score + "]");
		}
		is.close();
	}

}