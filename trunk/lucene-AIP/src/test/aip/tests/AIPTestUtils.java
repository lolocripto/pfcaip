package aip.tests;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.AttributeSource;

public final class AIPTestUtils {

	private static final String DIR_BASE = "H:/programacion/java/Lucene/";

	// Directorios donde estan los indices
	public static final String INDEX_DIR_FIXED_DOCS = DIR_BASE + "index/fixed_docs";

	public static final String INDEX_DIR_WIKI_SHORT = DIR_BASE + "index/wiki_short";
	public static final String INDEX_DIR_WIKI_MEDIUM = DIR_BASE + "index/wiki_medium";
	public static final String INDEX_DIR_WIKI_LARGE = DIR_BASE + "index/wiki_large";
	public static final String INDEX_DIR_WIKIPEDIA = DIR_BASE + "index/wikipedia";
	public static final String INDEX_DIR_TREC = DIR_BASE + "index/trec";
	public static final String INDEX_DIR_TREC_SHORT = DIR_BASE + "index/trec_short";
	public static final String INDEX_DIR_TREC_VERYSHORT = DIR_BASE + "index/trec_veryshort";

	// Directorios con los documentos que se van a indexar
	public static final String FILES_WIKI_SHORT = DIR_BASE + "files/wiki_short";
	public static final String FILES_WIKI_MEDIUM = DIR_BASE + "files/wiki_medium";
	public static final String FILES_WIKI_LARGE = DIR_BASE + "files/wiki_large";
	public static final String FILES_WIKIPEDIA = DIR_BASE + "files/wikipedia";
	public static final String FILES_TREC = DIR_BASE + "files/trec/TREC-4-5";
	public static final String FILES_TREC_SHORT = DIR_BASE + "files/trec_short/TREC-4-5";
	public static final String FILES_TREC_VERYSHORT = DIR_BASE + "files/trec_veryshort/TREC-4-5";

	// Documento especifico para indexar
	public static final String FIXED_DOC = DIR_BASE + "files/wiki_short/Lucene_conceptos.txt";

	// Documento con la lista de palabras vacias
	public static final String STOP_WORD_LIST = DIR_BASE + "files/english.stop";
	
	// Documento con las busquedas
	public static final String SEARCH_CONTENT_FILE = DIR_BASE + "files/topic-robust2004";
	public static final String SEARCH_CONTENT_FILE_SHORT = DIR_BASE + "files/topic-robust2004_test.txt";
	
	// Documento con el resultado de las busquedas
	public static final String SEARCH_RESULT_FILE = DIR_BASE + "files/search_result.txt";
	public static final String SEARCH_RESULT_FILE_STANDARD = DIR_BASE + "files/search_result_standard.txt";
	public static final String SEARCH_RESULT_FILE_BM25 = DIR_BASE + "files/search_result_bm25.txt";
	public static final String SEARCH_RESULT_FILE_LMQL = DIR_BASE + "files/search_result_lmql.txt";
	public static final String SEARCH_RESULT_FILE_LMKCD = DIR_BASE + "files/search_result_lmkcd.txt";

	public static void displayTokens(Analyzer analyzer, String text) throws IOException {
		AttributeSource[] tokens = tokensFromAnalysis(analyzer, text);
		for (int i = 0; i < tokens.length; i++) {
			AttributeSource token = tokens[i];
			TermAttribute term = (TermAttribute) token.addAttribute(TermAttribute.class);
			System.out.print("[" + term.term() + "] ");
		}
	}
	
	public static AttributeSource[] tokensFromAnalysis(Analyzer analyzer, String text) throws IOException {
		TokenStream stream = analyzer.tokenStream("contents", new StringReader(text));
		ArrayList tokenList = new ArrayList();
		while (true) {
			if (!stream.incrementToken())
				break;
			System.out.println(stream.toString());
			tokenList.add(stream.captureState());
		}
		return (AttributeSource[]) tokenList.toArray(new AttributeSource[0]);
	}
	
	public static String fitString(String text, int num){
		if (text.length() >= num){
			return text;
		}else{
			String spaces = " ";
			for (int i = 0; i < num - text.length(); i++) {
				spaces += " ";
			}
			return text + spaces;
		}
	}

}
