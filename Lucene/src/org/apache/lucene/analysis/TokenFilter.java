package org.apache.lucene.analysis;

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

/** A TokenFilter is a TokenStream whose input is another token stream.
  <p>
  This is an abstract class.
  NOTE: subclasses must override 
  {@link #incrementToken()} if the new TokenStream API is used
  and {@link #next(Token)} or {@link #next()} if the old
  TokenStream API is used.
  <p>
  See {@link TokenStream}
  */
public abstract class TokenFilter extends TokenStream {
  /** The source of tokens for this filter. */
  protected final TokenStream input;

  /** Construct a token stream filtering the given input. */
  protected TokenFilter(TokenStream input) {
    super(input);
    this.input = input;
  }
  
  /** Performs end-of-stream operations, if any, and calls then <code>end()</code> on the
   * input TokenStream.<p/> 
   * <b>NOTE:</b> Be sure to call <code>super.end()</code> first when overriding this method.*/
  public void end() throws IOException {
    input.end();
  }
  
  /** Close the input TokenStream. */
  public void close() throws IOException {
    input.close();
  }

  /** Reset the filter as well as the input TokenStream. */
  public void reset() throws IOException {
    input.reset();
  }
}
