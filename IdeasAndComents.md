# Ideas to be explore #

  * Try to create a global field, named "contentsXXX" that contains all the terms: this would be useful to have global counters of terms, no matter the field name

# Changes for parameters in a document level #

**¿Is it possible to calculate any statistic parameter in a document level?**
> Basically, according to what I've seen in the source code I think it is not possible, this is the list of the reasons:
  * If we want to associate the document number with a certain Term, we can face problems since that doc number might change after modification in the Index. According to the documentation (I've checked myself):
_> > Note that a documents number may change, so caution should be taken when storing these numbers outside of Lucene. In particular, numbers may change in the following situations:
      * The numbers stored in each segment are unique only within the segment, and must be converted before they can be used in a larger context. The standard technique is to allocate each segment a range of values, based on the range of numbers used in that segment. To convert a document number from a segment to an external value, the segment's base document number is added. To convert an external value back to a segment-specific value, the segment is identified by the range that the external value is in, and the segment's base value is subtracted. For example two five document segments might be combined, so that the first segment has a base value of zero, and the second of five. Document three from the second segment would have an external value of eight.
      * When documents are deleted, gaps are created in the numbering. These are eventually removed as the index evolves through merging. Deleted documents are dropped when segments are merged. A freshly-merged segment thus has no gaps in its numbering._
> > The previous information almost forbid us to use stats per document (using the docId) unless hard work changing Lucene is done whenever any change in the index is done (deleting, merging, etc ...) because basically it wouldn't be an "Inverted index" any more.


  * It seems the documents are processed by adding all the terms (don't forget a Term is compound by the Field name and the Term value) in a Collection and processing that Collection WITHOUT ANY reference of the doc number, basically in each loop that Coolection Terms of the documents are added to a global "stream" of Terms and for each document, and that big "stream" is processed without recording the document number.

  * Everything in Lucene is thought with the "Field" concept and not "Document":
    * The same Term (e.g. 'aaa') in different fields are DIFFERENT Terms no matter the Document where the Term is included: this forbid us to include information at Document level in TermInfo object since they are sticked to a Field and not to a Document
    * The same Term (e.g. 'aaa') in different documents but the same field are THE SAME Term

**So, what possibilities are to add Document Level Stats?**

> No matter the stats is a Document level or a Collection level I need to separate the meaning of "Field" as field\_name / term\_value, and store the terms\_value in a separated structure, so whenever any stat is updated, first is compared in this structure whether that term has already appear or not.

[Return to Project Description Page](ProjectDescription.md)