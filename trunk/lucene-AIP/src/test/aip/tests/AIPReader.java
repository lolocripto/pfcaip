package aip.tests;

import java.io.File;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Constants;

/**
 * Esta clase muestra el contenido de un indice
 * Los tamaños mostrados estan medidos en numero de terminos
 */
public class AIPReader {
    
    public static void main(String[] args) throws Exception {
	
//	read(AIPTestUtils.INDEX_DIR_FIXED_DOCS);
//	read(AIPTestUtils.INDEX_DIR_WIKI_SHORT);
    read("H:/programacion/java/Lucene/index/opos");
    }

    /**
     * Este metodo muestra parte del contenido de un indice
     */
    public static void read(String indexDir) throws Exception {

    	//obtenemos el objeto "IndexReader" que incorpora todos los datos del indice
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
	System.out.println("Suma tamaños de la coleccion[" + idx.docSizes() + "]");
	System.out.println("Tamaño medio (avg)[" + idx.avgDocSize() + "]");
	
	//Tamaño del campo "catchAll", lo que significa tamaño de cada documento
	/*
	 *  Leemos los tamaños de los documentos a través del campo "CatchAll", 
	 *  este campo agrupa todos los términos así que nos da el 
	 */
	int[] sizesDoc = idx.sizes(Constants.CATCHALL_FIELD);
	System.out.println("Tamaños de Documentos:");
	for (int i = 0; i < sizesDoc.length; i++) {
	    System.out.println("       Documento[" + i + "]: " + sizesDoc[i]);
	}

	//Tamaño del campo "content"
	int[] sizesField = idx.sizes("content");
	System.out.println("Tamaño Campo 'content': " );
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