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

import com.github.boukefalos.jlibloader.Native;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer.Mode;
import org.apache.lucene.analysis.ja.JapaneseTokenizer.Type;
import org.apache.lucene.analysis.ja.Token;
import org.apache.lucene.analysis.ja.dict.CharacterDefinition;
import org.apache.lucene.analysis.ja.mecab.dict.MecabWorkDictionary;
import org.apache.lucene.analysis.ja.tokenattributes.BaseFormAttribute;
import org.apache.lucene.analysis.ja.tokenattributes.InflectionAttribute;
import org.apache.lucene.analysis.ja.tokenattributes.PartOfSpeechAttribute;
import org.apache.lucene.analysis.ja.tokenattributes.ReadingAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.analysis.util.RollingCharBuffer;
import org.apache.lucene.util.AttributeFactory;
import org.chasen.mecab.Lattice;
import org.chasen.mecab.MeCabConstants;
import org.chasen.mecab.Model;
import org.chasen.mecab.Node;
import org.chasen.mecab.Path;
import org.chasen.mecab.Tagger;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

/**
 * Mecabによるliucene用tokenizer
 * 
 * @author matsu
 */
public final class MecabTokenizer extends Tokenizer {

    /** shared libraryの読み込み */
/*  static {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
          try {
            System.loadLibrary("MeCab");
          } catch (UnsatisfiedLinkError e) {
            throw new UnsatisfiedLinkError(
                "Cannot load the native code.\n"
                    + "Make sure your LD_LIBRARY_PATH contains MeCab.so path.\n" + e);
          }
          return null;
        });
      }*/
    
