# Performed Tests #

## Introduction ##
> Performed test are mainly JUNIT tests, original source code for these JUNIT tests has been taken from /src/test package of the standar test package used for Lucene community modified to include the checks focus to the changes apply to include Collection Frequecy parameter.

> Apart from the previous (detailed later in this document) I've made a basic check with a simple program which read an index, fech the terms included and makes a count of the both DF (document frequency) and CF (collection frequency), in this test the methods checked are: _idx.colDocFreq(indexedTerm)_ where "idx" is an IndexReader object and _termEnum.colFreq()_ where "termEnum" is a TermEnum object.

> From the previous test we can see the access to CF came from two different ways:
  * From the IndexReader object by giving the Term
  * From TermEnum object

## Tests ##

  * [Click here](http://docs.google.com/View?id=dgjkgr6b_126ggq5s6gz) to check the specifications of the JUNIT classes where the tests have been performed to check the calculation of CF (collection frequency) parameter.





[Return to Project Description Page](ProjectDescription.md)