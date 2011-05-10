package aip.tests;

import java.io.File;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.benchmark.byTask.feeds.DocData;
import org.apache.lucene.benchmark.byTask.feeds.NoMoreDataException;
import org.apache.lucene.benchmark.byTask.feeds.TrecContentSource;
import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

/**
 * Este programa nos servira para indexar los ficheros la coleccion TREC 4 y 5
 * 
 * @author AIP
 * 
 */
public class TRECIndexer {

	private boolean DEBUG = false;
	private static final int FACTOR = Integer.MAX_VALUE;

	private enum fileType {
		FR94, FT, FBIS, LATIMES
	};

	public static void main(String[] args) throws Exception {

		TRECIndexer indexer = new TRECIndexer();

		String indexDir = AIPTestUtils.INDEX_DIR_TREC_VERYSHORT;
		String dataDir = AIPTestUtils.FILES_TREC_VERYSHORT;
		indexer.debug("Directorio con los ficheros a indexar:" + dataDir);
		indexer.debug("Directorio con el contenido del indice:" + indexDir);

		long start = new Date().getTime();
		
		int num = indexer.index(dataDir,indexDir);

		long end = new Date().getTime();
		System.out.println("Total ficheros indexados["+ num + "] Tiempo consumido " + (end - start) + " milisegundos");
	}

	public int index(String dataDir, String indexDir) throws Exception{
		Directory dir = new SimpleFSDirectory(new File(indexDir));
		
		File stopWordList = new File(AIPTestUtils.STOP_WORD_LIST);
		
		IndexWriter writer = new IndexWriter(dir, new StandardAnalyzer(
				Version.LUCENE_30,stopWordList), true,
				IndexWriter.MaxFieldLength.UNLIMITED);
		writer.setMergeFactor(FACTOR);
//		writer.setInfoStream(System.out);
//		writer.setUseCompoundFile(false);

		indexTRECFiles(writer,dataDir,fileType.FR94,stopWordList);
		indexTRECFiles(writer,dataDir,fileType.FT,stopWordList);
		indexTRECFiles(writer,dataDir,fileType.FBIS,stopWordList);
		indexTRECFiles(writer,dataDir,fileType.LATIMES,stopWordList);
		
		int num = writer.numDocs();
		System.out.println("Total ficheros indexados["+writer.numDocs()+"]");

		writer.close();
		
		return num;
	}
	
	public void indexTRECFiles(IndexWriter writer, String dataDir, 
			fileType type,File stopWordList) throws Exception {
		Properties p = new Properties();
		p.put("content.source.excludeIteration", "true");
		p.put("content.source.forever", "false");
		switch (type) {
		case FR94:
			p.put("docs.dir", dataDir + "/trec4/fr94");
			p.put("trec.doc.parser", "org.apache.lucene.benchmark.byTask.feeds.TrecFR94Parser");
			break;
		case FT:
			p.put("docs.dir", dataDir + "/trec4/ft");
			p.put("trec.doc.parser", "org.apache.lucene.benchmark.byTask.feeds.TrecFTParser");
			break;
		case FBIS:
			p.put("docs.dir", dataDir + "/trec5/fbis");
			p.put("trec.doc.parser", "org.apache.lucene.benchmark.byTask.feeds.TrecFBISParser");
			break;
		case LATIMES:
			p.put("docs.dir", dataDir + "/trec5/latimes");
			p.put("trec.doc.parser", "org.apache.lucene.benchmark.byTask.feeds.TrecLATimesParser");
			break;
		default:
			System.out.println("El tipo '" + type + "' no existe.");
			break;
		}

		TrecContentSource trecSrc = new TrecContentSource();
		Config c = new Config(p);
		trecSrc.setConfig(c);
		DocData d = new DocData();
		String docNo;
		String body;
		while (true) {

			try {
				d = trecSrc.getNextDocData(d);
			} catch (NoMoreDataException e1) {
				System.out.println("NO hay mas datos");
				break;
			} catch (IOException e2) {
				e2.printStackTrace();
				break;
			}
			docNo = d.getName();
			body = d.getBody();
			debug("docNo[" + docNo + "] body[" + body + "]");
			
			Document doc = new Document();
			doc.add(new Field("docNo",docNo,Field.Store.YES,Field.Index.NOT_ANALYZED));
			doc.add(new Field("text",body,Field.Store.NO,Field.Index.ANALYZED));
			writer.addDocument(doc);
		}
	}

	private void debug(String text) {
		if (this.DEBUG) {
			System.out.println("[Debug trace][" + text + "]");
		}
	}
}
