package aip.tests;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.benchmark.byTask.feeds.DocData;
import org.apache.lucene.benchmark.byTask.feeds.NoMoreDataException;
import org.apache.lucene.benchmark.byTask.feeds.TrecContentSource;
import org.apache.lucene.benchmark.byTask.feeds.TrecDocParser.ParsePathType;
import org.apache.lucene.benchmark.byTask.feeds.TrecFR94Parser;
import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.lucene.document.Document;

/**
 * Este programa nos servira para indexar ficheros que no sean 'txt', para ello
 * usaremos "tika", que es otro proyecto de Apache que nos proporciona "parsers"
 * de muchos tipos de ficheros, en concreto, el proposito es indexar ficheros de
 * la wikipedia que son principalmente ficheros "html"
 * 
 * @author antonio
 * 
 */
public class TRECIndexer {

    private boolean DEBUG = false;

    public static void main(String[] args) throws Exception {

	String indexDir = AIPTestUtils.INDEX_DIR_TREC_SHORT;
	String dataDir = AIPTestUtils.FILES_TREC_SHORT;
	System.out.println("Directorio con los ficheros a indexar:"+dataDir);
	System.out.println("Directorio con el contenido del indice:"+indexDir);

	TRECIndexer indexer = new TRECIndexer();
	
	indexer.indexTRECFiles(true);
	indexer.indexTRECFiles(false);
	
	long start = new Date().getTime();
	
//	indexer.indexDir(dataDir);
	long end = new Date().getTime();
	System.out.println("Numero de ficheros indexados[" +  
		"] tiempo consumido " + (end - start) + " milisegundos");
    }


    
    public void indexTRECFiles(boolean frFiles){
	Properties p = new Properties();
	p.put("content.source.excludeIteration", "true");
	p.put("content.source.forever","false");
	if (frFiles){
		p.put("docs.dir", AIPTestUtils.FILES_TREC_SHORT+ "/fr94");
		p.put("trec.doc.parser","org.apache.lucene.benchmark.byTask.feeds.TrecFR94Parser");
	}else{
		p.put("docs.dir", AIPTestUtils.FILES_TREC_SHORT+ "/ft");
		p.put("trec.doc.parser","org.apache.lucene.benchmark.byTask.feeds.TrecFTParser");
	}
	
	TrecContentSource trecSrc = new TrecContentSource();
	Config c = new Config(p);
	trecSrc.setConfig(c);
	DocData d = new DocData();
	String docNo;
	String body;
	while (true){
	    
	    try {
		d = trecSrc.getNextDocData(d);
	    } catch (NoMoreDataException e1){
		System.out.println("NO hay mas datos");
		break;
	    } catch (IOException e2){
		e2.printStackTrace();
		break;
	    }
	    docNo=d.getName();
	    body = d.getBody();
	    System.out.println("docNo["+docNo+"] body["+body+"]");
	}

    }
    
    public int indexDir(String dataDir) throws Exception {
	File[] files = new File(dataDir).listFiles();

	for (int i = 0; i < files.length; i++) {
	    File f = files[i];
	    if (!f.isDirectory() && !f.isHidden() && f.exists() && f.canRead()) {
		System.out.println("Indexing file[" + f.getPath() + "\\"
			+ f.getName());
		indexFile(f);
	    } else if (f.isDirectory()) {
		indexDir(f.getAbsolutePath());
	    }
	}
	// return writer.numDocs();
	return 0;
    }

    private void indexFile(File f) throws Exception {
//	System.out.println("Indexing " + f.getCanonicalPath());
	Document doc = getDocument(f);
	if (doc != null) {
	    System.out.println("Fichero indexado.");
//	    writer.addDocument(doc); 
	}
    }

    protected Document getDocument(File f) throws Exception {

	TrecFR94Parser parser = new TrecFR94Parser();
	TrecContentSource trecSrc = new TrecContentSource();
	StringBuilder docBuf = new StringBuilder(FileUtils.readFileToString(f));

	DocData docData = new DocData();
	parser.parse(docData, "name", trecSrc, docBuf, ParsePathType.FR94);

	System.out.println("body["+docData.getBody()+"]" + docData.getDate() + " " + docData.getID());
	if (DEBUG) {
	    System.out.println();
	}
//	return doc;
	return new Document();
    }
}
