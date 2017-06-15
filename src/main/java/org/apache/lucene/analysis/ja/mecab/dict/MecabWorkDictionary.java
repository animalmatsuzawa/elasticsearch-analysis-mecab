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

package org.apache.lucene.analysis.ja.mecab.dict;

import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.ja.dict.Dictionary;
import org.elasticsearch.common.logging.Loggers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Mecabの形態素結果をFilterにてtokenattributesにて参照する為の辞書
 * 
 * @author matsu
 */
public class MecabWorkDictionary implements Dictionary {
    
    private static final Logger logger = Loggers.getLogger(MecabWorkDictionary.class);

  
  HashMap<Integer, WorkDict> map = null ;
  HashMap<String, List<WorkDict>> index = null ;
  enum DicType {
    // Juman
    JUMAN,
    // ipadic
    IPADIC,
    // unidic
    UNIDOC,
  }
  
  /**
   * Mecab辞書の素性情報
   */
  class WorkDict {
    WorkDict(
        int id, 
        String surface,
        int posid,
        long cost,
        short wcost, 
        List<String> features) {
      this.id = id ;
      this.surface = surface;
      this.posid = posid;
      this.cost = cost;
      this.wcost = wcost;
      this.features = new ArrayList<String>();
      this.features.addAll(features);
    }

    // 単語ID
    int id ;
    // 表層形
    String surface ;
    // 形態素 ID
    int posid ;
    // 累積コスト
    long cost ;
    // 単語生起コスト
    short wcost ;
    
    // 素性情報
    List<String> features ;
    
  }
 
  /**
   * コンストラクタ
   */
  public MecabWorkDictionary() {
    this.map = new HashMap<Integer, WorkDict>() ;
    this.index = new HashMap<String, List<WorkDict>>() ;
  }
  
  /**
   * 辞書に追加
   */
  
  /**
   * 辞書に追加
   * @param id  単語ID
   * @param surface 表層形
   * @param posid 品詞ID
   * @param cost  連結コスト
   * @param wcost 単語コスト
   * @param feature 素性情報
   * @throws IOException  例外
   */
  public void add( 
      int id,
      String surface,
      int posid,
      long cost,
      short wcost, 
      String feature ) throws IOException {
    
    // feautureの解析
    CSVTokenizer csv = new CSVTokenizer(feature);
    List<String> features = new ArrayList<String>();
    while (csv.hasMoreElement()) {
      features.add(csv.nextElement());
    }
    WorkDict dic = new WorkDict(id, surface, posid, cost, wcost, features);
    
    this.map.put(id, dic);
    
    List<WorkDict> list = null ;
    if( (list = this.index.get(surface)) == null ) {
      list = new ArrayList<WorkDict>() ;
    }
    boolean add = true;
    for( int i = 0 ; i < list.size() ; i++ ) {
      if( list.get(i).features.toString().equals(features.toString()) ) {
        add = false ;
        break ;
      }
    }
    if( add ) {
      list.add(dic);
    }
    this.index.put(surface, list) ;
    
  }
  /**
   * 表層形と素性情報から辞書を検索する
   * @param surface 表層形
   * @param feature 素性情報
   * @return  辞書情報
   * @throws IOException  例外
   */
  public WorkDict checkDict( String surface, String feature ) throws IOException {
    WorkDict dict = null ;
    List<WorkDict> list = null ;
    
    CSVTokenizer csv = new CSVTokenizer(feature);
    List<String> features = new ArrayList<String>();
    while (csv.hasMoreElement()) {
      features.add(csv.nextElement());
    }

    if( (list = this.index.get(surface)) == null ) {
      list = new ArrayList<WorkDict>() ;
    }
    for( int i = 0 ; i < list.size() ; i++ ) {
      if( list.get(i).features.toString().equals(features.toString())  ) {
        dict = list.get(i) ;
        break ;
      }
    }
    return dict ;
  }
  
  
  public void clear() {
    this.map.clear();
    this.index.clear();
  }


  @Override
  public int getLeftId(int wordId) {
    return 0;
  }


  @Override
  public int getRightId(int wordId) {
    return 0;
  }


  @Override
  public int getWordCost(int wordId) {
    WorkDict work = this.map.get(wordId) ;
    int cost = 0 ;
    if( work != null){
      cost = (int)work.wcost;
    }
    return cost;
  }

  /**
   * 素性情報の数から辞書の種類を判定する
   * @param dic 辞書要素
   * @return  辞書種別
   */
  private DicType getDicType( WorkDict dic ) {
    // juman 11 
    // 表層形,左文脈ID,右文脈ID,コスト,品詞,品詞細分類1,活用型,活用形,原形,読み,表記 
    // ipadic 13
    // 表層形,左文脈ID,右文脈ID,コスト,品詞,品詞細分類1,品詞細分類2,品詞細分類3,活用形,活用型,原形,読み,発音 
    // unidic 21
    // 表層形,左文脈ID,右文脈ID,コスト,品詞,品詞細分類1,品詞細分類2,品詞細分類3,活用型,活用形,語彙素読み,語彙素(語彙素表記 +
    //   語彙素細分類),書字形出現形,発音形出現形,書字形基本形,発音形基本形,語種,語頭変化型,語頭変化形,語末変化型,語末変化形

    int f_size = dic.features.size() ;
    if (f_size < 9 ){
      // juman
      return DicType.JUMAN ;
    } else if( f_size < 17 ) {
      // ipadic
      return DicType.IPADIC ;
    } else {
      // unidic
      return DicType.UNIDOC ;
    }
  }

