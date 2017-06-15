Elasticsearch Analysis Mecab
====
## elasticsearch-analysis-mecab
elasticsearchのanalysis plugin 形態素にmecabを使用

## ビルド
```
gradle assemble
```

## 使用方法
### Mecabのインストール
mecabをインストールしてください。
辞書はutf-8であれば、unidic、ipadic、juman、ipadic-neologd、unidic_neologd等を使用可能

### elasticsearchプラグインのインストール
```
bin/elasticsearch-plugin install analysis-mecab.zip
```

## kuromojiとの対比
kuromojiのTokenizer,Filterとの対比は以下。
| name                    | kuromoji name              | type        |
|:------------------------|:---------------------------|------------:|
| mecab\_iteration\_mark  | kuromoji\_iteration\_mark  | charfilter  |
| mecab                   | kuromoji                   | analyzer    |
| mecab\_tokenizer        | kuromoji\_tokenizer        | tokenizer   |
| mecab\_baseform         | kuromoji\_baseform         | tokenfilter |
| mecab\_part\_of\_speech | kuromoji\_part\_of\_speech | tokenfilter |
| mecab\_readingform      | kuromoji\_readingform      | tokenfilter |
| mecab\_stemmer          | kuromoji\_stemmer          | tokenfilter |

kuromojiとmecabのfilterの同時の使用はできません。
(tokenizerをmecab_tokenizerにしてfilterをkuromoji_baseformは不可)


