package org.apache.lucene.index;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.search.DefaultSimilarity;
import org.apache.lucene.store.BufferedIndexInput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BitVector;
import org.apache.lucene.util.CloseableThreadLocal;
import org.apache.lucene.util.Constants;
import org.apache.lucene.search.FieldCache; // not great (circular); used only to purge FieldCache entry on close

/** @version $Id */
/**
 * <p><b>NOTE:</b> This API is new and still experimental
 * (subject to change suddenly in the next release)</p>
 */
public class SegmentReader extends IndexReader implements Cloneable {
  protected boolean readOnly;

  private SegmentInfo si;
  private int readBufferSize;

  CloseableThreadLocal<FieldsReader> fieldsReaderLocal = new FieldsReaderLocal();
  CloseableThreadLocal<TermVectorsReader> termVectorsLocal = new CloseableThreadLocal<TermVectorsReader>();

  BitVector deletedDocs = null;
  Ref deletedDocsRef = null;
  private boolean deletedDocsDirty = false;
  private boolean normsDirty = false;
  private int pendingDeleteCount;

  private boolean rollbackHasChanges = false;
  private boolean rollbackDeletedDocsDirty = false;
  private boolean rollbackNormsDirty = false;
  private SegmentInfo rollbackSegmentInfo;
  private int rollbackPendingDeleteCount;

  // optionally used for the .nrm file shared by multiple norms
  private IndexInput singleNormStream;
  //AIP change code (DL): a new Stream to store the sizes
  private IndexInput singleSizesStream;
  
  //AIP change code (AVGL): this new stream will contain the avg size of the documents
  private IndexInput avgSizeStream;
  
  private Ref singleNormRef;

  CoreReaders core;

  // Holds core readers that are shared (unchanged) when
  // SegmentReader is cloned or reopened
  static final class CoreReaders {

    // Counts how many other reader share the core objects
    // (freqStream, proxStream, tis, etc.) of this reader;
    // when coreRef drops to 0, these core objects may be
    // closed.  A given instance of SegmentReader may be
    // closed, even those it shares core objects with other
    // SegmentReaders:
    private final Ref ref = new Ref();

    final String segment;
    final FieldInfos fieldInfos;
    final IndexInput freqStream;
    final IndexInput proxStream;
    final TermInfosReader tisNoIndex;

    final Directory dir;
    final Directory cfsDir;
    final int readBufferSize;
    final int termsIndexDivisor;

    private final SegmentReader origInstance;

    TermInfosReader tis;
    FieldsReader fieldsReaderOrig;
    TermVectorsReader termVectorsReaderOrig;
    CompoundFileReader cfsReader;
    CompoundFileReader storeCFSReader;

    CoreReaders(SegmentReader origInstance, Directory dir, SegmentInfo si, int readBufferSize, int termsIndexDivisor) throws IOException {
      segment = si.name;
      this.readBufferSize = readBufferSize;
      this.dir = dir;

      boolean success = false;

      try {
        Directory dir0 = dir;
        if (si.getUseCompoundFile()) {
          cfsReader = new CompoundFileReader(dir, segment + "." + IndexFileNames.COMPOUND_FILE_EXTENSION, readBufferSize);
          dir0 = cfsReader;
        }
        cfsDir = dir0;

        fieldInfos = new FieldInfos(cfsDir, segment + "." + IndexFileNames.FIELD_INFOS_EXTENSION);

        this.termsIndexDivisor = termsIndexDivisor;
        TermInfosReader reader = new TermInfosReader(cfsDir, segment, fieldInfos, readBufferSize, termsIndexDivisor);
        if (termsIndexDivisor == -1) {
          tisNoIndex = reader;
        } else {
          //AIP comment: "tis" will have the content of "tis" file
          tis = reader;
          tisNoIndex = null;
        }

        // make sure that all index files have been read or are kept open
        // so that if an index update removes them we'll still have them
        freqStream = cfsDir.openInput(segment + "." + IndexFileNames.FREQ_EXTENSION, readBufferSize);

        if (fieldInfos.hasProx()) {
          proxStream = cfsDir.openInput(segment + "." + IndexFileNames.PROX_EXTENSION, readBufferSize);
        } else {
          proxStream = null;
        }
        success = true;
      } finally {
        if (!success) {
          decRef();
        }
      }

      // Must assign this at the end -- if we hit an
      // exception above core, we don't want to attempt to
      // purge the FieldCache (will hit NPE because core is
      // not assigned yet).
      this.origInstance = origInstance;
    }

    synchronized TermVectorsReader getTermVectorsReaderOrig() {
      return termVectorsReaderOrig;
    }

    synchronized FieldsReader getFieldsReaderOrig() {
      return fieldsReaderOrig;
    }

    synchronized void incRef() {
      ref.incRef();
    }

    synchronized Directory getCFSReader() {
      return cfsReader;
    }

    synchronized TermInfosReader getTermsReader() {
      if (tis != null) {
        return tis;
      } else {
        return tisNoIndex;
      }
    }      

    synchronized boolean termsIndexIsLoaded() {
      return tis != null;
    }      

    // NOTE: only called from IndexWriter when a near
    // real-time reader is opened, or applyDeletes is run,
    // sharing a segment that's still being merged.  This
    // method is not fully thread safe, and relies on the
    // synchronization in IndexWriter
    synchronized void loadTermsIndex(SegmentInfo si, int termsIndexDivisor) throws IOException {
      if (tis == null) {
        Directory dir0;
        if (si.getUseCompoundFile()) {
          // In some cases, we were originally opened when CFS
          // was not used, but then we are asked to open the
          // terms reader with index, the segment has switched
          // to CFS
          if (cfsReader == null) {
            cfsReader = new CompoundFileReader(dir, segment + "." + IndexFileNames.COMPOUND_FILE_EXTENSION, readBufferSize);
          }
          dir0 = cfsReader;
        } else {
          dir0 = dir;
        }
        //AIP comment: 'tis' will have the term information
        tis = new TermInfosReader(dir0, segment, fieldInfos, readBufferSize, termsIndexDivisor);
      }
    }