  /**
   * 品詞の取得
   * 
   * @param wordId  単語ID
   */
  @Override
  public String getPartOfSpeech(int wordId) {
    
    WorkDict work = this.map.get(wordId) ;
    String type = new String( "" );
    if( getDicType(work) == DicType.JUMAN ) {
      if( work != null){
        for( int i = 0 ; i < 2 ; i++ ) {
          String pos = work.features.get(i);
          if ( !"*".equals( pos ) ) {
            if( i > 0 ) {
              type += "-";
            }
            type += pos;
          }
        }
      }
    } else {
      if( work != null){
        for( int i = 0 ; i < 4 ; i++ ) {
          String pos = work.features.get(i);
          if ( !"*".equals( pos ) ) {
            if( i > 0 ) {
              type += "-";
            }
            type += pos;
          }
        }
      }
    }
    return type;
  }
  
  /**
   * 読み
   */
  @Override
  public String getReading(int wordId, char[] surface, int off, int len) {
    WorkDict work = this.map.get(wordId) ;
    //logger.debug(work.features);
    int index = 7 ;
    switch(getDicType(work)){
      case JUMAN :
        index = 5 ;
        break ;
      case IPADIC :
        index = 7 ;
        break ;
      case UNIDOC :
        index = 6 ;
        break ;
      default:
        break ;
    }
    return work.features.get(index);
  }


  /**
   * 原形
   */
  @Override
  public String getBaseForm(int wordId, char[] surface, int off, int len) {
    WorkDict work = this.map.get(wordId) ;
    //logger.debug(work.features);
    int index = 6 ;
    switch(getDicType(work)){
      case JUMAN :
        index = 4 ;
        break ;
      case IPADIC :
        index = 6 ;
        break ;
      case UNIDOC :
        index = 10 ;
        break ;
      default:
        break ;
    }
    return work.features.get(index);
  }


  /**
   * 発音
   */
  @Override
  public String getPronunciation(int wordId, char[] surface, int off, int len) {
    WorkDict work = this.map.get(wordId) ;
    //logger.debug(work.features);
    int index = 8 ;
    switch(getDicType(work)){
      case JUMAN :
        index = 5 ;
        break ;
      case IPADIC :
        index = 8 ;
        break ;
      case UNIDOC :
        index = 11 ;
        break ;
      default:
        break ;
    }
    return work.features.get(index);
  }

  /**
   * 活用型
   */
  @Override
  public String getInflectionType(int wordId) {
    WorkDict work = this.map.get(wordId) ;
    //logger.debug(work.features);
    int index = 5 ;
    switch(getDicType(work)){
      case JUMAN :
        index = 2 ;
        break ;
      case IPADIC :
        index = 5 ;
        break ;
      case UNIDOC :
        index = 4 ;
        break ;
      default:
        break ;
    }
    return work.features.get(index);
  }


  /**
   * 活用形
   */
  @Override
  public String getInflectionForm(int wordId) {
    WorkDict work = this.map.get(wordId) ;
    int index = 4 ;
    switch(getDicType(work)){
      case JUMAN :
        index = 3 ;
        break ;
      case IPADIC :
        index = 4 ;
        break ;
      case UNIDOC :
        index = 5 ;
        break ;
      default:
        break ;
    }
    return work.features.get(index);
  }
  
  /**
   * CSV解析用Tokenizer
   * 
   * @author matsu
   */
  class CSVTokenizer {
    /** CSVの1レコード（１行）データ */
    private String source = null;

    /** CSVのデフォルトセパレータ */
    private char separator = ',';
    /** ソース文字列の長さ */
    private int maxPotiosion = 0;

    /** 現在の操作位置を表す */
    private int currentPotision = 0;

    /**
     * コンストラクタ
     * 
     * @param source
     *          元文字列
     * @throws IOException
     *           source が null
     */
    CSVTokenizer(String source) throws IOException {
      // Source文字列のNullは許さない
      if (source != null) {
        this.source = source;
        this.maxPotiosion = this.source.length();
        this.currentPotision = 0;
      } else {
        throw new IOException("Parameter is null !!");
      }
    }

    /**
     * コンストラクタ
     * 
     * @param source
     *          元文字列
     * @param separator
     *          CSVセパレータ
     * @throws IOException
     *           source が null
     */
    CSVTokenizer(String source, char separator) throws IOException {
      // Source文字列のNullは許さない
      if (source != null) {
        this.source = source;
        this.maxPotiosion = this.source.length();
        this.currentPotision = 0;
      } else {
        throw new IOException("Parameter is null !!");
      }

      // セパレータをセット
      // セパレータがNullの場合はデフォルトで操作を行う
      if (separator != 0) {
        this.separator = separator;
      }
    }

    /**
     * 次のエレメントを返す
     * 
     * @return 取得したエレメントデータ
     */
    public String nextElement() {
      int i = 0;
      StringBuffer buffer = new StringBuffer();
      boolean data = false;

      for (i = 0; (i + this.currentPotision) < this.maxPotiosion; i++) {
        char ch = this.source.charAt(i + this.currentPotision);

        if (!data && (ch == this.separator)) {
          // カウンタを次の文字の位置に進めてからブレイク
          i++;
          break;
        } else if (ch == '\"') {
          if ((i + this.currentPotision + 1) < this.maxPotiosion) {
            if (this.source.charAt(i + this.currentPotision + 1) == '\"') {
              // ダブルクォーテーションが２回続く場合は１つにする
              buffer.append(ch);
              i++;
              continue;
            }
          }
          data = !data;
        } else {
          buffer.append(ch);
        }
      }

      this.currentPotision += i;
      return buffer.toString();
    }

    /**
     * まだ続くエレメントがあるかを返す
     * 
     * @return true:エレメント有/false:エレメント無
     */
    public boolean hasMoreElement() {
      return (this.currentPotision >= this.maxPotiosion) ? false : true;
    }
  }



}
