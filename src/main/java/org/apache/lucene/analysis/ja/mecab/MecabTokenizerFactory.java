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


import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer.Mode;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * Factory for {@link org.apache.lucene.analysis.ja.mecab.MecabTokenizer}.
 * <pre class="prettyprint">
 * &lt;fieldType name="text_ja" class="solr.TextField"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="org.apache.lucene.analysis.ja.mecab.MecabTokenizerFactory"
 *       mode="NORMAL"
 *       userDictionary="user.dic"
 *       discardPunctuation="true"
 *     /&gt;
 *     &lt;filter class="org.apache.lucene.analysis.ja.JapaneseBaseFormFilterFactory"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;
 * </pre>
 */
public class MecabTokenizerFactory extends TokenizerFactory implements ResourceLoaderAware {
  private static final String MODE = "mode";

  private static final String USER_DICT_PATH = "userDictionary";

  private static final String DICT_PATH = "dictionaryPath";

  private static final String DISCARD_PUNCTUATION = "discardPunctuation";


  private final Mode mode;
  private final boolean discardPunctuation;
  private final String userDictionaryPath;
  private final String dictionaryPath;

  /** 
   * Creates a new MecabTokenizerFactory 
   * 
   * @param args  引数
   * */
  public MecabTokenizerFactory(Map<String,String> args) {
    super(args);
    mode = Mode.valueOf(get(args, MODE, JapaneseTokenizer.DEFAULT_MODE.toString()).toUpperCase(Locale.ROOT));
    userDictionaryPath = args.remove(USER_DICT_PATH);
    discardPunctuation = getBoolean(args, DISCARD_PUNCTUATION, true);
    dictionaryPath  = args.remove(DICT_PATH);

    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
  }
  
  @Override
  public void inform(ResourceLoader loader) throws IOException {
  }

  @Override
  public MecabTokenizer create(AttributeFactory factory) {
    MecabTokenizer t = new MecabTokenizer(factory, dictionaryPath, userDictionaryPath, discardPunctuation, mode);

    return t;
  }

}
