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
 * Esta clase muestra el contenido de un indice
 * Los tama�os mostrados estan medidos en numero de terminos
 */
public class AIPReader {
    //Directorios donde estan los indices
    public static final String INDEX_DIR_FIXED_DOCS = "H:/programacion/java/Lucene/index/fixed_docs";
    public static final String INDEX_DIR_WIKI_SHORT = "H:/programacion/java/Lucene/index/wiki_short";
    public static final String INDEX_DIR_WIKI_MEDIUM = "H:/programacion/java/Lucene/index/wiki_medium";
    public static final String INDEX_DIR_WIKI_LARGE = "H:/programacion/java/Lucene/index/wiki_large";
    public static final String INDEX_DIR_WIKIPEDIA = "H:/programacion/java/Lucene/index/wikipedia";

    public static void main(String[] args) throws Exception {
	
	read(INDEX_DIR_FIXED_DOCS);
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
	System.out.println("Suma tama�os de la coleccion[" + idx.docSizes() + "]");
	System.out.println("Tama�o medio (avg)[" + idx.avgDocSize() + "]");
	
	//Tama�o del campo "catchAll", lo que significa tama�o de cada documento
	int[] sizesDoc = idx.sizes(Constants.CATCHALL_FIELD);
	System.out.println("Tama�os de Documentos:");
	for (int i = 0; i < sizesDoc.length; i++) {
	    System.out.println("       Documento[" + i + "]: " + sizesDoc[i]);
	}

	//Tama�o del campo "content"
	int[] sizesField = idx.sizes("content");
	System.out.println("Tama�o Campo 'content': " );
	for (int i = 0; i < sizesField.length; i++) {
	    System.out.println("       Documento["+i+"]: " + sizesField[i]);
	}

	System.out.println("Campos del indice:");
	for (String name : idx.getFieldNames(IndexReader.FieldOption.ALL)) {
	    System.out.println("       " + name);
	}

	System.out.println("Document Frequency Vs Collencion Frequency en el campo 'CatchAll':");
	for (TermEnum termEnum = idx.terms(); termEnum.next();) {
	    Term indexedTerm = termEnum.term();
	    field = indexedTerm.field();

	    if (field.equals(Constants.CATCHALL_FIELD)) {
		text = indexedTerm.text();
		docFreq = idx.docFreq(indexedTerm);
		
		// AIP comment: cualquiera de las dos lineas de abajo valen
		// colFreq = idx.colDocFreq(indexedTerm);
		colFreq = termEnum.colFreq();

		System.out.println("     Termino[" + text+ "] doc. frequency[" + docFreq + 
			                                   "] colection freq [" + colFreq + "]");
		// System.out.println(text + " " + docFreq);
	    }
	}
	idx.close();
    }
}