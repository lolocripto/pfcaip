/*
 * File name: TikaIndexer.java
 * Created on 11/09/2009
 */
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
import org.apache.tika.config.TikaConfig;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;

public class TikaIndexer extends Indexer {
	private boolean DEBUG = false; // 1
	static Set<String> textualMetadataFields = new HashSet<String>(); // 2
	static { // 2
		textualMetadataFields.add(Metadata.TITLE); // 2
		textualMetadataFields.add(Metadata.AUTHOR); // 2
		textualMetadataFields.add(Metadata.COMMENTS); // 2
		textualMetadataFields.add(Metadata.KEYWORDS); // 2
		textualMetadataFields.add(Metadata.DESCRIPTION); // 2
		textualMetadataFields.add(Metadata.SUBJECT); // 2
	}
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			throw new Exception("Usage: java " + TikaIndexer.class.getName() + " <index dir> <data dir>");
		}
		TikaConfig config = TikaConfig.getDefaultConfig(); // 3
//		List<String> parsers = new ArrayList(config.getParsers().keySet()); // 3
//		Collections.sort(parsers); // 3
//		Iterator<String> it = parsers.iterator(); // 3
//		System.out.println("Mime type parsers:"); // 3
//		while (it.hasNext()) { // 3
//			System.out.println(" " + it.next()); // 3
//		} // 3
//		System.out.println(); // 3
		String indexDir = args[0];
		String dataDir = args[1];
		long start = new Date().getTime();
		TikaIndexer indexer = new TikaIndexer(indexDir); //set the output index dir
		// fetch all the files contained in the directory "dataDir" and index all the files
		int numIndexed = indexer.index(dataDir);
		indexer.close();
		long end = new Date().getTime();
		System.out.println("Indexing " + numIndexed + " files took " + (end - start) + " milliseconds");
	}
	public TikaIndexer(String indexDir) throws IOException {
		super(indexDir);
	}
	@Override
	protected boolean acceptFile(File f) {
		return true; // 4
	}
	
	@Override
	protected Document getDocument(File f) throws Exception {
		Metadata metadata = new Metadata();

		metadata.set(Metadata.RESOURCE_NAME_KEY, // 5
				f.getCanonicalPath());
		// If you know content type (eg because this document
		// was loaded from an HTTP server), then you should also
		// set Metadata.CONTENT_TYPE
		// If you know content encoding (eg because this
		// document was loaded from an HTTP server), then you
		// should also set Metadata.CONTENT_ENCODING
		InputStream is = new FileInputStream(f);
		AutoDetectParser parser = new AutoDetectParser();
		ContentHandler handler = new BodyContentHandler(-1);
		try {
			parser.parse(is, handler, metadata);
		} finally {
			is.close();
		}
		Document doc = new Document();
		doc.add(new Field("contents", handler.toString(), Field.Store.NO, Field.Index.ANALYZED));
		if (DEBUG) {
			System.out.println(" all text: " + handler.toString());
		}
		for (String name : metadata.names()) { // 6
			String value = metadata.get(name);
			if (textualMetadataFields.contains(name)) {
				doc.add(new Field("contents", value, // 7
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