   static {
     AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
       try {
           Native.load("org.chasen.mecab", "MeCab");
         } catch (UnsatisfiedLinkError e) {
           throw new UnsatisfiedLinkError(
               "Cannot load the native code.\n"
                   + "Make sure your LD_LIBRARY_PATH contains MeCab.so path.\n" + e);
         }
       return null;
     });
   }

  private static final boolean VERBOSE = false;

  /** SEARCHモード用 漢字単語の判定長 */
  private static final int SEARCH_MODE_KANJI_LENGTH = 2;
  /** SEARCHモード用 漢字以外単語の判定長 */
  private static final int SEARCH_MODE_OTHER_LENGTH = 7; 


  /** Mecabインスタンス */
  private Model model = null;
  private Tagger tagger = null;
  private Lattice lattice = null;

  /** 表層形 */
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  /** Posisioon */
  private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
  private final PositionLengthAttribute posLengthAtt = addAttribute(PositionLengthAttribute.class);
  /** 単語位置 */
  private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

  /** 品詞 */
  private final PartOfSpeechAttribute posAtt = addAttribute(PartOfSpeechAttribute.class);
  /** 原形 */
  private final BaseFormAttribute basicFormAtt = addAttribute(BaseFormAttribute.class);
  /** 読み/発音 */
  private final ReadingAttribute readingAtt = addAttribute(ReadingAttribute.class);
  /** 活用型/活用形 */
  private final InflectionAttribute inflectionAtt = addAttribute(InflectionAttribute.class);

  /** 入力バッファ */
  private final RollingCharBuffer buffer = new RollingCharBuffer();
  
  /** Mecab形態素結果取得用辞書 */
  private final MecabWorkDictionary dictionary = new MecabWorkDictionary() ;
  
  /** 形態素結果保存用リスト */
  private final List<Token> pending = new ArrayList<>();

  /** Character category data(漢字判定に使用) */
  private final CharacterDefinition characterDefinition = CharacterDefinition.getInstance(); ;

  /** SEARCHモード */
  private final boolean searchMode;
  /** EXTENDEDモード */
  private final boolean extendedMode;
  /** 句読点の扱い */
  private final boolean discardPunctuation;
  
  
  /**
   * コンストラクタ
   */
  public MecabTokenizer() {
    this(DEFAULT_TOKEN_ATTRIBUTE_FACTORY, null, null, true, Mode.NORMAL );
  }

  /**
   * コンストラクタ
   * 
   * @param dicdir 辞書のpathを指定
   * @param userdic ユーザ辞書ファイルを指定(dicファイル)
   * @param discardPunctuation  句読点の取扱い。true:句読点は除外
   * @param mode  mode tokenization mode.
   */
  public MecabTokenizer( String dicdir, String userdic, boolean discardPunctuation, Mode mode ) {
    this(DEFAULT_TOKEN_ATTRIBUTE_FACTORY, dicdir, userdic, discardPunctuation, mode);
//    this(DEFAULT_TOKEN_ATTRIBUTE_FACTORY, dicdir, userdic, discardPunctuation, Mode.EXTENDED);
//    this(DEFAULT_TOKEN_ATTRIBUTE_FACTORY, "/var/lib/mecab/dic/ipadic-utf8", this.discartPunctuation, Mode.SEARCH);
  }
  /**
   * コンストラクタ
   * 
   * @param factor  the AttributeFactory to use
   * @param dicdir 辞書のpathを指定
   * @param userdic ユーザ辞書ファイルを指定(dicファイル)
   * @param discardPunctuation  句読点の取扱い。true:句読点は除外
   * @param mode  mode tokenization mode.
   */
  public MecabTokenizer(AttributeFactory factor, String dicdir, String userdic, boolean discardPunctuation, Mode mode) {
    super(factor);

    // Node出力フォーマットを「素性 (品詞, 活用, 読み) 等を CSV で表現したもの」を指定
    String arg = "--node-format=%H ";

    // 辞書のpathを指定する場合
    if (dicdir != null) {
      arg = arg + " --dicdir=" + dicdir;
    }
    // ユーザ辞書のfileを指定する場合(full path)
    if (userdic != null) {
      arg = arg + " -userdic=" + userdic;
    }

    // mecab Model作成
    this.model = new Model(arg);
    this.tagger = this.model.createTagger();

    // 入力のリセット
    this.buffer.reset(this.input);
    
    // 動作モード指定
    switch(mode){
      // 複合語で構成された単語を細かく分割
      case SEARCH:
        this.searchMode = true;
        this.extendedMode = false;
        break;
      // searchモードの処理をしつつ、追加で、辞書にない未知語を1-gramに分割
      case EXTENDED:
        this.searchMode = true;
        this.extendedMode = true;
        break;
      // 形態素解析による通常の単語分割
      default:
        this.searchMode = false;
        this.extendedMode = false;
        break;
    }
    // 句読点
    this.discardPunctuation = discardPunctuation;


  }

  @Override
  public void close() throws IOException {
    super.close();
    // Mecab後始末
    try {
      if (this.lattice != null) {
        this.lattice.clear();
      }
    } finally {
      this.lattice = null;
    }
    // Mecab用work辞書クリア
    this.dictionary.clear();
    this.buffer.reset(this.input);
  }
  
  @Override
  public void reset() throws IOException {
    super.reset();
    // Mecab後始末
    try {
      if (this.lattice != null) {
        this.lattice.clear();
        this.lattice.delete();;
      }
    } finally {
      this.lattice = null;
    }
    // Mecab用work辞書クリア
    this.dictionary.clear();
    this.buffer.reset(this.input);

    // 形態素処理
    this.parse();
  }
  
  
  /** 探索用最小コスト */
  private int min_cost = Integer.MAX_VALUE ;
  
  /** 探索結果形態素リスト */
  private List<Node> search_list = null ;
  
  /**
   * 形態素に対して連結するものを探索する
   * 
   * @param other 探索する対象Node
   * @param nodes 候補リスト
   * @param cost  ここまでの算出コスト
   * @param len   ここまでの連結文字長
   * @param node_len  対象文字長
   */
  public void SearchNextMorpheme( Node other, List<Node> nodes, int cost, int len, int node_len) {
    // 右への連結を取得
    Path rpath = other.getRpath() ;
    do {
      // 候補リスト
      List<Node> next_nodes = new ArrayList<Node>(nodes) ;
      // 左形態素のコスト＋連結コスト
      int pcost = cost + rpath.getCost();
      
      // 右形態素を取得
      Node rnext = rpath.getRnode() ;
      do{
        if (rnext.getStat() == MeCabConstants.MECAB_BOS_NODE || 
            rnext.getStat() == MeCabConstants.MECAB_EOS_NODE) {
          continue;
        }
        int rnext_len = rnext.getSurface().length() ;
        int check_len = len + rnext_len ;
        // ここまでのコスト＋単語コスト
        int ncost = pcost + rnext.getWcost();
        if (VERBOSE) {
          System.out.println("\tNEXT\t" 
              + rnext.getId() + "\t" 
              + rpath.getCost() + "\t"
              + rnext.getCost() + "\t" 
              + (ncost) + "\t" 
              + rnext.getSurface() 
              + "\t" + rnext.getFeature());
        }
        // 探索対象文字長未満の場合
        if ( check_len < node_len ) {
          // 候補リストに格納
          next_nodes.add( rnext ) ;
          
          // 形態素に対して連結するものを探索する
          SearchNextMorpheme(rnext, next_nodes, ncost, check_len, node_len ) ;
        }
        // 探索対象文字長以上の場合
        else
        {
          // 探索を終了する
          // ここまでのコストが最小か判定
          if( this.min_cost > ncost ) {
            // 最小の場合、最小値更新
            this.min_cost = ncost ;
            // 候補リストの保存
            this.search_list = new ArrayList<Node>(nodes) ;
            this.search_list.add(rnext) ;
            
            if (VERBOSE) {
              System.out.println("\t!!!!!!!\t" + min_cost + "\t" + rpath.getCost() + "\t" +rnext.getCost() );
            }
          }
        }
      // 次の形態素を取得
      } while( (rnext = rnext.getNext()) != null );

    // 次の右への連結を取得
    } while( (rpath = rpath.getRnext()) != null) ;
  }

  
  /**
   * 未知語に対して最小コストの1gramの形態素を探索する
   * 
   * @param other 探索する対象Node
   * @param nodes 候補リスト
   * @param cost  ここまでの算出コスト
   * @param len   ここまでの連結文字長
   * @param node_len  対象文字長
   */
  public void UnknownNextMorpheme( Node other, List<Node> nodes, int cost, int len, int node_len) {
    // 右への連結を取得
    Path rpath = other.getRpath() ;
    do {
      // 候補リスト
      List<Node> next_nodes = new ArrayList<Node>(nodes) ;
      // 左形態素のコスト＋連結コスト
      int pcost = cost + rpath.getCost();
      
      // 右形態素を取得
      Node rnext = rpath.getRnode() ;
      do{
        if (rnext.getStat() == MeCabConstants.MECAB_BOS_NODE || 
            rnext.getStat() == MeCabConstants.MECAB_EOS_NODE ||
            rnext.getSurface().length() != 1) {
          continue;
        }
        int rnext_len = rnext.getSurface().length() ;
        int check_len = len + rnext_len ;
        // ここまでのコスト＋単語コスト
        int ncost = pcost + rnext.getWcost();
        
        if (VERBOSE) {
          System.out.println("\tNEXT\t" 
              + rnext.getId() + "\t" 
              + rpath.getCost() + "\t"
              + rnext.getCost() + "\t" 
              + (ncost) + "\t" 
              + rnext.getSurface() 
              + "\t" + rnext.getFeature());
        }
        
        // 探索対象文字長未満の場合
        if ( check_len < node_len ) {
          // 候補リストに格納
          next_nodes.add( rnext ) ;
          
          // 形態素に対して連結するものを探索する
          UnknownNextMorpheme(rnext, next_nodes, ncost, check_len, node_len ) ;
        }
        // 探索対象文字長以上の場合
        else
        {
          // 探索を終了する
          // ここまでのコストが最小か判定
          if( this.min_cost > ncost ) {
            // 最小の場合、最小値更新
            this.min_cost = ncost ;
            // 候補リストの保存
            this.search_list = new ArrayList<Node>(nodes) ;
            this.search_list.add(rnext) ;
            
            if (VERBOSE) {
              System.out.println("\t!!!!!!!\t" + min_cost + "\t" + rpath.getCost() + "\t" +rnext.getCost() );
            }
          }
        }
      // 次の形態素を取得
      } while( (rnext = rnext.getNext()) != null );

    // 次の右への連結を取得
    } while( (rpath = rpath.getRnext()) != null) ;
  }

  /**
   * 漢字３文字以上若しくは、８文字以上の単語かの判定
   * 
   * @param surface 判定文字列
   * @return  true:対象文字列/false:それ以外
   */
  private boolean isSearchTarget(String surface ) {
    boolean ret = false ;
    // 文字列が３文字以上の場合
    if (surface.length() > SEARCH_MODE_KANJI_LENGTH) {
      boolean allKanji = true;
      
      char[] chars = surface.toCharArray();
      
      // 漢字かどうか判定する
      for (int pos = 0; pos < chars.length; pos++) {
        if (!this.characterDefinition.isKanji( chars[pos]) ) {
          allKanji = false;
          break;
        }
      }
      // 漢字の場合
      if (allKanji) {
        ret = true ;
      // 漢字以外の文字の場合で且つ、８文字以上の場合
      } else if ( surface.length() > SEARCH_MODE_OTHER_LENGTH) {
        ret = true ;
      }
    }
    return ret ;
  }
  
  
  /**
   * Mecabによる形態素処理
   * 
   * @throws IOException  mecab実行例外発生
   */
  void parse() throws IOException {

    try {
      if (this.lattice != null) {
        this.lattice.clear();
        this.lattice.delete();
      }
    } finally {
      this.lattice = this.model.createLattice();
    }

    StringBuilder strbuilder = new StringBuilder();

    int inputOff = 0;
    int c = 0;
    // 入力バッファから取り出し、文字列に変換する
    while ((c = this.buffer.get(inputOff)) != -1) {
      strbuilder.append((char) c);
      inputOff++;
      this.buffer.freeBefore(inputOff);
    }
    String str = strbuilder.toString();
    
    // SEARCHモードの場合
    if (this.searchMode ) {
      // MecabをNbestで形態素する
      this.lattice.add_request_type(MeCabConstants.MECAB_NBEST);
    }
    // 形態素対象文字列の設定
    this.lattice.set_sentence(str);

    // 形態素実行
    if (!this.tagger.parse(this.lattice)) {
      throw new IOException(this.lattice.what());
    }

    int start = 0;
    int end = 0;

    Node node = null;
    start = 0;
    end = 0;
    // 形態素を取得
    for (node = this.lattice.bos_node(); node != null; node = node.getNext()) {
      // BOS,EOSの場合、無視
      if (node.getStat() == MeCabConstants.MECAB_BOS_NODE || 
          node.getStat() == MeCabConstants.MECAB_EOS_NODE) {
        continue;
      }

      this.min_cost = Integer.MAX_VALUE;
      this.search_list = null;

      String surface = node.getSurface();
      
      
      // 未知語の場合で且つ、EXTENDEDモードの場合
      if( node.getStat() == MeCabConstants.MECAB_UNK_NODE && extendedMode ) {
        // 未知語を1-gramに分割
        
        int node_len = surface.length();
        Node other = node;
        // 対象単語と同じ開始位置で始まる形態素を取得
        while ((other = other.getBnext()) != null) {
          int other_len = other.getSurface().length();
          if( other_len != 1) {
            continue;
          }
          int len = other_len;
          
          // 形態素候補のList
          List<Node> nodes = new ArrayList<Node>();
          nodes.add(other);
          // 未知語に対して最小コストの1gramの形態素を探索する
          UnknownNextMorpheme(other, nodes, other.getWcost(), len, node_len);
        }
        
      }
      /**
       * 漢字３文字以上若しくは、８文字以上の単語の場合、      
       * SEARCHモードとして次に連結コストの高い形態素を追加する
       */
      else if (isSearchTarget(surface) && this.searchMode ) {

        int node_len = surface.length();
        Node other = node;
        // 対象単語と同じ開始位置で始まる形態素を取得
        while ((other = other.getBnext()) != null) {
          int other_len = other.getSurface().length();
          if( other_len >= node_len) {
            continue;
          }

          int len = other_len;
          
          // 形態素候補のList
          List<Node> nodes = new ArrayList<Node>();
          nodes.add(other);
          // 形態素に対して連結するものを探索する
          SearchNextMorpheme(other, nodes, other.getWcost(), len, node_len);
        }
      }

      Token token = null;
      // SEARCH対象、形態素がある場合
      if (this.search_list != null) {
        for (int i = 0; i < this.search_list.size(); i++) {
          Node search_node = this.search_list.get(i);

          start = start + (search_node.getRlength() - search_node.getLength());
          end = start + search_node.getSurface().length();

          // 辞書に対象情報設定
          this.dictionary.add(
              (int) search_node.getId(), 
              search_node.getSurface(), 
              search_node.getPosid(), 
              search_node.getCost(), 
              search_node.getWcost(), 
              search_node.getFeature());
          if (VERBOSE) {
            System.out.println("\t" 
                + search_node.getId() + "\t" 
                + search_node.getCost() + "\t" 
                + search_node.getWcost() + "\t" 
                + search_node.getSurface() + "\t"
                + search_node.getFeature());
          }
          if ( surface.length() > 1 ||
              ( search_node.getSurface().length() == 1 &&
              (!this.discardPunctuation || !isPunctuation(search_node.getSurface().toCharArray()[0])))) {

            // Tokenの作成
            token = new Token(
                (int) search_node.getId(), 
                str.toCharArray(), 
                start, 
                search_node.getSurface().length(), 
                Type.KNOWN, 
                start,
                this.dictionary);
            // Tokenの保持
            this.pending.add(token);
          }

          if (i == 0) {

            start = start + (node.getRlength() - node.getLength());

            // 辞書に対象情報設定
            this.dictionary.add(
                (int) node.getId(), 
                surface, 
                node.getPosid(), 
                node.getCost(), 
                node.getWcost(),
                node.getFeature());

            if (VERBOSE) {
              System.out.println("\t" 
                  + node.getId() + "\t" 
                  + node.getCost() + "\t" 
                  + node.getWcost() + "\t"
                  + node.getSurface() + "\t" 
                  + node.getFeature());
            }
            
            if ( surface.length() > 1 ||
                (surface.length() == 1 &&
                (!this.discardPunctuation || !isPunctuation(surface.toCharArray()[0])))) {
            
              // Tokenの作成
              token = new Token(
                  (int) node.getId(), 
                  str.toCharArray(), 
                  start, 
                  surface.length(), 
                  Type.KNOWN, 
                  start,
                  this.dictionary);
              // SERCHモードで探索した形態素の分割数を格納
              token.setPositionLength(this.search_list.size());
              
              // Tokenの保持
              this.pending.add(token);
            }
          }
          // 次の形態素の文字位置更新
          start = end;
        }
      // SEARCH対象、形態素がない場合
      } else {
        start = start + (node.getRlength() - node.getLength());
        end = start + node.getSurface().length();

        // 辞書に対象情報設定
        this.dictionary.add(
            (int) node.getId(), 
            surface, 
            node.getPosid(), 
            node.getCost(), 
            node.getWcost(),
            node.getFeature());

        if (VERBOSE) {
          System.out.println("\t" 
              + node.getId() + "\t" 
              + node.getCost() + "\t" 
              + node.getWcost() + "\t"
              + node.getSurface() + "\t" 
              + node.getFeature());
        }

        if ( surface.length() > 1 ||
            (surface.length() == 1 &&
            (!this.discardPunctuation || !isPunctuation(surface.toCharArray()[0])))) {
  
          // Tokenの作成
          token = new Token(
              (int) node.getId(), 
              str.toCharArray(), 
              start, 
              surface.length(), 
              Type.KNOWN, 
              start,
              this.dictionary);
          // Tokenの保持
          this.pending.add(token);
        }
        
        // 次の形態素の文字位置更新
        start = end;
      }

    }

    // Mecabの後処理
    try {
      if (this.lattice != null) {
        this.lattice.clear();
        this.lattice.delete();
      }
    } finally {
      this.lattice = null;
    }
  }

  @Override
  public boolean incrementToken() throws IOException {
    boolean ret = false ;
    // 形態素結果があるか？
    if ( pending.size() == 0 ){
      ret = false;
    }
    else
    {
      // AttributeSourceのクリア
      clearAttributes();
      
      // 形態素結果からToken取得
      final Token token = pending.remove(0);
  
      // tokenattributesの設定

      int offset = token.getOffset();
      int length = token.getLength() ;
      
      termAtt.copyBuffer(token.getSurfaceForm(), offset, length);
      offsetAtt.setOffset(correctOffset(offset), correctOffset(offset+length));
      basicFormAtt.setToken(token);
      posAtt.setToken(token);
      readingAtt.setToken(token);
      inflectionAtt.setToken(token);
      
      int poslen = token.getPositionLength() ;
      if( poslen > 0 ) {
        posIncAtt.setPositionIncrement(0);
        posLengthAtt.setPositionLength(poslen);
      }
      else {
        posIncAtt.setPositionIncrement(1);
        posLengthAtt.setPositionLength(1);
      }
      ret = true;
    }
    return ret ;
  }
  
  /**
   * 句読点判定
   * @see org.apache.lucene.analysis.ja.JapaneseTokenizer
   */
  private static boolean isPunctuation(char ch) {
    switch(Character.getType(ch)) {
      case Character.SPACE_SEPARATOR:
      case Character.LINE_SEPARATOR:
      case Character.PARAGRAPH_SEPARATOR:
      case Character.CONTROL:
      case Character.FORMAT:
      case Character.DASH_PUNCTUATION:
      case Character.START_PUNCTUATION:
      case Character.END_PUNCTUATION:
      case Character.CONNECTOR_PUNCTUATION:
      case Character.OTHER_PUNCTUATION:
      case Character.MATH_SYMBOL:
      case Character.CURRENCY_SYMBOL:
      case Character.MODIFIER_SYMBOL:
      case Character.OTHER_SYMBOL:
      case Character.INITIAL_QUOTE_PUNCTUATION:
      case Character.FINAL_QUOTE_PUNCTUATION:
        return true;
      default:
        return false;
    }
  }
}