    synchronized void decRef() throws IOException {

      if (ref.decRef() == 0) {

        // close everything, nothing is shared anymore with other readers
        if (tis != null) {
          tis.close();
          // null so if an app hangs on to us we still free most ram
          tis = null;
        }
        
        if (tisNoIndex != null) {
          tisNoIndex.close();
        }
        
        if (freqStream != null) {
          freqStream.close();
        }

        if (proxStream != null) {
          proxStream.close();
        }

        if (termVectorsReaderOrig != null) {
          termVectorsReaderOrig.close();
        }
  
        if (fieldsReaderOrig != null) {
          fieldsReaderOrig.close();
        }
  
        if (cfsReader != null) {
          cfsReader.close();
        }
  
        if (storeCFSReader != null) {
          storeCFSReader.close();
        }

        // Force FieldCache to evict our entries at this point
        if (origInstance != null) {
          FieldCache.DEFAULT.purge(origInstance);
        }
      }
    }

    synchronized void openDocStores(SegmentInfo si) throws IOException {

      assert si.name.equals(segment);

      if (fieldsReaderOrig == null) {
        final Directory storeDir;
        if (si.getDocStoreOffset() != -1) {
          if (si.getDocStoreIsCompoundFile()) {
            assert storeCFSReader == null;
            storeCFSReader = new CompoundFileReader(dir,
                                                    si.getDocStoreSegment() + "." + IndexFileNames.COMPOUND_FILE_STORE_EXTENSION,
                                                    readBufferSize);
            storeDir = storeCFSReader;
            assert storeDir != null;
          } else {
            storeDir = dir;
            assert storeDir != null;
          }
        } else if (si.getUseCompoundFile()) {
          // In some cases, we were originally opened when CFS
          // was not used, but then we are asked to open doc
          // stores after the segment has switched to CFS
          if (cfsReader == null) {
            cfsReader = new CompoundFileReader(dir, segment + "." + IndexFileNames.COMPOUND_FILE_EXTENSION, readBufferSize);
          }
          storeDir = cfsReader;
          assert storeDir != null;
        } else {
          storeDir = dir;
          assert storeDir != null;
        }

        final String storesSegment;
        if (si.getDocStoreOffset() != -1) {
          storesSegment = si.getDocStoreSegment();
        } else {
          storesSegment = segment;
        }

        fieldsReaderOrig = new FieldsReader(storeDir, storesSegment, fieldInfos, readBufferSize,
                                            si.getDocStoreOffset(), si.docCount);

        // Verify two sources of "maxDoc" agree:
        if (si.getDocStoreOffset() == -1 && fieldsReaderOrig.size() != si.docCount) {
          throw new CorruptIndexException("doc counts differ for segment " + segment + ": fieldsReader shows " + fieldsReaderOrig.size() + " but segmentInfo shows " + si.docCount);
        }

        if (fieldInfos.hasVectors()) { // open term vector files only as needed
          termVectorsReaderOrig = new TermVectorsReader(storeDir, storesSegment, fieldInfos, readBufferSize, si.getDocStoreOffset(), si.docCount);
        }
      }
    }
  }

  /**
   * Sets the initial value 
   */
  private class FieldsReaderLocal extends CloseableThreadLocal<FieldsReader> {
    @Override
    protected FieldsReader initialValue() {
      return (FieldsReader) core.getFieldsReaderOrig().clone();
    }
  }
  
  static class Ref {
    private int refCount = 1;
    
    @Override
    public String toString() {
      return "refcount: "+refCount;
    }
    
    public synchronized int refCount() {
      return refCount;
    }
    
    public synchronized int incRef() {
      assert refCount > 0;
      refCount++;
      return refCount;
    }

    public synchronized int decRef() {
      assert refCount > 0;
      refCount--;
      return refCount;
    }
  }
  
  /**
   * Byte[] referencing is used because a new norm object needs 
   * to be created for each clone, and the byte array is all 
   * that is needed for sharing between cloned readers.  The 
   * current norm referencing is for sharing between readers 
   * whereas the byte[] referencing is for copy on write which 
   * is independent of reader references (i.e. incRef, decRef).
   */

  final class Norm implements Cloneable {
    private int refCount = 1;

    // If this instance is a clone, the originalNorm
    // references the Norm that has a real open IndexInput:
    private Norm origNorm;

    private IndexInput in;
    private IndexInput inS;//AIP change code (DL)
    private long normSeek;
    private long sizeSeek;//AIP change code (DL)

    // null until bytes is set
    private Ref bytesRef;
    private byte[] bytes;
    private int[] fSizes;//AIP change code (DL)
    private boolean dirty;
    private int number;
    private boolean rollbackDirty;
    
//    public Norm(IndexInput in, int number, long normSeek) {
    //AIP change code (DL)
    public Norm(IndexInput in, IndexInput inS, int number, long normSeek, long sizeSeek) {
      this.in = in;  	   //AIP comment: contiene una referencia al fichero con los datos de los Norms
      this.inS= inS; // AIP change code (DL)
      this.number = number;//AIP comment: Field number
      this.normSeek = normSeek;//AIP comment: el valor del norm es, dentro del "in" a partir de normSeek + number
      this.sizeSeek = sizeSeek;//AIP change code (DL)
    }

    public synchronized void incRef() {
      assert refCount > 0 && (origNorm == null || origNorm.refCount > 0);
      refCount++;
    }

    private void closeInput() throws IOException {
      if (in != null) {
        if (in != singleNormStream) {
          // It's private to us -- just close it
          in.close();
        } else {
          // We are sharing this with others -- decRef and
          // maybe close the shared norm stream
          if (singleNormRef.decRef() == 0) {
            singleNormStream.close();
            singleNormStream = null;
            
            //AIP change code (DL)
            singleSizesStream.close();
            singleSizesStream = null;
            //end AIP change code
          }
        }

        in = null;
        inS= null;
      }
    }

