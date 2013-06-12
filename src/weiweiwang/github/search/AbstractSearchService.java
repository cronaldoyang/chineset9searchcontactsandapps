
package weiweiwang.github.search;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import weiweiwang.github.search.analysis.NGramAnalyzer;
import weiweiwang.github.search.analysis.PinyinAnalyzer;
import weiweiwang.github.search.analysis.T9Analyzer;
import weiweiwang.github.search.utils.StringUtils;
import weiweiwang.github.search.utils.T9Converter;

public abstract class AbstractSearchService {
    protected static final String TAG = AbstractSearchService.class.getSimpleName();

    public static final Pattern PHONE_STRIP_PATTERN = Pattern.compile("[^+\\d]");

    public static final String FIELD_NAME = "name";

    public static final String FIELD_PINYIN = "pinyin";

    public static final String FIELD_NUMBER = "number";

    public static final String FIELD_HIGHLIGHTED_NUMBER = "hl_number";

    public static final String FIELD_ID = "id";

    public static final String FIELD_TYPE = "type";

    public static final String INDEX_TYPE_CALLLOG = "CALLLOG";

    public static final String INDEX_TYPE_CONTACT = "CONTACT";

    public static final String INDEX_TYPE_APPS = "APPS";

    public static final String FIELD_LABEL = "label";

    public static final String FIELD_PKG = "pkg";

    public static final String FIELD_ACTIVITY = "activity";

    public static final long ONE_DAY_IN_MILLISECONDS = 24 * 60 * 60 * 1000;

    public static final long THIRTY_DAYS_IN_MILLISECONDS = 30 * ONE_DAY_IN_MILLISECONDS;// 30
                                                                                        // days

    protected static final FieldType TYPE_STORED_WITH_TERM_VECTORS = new FieldType();

    static {
        TYPE_STORED_WITH_TERM_VECTORS.setIndexed(true);
        TYPE_STORED_WITH_TERM_VECTORS.setTokenized(true);
        TYPE_STORED_WITH_TERM_VECTORS.setStored(true);
        TYPE_STORED_WITH_TERM_VECTORS.setStoreTermVectors(true);
        TYPE_STORED_WITH_TERM_VECTORS.setStoreTermVectorPositions(true);
        TYPE_STORED_WITH_TERM_VECTORS.setStoreTermVectorOffsets(true);
        TYPE_STORED_WITH_TERM_VECTORS.freeze();
    }

    protected static final String PRE_TAG = "<font color='red'>";

    protected static final String POST_TAG = "</font>";

    protected IndexWriter mIndexWriter = null;

    protected IndexWriterConfig mIndexWriterConfig = null;

    protected Analyzer mIndexAnalyzer = null;

    protected Analyzer mSearchAnalyzer = null;

    protected ThreadPoolExecutor mSearchThreadPool = null;

    protected ThreadPoolExecutor mRebuildThreadPool = null;

    protected String mPreTag = PRE_TAG;

    protected String mPostTag = POST_TAG;

    protected AbstractSearchService() {
        init(new RAMDirectory());
    }

    protected AbstractSearchService(File directory) {
        try {
            init(new MMapDirectory(directory));
        } catch (IOException e) {
            log(TAG, e.toString());
            init(new RAMDirectory());
        }
    }

