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

import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.Constants;
import org.apache.lucene.search.Similarity;

/** Taps into DocInverter, as an InvertedDocEndConsumer,
 *  which is called at the end of inverting each field.  We
 *  just look at the length for the field (docState.length)
 *  and record the norm. */

final class NormsWriterPerField extends InvertedDocEndConsumerPerField implements Comparable<NormsWriterPerField> {

  final NormsWriterPerThread perThread;
  final FieldInfo fieldInfo;
  final DocumentsWriter.DocState docState;

  //AIP comment: the below arrays will store the norms/sizes (number of terms), each position of the array correspond to the docId
  // Holds all docID/norm pairs we've seen
  int[] docIDs = new int[1];
  byte[] norms = new byte[1];
  int[] sizes = new int[1];//AIP change code (DL) this will store document sizes (number of terms) taking that size from CatchAll Field
  int upto;

  final FieldInvertState fieldState;

  public void reset() {
    // Shrink back if we are overallocated now:
    docIDs = ArrayUtil.shrink(docIDs, upto);
    norms = ArrayUtil.shrink(norms, upto);
    sizes = ArrayUtil.shrink(sizes, upto);//AIP change code (DL)
    upto = 0;
  }

  public NormsWriterPerField(final DocInverterPerField docInverterPerField, final NormsWriterPerThread perThread, final FieldInfo fieldInfo) {
    this.perThread = perThread;
    this.fieldInfo = fieldInfo;
    docState = perThread.docState;
    fieldState = docInverterPerField.fieldState;
  }

  @Override
  void abort() {
    upto = 0;
  }

  public int compareTo(NormsWriterPerField other) {
    return fieldInfo.name.compareTo(other.fieldInfo.name);
  }
  //AIP TODO ahora tendriamos que cambiar este metodo, el nuevo CatchAll Field computa el norm y afectara a los computos del resto no?
  @Override
  void finish() {
    assert docIDs.length == norms.length;
    if (fieldInfo.isIndexed && !fieldInfo.omitNorms) {
      if (docIDs.length <= upto) {
        assert docIDs.length == upto;
        docIDs = ArrayUtil.grow(docIDs, 1+upto);
        norms = ArrayUtil.grow(norms, 1+upto);
        sizes = ArrayUtil.grow(sizes, 1+upto);
      }
      final float norm = docState.similarity.computeNorm(fieldInfo.name, fieldState);
      norms[upto] = Similarity.encodeNorm(norm);//AIP Comment: this array stores the "norm" value per document (upto==docId)
      docIDs[upto] = docState.docID;
      sizes[upto] = fieldState.getLength(); //AIP change code (DL)	
      upto++;
    }
  }
}
