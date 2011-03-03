package aip.tests;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MockRAMDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

/**
 * This class creates an Index in the dir selected by the argument The documents
 * are created here in the test program
 */
public class EasyIndexer {

    public static void main(String[] args) throws Exception {
	if (args.length == 0) {
	    throw new Exception("Usage: java " + EasyIndexer.class.getName()
		    + " <index dir>");
	}
	String indexDir = args[0];
	EasyIndexer indexer = new EasyIndexer(indexDir);

	String fileToIndex = args[1];
	int numIndexed = indexer.index01();
//	indexer.index02();
//	indexer.completeTest01();
//	indexer.indexFile(fileToIndex);
	assert indexer.test("probando chiquillo");
	indexer.close();
    }

    private IndexWriter writer;

    public EasyIndexer(String indexDir) throws IOException {
	Directory dir = new SimpleFSDirectory(new File(indexDir));
    	 
	writer = new IndexWriter(dir, new StandardAnalyzer(
		Version.LUCENE_30), true,
		IndexWriter.MaxFieldLength.UNLIMITED);
	writer.setInfoStream(System.out);
	writer.setUseCompoundFile(false);
    }

    public void close() throws IOException {
	writer.close(); // 4
    }

    public int index01() throws Exception {
	for (int i = 0; i < 10; i++) {
	    addDoc(i);
	}
	
	return 0;
    }

    public boolean test(String a){
	return true;
    }
    void addDoc(int i) throws Exception {
	Document doc = new Document();
	doc.add(new Field("id", "document_" + i, Field.Store.YES,Field.Index.ANALYZED));
	doc.add(new Field("content", "aaa bb ss", Field.Store.YES,Field.Index.ANALYZED));
	doc.add(new Field("content", "aaa cc", Field.Store.YES,Field.Index.ANALYZED));
	
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
    protected Document getDocument(File f,TermVector tv) throws Exception {

	String content = FileUtils.readFileToString(f);
	System.out.println("content of the file["+content+"]");

	Document doc = new Document();
	Field f1 = new Field("contents", new FileReader(f), tv);
	System.out.println("f1["+f1+"]");
	Field f2 = new Field("filename", f.getCanonicalPath(), Field.Store.YES, Field.Index.NOT_ANALYZED);
//	Field f2 = new Field("contents", f.getCanonicalPath(), Field.Store.YES, Field.Index.NOT_ANALYZED);
	System.out.println("f2["+f2+"]");
	doc.add(f1); // 7 por defecto aqui los valores:	Field.Store.NO / Field.Index.ANALYZED
	doc.add(f2);
	
	return doc;
    }

    private void indexFile(String file) throws Exception {
	System.out.println("indexing "+file);
	File f = new File(file);
	System.out.println("Indexing " + f.getCanonicalPath());
	Document doc = getDocument(f, TermVector.YES);
	if (doc != null) {
	    writer.addDocument(doc); // 9
	}
    }
    
    public void index02() throws Exception{
	  Document doc = new Document();
	    String contents = "aa bb cc dd ee ff gg hh ii jj kk";
	    doc.add(new Field("content", contents, Field.Store.NO,
	        Field.Index.ANALYZED));
	    try {
	      writer.addDocument(doc);
	    } catch (Exception e) {
	    }

	    // Make sure we can add another normal document
	    doc = new Document();
	    doc.add(new Field("content", "aa bb cc dd", Field.Store.NO,
	        Field.Index.ANALYZED));
	    writer.addDocument(doc);

	    // Make sure we can add another normal document
	    doc = new Document();
	    doc.add(new Field("content", "aa bb cc dd", Field.Store.NO,
	        Field.Index.ANALYZED));
	    writer.addDocument(doc);

	    writer.close();
//	    IndexReader reader = IndexReader.open(dir);
//	    final Term t = new Term("content", "aa");
//	    assertEquals(reader.docFreq(t), 3);

    }

    public void completeTest01() throws Exception{
    	    RAMDirectory dir = new MockRAMDirectory();
    	    IndexWriter writer = new IndexWriter(dir, new Analyzer() {

    	      public TokenStream tokenStream(String fieldName, Reader reader) {
    	        return new TokenFilter(new StandardTokenizer(Version.LUCENE_30,reader)) {
    	          private int count = 0;

    	          public boolean incrementToken() throws IOException {
    	            if (count++ == 5) {
    	              throw new IOException();
    	            }
    	            return input.incrementToken();
    	          }
    	        };
    	      }

    	    }, true, IndexWriter.MaxFieldLength.LIMITED);

//    	    IndexWriter writer = new IndexWriter(dir,new StandardAnalyzer(
//		Version.LUCENE_CURRENT), true,IndexWriter.MaxFieldLength.LIMITED);
//    	    
    	    Document doc = new Document();
    	    String contents = "aa bb cc dd";
    	    doc.add(new Field("content", contents, Field.Store.NO,
    	        Field.Index.ANALYZED));
    	    try {
    	      writer.addDocument(doc);
    	    } catch (Exception e) {
    		e.printStackTrace();
    	    }

    	    // Make sure we can add another normal document
    	    doc = new Document();
    	    doc.add(new Field("content", "aa bb cc dd", Field.Store.NO,
    	        Field.Index.ANALYZED));
    	    writer.addDocument(doc);

    	    // Make sure we can add another normal document
    	    doc = new Document();
    	    doc.add(new Field("content", "aa bb cc dd", Field.Store.NO,
    	        Field.Index.ANALYZED));
    	    writer.addDocument(doc);

    	    writer.close();
    	    IndexReader reader = IndexReader.open(dir);
    	    final Term t = new Term("content", "aa");
    	    System.out.println("DocFreq:"+reader.docFreq(t));

    	    // Make sure the doc that hit the exception was marked
    	    // as deleted:
    	    TermDocs tdocs = reader.termDocs(t);
    	    int count = 0;
    	    while(tdocs.next()) {
    	      count++;
    	    }

    	    assert true:"blabla";
    	    
    	    System.out.println("docFreq gg:"+reader.docFreq(new Term("content", "gg")));
    	    reader.close();
    	    dir.close();
    }
    
}