/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer.Mode;
import org.apache.lucene.analysis.ja.mecab.MecabTokenizer;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;

public class MecabTokenizerFactory extends AbstractTokenizerFactory {

  private static final String USER_DICT_OPTION = "user_dictionary";
  private static final String DICT_OPTION = "dictionary_path";
  private static final String MODE_OPTION = "mode";
  private static final String DISCARD_PUNCTUATION_OPTION = "discard_punctuation";

  private final Mode mode;
  private final String dictionaryPath;
  private final String userDictionaryFile;
  private boolean discartPunctuation;


  public MecabTokenizerFactory(IndexSettings indexSettings, Environment env, String name, Settings settings) {
        super(indexSettings, name, settings);
        this.mode = getMode( settings );
        this.dictionaryPath = getDictionaryPath( settings );
        this.userDictionaryFile = getUserDictionaryFile( settings ) ;
        this.discartPunctuation = getDiscartPunctuation( settings );

    }

  public static Boolean getDiscartPunctuation( Settings settings ) {
    return settings.getAsBoolean(DISCARD_PUNCTUATION_OPTION, true);
  }
  
  public static String getDictionaryPath( Settings settings ) {
    return settings.get(DICT_OPTION, null);
  }

  public static String getUserDictionaryFile( Settings settings ) {
    return settings.get(USER_DICT_OPTION, null);
  }

  public static JapaneseTokenizer.Mode getMode(Settings settings) {
    JapaneseTokenizer.Mode mode = JapaneseTokenizer.DEFAULT_MODE;
    String modeSetting = settings.get(MODE_OPTION, null);
    if (modeSetting != null) {
      if ("search".equalsIgnoreCase(modeSetting)) {
        mode = JapaneseTokenizer.Mode.SEARCH;
      } else if ("normal".equalsIgnoreCase(modeSetting)) {
        mode = JapaneseTokenizer.Mode.NORMAL;
      } else if ("extended".equalsIgnoreCase(modeSetting)) {
        mode = JapaneseTokenizer.Mode.EXTENDED;
      }
    }
    return mode;
  }

  @Override
  public Tokenizer create() {
    MecabTokenizer t = new MecabTokenizer( 
                          this.dictionaryPath, 
                          this.userDictionaryFile, 
                          this.discartPunctuation, 
                          this.mode );

    return t;
  }

}
