package aip.tests;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import java.io.File;
import java.util.Date;


/**
 * This code was originally written for Erik's Lucene intro java.net article
 */
public class IdxReader {
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
		    throw new Exception("Usage: java " + IdxReader.class.getName()
			    + " <index dir>");
		}
		String indexDir = args[0];
		read(indexDir);
	}
	public static void read(String indexDir) throws Exception {

		IndexReader idx = IndexReader.open(indexDir);

		int docFreq;
		int colFreq;
		String text;
		/**
		 * AIP: el fichero del indice lo va leyendo directamente desde el fichero en el metodo "next"
		 * 		el metodo idx.terms() lo lee del SegmentReader
		 */
		for (TermEnum termEnum = idx.terms(); termEnum.next();) {
			Term indexedTerm = termEnum.term();
			text = indexedTerm.text();
			docFreq = idx.docFreq(indexedTerm);
			
			//AIP comment: cualquiera de las dos lineas de abajo valen
//			colFreq = idx.colDocFreq(indexedTerm);
//			colFreq = termEnum.colFreq();
//			System.out.println(text + " " + docFreq + " " + colFreq);
			System.out.println(text + " " + docFreq);
		}

		idx.close();
	}
}