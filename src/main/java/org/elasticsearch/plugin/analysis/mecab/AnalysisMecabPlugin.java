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

package org.elasticsearch.plugin.analysis.mecab;

import org.apache.lucene.analysis.Analyzer;
import org.elasticsearch.index.analysis.AnalyzerProvider;
import org.elasticsearch.index.analysis.CharFilterFactory;
import org.elasticsearch.index.analysis.MecabAnalyzerProvider;
import org.elasticsearch.index.analysis.MecabBaseFormFilterFactory;
import org.elasticsearch.index.analysis.MecabIterationMarkCharFilterFactory;
import org.elasticsearch.index.analysis.MecabKatakanaStemmerFactory;
import org.elasticsearch.index.analysis.MecabNumberFilterFactory;
import org.elasticsearch.index.analysis.MecabPartOfSpeechFilterFactory;
import org.elasticsearch.index.analysis.MecabReadingFormFilterFactory;
import org.elasticsearch.index.analysis.MecabTokenizerFactory;
import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.index.analysis.TokenizerFactory;
import org.elasticsearch.indices.analysis.AnalysisModule.AnalysisProvider;
import org.elasticsearch.plugins.AnalysisPlugin;
import org.elasticsearch.plugins.Plugin;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;

public class AnalysisMecabPlugin extends Plugin implements AnalysisPlugin {
    @Override
    public Map<String, AnalysisProvider<CharFilterFactory>> getCharFilters() {
        return singletonMap("mecab_iteration_mark", MecabIterationMarkCharFilterFactory::new);
    }

    @Override
    public Map<String, AnalysisProvider<TokenFilterFactory>> getTokenFilters() {
        Map<String, AnalysisProvider<TokenFilterFactory>> extra = new HashMap<>();
        extra.put("mecab_baseform", MecabBaseFormFilterFactory::new);
        extra.put("mecab_part_of_speech", MecabPartOfSpeechFilterFactory::new);
        extra.put("mecab_readingform", MecabReadingFormFilterFactory::new);
        extra.put("mecab_stemmer", MecabKatakanaStemmerFactory::new);
//        extra.put("ja_stop", JapaneseStopTokenFilterFactory::new);
        extra.put("mecab_number", MecabNumberFilterFactory::new);
        return extra;
    }


    @Override
    public Map<String, AnalysisProvider<TokenizerFactory>> getTokenizers() {
        return singletonMap("mecab_tokenizer", MecabTokenizerFactory::new);
    }

    @Override
    public Map<String, AnalysisProvider<AnalyzerProvider<? extends Analyzer>>> getAnalyzers() {
        return singletonMap("mecab", MecabAnalyzerProvider::new);
    }
    
}
