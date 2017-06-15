/*
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
package org.apache.lucene.analysis.ja.mecab;


import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cjk.CJKWidthFilter;
import org.apache.lucene.analysis.ja.JapaneseBaseFormFilter;
import org.apache.lucene.analysis.ja.JapaneseKatakanaStemFilter;
import org.apache.lucene.analysis.ja.JapanesePartOfSpeechStopFilter;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer.Mode;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Analyzer for Mecab that uses morphological analysis.
 * 
 * @see org.apache.lucene.analysis.ja.JapaneseTokenizer
 * @see org.apache.lucene.analysis.ja.JapaneseAnalyzer
 * @see MecabTokenizer
 */
public class MecabAnalyzer extends StopwordAnalyzerBase {
  private final Mode mode;
  private final Set<String> stoptags;
  
  private final String userDict;
  private final String dictPath;
  private final boolean discardPunctuation;
  
  public MecabAnalyzer() {
    this(null, null, true, 
            JapaneseTokenizer.DEFAULT_MODE, 
            DefaultSetHolder.DEFAULT_STOP_SET, 
            DefaultSetHolder.DEFAULT_STOP_TAGS);
  }
  
  public MecabAnalyzer(
          String dictPath, 
          String userDict, 
          boolean discardPunctuation, 
          Mode mode, CharArraySet stopwords, 
          Set<String> stoptags) {
    super(stopwords);
    this.dictPath = dictPath;
    this.userDict = userDict;
    this.mode = mode;
    this.stoptags = stoptags;
    this.discardPunctuation = discardPunctuation;
  }
  
  public static CharArraySet getDefaultStopSet(){
    return DefaultSetHolder.DEFAULT_STOP_SET;
  }
  
  public static Set<String> getDefaultStopTags(){
    return DefaultSetHolder.DEFAULT_STOP_TAGS;
  }
  
  /**
   * Atomically loads DEFAULT_STOP_SET, DEFAULT_STOP_TAGS in a lazy fashion once the 
   * outer class accesses the static final set the first time.
   */
  private static class DefaultSetHolder {
    static final CharArraySet DEFAULT_STOP_SET;
    static final Set<String> DEFAULT_STOP_TAGS;

    static {
      try {
        DEFAULT_STOP_SET = loadStopwordSet(true, MecabAnalyzer.class, "stopwords.txt", "#");  // ignore case
        final CharArraySet tagset = loadStopwordSet(false, MecabAnalyzer.class, "stoptags.txt", "#");
        DEFAULT_STOP_TAGS = new HashSet<>();
        for (Object element : tagset) {
          char chars[] = (char[]) element;
          DEFAULT_STOP_TAGS.add(new String(chars));
        }
      } catch (IOException ex) {
        // default set should always be present as it is part of the distribution (JAR)
        throw new RuntimeException("Unable to load default stopword or stoptag set");
      }
    }
  }
  
  @Override
  protected TokenStreamComponents createComponents(String fieldName) {
    Tokenizer tokenizer = new MecabTokenizer(dictPath, userDict, discardPunctuation, mode);
    TokenStream stream = new JapaneseBaseFormFilter(tokenizer);
    stream = new JapanesePartOfSpeechStopFilter(stream, stoptags);
    stream = new CJKWidthFilter(stream);
    stream = new StopFilter(stream, stopwords);
    stream = new JapaneseKatakanaStemFilter(stream);
    stream = new LowerCaseFilter(stream);
    return new TokenStreamComponents(tokenizer, stream);
  }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new CJKWidthFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
}