    private void init(Directory directory) {
        try {
            long start = System.currentTimeMillis();
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            mSearchThreadPool = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(1),
                    new ThreadPoolExecutor.DiscardOldestPolicy());
            mRebuildThreadPool = new ThreadPoolExecutor(1, 2, 60L, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(1),
                    new ThreadPoolExecutor.DiscardOldestPolicy());
            Map<String, Analyzer> mIndexAnalyzers = new HashMap<String, Analyzer>();
            mIndexAnalyzers.put(FIELD_NUMBER, new NGramAnalyzer(Version.LUCENE_40, 1, 7));
            mIndexAnalyzers.put(FIELD_PINYIN, new PinyinAnalyzer(Version.LUCENE_40, true));

            Map<String, Analyzer> mSearchAnalyzers = new HashMap<String, Analyzer>();
            mSearchAnalyzers.put(FIELD_PINYIN, new T9Analyzer(Version.LUCENE_40));

            mIndexAnalyzer = new PerFieldAnalyzerWrapper(new KeywordAnalyzer(), mIndexAnalyzers);
            mSearchAnalyzer = new PerFieldAnalyzerWrapper(new KeywordAnalyzer(), mSearchAnalyzers);
            mIndexWriterConfig = new IndexWriterConfig(Version.LUCENE_40, mIndexAnalyzer);
            mIndexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            TieredMergePolicy mergePolicy = (TieredMergePolicy) mIndexWriterConfig.getMergePolicy();
            mergePolicy.setUseCompoundFile(false);
            mIndexWriterConfig.setRAMBufferSizeMB(2.0);
            mIndexWriter = new IndexWriter(directory, mIndexWriterConfig);
            long end = System.currentTimeMillis();
            log(TAG, "init time used:" + (end - start) + ",numDocs:" + mIndexWriter.numDocs());
        } catch (IOException e) {
            log(TAG, e.toString());
        }
    }

    protected String stripNumber(String number) {
        return PHONE_STRIP_PATTERN.matcher(number).replaceAll("");
    }

    // protected abstract ThreadPoolExecutor getSearchThreadPool();

    protected abstract void log(String tag, String msg);

    /**
     * rebuild contacts in the callers' thread
     * 
     * @param urgent
     * @return
     */
    protected abstract long rebuildContacts(boolean urgent);

    /**
     * rebuild calllogs in the callers' thread
     * 
     * @param urgent
     * @return
     */
    protected abstract long rebuildCalllog(boolean urgent);

    protected abstract long rebuildAllApps(boolean urgent);

    public String getPreTag() {
        return mPreTag;
    }

    public void setPreTag(String mPreTag) {
        this.mPreTag = mPreTag;
    }

    public String getPostTag() {
        return mPostTag;
    }

    public void setPostTag(String mPostTag) {
        this.mPostTag = mPostTag;
    }

    public void query(String query, int maxHits, boolean highlight, SearchCallback searchCallback) {
        mSearchThreadPool.execute(new SearchRunnable(query, maxHits, highlight, searchCallback));
    }

    public void destroy() {
        try {
            mSearchThreadPool.shutdown();
            mSearchThreadPool.awaitTermination(1, TimeUnit.SECONDS);
            mRebuildThreadPool.shutdown();
            mRebuildThreadPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log(TAG, e.toString());
        } finally {
            try {
                mIndexWriter.close();
            } catch (IOException e) {
                log(TAG, e.toString());
            }
            log(TAG, "index writer closed");
        }
    }

    /**
     * rebuild contacts and calllogs in a new thread
     * 
     * @param urgent
     */
    public void asyncRebuild(final boolean urgent) {
        mRebuildThreadPool.execute(new Runnable() {
            public void run() {
                rebuildContacts(urgent);
            }
        });
        mRebuildThreadPool.execute(new Runnable() {
            public void run() {
                rebuildCalllog(urgent);
            }
        });

        mRebuildThreadPool.execute(new Runnable() {
            public void run() {
                rebuildAllApps(urgent);
            }
        });
    }

    private class SearchRunnable implements Runnable {

        private String mQuery;

        private int mMaxHits;

        private boolean mHighlight;

        private SearchCallback mSearchCallback;

        public SearchRunnable(String query, int maxHits, boolean highlight,
                SearchCallback searchCallback) {
            mQuery = query;
            mMaxHits = maxHits;
            mHighlight = highlight;
            mSearchCallback = searchCallback;
        }

        @Override
        public void run() {
            doQuery(mQuery, mMaxHits, mHighlight, mSearchCallback);
        }
    }

    protected void doQuery(String mQuery, int mMaxHits, boolean mHighlight,
            SearchCallback mSearchCallback) {
        long start = System.currentTimeMillis();
        Map<String, Float> boosts = new HashMap<String, Float>();
        if (!StringUtils.isBlank(mQuery)) {
            if (mQuery.indexOf('0') != -1 || mQuery.indexOf('1') != -1) {
                boosts.put(FIELD_NUMBER, 1.0F);
            } else if (Character.isLetter(mQuery.charAt(0))) {
                boosts.put(FIELD_PINYIN, 4.0F);
            } else {
                boosts.put(FIELD_PINYIN, 4.0F);
                if (mQuery.length() >= 2) {
                    boosts.put(FIELD_NUMBER, 1.0F);
                }
            }
        } else {
            mHighlight = false;
        }
        MultiFieldQueryParser multiFieldQueryParser = new MultiFieldQueryParser(Version.LUCENE_40,
                boosts.keySet().toArray(new String[0]), mSearchAnalyzer, boosts);
        multiFieldQueryParser.setAllowLeadingWildcard(false);
        multiFieldQueryParser.setDefaultOperator(QueryParser.Operator.AND);
        long highlightedTimeUsed = 0;
        try {
            Query q = boosts.isEmpty() ? new MatchAllDocsQuery() : multiFieldQueryParser
                    .parse(mQuery);
            IndexReader indexReader = DirectoryReader.open(mIndexWriter, false);
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);
            TopDocs td = indexSearcher.search(q, mMaxHits);
            long hits = td.totalHits;
            ScoreDoc[] scoreDocs = td.scoreDocs;
            List<Map<String, Object>> docs = new ArrayList<Map<String, Object>>(mMaxHits);
            for (ScoreDoc scoreDoc : scoreDocs) {
                Map<String, Object> doc = new HashMap<String, Object>();
                Document document = indexReader.document(scoreDoc.doc);
                String name = document.get(FIELD_NAME);
                String number = document.get(FIELD_NUMBER);
                String pinyin = document.get(FIELD_PINYIN);
                String pkg = document.get(FIELD_PKG);
                String activity = document.get(FIELD_ACTIVITY);
                log(TAG, "name:" + name + "; pkg:" + pkg + ";activity:" + activity);

                if (null != name) {
                    doc.put(FIELD_NAME, document.get(FIELD_NAME));
                }
                if (null != number) {
                    doc.put(FIELD_NUMBER, number);
                }
                if (null != pkg && null != activity) {
                    doc.put(FIELD_PKG, pkg);
                    doc.put(FIELD_ACTIVITY, activity);
                }

                long begin = System.currentTimeMillis();
                if (mHighlight) {
                   /* String highlightedNumber = (number != null) ? highlightNumber(number, mQuery)
                            : null;*/
                    String highlightedPinyin = (null != pinyin) ? highlightPinyin(pinyin, mQuery)
                            : null;

                    /*if (null != highlightedNumber) {
                        doc.put(FIELD_HIGHLIGHTED_NUMBER, highlightedNumber);
                    } else*/ if (null != highlightedPinyin) {
                        if (pinyin.equals(name)) {
                            doc.put(FIELD_NAME, highlightedPinyin);
                        } else {
                            doc.put(FIELD_PINYIN, highlightedPinyin);
                        }
                    } else {
                        continue;
                    }
                }
                long end = System.currentTimeMillis();
                highlightedTimeUsed += (end - begin);
                doc.put(FIELD_TYPE, document.get(FIELD_TYPE));
                docs.add(doc);
            }
            indexReader.close();
            long end = System.currentTimeMillis();
            log(TAG, q.toString() + "\t" + hits + "\t" + (end - start) + "\t" + highlightedTimeUsed);
            mSearchCallback.onSearchResult(mQuery, hits, docs);
        } catch (Exception e) {
            e.printStackTrace();
            log(TAG, e.toString());
        }
    }

    /**
     * @param pinyin 鎷奸煶琛ㄧず锛屾瘮濡俉angWeiWei,娉ㄦ剰姝ゅ棣栧瓧姣嶆槸澶у啓鐨� * @param query
     *            鏌ヨ锛屽彲鑳戒负t9,涔熷彲鑳芥槸alpha瀛楁瘝
     * @return 杩斿洖楂樹寒涔嬪悗鐨勭粨鏋滄垨鑰呭湪娌℃湁楂樹寒鐨勬儏鍐典笅杩斿洖鍘绘帀鎷奸煶棣栧瓧姣嶇殑閮ㄥ垎
     * @throws java.io.IOException
     */
    protected String highlightPinyin(String pinyin, String query) throws IOException {
        if (StringUtils.isEmpty(query)) {
            return null;
        }
        int index = pinyin.lastIndexOf('|');
        String full = index > -1 ? pinyin.substring(0, index) : pinyin;
        // 閮借浆鎹负t9鍚庡啀鍖归厤
        String t9 = pinyin.toLowerCase();
        String t9Query = query.toLowerCase();
        if (!Character.isLetter(query.charAt(0)))// t9 match
        {
            t9 = T9Converter.convert(t9);
            t9Query = T9Converter.convert(query);
        }
        int start = t9.lastIndexOf(t9Query);
        if (index > -1 && start > index) {
            String match = pinyin.substring(start, start + t9Query.length());
            StringBuilder stringBuilder = new StringBuilder(pinyin.length() + match.length()
                    * (1 + mPreTag.length() + mPostTag.length()));
            for (int i = 0, j = 0; i < full.length(); i++) {
                char c = full.charAt(i);
                if (j < match.length()) {
                    if (c != match.charAt(j)) {
                        stringBuilder.append(c);
                    } else {
                        stringBuilder.append(mPreTag).append(c).append(mPostTag);
                        j++;
                    }
                } else {
                    stringBuilder.append(c);
                }
            }
            return stringBuilder.toString();
        } else if (start > -1) {
            start = t9.indexOf(t9Query);
            StringBuilder stringBuilder = new StringBuilder(pinyin.length() + mPreTag.length()
                    + mPostTag.length());
            stringBuilder.append(full.substring(0, start)).append(mPreTag)
                    .append(full.substring(start, start + t9Query.length())).append(mPostTag)
                    .append(full.substring(start + t9Query.length()));
            return stringBuilder.toString();
        }
        return null;
    }

    /**
     * 涓嶈兘浣跨敤lucene鐨刪ighlight,鍥犱负鍙风爜浣跨敤鐨勬槸NGram鍒嗚瘝锛屾墍浠
     * oken椤瑰緢澶氾紝杩欏氨瀵艰嚧楂樹寒鏋氫妇token鐗瑰埆鑰楁椂
     * 
     * @param number
     * @param query
     * @return
     * @throws java.io.IOException
     */
    protected String highlightNumber(String number, String query) throws IOException {
        if (StringUtils.isEmpty(query)) {
            return null;
        }
        int start = number.indexOf(query);
        if (start != -1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(number.substring(0, start));
            stringBuilder.append(mPreTag).append(query).append(mPostTag)
                    .append(number.substring(start + query.length()));
            return stringBuilder.toString();
        }
        return null;
    }

    protected Field createStringField(String field, String value) {
        return new StringField(field, value, Field.Store.YES);
    }

    protected Field createTextField(String field, String value) {
        return new TextField(field, value, Field.Store.YES);
    }

    protected Field createHighlightedField(String field, String value) {
        return new Field(field, value, TYPE_STORED_WITH_TERM_VECTORS);
    }

    protected final void yieldInterrupt() throws InterruptedException {
        Thread.yield();
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }
}
