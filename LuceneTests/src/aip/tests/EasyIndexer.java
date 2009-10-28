package aip.tests;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.Reader;
import java.util.Date;

/**
 * This class creates an Index in the dir selected by the argument The documents
 * are created here in the test program
 */
public class EasyIndexer {

    public static void main(String[] args) throws Exception {
	if (args.length != 1) {
	    throw new Exception("Usage: java " + EasyIndexer.class.getName()
		    + " <index dir>");
	}
	String indexDir = args[0];
	long start = System.currentTimeMillis();
	EasyIndexer indexer = new EasyIndexer(indexDir);

	int numIndexed = indexer.index();
	indexer.close();
	long end = System.currentTimeMillis();
	System.out.println("Indexing " + numIndexed + " files took "
		+ (end - start) + " milliseconds");
    }

    private IndexWriter writer;

    public EasyIndexer(String indexDir) throws IOException {
	Directory dir = new SimpleFSDirectory(new File(indexDir));
	writer = new IndexWriter(dir, new StandardAnalyzer(
		Version.LUCENE_CURRENT), true,
		IndexWriter.MaxFieldLength.UNLIMITED);
	writer.setInfoStream(System.out);
	writer.setUseCompoundFile(false);
    }

    public void close() throws IOException {
	writer.close(); // 4
    }

    public int index() throws Exception {
	for (int i = 0; i < 5; i++) {
	    addDoc(i);
	}
	return 4;
    }

    void addDoc(int i) throws Exception {
	Document doc = new Document();
	doc.add(new Field("id", "document_" + i, Field.Store.YES,Field.Index.ANALYZED));
	doc.add(new Field("content", "aaa", Field.Store.YES,Field.Index.ANALYZED));
	doc.add(new Field("content", "aaa", Field.Store.YES,Field.Index.ANALYZED));
	doc.add(new Field("content", "bbb", Field.Store.YES,Field.Index.ANALYZED));
	
	writer.addDocument(doc);
    }

    /**
     * AIP: El objeto Document solo es una lista de "Field", no guarda ningun
     * dato estadistico de nada, solo guarda los detalles, si es stored,
     * indexed, etc ...
     * 
     * @param f
     * @return
     * @throws Exception
     */
    protected Document getDocument(File f) throws Exception {

	String content = FileUtils.readFileToString(f);

	Document doc = new Document();
	doc.add(new Field("contents", new FileReader(f))); // 7
	doc.add(new Field("filename", f.getCanonicalPath(), // 8
		Field.Store.YES, Field.Index.NOT_ANALYZED));
	return doc;
    }

    private void indexFile(File f) throws Exception {
	System.out.println("Indexing " + f.getCanonicalPath());
	Document doc = getDocument(f);
	if (doc != null) {
	    writer.addDocument(doc); // 9
	}
    }

}