    public synchronized void decRef() throws IOException {
      assert refCount > 0 && (origNorm == null || origNorm.refCount > 0);

      if (--refCount == 0) {
        if (origNorm != null) {
          origNorm.decRef();
          origNorm = null;
        } else {
          closeInput();
        }

        if (bytes != null) {
          assert bytesRef != null;
          bytesRef.decRef();
          bytes = null;
          bytesRef = null;
        } else {
          assert bytesRef == null;
        }
      }
    }

    // Load bytes but do not cache them if they were not
    // already cached
    public synchronized void bytes(byte[] bytesOut, int offset, int len) throws IOException {
      assert refCount > 0 && (origNorm == null || origNorm.refCount > 0);
      if (bytes != null) {
        // Already cached -- copy from cache:
        assert len <= maxDoc();
        System.arraycopy(bytes, 0, bytesOut, offset, len);
      } else {
        // Not cached
        if (origNorm != null) {
          // Ask origNorm to load
          origNorm.bytes(bytesOut, offset, len);
        } else {
          // We are orig -- read ourselves from disk:
          synchronized(in) {
            in.seek(normSeek);
            in.readBytes(bytesOut, offset, len, false);
          }
        }
      }
    }

    /*
     * AIP comment: te devuelve un array de bytes, cada uno de los cuales es el "norm" de un doc
     * 		"normSeek" es el comienzo y lee desde "normSeek" y luego un byte por documento
     * 		hasta "maxDoc", el grupo de bytes[] leido corresponde a un Field espeficico
     */
    // Load & cache full bytes array.  Returns bytes.
    public synchronized byte[] bytes() throws IOException {
      assert refCount > 0 && (origNorm == null || origNorm.refCount > 0);
      if (bytes == null) {                     // value not yet read
        assert bytesRef == null;
        if (origNorm != null) {
          // Ask origNorm to load so that for a series of
          // reopened readers we share a single read-only
          // byte[]
          bytes = origNorm.bytes();
          bytesRef = origNorm.bytesRef;
          bytesRef.incRef();

          // Once we've loaded the bytes we no longer need
          // origNorm:
          origNorm.decRef();
          origNorm = null;

        } else {
          // We are the origNorm, so load the bytes for real
          // ourself:
          final int count = maxDoc();
          bytes = new byte[count];

          // Since we are orig, in must not be null
          assert in != null;

          // Read from disk.
          synchronized(in) {
            in.seek(normSeek);
            in.readBytes(bytes, 0, count, false);
          }

          bytesRef = new Ref();
          closeInput();
        }
      }

      return bytes;
    }

    /*
     * AIP change code: return int[], each position correspond to the size of one document, taking
     * 			the number of the term as the size of the document.
     * 
     * 		"sizeSeek" is the start and we have to read from "sizeSeek" until maxDoc()*4
     * 		since we will read bytes and 1 int is 4 bytes
     * 		The group of int[] correspond to one certain Field
     */
    public synchronized int[] sizes() throws IOException {
	if (fSizes == null){		// value not yet read
          final int count = maxDoc();
          fSizes = new int[count];

          byte[] bAux = new byte[count*4];
          
          // Read from disk.
          synchronized(inS) {
            inS.seek(sizeSeek);
            inS.readBytes(bAux, 0, count*4, false);//AIP comment: 1 int = 4 bytes
          }

          fSizes = ArrayUtil.byteArrayToInt(bAux);  //AIP comment: casting from byte[] to int[]
          closeInput();
	}

      return fSizes;
    } 
    
    /*
     * AIP change code (DL)
     * Load bytes but do not cache them if they were not already cached
     */
    public synchronized void sizes(int[] sizesOut, int offset, int len) throws IOException {
      if (fSizes != null) {
        // Already cached -- copy from cache:
        System.arraycopy(fSizes, 0, sizesOut, offset, len);
      } else {
          // We are orig -- read ourselves from disk:
	  byte[] bAux = new byte[len*4];
	  
          synchronized(inS) {
            inS.seek(sizeSeek);
            inS.readBytes(bAux, offset, len*4, false); //AIP comment: 1 int = 4 bytes
          }
          
          sizesOut = ArrayUtil.byteArrayToInt(bAux); //AIP comment: casting from byte[] to int[]
          closeInput();
      }
    }
    
    /*
     * AIP change code (DL) this method is needed for the merging, it does the same than the one which 
     * 		returns the int[], the only different is that this one, apart from not reading it from
     * 		cache never, this one doesn't transform the result to int[] but it leave the result in byte[]
     * 		thus, in the merging, that have to use the method writeBytes we have not transform it again
     */
    public synchronized void sizes(byte[] sizesOutBytes, int offset, int len) throws IOException{
	byte[] bAux = new byte[len*4];
	
	synchronized(inS){
	    inS.seek(sizeSeek);
	    inS.readBytes(bAux, offset, len*4, false);
	}
	closeInput();
    }
    
    // Only for testing
    Ref bytesRef() {
      return bytesRef;
    }

    // Called if we intend to change a norm value.  We make a
    // private copy of bytes if it's shared with others:
    public synchronized byte[] copyOnWrite() throws IOException {
      assert refCount > 0 && (origNorm == null || origNorm.refCount > 0);
      bytes();
      assert bytes != null;
      assert bytesRef != null;
      if (bytesRef.refCount() > 1) {
        // I cannot be the origNorm for another norm
        // instance if I'm being changed.  Ie, only the
        // "head Norm" can be changed:
        assert refCount == 1;
        final Ref oldRef = bytesRef;
        bytes = cloneNormBytes(bytes);
        bytesRef = new Ref();
        oldRef.decRef();
      }
      dirty = true;
      return bytes;
    }
    
    // Returns a copy of this Norm instance that shares
    // IndexInput & bytes with the original one
    @Override
    public synchronized Object clone() {
      assert refCount > 0 && (origNorm == null || origNorm.refCount > 0);
        
      Norm clone;
      try {
        clone = (Norm) super.clone();
      } catch (CloneNotSupportedException cnse) {
        // Cannot happen
        throw new RuntimeException("unexpected CloneNotSupportedException", cnse);
      }
      clone.refCount = 1;

      if (bytes != null) {
        assert bytesRef != null;
        assert origNorm == null;

        // Clone holds a reference to my bytes:
        clone.bytesRef.incRef();
      } else {
        assert bytesRef == null;
        if (origNorm == null) {
          // I become the origNorm for the clone:
          clone.origNorm = this;
        }
        clone.origNorm.incRef();
      }

      // Only the origNorm will actually readBytes from in:
      clone.in = null;

      return clone;
    }

