package aip.tests;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
/**
 * This code was originally written for Erik's Lucene intro java.net article
 */
public class Indexer {
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			throw new Exception("Usage: java " + Indexer.class.getName() + " <index dir> <data dir>");
		}
		String indexDir = args[0]; 
		String dataDir = args[1]; 
		long start = System.currentTimeMillis();
		Indexer indexer = new Indexer(indexDir);
		
		int numIndexed = indexer.index(dataDir);
		indexer.close();
		long end = System.currentTimeMillis();
		System.out.println("Indexing " + numIndexed + " files took " + (end - start) + " milliseconds");
	}
	private IndexWriter writer;
	public Indexer(String indexDir) throws IOException {
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
	public int index(String dataDir) throws Exception {
		File[] files = new File(dataDir).listFiles();

		for (int i = 0; i < files.length; i++) {
			File f = files[i];
			if (!f.isDirectory() && !f.isHidden() && f.exists() && f.canRead() && acceptFile(f)) {
				System.out.println("Indexing file["+f.getPath()+"\\"+f.getName());
				indexFile(f);
			}else if (f.isDirectory()){
				index(f.getAbsolutePath());
			}
		}
		return writer.numDocs(); // 5
	}
	protected boolean acceptFile(File f) { // 6
		return f.getName().endsWith(".txt");
	}
	/**
	 * AIP: El objeto Document solo es una lista de "Field", no guarda ningun dato estadistico
	 * 		de nada, solo guarda los detalles, si es stored, indexed, etc ...
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