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

/**
 * This code was originally written for Erik's Lucene intro java.net article
 */
public class Searcher {
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			throw new Exception("Usage: java " + Searcher.class.getName() + " <index dir> <query>");
		}
		String indexDir = args[0]; // 1
		String query = "aaa AND ss";
		String field = "content";
		search(indexDir, field, query);
		System.out.println("Ahora con BM25 ...");
		searchBM25(indexDir, field, query);
	}
	public static void search(String indexDir, String field, String q) throws Exception {
		Directory dir = new SimpleFSDirectory(new File(indexDir), null);
		IndexSearcher is = new IndexSearcher(dir,true); // 3
		
		QueryParser parser = new QueryParser(Version.LUCENE_30, field, new StandardAnalyzer(Version.LUCENE_30)); // 4
		Query query = parser.parse(q); // 4
		
		long start = System.currentTimeMillis();
		TopDocs hits = is.search(query, 10); // 5
		long end = System.currentTimeMillis();
		System.out.println("Found " + hits.totalHits + // 6
				" document(s) (in " + (end - start) + " milliseconds) that matched query '" + q + "':");
		for (int i = 0; i < hits.scoreDocs.length; i++) {
			ScoreDoc scoreDoc = hits.scoreDocs[i];
			Document doc = is.doc(scoreDoc.doc); // 7
			System.out.println(doc.get("filename") + " score["+scoreDoc.score+"]"); // 8
		}
		is.close(); // 9
	}
	
	public static void searchBM25(String indexDir, String field, String q) throws Exception{
		Directory dir = new SimpleFSDirectory(new File(indexDir), null);
		IndexSearcher is = new IndexSearcher(dir,true);
		BM25BooleanQuery query = new BM25BooleanQuery(q, field, new StandardAnalyzer(Version.LUCENE_30));
		
		long start = System.currentTimeMillis();
		TopDocs hits = is.search(query, 10); // 5
		long end = System.currentTimeMillis();
		System.out.println("Found " + hits.totalHits + // 6
				" document(s) (in " + (end - start) + " milliseconds) that matched query '" + q + "':");
		for (int i = 0; i < hits.scoreDocs.length; i++) {
			ScoreDoc scoreDoc = hits.scoreDocs[i];
			Document doc = is.doc(scoreDoc.doc); // 7
			System.out.println(doc.get("filename") + " score["+scoreDoc.score+"]"); // 8
		}
		is.close(); // 9
	}

	public static void searchBM25F(String indexDir, String[] fields, String q) throws Exception{
		Directory dir = new SimpleFSDirectory(new File(indexDir), null);
		IndexSearcher is = new IndexSearcher(dir,true);
		BM25FParameters.setK1(1.2f);
		
		BM25BooleanQuery query = new BM25BooleanQuery(q, fields, new StandardAnalyzer(Version.LUCENE_30));
		
		long start = System.currentTimeMillis();
		TopDocs hits = is.search(query, 10); // 5
		long end = System.currentTimeMillis();
		System.out.println("Found " + hits.totalHits + // 6
				" document(s) (in " + (end - start) + " milliseconds) that matched query '" + q + "':");
		for (int i = 0; i < hits.scoreDocs.length; i++) {
			ScoreDoc scoreDoc = hits.scoreDocs[i];
			Document doc = is.doc(scoreDoc.doc); // 7
			System.out.println(doc.get("filename") + " score["+scoreDoc.score+"]"); // 8
		}
		is.close(); // 9
	}

}