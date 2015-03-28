# Lucene IR Improvement #

The mail goal of the project is the improvement of Lucene project, which is an Retrival Information application, Open Source under Apache Licence.
The modifications are based on the _java_ version however there are other versions in different programming languages.
The based version for the modifications is 2.9.0 release on September 25th (2009) which by the way included several changes from the previous version 2.4.1 (9 March 2009).
The modifications include the modification of the inverted index format to include more parameters to be used in the computation of ranking formulas.

This project starts from a request of another project, which is an alternative of Lucene Scoring (which is a version of mixed boolean/TFIDF) programmed by Uned teacher Joaquin Perez Iglesias, the names are BM25 (for documents without fields) and BM25F (for documents with structure (fields)), you can see more detail about this in http://nlp.uned.es/~jperezi/Lucene-BM25.

Basically, the target of the modifications is to improve the ranking feature of Lucene to determine how important a document is for a given query that is built on a combination of the Vector Space Model (VSM) and the Boolean model of Information Retrieval. The main idea behind VSM approach is the more times a query term appears in a document relative to the number of times the term appears in all the documents in the collection, the more relevant that document is to the query. Lucene uses also the Boolean model to first narrow down the documents that need to be scored based on the use of boolean logic in the query specification.

# Information given by Lucene currently and which would be needed #

Information missing in Lucene and needed in BM25: according to the discussion in "https://issues.apache.org/jira/browse/LUCENE-2091" is:
  * docFreq at document level, something like "int docFreq(term, doc\_id)" and return the number of documents where term occurs, but if it is not possible a catch-all field will be enough.
  * The Collection Average Document Length and Collection Average Field Length (per each field).

According the same conversation:
> I don't think that we need "How many times does term T occur in all fields for doc D", frequency is necessary per field and not per document.

> The main problem to use BM25 formulas is that it is against the deep concept of Lucene: "The same string in two different fields is considered a different term"

For more information, this is an index of the work done so far:

  * [Modifications](modifications_ppal.md)
  * [Performed tests](Tests.md)
  * [TODO Tasks Lists](TODO_list.md)



Check main page of [Apache Lucene page](http://lucene.apache.org/java/2_9_0/).