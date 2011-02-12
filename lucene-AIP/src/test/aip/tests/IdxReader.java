package aip.tests;

import java.io.File;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Constants;


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

	    Directory dir = new SimpleFSDirectory(new File(indexDir),null);
	    IndexReader idx = IndexReader.open(dir,true);

	    int docFreq;
	    int colFreq;
	    String text;
	    String field;
	    
	    System.out.println("Fields[" + idx.getFieldNames(IndexReader.FieldOption.ALL) + "]");
		/**
		 * AIP: el fichero del indice lo va leyendo directamente desde el fichero en el metodo "next"
		 * 		el metodo idx.terms() lo lee del SegmentReader
		 */
		for (TermEnum termEnum = idx.terms(); termEnum.next();) {
			Term indexedTerm = termEnum.term();
			field = indexedTerm.field();
			
			text = indexedTerm.text();
			docFreq = idx.docFreq(indexedTerm);
			//AIP comment: cualquiera de las dos lineas de abajo valen
//			colFreq = idx.colDocFreq(indexedTerm);
			colFreq = termEnum.colFreq();
			
			System.out.println(field + " " + text + " " + docFreq + " " + colFreq);
//			System.out.println(text + " " + docFreq);
		}
		//lectura del CatchAll fields
		System.out.println("Col Freq de aaa["+idx.colDocFreq("aaa")+"]");
		
		
		idx.close();
	}
}