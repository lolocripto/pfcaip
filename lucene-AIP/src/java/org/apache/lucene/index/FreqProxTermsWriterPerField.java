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
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;

// TODO: break into separate freq and prox writers as
// codecs; make separate container (tii/tis/skip/*) that can
// be configured as any number of files 1..N
final class FreqProxTermsWriterPerField extends TermsHashConsumerPerField implements Comparable<FreqProxTermsWriterPerField> {

  final FreqProxTermsWriterPerThread perThread;
  final TermsHashPerField termsHashPerField;
  final FieldInfo fieldInfo;
  final DocumentsWriter.DocState docState;
  final FieldInvertState fieldState;
  boolean omitTermFreqAndPositions;
  PayloadAttribute payloadAttribute;

  public FreqProxTermsWriterPerField(TermsHashPerField termsHashPerField, FreqProxTermsWriterPerThread perThread, FieldInfo fieldInfo) {
    this.termsHashPerField = termsHashPerField;
    this.perThread = perThread;
    this.fieldInfo = fieldInfo;
    docState = termsHashPerField.docState;
    fieldState = termsHashPerField.fieldState;
    omitTermFreqAndPositions = fieldInfo.omitTermFreqAndPositions;
  }

  @Override
  int getStreamCount() {
    if (fieldInfo.omitTermFreqAndPositions)
      return 1;
    else
      return 2;
  }

  @Override
  void finish() {}

  boolean hasPayloads;

  @Override
  void skippingLongTerm() throws IOException {}

  public int compareTo(FreqProxTermsWriterPerField other) {
    return fieldInfo.name.compareTo(other.fieldInfo.name);
  }

  void reset() {
    // Record, up front, whether our in-RAM format will be
    // with or without term freqs:
    omitTermFreqAndPositions = fieldInfo.omitTermFreqAndPositions;
    payloadAttribute = null;
  }

  @Override
  boolean start(Fieldable[] fields, int count) {
    for(int i=0;i<count;i++)
      if (fields[i].isIndexed())
        return true;
    return false;
  }     
  
  @Override
  void start(Fieldable f) {
    if (fieldState.attributeSource.hasAttribute(PayloadAttribute.class)) {
      payloadAttribute = fieldState.attributeSource.getAttribute(PayloadAttribute.class);
    } else {
      payloadAttribute = null;
    }
  }

  final void writeProx(FreqProxTermsWriter.PostingList p, int proxCode) {
    final Payload payload;
    if (payloadAttribute == null) {
      payload = null;
    } else {
      payload = payloadAttribute.getPayload();
    }
    
    if (payload != null && payload.length > 0) {
      termsHashPerField.writeVInt(1, (proxCode<<1)|1);
      termsHashPerField.writeVInt(1, payload.length);
      termsHashPerField.writeBytes(1, payload.data, payload.offset, payload.length);
      hasPayloads = true;      
    } else
      termsHashPerField.writeVInt(1, proxCode<<1);
    p.lastPosition = fieldState.position;
  }

  /**
   * AIP comment: aqui es donde escribe en el indice la frecuencia del termino en el documento
   * 		vamos lo que hace es: como es un termino nuevo apunta en el indice que dicho
   * 		termino aparece en el documento poniendo 1 y en el indice guardara el num de 
   * 		documento en el que aparece dicho termino
   * 		En mi caso como el CF cuenta el total, la primera vez lo iniciamos en 1 como el DF.
   */
  @Override
  final void newTerm(RawPostingList p0) {
    // First time we're seeing this term since the last
    // flush
    assert docState.testPoint("FreqProxTermsWriterPerField.newTerm start");
    FreqProxTermsWriter.PostingList p = (FreqProxTermsWriter.PostingList) p0;
    p.lastDocID = docState.docID;
    if (omitTermFreqAndPositions) {
      p.lastDocCode = docState.docID;
    } else {
      p.lastDocCode = docState.docID << 1;
      p.docFreq = 1;
      //AIP change code: a�adimos la nueva cf
      p.colFreq = 1;
      writeProx(p, fieldState.position);
    }
  }

  /**
   * AIP comment: actualiza la frecuencia de los tokens en los nuevos documentID
   * 		este metodo lo usa para actualizar terminos que ya estan insertados en el indice
   * 		y tiene que incrementar la frecuencia del termino
   * 		primero comprueba que los docID son distintos, para actualizarlo con el CF
   * 		simplemente tengo que incrementarlo aunque fuesen el mismo documento
   */
  @Override
  final void addTerm(RawPostingList p0) {

    assert docState.testPoint("FreqProxTermsWriterPerField.addTerm start");

    FreqProxTermsWriter.PostingList p = (FreqProxTermsWriter.PostingList) p0;

    assert omitTermFreqAndPositions || p.docFreq > 0;

    if (omitTermFreqAndPositions) {
      if (docState.docID != p.lastDocID) {
        assert docState.docID > p.lastDocID;
        termsHashPerField.writeVInt(0, p.lastDocCode);
        p.lastDocCode = docState.docID - p.lastDocID;
        p.lastDocID = docState.docID;
      }
    } else {
      if (docState.docID != p.lastDocID) {
        assert docState.docID > p.lastDocID;
        // Term not yet seen in the current doc but previously
        // seen in other doc(s) since the last flush

        // Now that we know doc freq for previous doc,
        // write it & lastDocCode
        if (1 == p.docFreq)
          termsHashPerField.writeVInt(0, p.lastDocCode|1);
        else {
          termsHashPerField.writeVInt(0, p.lastDocCode);
          termsHashPerField.writeVInt(0, p.docFreq);
        }
        p.docFreq = 1;
        p.lastDocCode = (docState.docID - p.lastDocID) << 1;
        p.lastDocID = docState.docID;
        p.colFreq++; //AIP change code
        writeProx(p, fieldState.position);
      } else {
        p.docFreq++;
        p.colFreq++; //AIP change code
        writeProx(p, fieldState.position-p.lastPosition);
      }
    }
  }

  public void abort() {}
}

