package aip.tests;

import java.io.File;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;

/**
 * Este programa nos servira para indexar ficheros que no sean 'txt', para ello
 * usaremos "tika", que es otro proyecto de Apache que nos proporciona "parsers"
 * de muchos tipos de ficheros, en concreto, el proposito es indexar ficheros de
 * la wikipedia que son principalmente ficheros "html"
 * 
 * @author antonio
 * 
 */
public class WikiIndexer extends Indexer {

    private boolean DEBUG = false;
    
    static Set<String> textualMetadataFields = new HashSet<String>();
    static {
	textualMetadataFields.add(Metadata.TITLE);
	textualMetadataFields.add(Metadata.AUTHOR);
	textualMetadataFields.add(Metadata.COMMENTS);
	textualMetadataFields.add(Metadata.KEYWORDS);
	textualMetadataFields.add(Metadata.DESCRIPTION);
	textualMetadataFields.add(Metadata.SUBJECT);
    }

    public static void main(String[] args) throws Exception {

//	TikaConfig config = TikaConfig.getDefaultConfig();
	
	String indexDir = AIPTestUtils.INDEX_DIR_WIKI_SHORT;
	String dataDir = AIPTestUtils.FILES_WIKI_SHORT;
	System.out.println("Directorio con los ficheros a indexar:"+dataDir);
	System.out.println("Directorio con el contenido del indice:"+indexDir);
	
	long start = new Date().getTime();
	WikiIndexer indexer = new WikiIndexer(indexDir); 
							 
	int numIndexed = indexer.index(dataDir);
	indexer.close();
	long end = new Date().getTime();
	System.out.println("Numero de ficheros indexados[" + numIndexed + 
		"] tiempo consumido " + (end - start) + " milisegundos");
    }

    public WikiIndexer(String indexDir) throws IOException {
	super(indexDir);
    }

    @Override
    protected boolean acceptFile(File f) {
	return true; //acteptamos cualquier tipo de ficheros (txt, html, pdf, etc ...) 
    }

    @Override
    protected Document getDocument(File f) throws Exception {
	Metadata metadata = new Metadata();

	metadata.set("filename", f.getCanonicalPath());
	InputStream is = new FileInputStream(f);
	AutoDetectParser parser = new AutoDetectParser();
	ContentHandler handler = new BodyContentHandler(-1);
	try {
	    parser.parse(is, handler, metadata);
	} finally {
	    is.close();
	}
	Document doc = new Document();
	doc.add(new Field("content", handler.toString(), Field.Store.NO,
		Field.Index.ANALYZED));
	if (DEBUG) {
	    System.out.println(" texto: " + handler.toString());
	}
	
	for (String name : metadata.names()) {
	    String value = metadata.get(name);
	    if (textualMetadataFields.contains(name)) {
		doc.add(new Field("content", value, 
			Field.Store.NO, Field.Index.ANALYZED));
	    }
	    doc.add(new Field(name, value, Field.Store.YES, Field.Index.NO));
	    if (DEBUG) {
		System.out.println(" " + name + ": " + value);
	    }
	}
	if (DEBUG) {
	    System.out.println();
	}
	return doc;
    }
}