    // Flush all pending changes to the next generation
    // separate norms file.
    public void reWrite(SegmentInfo si) throws IOException {
      assert refCount > 0 && (origNorm == null || origNorm.refCount > 0): "refCount=" + refCount + " origNorm=" + origNorm;

      // NOTE: norms are re-written in regular directory, not cfs
      si.advanceNormGen(this.number);
      final String normFileName = si.getNormFileName(this.number);
      IndexOutput out = directory().createOutput(normFileName);
      boolean success = false;
      try {
        try {
          out.writeBytes(bytes, maxDoc());
        } finally {
          out.close();
        }
        success = true;
      } finally {
        if (!success) {
          try {
            directory().deleteFile(normFileName);
          } catch (Throwable t) {
            // suppress this so we keep throwing the
            // original exception
          }
        }
      }
      this.dirty = false;
    }
  }//AIP comment: end Norm class

  Map<String,Norm> norms = new HashMap<String,Norm>();
  float avgDocSize = 0;
  long docSizes = 0;
  
  /**
   * @throws CorruptIndexException if the index is corrupt
   * @throws IOException if there is a low-level IO error
   */
  public static SegmentReader get(boolean readOnly, SegmentInfo si, int termInfosIndexDivisor) throws CorruptIndexException, IOException {
    return get(readOnly, si.dir, si, BufferedIndexInput.BUFFER_SIZE, true, termInfosIndexDivisor);
  }

  /**
   * @throws CorruptIndexException if the index is corrupt
   * @throws IOException if there is a low-level IO error
   */
  public static SegmentReader get(boolean readOnly,
                                  Directory dir,
                                  SegmentInfo si,
                                  int readBufferSize,
                                  boolean doOpenStores,
                                  int termInfosIndexDivisor)
    throws CorruptIndexException, IOException {
    SegmentReader instance = readOnly ? new ReadOnlySegmentReader() : new SegmentReader();
    instance.readOnly = readOnly;
    instance.si = si;
    instance.readBufferSize = readBufferSize;

    boolean success = false;

    try {
      instance.core = new CoreReaders(instance, dir, si, readBufferSize, termInfosIndexDivisor);
      if (doOpenStores) {
        instance.core.openDocStores(si);
      }
      instance.loadDeletedDocs();
      instance.openNorms(instance.core.cfsDir, readBufferSize);
      success = true;
    } finally {

      // With lock-less commits, it's entirely possible (and
      // fine) to hit a FileNotFound exception above.  In
      // this case, we want to explicitly close any subset
      // of things that were opened so that we don't have to
      // wait for a GC to do so.
      if (!success) {
        instance.doClose();
      }
    }
    return instance;
  }

  void openDocStores() throws IOException {
    core.openDocStores(si);
  }

  private boolean checkDeletedCounts() throws IOException {
    final int recomputedCount = deletedDocs.getRecomputedCount();
     
    assert deletedDocs.count() == recomputedCount : "deleted count=" + deletedDocs.count() + " vs recomputed count=" + recomputedCount;

    assert si.getDelCount() == recomputedCount : 
    "delete count mismatch: info=" + si.getDelCount() + " vs BitVector=" + recomputedCount;

    // Verify # deletes does not exceed maxDoc for this
    // segment:
    assert si.getDelCount() <= maxDoc() : 
    "delete count mismatch: " + recomputedCount + ") exceeds max doc (" + maxDoc() + ") for segment " + si.name;

    return true;
  }

  private void loadDeletedDocs() throws IOException {
    // NOTE: the bitvector is stored using the regular directory, not cfs
    if (hasDeletions(si)) {
      deletedDocs = new BitVector(directory(), si.getDelFileName());
      deletedDocsRef = new Ref();
      assert checkDeletedCounts();
    } else
      assert si.getDelCount() == 0;
  }
  
  /**
   * Clones the norm bytes.  May be overridden by subclasses.  New and experimental.
   * @param bytes Byte array to clone
   * @return New BitVector
   */
  protected byte[] cloneNormBytes(byte[] bytes) {
    byte[] cloneBytes = new byte[bytes.length];
    System.arraycopy(bytes, 0, cloneBytes, 0, bytes.length);
    return cloneBytes;
  }
  
  /**
   * Clones the deleteDocs BitVector.  May be overridden by subclasses. New and experimental.
   * @param bv BitVector to clone
   * @return New BitVector
   */
  protected BitVector cloneDeletedDocs(BitVector bv) {
    return (BitVector)bv.clone();
  }

