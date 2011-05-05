package aip.tests;

public final class AIPTestUtils {
    
    private static final String DIR_BASE = "H:/programacion/java/Lucene/";
    
    //Directorios donde estan los indices
    public static final String INDEX_DIR_FIXED_DOCS = DIR_BASE + "index/fixed_docs";
    
    public static final String INDEX_DIR_WIKI_SHORT = DIR_BASE + "index/wiki_short";
    public static final String INDEX_DIR_WIKI_MEDIUM = DIR_BASE + "index/wiki_medium";
    public static final String INDEX_DIR_WIKI_LARGE = DIR_BASE + "index/wiki_large";
    public static final String INDEX_DIR_WIKIPEDIA = DIR_BASE + "index/wikipedia";
    public static final String INDEX_DIR_TREC_SHORT = DIR_BASE + "index/trec_short";
    public static final String INDEX_DIR_TREC = DIR_BASE + "index/trec";

    // Directorios con los documentos que se van a indexar
    public static final String FILES_WIKI_SHORT = DIR_BASE + "files/wiki_short";
    public static final String FILES_WIKI_MEDIUM = DIR_BASE + "files/wiki_medium";
    public static final String FILES_WIKI_LARGE = DIR_BASE + "files/wiki_large";
    public static final String FILES_WIKIPEDIA = DIR_BASE + "files/wikipedia";
    public static final String FILES_TREC_SHORT = DIR_BASE + "files/trec_short";
    public static final String FILES_TREC = DIR_BASE + "files/trec";

    // Documento especifico para indexar
    public static final String FIXED_DOC = DIR_BASE + "files/wiki_short/Lucene_conceptos.txt";
}
