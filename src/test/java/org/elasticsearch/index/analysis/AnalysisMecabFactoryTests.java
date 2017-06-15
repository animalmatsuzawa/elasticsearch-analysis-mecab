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

import org.apache.lucene.analysis.ja.JapaneseBaseFormFilter;
import org.apache.lucene.analysis.ja.JapaneseIterationMarkCharFilter;
import org.apache.lucene.analysis.ja.JapaneseKatakanaStemFilter;
import org.apache.lucene.analysis.ja.JapaneseNumberFilter;
import org.apache.lucene.analysis.ja.JapanesePartOfSpeechStopFilter;
import org.apache.lucene.analysis.ja.JapaneseReadingFormFilter;
import org.apache.lucene.analysis.ja.JapaneseTokenizerFactory;
import org.elasticsearch.AnalysisFactoryTestCase;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class AnalysisMecabFactoryTests extends AnalysisFactoryTestCase {

    @Override
    protected Map<String, Class<?>> getTokenizers() {
        Map<String, Class<?>> tokenizers = new HashMap<>(super.getTokenizers());
        // lucene-analyzers-kuromojiを参照している為、必要
        tokenizers.put("japanese", JapaneseTokenizerFactory.class);
        
        // MecabTokenizerFactoryのテスト
        tokenizers.put("mecab", MecabTokenizerFactory.class);
        return tokenizers;
    }
    
    @Override
    protected Map<String, Class<?>> getTokenFilters() {
        Map<String, Class<?>> filters = new HashMap<>(super.getTokenFilters());
        // lucene-analyzers-kuromojiを参照している為、必要
        filters.put("japanesebaseform", JapaneseBaseFormFilter.class);
        filters.put("japanesepartofspeechstop", JapanesePartOfSpeechStopFilter.class);
        filters.put("japanesereadingform", JapaneseReadingFormFilter.class);
        filters.put("japanesekatakanastem", JapaneseKatakanaStemFilter.class);
        filters.put("japanesenumber", JapaneseNumberFilter.class);
        return filters;
    }

    @Override
    protected Map<String, Class<?>> getCharFilters() {
        Map<String, Class<?>> filters = new HashMap<>(super.getCharFilters());
        // lucene-analyzers-kuromojiを参照している為、必要
        filters.put("japaneseiterationmark", TestIterationMarkCharFilterFactory.class);
        return filters;
    }

    // lucene-analyzers-kuromojiを参照している為、必要
    public class TestIterationMarkCharFilterFactory extends AbstractCharFilterFactory implements MultiTermAwareComponent {

        private final boolean normalizeKanji;
        private final boolean normalizeKana;

        public TestIterationMarkCharFilterFactory(IndexSettings indexSettings, Environment env, String name, Settings settings) {
            super(indexSettings, name);
            normalizeKanji = settings.getAsBoolean("normalize_kanji", JapaneseIterationMarkCharFilter.NORMALIZE_KANJI_DEFAULT);
            normalizeKana = settings.getAsBoolean("normalize_kana", JapaneseIterationMarkCharFilter.NORMALIZE_KANA_DEFAULT);
        }

        @Override
        public Reader create(Reader reader) {
            return new JapaneseIterationMarkCharFilter(reader, normalizeKanji, normalizeKana);
        }

        @Override
        public Object getMultiTermComponent() {
            return this;
        }
    }
}