  @Override
  public final synchronized Object clone() {
    try {
      return clone(readOnly); // Preserve current readOnly
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public final synchronized IndexReader clone(boolean openReadOnly) throws CorruptIndexException, IOException {
    return reopenSegment(si, true, openReadOnly);
  }

  synchronized SegmentReader reopenSegment(SegmentInfo si, boolean doClone, boolean openReadOnly) throws CorruptIndexException, IOException {
    boolean deletionsUpToDate = (this.si.hasDeletions() == si.hasDeletions()) 
                                  && (!si.hasDeletions() || this.si.getDelFileName().equals(si.getDelFileName()));
    boolean normsUpToDate = true;
    
    boolean[] fieldNormsChanged = new boolean[core.fieldInfos.size()];
    final int fieldCount = core.fieldInfos.size();
    for (int i = 0; i < fieldCount; i++) {
      if (!this.si.getNormFileName(i).equals(si.getNormFileName(i))) {
        normsUpToDate = false;
        fieldNormsChanged[i] = true;
      }
    }

    // if we're cloning we need to run through the reopenSegment logic
    // also if both old and new readers aren't readonly, we clone to avoid sharing modifications
    if (normsUpToDate && deletionsUpToDate && !doClone && openReadOnly && readOnly) {
      return this;
    }    

    // When cloning, the incoming SegmentInfos should not
    // have any changes in it:
    assert !doClone || (normsUpToDate && deletionsUpToDate);

    // clone reader
    SegmentReader clone = openReadOnly ? new ReadOnlySegmentReader() : new SegmentReader();

    boolean success = false;
    try {
      core.incRef();
      clone.core = core;
      clone.readOnly = openReadOnly;
      clone.si = si;
      clone.readBufferSize = readBufferSize;

      if (!openReadOnly && hasChanges) {
        // My pending changes transfer to the new reader
        clone.pendingDeleteCount = pendingDeleteCount;
        clone.deletedDocsDirty = deletedDocsDirty;
        clone.normsDirty = normsDirty;
        clone.hasChanges = hasChanges;
        hasChanges = false;
      }
      
      if (doClone) {
        if (deletedDocs != null) {
          deletedDocsRef.incRef();
          clone.deletedDocs = deletedDocs;
          clone.deletedDocsRef = deletedDocsRef;
        }
      } else {
        if (!deletionsUpToDate) {
          // load deleted docs
          assert clone.deletedDocs == null;
          clone.loadDeletedDocs();
        } else if (deletedDocs != null) {
          deletedDocsRef.incRef();
          clone.deletedDocs = deletedDocs;
          clone.deletedDocsRef = deletedDocsRef;
        }
      }

      clone.norms = new HashMap<String,Norm>();

      // Clone norms
      for (int i = 0; i < fieldNormsChanged.length; i++) {

        // Clone unchanged norms to the cloned reader
        if (doClone || !fieldNormsChanged[i]) {
          final String curField = core.fieldInfos.fieldInfo(i).name;
          Norm norm = this.norms.get(curField);
          if (norm != null)
            clone.norms.put(curField, (Norm) norm.clone());
        }
      }

      // If we are not cloning, then this will open anew
      // any norms that have changed:
      clone.openNorms(si.getUseCompoundFile() ? core.getCFSReader() : directory(), readBufferSize);

      success = true;
    } finally {
      if (!success) {
        // An exception occurred during reopen, we have to decRef the norms
        // that we incRef'ed already and close singleNormsStream and FieldsReader
        clone.decRef();
      }
    }
    
    return clone;
  }

  @Override
  protected void doCommit(Map<String,String> commitUserData) throws IOException {
    if (hasChanges) {
      startCommit();
      boolean success = false;
      try {
        commitChanges(commitUserData);
        success = true;
      } finally {
        if (!success) {
          rollbackCommit();
        }
      }
    }
  }

  private void commitChanges(Map<String,String> commitUserData) throws IOException {
    if (deletedDocsDirty) {               // re-write deleted
      si.advanceDelGen();

      // We can write directly to the actual name (vs to a
      // .tmp & renaming it) because the file is not live
      // until segments file is written:
      final String delFileName = si.getDelFileName();
      boolean success = false;
      try {
        deletedDocs.write(directory(), delFileName);
        success = true;
      } finally {
        if (!success) {
          try {
            directory().deleteFile(delFileName);
          } catch (Throwable t) {
            // suppress this so we keep throwing the
            // original exception
          }
        }
      }

      si.setDelCount(si.getDelCount()+pendingDeleteCount);
      pendingDeleteCount = 0;
      assert deletedDocs.count() == si.getDelCount(): "delete count mismatch during commit: info=" + si.getDelCount() + " vs BitVector=" + deletedDocs.count();
    } else {
      assert pendingDeleteCount == 0;
    }

    if (normsDirty) {               // re-write norms
      si.setNumFields(core.fieldInfos.size());
      for (final Norm norm : norms.values()) {
        if (norm.dirty) {
          norm.reWrite(si);
        }
      }
    }
    deletedDocsDirty = false;
    normsDirty = false;
    hasChanges = false;
  }

  FieldsReader getFieldsReader() {
    return fieldsReaderLocal.get();
  }

  @Override
  protected void doClose() throws IOException {
    termVectorsLocal.close();
    fieldsReaderLocal.close();
    
    if (deletedDocs != null) {
      deletedDocsRef.decRef();
      // null so if an app hangs on to us we still free most ram
      deletedDocs = null;
    }

    for (final Norm norm : norms.values()) {
      norm.decRef();
    }
    if (core != null) {
      core.decRef();
    }
  }

  static boolean hasDeletions(SegmentInfo si) throws IOException {
    // Don't call ensureOpen() here (it could affect performance)
    return si.hasDeletions();
  }

  @Override
  public boolean hasDeletions() {
    // Don't call ensureOpen() here (it could affect performance)
    return deletedDocs != null;
  }

  static boolean usesCompoundFile(SegmentInfo si) throws IOException {
    return si.getUseCompoundFile();
  }

  static boolean hasSeparateNorms(SegmentInfo si) throws IOException {
    return si.hasSeparateNorms();
  }

  @Override
  protected void doDelete(int docNum) {
    if (deletedDocs == null) {
      deletedDocs = new BitVector(maxDoc());
      deletedDocsRef = new Ref();
    }
    // there is more than 1 SegmentReader with a reference to this
    // deletedDocs BitVector so decRef the current deletedDocsRef,
    // clone the BitVector, create a new deletedDocsRef
    if (deletedDocsRef.refCount() > 1) {
      Ref oldRef = deletedDocsRef;
      deletedDocs = cloneDeletedDocs(deletedDocs);
      deletedDocsRef = new Ref();
      oldRef.decRef();
    }
    deletedDocsDirty = true;
    if (!deletedDocs.getAndSet(docNum))
      pendingDeleteCount++;
  }

  @Override
  protected void doUndeleteAll() {
    deletedDocsDirty = false;
    if (deletedDocs != null) {
      assert deletedDocsRef != null;
      deletedDocsRef.decRef();
      deletedDocs = null;
      deletedDocsRef = null;
      pendingDeleteCount = 0;
      si.clearDelGen();
      si.setDelCount(0);
    } else {
      assert deletedDocsRef == null;
      assert pendingDeleteCount == 0;
    }
  }

  List<String> files() throws IOException {
    return new ArrayList<String>(si.files());
  }

  @Override
  public TermEnum terms() {
    ensureOpen();
    return core.getTermsReader().terms();
  }

  @Override
  public TermEnum terms(Term t) throws IOException {
    ensureOpen();
    return core.getTermsReader().terms(t);
  }

  FieldInfos fieldInfos() {
    return core.fieldInfos;
  }

  @Override
  public Document document(int n, FieldSelector fieldSelector) throws CorruptIndexException, IOException {
    ensureOpen();
    return getFieldsReader().doc(n, fieldSelector);
  }

  @Override
  public synchronized boolean isDeleted(int n) {
    return (deletedDocs != null && deletedDocs.get(n));
  }

  @Override
  public TermDocs termDocs(Term term) throws IOException {
    if (term == null) {
      return new AllTermDocs(this);
    } else {
      return super.termDocs(term);
    }
  }

  @Override
  public TermDocs termDocs() throws IOException {
    ensureOpen();
    return new SegmentTermDocs(this);
  }

  @Override
  public TermPositions termPositions() throws IOException {
    ensureOpen();
    return new SegmentTermPositions(this);
  }

  @Override
  public int docFreq(Term t) throws IOException {
    ensureOpen();
    TermInfo ti = core.getTermsReader().get(t);
    if (ti != null)
      return ti.docFreq;
    else
      return 0;
  }
  //AIP change code: getting the docFreq from CatchAll Field
  @Override
  public int docFreq(String t) throws IOException {
      Term term = new Term(Constants.CATCHALL_FIELD,t);
    ensureOpen();
    TermInfo ti = core.getTermsReader().get(term);
    if (ti != null)
      return ti.docFreq;
    else
      return 0;
  }

  //AIP change code: adding a similar method to get the CF
  @Override
  public int colDocFreq(Term t) throws IOException {
      ensureOpen();
      TermInfo ti = core.getTermsReader().get(t);
      if (ti != null)
	  return ti.colFreq;
      else
	  return 0;
  }
  //AIP change code: adding a similar method to get the CF but getting the value from CatchAll Field
  @Override
  public int colDocFreq(String t) throws IOException {
      Term term = new Term(Constants.CATCHALL_FIELD,t);
      ensureOpen();
      TermInfo ti = core.getTermsReader().get(term);
      if (ti != null)
	  return ti.colFreq;
      else
	  return 0;
  }

  @Override
  public int numDocs() {
    // Don't call ensureOpen() here (it could affect performance)
    int n = maxDoc();
    if (deletedDocs != null)
      n -= deletedDocs.count();
    return n;
  }

  @Override
  public int maxDoc() {
    // Don't call ensureOpen() here (it could affect performance)
    return si.docCount;
  }

  /**
   * @see IndexReader#getFieldNames(org.apache.lucene.index.IndexReader.FieldOption)
   */
  @Override
  public Collection<String> getFieldNames(IndexReader.FieldOption fieldOption) {
    ensureOpen();

    Set<String> fieldSet = new HashSet<String>();
    for (int i = 0; i < core.fieldInfos.size(); i++) {
      FieldInfo fi = core.fieldInfos.fieldInfo(i);
      if (fieldOption == IndexReader.FieldOption.ALL) {
        fieldSet.add(fi.name);
      }
      else if (!fi.isIndexed && fieldOption == IndexReader.FieldOption.UNINDEXED) {
        fieldSet.add(fi.name);
      }
      else if (fi.omitTermFreqAndPositions && fieldOption == IndexReader.FieldOption.OMIT_TERM_FREQ_AND_POSITIONS) {
        fieldSet.add(fi.name);
      }
      else if (fi.storePayloads && fieldOption == IndexReader.FieldOption.STORES_PAYLOADS) {
        fieldSet.add(fi.name);
      }
      else if (fi.isIndexed && fieldOption == IndexReader.FieldOption.INDEXED) {
        fieldSet.add(fi.name);
      }
      else if (fi.isIndexed && fi.storeTermVector == false && fieldOption == IndexReader.FieldOption.INDEXED_NO_TERMVECTOR) {
        fieldSet.add(fi.name);
      }
      else if (fi.storeTermVector == true &&
               fi.storePositionWithTermVector == false &&
               fi.storeOffsetWithTermVector == false &&
               fieldOption == IndexReader.FieldOption.TERMVECTOR) {
        fieldSet.add(fi.name);
      }
      else if (fi.isIndexed && fi.storeTermVector && fieldOption == IndexReader.FieldOption.INDEXED_WITH_TERMVECTOR) {
        fieldSet.add(fi.name);
      }
      else if (fi.storePositionWithTermVector && fi.storeOffsetWithTermVector == false && fieldOption == IndexReader.FieldOption.TERMVECTOR_WITH_POSITION) {
        fieldSet.add(fi.name);
      }
      else if (fi.storeOffsetWithTermVector && fi.storePositionWithTermVector == false && fieldOption == IndexReader.FieldOption.TERMVECTOR_WITH_OFFSET) {
        fieldSet.add(fi.name);
      }
      else if ((fi.storeOffsetWithTermVector && fi.storePositionWithTermVector) &&
                fieldOption == IndexReader.FieldOption.TERMVECTOR_WITH_POSITION_OFFSET) {
        fieldSet.add(fi.name);
      }
    }
    return fieldSet;
  }


  @Override
  public synchronized boolean hasNorms(String field) {
    ensureOpen();
    return norms.containsKey(field);
  }

  // can return null if norms aren't stored
  protected synchronized byte[] getNorms(String field) throws IOException {
    Norm norm = norms.get(field);
    if (norm == null) return null;  // not indexed, or norms not stored
    return norm.bytes();
  }

  // returns fake norms if norms aren't available
  @Override
  public synchronized byte[] norms(String field) throws IOException {
    ensureOpen();
    byte[] bytes = getNorms(field);
    return bytes;
  }
  
  //AIP change code (DL)
  @Override
  public synchronized int[] sizes(String field) throws IOException {
      Norm norm = norms.get(field);
      if (norm == null) return null;  // not indexed, or norms not stored
      return norm.sizes();
    }

  //AIP change code (AVGL)
  @Override
  public synchronized float avgDocSize() throws IOException{
      return avgDocSize;
  }
  
  //AIP change code (AVGL)
  @Override
  public synchronized long docSizes() throws IOException{
      return docSizes;
  }
  
  @Override
  protected void doSetNorm(int doc, String field, byte value)
          throws IOException {
    Norm norm = norms.get(field);
    if (norm == null)                             // not an indexed field
      return;

    normsDirty = true;
    norm.copyOnWrite()[doc] = value;                    // set the value
  }

  /** Read norms into a pre-allocated array. */
  @Override
  public synchronized void norms(String field, byte[] bytes, int offset)
    throws IOException {

    ensureOpen();
    Norm norm = norms.get(field);
    if (norm == null) {
      Arrays.fill(bytes, offset, bytes.length, DefaultSimilarity.encodeNorm(1.0f));
      return;
    }
  
    norm.bytes(bytes, offset, maxDoc());
  }

  /*
   * AIP change code (DL): this method is needed for merging segments
   * (non-Javadoc)
   * @see org.apache.lucene.index.IndexReader#sizes(java.lang.String, int[], int)
   */
  public synchronized void sizes(String field, int[] fSizes, int offset) throws IOException{
      ensureOpen();
      Norm norm = norms.get(field);
      if (norm == null){
	  Arrays.fill(fSizes, offset, fSizes.length, Constants.DEFAULT_SIZE);
	  return;
      }
      
      norm.sizes(fSizes, offset, maxDoc());
  }

  /*
   * AIP change code (DL), needed for Merging, it doesn't transform the byte[] to int[]
   * (non-Javadoc)
   * @see org.apache.lucene.index.IndexReader#sizes(java.lang.String, int[], int)
   */
  public synchronized void sizes(String field, byte[] fSizesBytes, int offset) throws IOException{
      ensureOpen();
      Norm norm = norms.get(field);
      if (norm == null){
	  Arrays.fill(fSizesBytes, offset, fSizesBytes.length, (byte)Constants.DEFAULT_SIZE);
	  return;
      }
      
      norm.sizes(fSizesBytes, offset, maxDoc());//AIP comment: the result byte[] length is 4*maxDoc()
  }
  

  /*
   * AIP comment: this method doesn't read directly from the file but this stores info about where in the
   * 		info should be present in the file, everything is stored in the map<field,norm>
   * 		Afterwards, when the user ask for the norm, if it has not read before, it is read
   * 		from the file and stored in the map so it is available for future requests 
   */
  private void openNorms(Directory cfsDir, int readBufferSize) throws IOException {
    long nextNormSeek = SegmentMerger.NORMS_HEADER.length; //skip header (header unused for now)
    long sizeSeek = 0;
    int maxDoc = maxDoc();
    String avgFileName = si.getAvgFileName();     //AIP change code (AVGL)
    String sizesFileName = si.getSizesFileName(); //AIP change code (DL)
    for (int i = 0; i < core.fieldInfos.size(); i++) {
      FieldInfo fi = core.fieldInfos.fieldInfo(i);
      if (norms.containsKey(fi.name)) {
        // in case this SegmentReader is being re-opened, we might be able to
        // reuse some norm instances and skip loading them here
        continue;
      }
      if (fi.isIndexed && !fi.omitNorms) {
        Directory d = directory();
        //AIP comment: for 'norms' sometimes the file name depends on the field number (only for separate norms)
        String fileName = si.getNormFileName(fi.number);
        
        if (!si.hasSeparateNorms(fi.number)) {
          d = cfsDir;
        }
        
        // singleNormFile means multiple norms share this file
        boolean singleNormFile = fileName.endsWith("." + IndexFileNames.NORMS_EXTENSION);
        IndexInput normInput = null;
        IndexInput sizesInput= null;//AIP change code (DL)
        
        long normSeek;

        //AIP comment: for size file always will be just one file
        if (singleNormFile) {
          normSeek = nextNormSeek;
          //AIP Comment: parece que va leyendo el fichero .nrm en bloques de "readBufferSize" y lo asigna a "singleNormStream"
          if (singleNormStream == null) {
            singleNormStream = d.openInput(fileName, readBufferSize);
            singleNormRef = new Ref();
          } else {
            singleNormRef.incRef();
          }
          // All norms in the .nrm file can share a single IndexInput since
          // they are only used in a synchronized context.
          // If this were to change in the future, a clone could be done here.
          normInput = singleNormStream;
        } else {
          normSeek = 0;
          normInput = d.openInput(fileName);
        }
        
        //AIP change code (DL)
        if (singleSizesStream == null){
            singleSizesStream = d.openInput(sizesFileName,readBufferSize);
        }
        sizesInput = singleSizesStream;
        //end AIP change code
        
        //AIP comment: parece que el "norm" del campo "fi.name" es uno de los bytes de "normInput", 
        //	       justo el que empieza en el byte "normSeek" mas "fi.number"
        //	       como aclaracion decir que guarda un puntero a "normInput" no clona el objeto
        norms.put(fi.name, new Norm(normInput, sizesInput, fi.number, normSeek, sizeSeek));
        nextNormSeek += maxDoc; // increment also if some norms are separate
        sizeSeek += maxDoc * 4; //AIP change code(DL) por cada doc hay 4bytes (1 int)
      }
    }//end for
    
    //AIP change code (AVGL) read the avgSize
    avgSizeStream = cfsDir.openInput(avgFileName);
    avgDocSize    = Float.parseFloat(avgSizeStream.readString());
    docSizes      = avgSizeStream.readLong();
  }

  boolean termsIndexLoaded() {
    return core.termsIndexIsLoaded();
  }

  // NOTE: only called from IndexWriter when a near
  // real-time reader is opened, or applyDeletes is run,
  // sharing a segment that's still being merged.  This
  // method is not thread safe, and relies on the
  // synchronization in IndexWriter
  void loadTermsIndex(int termsIndexDivisor) throws IOException {
    core.loadTermsIndex(si, termsIndexDivisor);
  }

  // for testing only
  boolean normsClosed() {
    if (singleNormStream != null) {
      return false;
    }
    for (final Norm norm : norms.values()) {
      if (norm.refCount > 0) {
        return false;
      }
    }
    return true;
  }

  // for testing only
  boolean normsClosed(String field) {
    return norms.get(field).refCount == 0;
  }

  /**
   * Create a clone from the initial TermVectorsReader and store it in the ThreadLocal.
   * @return TermVectorsReader
   */
  TermVectorsReader getTermVectorsReader() {
    TermVectorsReader tvReader = termVectorsLocal.get();
    if (tvReader == null) {
      TermVectorsReader orig = core.getTermVectorsReaderOrig();
      if (orig == null) {
        return null;
      } else {
        try {
          tvReader = (TermVectorsReader) orig.clone();
        } catch (CloneNotSupportedException cnse) {
          return null;
        }
      }
      termVectorsLocal.set(tvReader);
    }
    return tvReader;
  }

  TermVectorsReader getTermVectorsReaderOrig() {
    return core.getTermVectorsReaderOrig();
  }
  
  /** Return a term frequency vector for the specified document and field. The
   *  vector returned contains term numbers and frequencies for all terms in
   *  the specified field of this document, if the field had storeTermVector
   *  flag set.  If the flag was not set, the method returns null.
   * @throws IOException
   */
  @Override
  public TermFreqVector getTermFreqVector(int docNumber, String field) throws IOException {
    // Check if this field is invalid or has no stored term vector
    ensureOpen();
    FieldInfo fi = core.fieldInfos.fieldInfo(field);
    if (fi == null || !fi.storeTermVector) 
      return null;
    
    TermVectorsReader termVectorsReader = getTermVectorsReader();
    if (termVectorsReader == null)
      return null;
    
    return termVectorsReader.get(docNumber, field);
  }


  @Override
  public void getTermFreqVector(int docNumber, String field, TermVectorMapper mapper) throws IOException {
    ensureOpen();
    FieldInfo fi = core.fieldInfos.fieldInfo(field);
    if (fi == null || !fi.storeTermVector)
      return;

    TermVectorsReader termVectorsReader = getTermVectorsReader();
    if (termVectorsReader == null) {
      return;
    }


    termVectorsReader.get(docNumber, field, mapper);
  }


  @Override
  public void getTermFreqVector(int docNumber, TermVectorMapper mapper) throws IOException {
    ensureOpen();

    TermVectorsReader termVectorsReader = getTermVectorsReader();
    if (termVectorsReader == null)
      return;

    termVectorsReader.get(docNumber, mapper);
  }

  /** Return an array of term frequency vectors for the specified document.
   *  The array contains a vector for each vectorized field in the document.
   *  Each vector vector contains term numbers and frequencies for all terms
   *  in a given vectorized field.
   *  If no such fields existed, the method returns null.
   * @throws IOException
   */
  @Override
  public TermFreqVector[] getTermFreqVectors(int docNumber) throws IOException {
    ensureOpen();
    
    TermVectorsReader termVectorsReader = getTermVectorsReader();
    if (termVectorsReader == null)
      return null;
    
    return termVectorsReader.get(docNumber);
  }
  
  /**
   * Return the name of the segment this reader is reading.
   */
  public String getSegmentName() {
    return core.segment;
  }
  
  /**
   * Return the SegmentInfo of the segment this reader is reading.
   */
  SegmentInfo getSegmentInfo() {
    return si;
  }

  void setSegmentInfo(SegmentInfo info) {
    si = info;
  }

  void startCommit() {
    rollbackSegmentInfo = (SegmentInfo) si.clone();
    rollbackHasChanges = hasChanges;
    rollbackDeletedDocsDirty = deletedDocsDirty;
    rollbackNormsDirty = normsDirty;
    rollbackPendingDeleteCount = pendingDeleteCount;
    for (Norm norm : norms.values()) {
      norm.rollbackDirty = norm.dirty;
    }
  }

  void rollbackCommit() {
    si.reset(rollbackSegmentInfo);
    hasChanges = rollbackHasChanges;
    deletedDocsDirty = rollbackDeletedDocsDirty;
    normsDirty = rollbackNormsDirty;
    pendingDeleteCount = rollbackPendingDeleteCount;
    for (Norm norm : norms.values()) {
      norm.dirty = norm.rollbackDirty;
    }
  }

  /** Returns the directory this index resides in. */
  @Override
  public Directory directory() {
    // Don't ensureOpen here -- in certain cases, when a
    // cloned/reopened reader needs to commit, it may call
    // this method on the closed original reader
    return core.dir;
  }

  // This is necessary so that cloned SegmentReaders (which
  // share the underlying postings data) will map to the
  // same entry in the FieldCache.  See LUCENE-1579.
  @Override
  public final Object getFieldCacheKey() {
    return core.freqStream;
  }

  @Override
  public Object getDeletesCacheKey() {
    return deletedDocs;
  }

  @Override
  public long getUniqueTermCount() {
    return core.getTermsReader().size();
  }

  /**
   * Lotsa tests did hacks like:<br/>
   * SegmentReader reader = (SegmentReader) IndexReader.open(dir);<br/>
   * They broke. This method serves as a hack to keep hacks working
   * We do it with R/W access for the tests (BW compatibility)
   * @deprecated Remove this when tests are fixed!
   */
  static SegmentReader getOnlySegmentReader(Directory dir) throws IOException {
    return getOnlySegmentReader(IndexReader.open(dir,false));
  }

  static SegmentReader getOnlySegmentReader(IndexReader reader) {
    if (reader instanceof SegmentReader)
      return (SegmentReader) reader;

    if (reader instanceof DirectoryReader) {
      IndexReader[] subReaders = reader.getSequentialSubReaders();
      if (subReaders.length != 1)
        throw new IllegalArgumentException(reader + " has " + subReaders.length + " segments instead of exactly one");

      return (SegmentReader) subReaders[0];
    }

    throw new IllegalArgumentException(reader + " is not a SegmentReader or a single-segment DirectoryReader");
  }

  @Override
  public int getTermInfosIndexDivisor() {
    return core.termsIndexDivisor;
  }
}
