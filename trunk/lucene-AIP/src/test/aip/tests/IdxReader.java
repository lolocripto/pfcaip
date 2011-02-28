package aip.tests;

import java.io.File;
import java.util.List;

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

	Directory dir = new SimpleFSDirectory(new File(indexDir), null);
	IndexReader idx = IndexReader.open(dir, true);

	int docFreq;
	int colFreq;
	String text;
	String field;

	// int[] sizes = idx.sizes("contents");
	// for (int i = 0; i < sizes.length; i++) {
	// System.out.println("size["+i+"]: " + sizes[i]);
	// }
	System.out.println("total docs size[" + idx.docSizes() + "]");
	System.out.println("avg docs size[" + idx.avgDocSize() + "]");
	int[] sizes2 = idx.sizes(Constants.CATCHALL_FIELD);
	for (int i = 0; i < sizes2.length; i++) {
	    System.out.println("size[" + i + "]: " + sizes2[i]);
	}

	/**
	 * AIP: el fichero del indice lo va leyendo directamente desde el
	 * fichero en el metodo "next" el metodo idx.terms() lo lee del
	 * SegmentReader
	 */
	for (TermEnum termEnum = idx.terms(); termEnum.next();) {
	    Term indexedTerm = termEnum.term();
	    field = indexedTerm.field();

	    if (field.equals(Constants.CATCHALL_FIELD)) {
		text = indexedTerm.text();
		docFreq = idx.docFreq(indexedTerm);
		// AIP comment: cualquiera de las dos lineas de abajo valen
		// colFreq = idx.colDocFreq(indexedTerm);
		colFreq = termEnum.colFreq();

		System.out.println(field + " " + text + " " + docFreq + " "
			+ colFreq);
		// System.out.println(text + " " + docFreq);
	    }
	}
	// lectura del CatchAll fields
	// System.out.println("Col Freq de aaa["+idx.colDocFreq("aaa")+"]");

	for (String name : idx.getFieldNames(IndexReader.FieldOption.ALL)) {
	    System.out.println("fieldName[" + name + "]");
	}
	idx.close();
    }
}