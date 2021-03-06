
package com.homehub.t9search.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;

import com.homehub.t9search.BuildConfig;
import com.homehub.t9search.mode.AppInfo;
import com.homehub.t9search.mode.SearchResultItem;
import com.homehub.t9search.service.utils.PinyinConverter;

public class SearchService extends AbstractSearchService {
    public static final String TAG = "SearchService";

    private static final String INDEX_DIR = "idx";

    private static final int REBUILD_TRIGGER_THRESHOLD = 50;

    private static AbstractSearchService sInstance = null;

    private static HashMap<String, SearchResultItem> mRecentlyInstalledAppsList;

    private Context mContext;

    private SearchService(Context context) {
        super(context.getDir(INDEX_DIR, Context.MODE_PRIVATE));
        mContext = context;
        mRecentlyInstalledAppsList = new HashMap<String, SearchResultItem>();
        if (mIndexWriter.numDocs() < REBUILD_TRIGGER_THRESHOLD) {
            asyncRebuild(true);
        }
    }

    public static synchronized AbstractSearchService getInstance(Context context) {
        if (null == sInstance) {
            sInstance = new SearchService(context);
        }
        return sInstance;
    }

    @Override
    public void destroy() {
        super.destroy();
        sInstance = null;
        log(TAG, "destroy " + getClass().getSimpleName());
    }

    @Override
    public long rebuildCalllog(boolean urgent) {
        long start = System.currentTimeMillis();
        ContentResolver cr = mContext.getContentResolver();
        Cursor cur = cr.query(Calls.CONTENT_URI, new String[] {
                Calls._ID, // 0
                Calls.NUMBER, // 1
                Calls.DATE, // 2
                Calls.TYPE, // 3
        }, Calls.CACHED_NAME + " is NULL", null, Calls.DEFAULT_SORT_ORDER + " LIMIT 200");
        long hits = 0;
        try {
            mIndexWriter.deleteDocuments(new Term(FIELD_TYPE, INDEX_TYPE_CALLLOG));
            // reuseable document and fields
            Document document = new Document();
            Field numberField = createTextField(FIELD_NUMBER, "");
            Field typeField = createStringField(FIELD_TYPE, INDEX_TYPE_CALLLOG);
            document.add(numberField);
            document.add(typeField);

            long current = System.currentTimeMillis();
            Map<String, Object[]> aggregatedCalllogs = new HashMap<String, Object[]>();
            while (cur.moveToNext()) {
                hits++;
                try {
                    String number = stripNumber(cur.getString(1));
                    long date = cur.getLong(2);
                    int type = cur.getInt(3);
                    long diff = (current - date);
                    float boost = (type == Calls.OUTGOING_TYPE ? 2.0f : 1.0f) + 1.0f
                            / (diff / (float) ONE_DAY_IN_MILLISECONDS + 1.0f);
                    if (!aggregatedCalllogs.containsKey(number)) {
                        aggregatedCalllogs.put(number, new Object[] {
                                number, boost
                        });
                    }
                    // log(TAG,"rebuild calllog:"+number+",boost:"+boost);
                } catch (Exception e) {
                    log(TAG, e.toString());
                }
            }
            cur.close();
            for (Map.Entry<String, Object[]> entry : aggregatedCalllogs.entrySet()) {
                Object[] arr = entry.getValue();
                String number = (String) arr[0];
                float boost = (Float) arr[1];
                numberField.setStringValue(number);
                numberField.setBoost(boost);
                mIndexWriter.addDocument(document);
                if (!urgent) {
                    yieldInterrupt();
                }
            }
            Map<String, String> userData = new HashMap<String, String>();
            userData.put("action", "rebuild");
            mIndexWriter.commit(userData);
            long end = System.currentTimeMillis();
            log(TAG, "commited " + hits + " calllogs, time used:" + (end - start) + ",numDocs:"
                    + mIndexWriter.numDocs());
        } catch (Exception e) {
            android.util.Log.w(TAG, e.toString(), e);
        }
        return hits;
    }

    @Override
    public long rebuildContacts(boolean urgent) {
        long start = System.currentTimeMillis();
        ContentResolver cr = mContext.getContentResolver();
        Cursor cur = cr.query(Phone.CONTENT_URI, new String[] {
                Phone.CONTACT_ID, // 0
                Phone.DISPLAY_NAME, // 1
                Phone.NUMBER, // 2
                Phone.STARRED, // 3
                Phone.TIMES_CONTACTED, // 4
                Phone.LAST_TIME_CONTACTED, // 5
        }, null, null, String.format("%s DESC, %s DESC, %s DESC", Phone.STARRED,
                Phone.LAST_TIME_CONTACTED, Phone.TIMES_CONTACTED));
        long hits = 0;
        PinyinConverter pinyinConverter = PinyinConverter.getInstance(mContext);

        Document document = new Document();
        Field idField = createStringField(FIELD_ID, "");
        Field typeField = createStringField(FIELD_TYPE, INDEX_TYPE_CONTACT);
        Field nameField = createStringField(FIELD_NAME, "");
        Field numberField = createTextField(FIELD_NUMBER, "");
        Field pinyinField = createTextField(FIELD_PINYIN, "");
        // Field t9Field = createTextField(FIELD_T9,"");

        document.add(pinyinField);
        // document.add(t9Field);
        document.add(nameField);
        document.add(numberField);
        document.add(idField);
        document.add(typeField);

        try {
            mIndexWriter.deleteDocuments(new Term(FIELD_TYPE, INDEX_TYPE_CONTACT));
            long current = System.currentTimeMillis();
            while (cur.moveToNext()) {
                hits++;
                try {
                    String id = cur.getString(0);
                    String name = cur.getString(1);
                    String number = stripNumber(cur.getString(2));
                    boolean starred = cur.getInt(3) != 0;
                    int contacted = cur.getInt(4);
                    long lastContacted = cur.getLong(5);
                    float boost = (starred ? 5.0f : 1.0f) + contacted
                            / (1.0f + (current - lastContacted) / (7.0f * ONE_DAY_IN_MILLISECONDS));
                    String pinyin = pinyinConverter.convert(name, true);
                    idField.setStringValue(id);
                    numberField.setStringValue(number);
                    nameField.setStringValue(name);
                    // t9Field.setStringValue(pinyin);
                    pinyinField.setStringValue(pinyin);

                    pinyinField.setBoost(boost);
                    // t9Field.setBoost(boost);
                    numberField.setBoost(boost);
                    mIndexWriter.addDocument(document);
                    if (!urgent) {
                        yieldInterrupt();
                    }
                } catch (Exception e) {
                    log(TAG, e.toString());
                }
            }
            cur.close();
            Map<String, String> userData = new HashMap<String, String>();
            userData.put("action", "rebuild-contacts");
            userData.put("time", String.valueOf(System.currentTimeMillis()));
            mIndexWriter.commit(userData);
            long end = System.currentTimeMillis();
            log(TAG, "commited " + hits + " contacts, time used:" + (end - start) + ",numDocs:"
                    + mIndexWriter.numDocs());
        } catch (Exception e) {
            android.util.Log.w(TAG, e.toString(), e);
        }
        return hits;
    }

    @Override
    protected void log(String tag, String msg) {
        android.util.Log.d(tag, msg);

    }

    @Override
    protected long rebuildAllApps(boolean urgent) {
        PackageManager pm = mContext.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> appList = pm.queryIntentActivities(intent, 0);

        long hits = 0;
        PinyinConverter pinyinConverter = PinyinConverter.getInstance(mContext);

        Document document = new Document();
        Field typeField = createStringField(FIELD_TYPE, INDEX_TYPE_APPS);
        Field labelField = createStringField(FIELD_NAME, "");
        Field pkgField = createStringField(FIELD_PKG, "");
        Field activityFiled = createStringField(FIELD_ACTIVITY, "");
        Field pinyinField = createTextField(FIELD_PINYIN, "");

        document.add(pinyinField);
        document.add(labelField);
        document.add(pkgField);
        document.add(activityFiled);
        document.add(typeField);

        try {
            mIndexWriter.deleteDocuments(new Term(FIELD_TYPE, INDEX_TYPE_APPS));
            for (ResolveInfo info : appList) {
                hits++;
                String label = info.loadLabel(pm).toString();
                String pinyin = pinyinConverter.convert(label, true);
                String pkg = info.activityInfo.applicationInfo.packageName;
                String activity = info.activityInfo.name;
                long installedDate = pm.getPackageInfo(pkg, 0).firstInstallTime;

                labelField.setStringValue(label);
                pkgField.setStringValue(pkg);
                pinyinField.setStringValue(pinyin);
                activityFiled.setStringValue(activity);
                Log.e(TAG, "rebuildAllApps: label:" + label + "; pkg:" + pkg + "; pingyin:"
                        + pinyin + "; activity:" + activity);

                mIndexWriter.addDocument(document);
                mRecentlyInstalledAppsList.put(pinyin, new SearchResultItem(new AppInfo(label, pkg,
                        label, activity, installedDate), null, false));
                if (!urgent) {
                    yieldInterrupt();
                }
            }
            Map<String, String> userData = new HashMap<String, String>();
            userData.put("action", "rebuild-allapps");
            mIndexWriter.commit(userData);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // call the callback to tell them async build complete.
        if (mAsyncBuildCompleteCallback != null && mAsyncBuildCompleteCallback.get() != null) {
            mAsyncBuildCompleteCallback.get().onAsyncBuildComplete(getRecentlyInstalled6Apps());
        }
        return hits;
    }

    @Override
    public List<SearchResultItem> getRecentlyInstalled6Apps() {
        ArrayList<SearchResultItem> resultList = new ArrayList<SearchResultItem>();
        ArrayList<SearchResultItem> result6Apps = new ArrayList<SearchResultItem>();

        if (mRecentlyInstalledAppsList != null && mRecentlyInstalledAppsList.size() > 0) {
            resultList.addAll(mRecentlyInstalledAppsList.values());
            Collections.sort(resultList, APP_INSTALLEDDATE_COMPARATOR);
            for (int i = 0; i < 6; i++) {
                result6Apps.add(resultList.get(i));
            }
        }

        return result6Apps;
    }

    public Comparator<SearchResultItem> APP_INSTALLEDDATE_COMPARATOR = new Comparator<SearchResultItem>() {
        @Override
        public int compare(SearchResultItem first, SearchResultItem second) {
            if (first.appInfo.installedDate >= second.appInfo.installedDate) {
                return -1;
            } else {
                return 1;
            }
        }
    };

    @Override
    public void updatePackage(String pkg) {
        SearchResultItem info = getSearchResultItemByPkg(pkg);
        if (info != null) {
            mRecentlyInstalledAppsList.put(pkg, info);
        }
    }

    public SearchResultItem getSearchResultItemByPkg(String pkg) {
        AppInfo info = null;
        SearchResultItem searchResultItem = null;
        try {
            PackageManager pm = mContext.getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> appList = pm.queryIntentActivities(intent, 0);
            for (ResolveInfo resolveInfo : appList) {
                String currentpkg = resolveInfo.activityInfo.applicationInfo.packageName;
                if (currentpkg.equals(pkg)) {
                    String label = resolveInfo.loadLabel(pm).toString();
                    String activity = resolveInfo.activityInfo.name;
                    long installedDate = pm.getPackageInfo(pkg, 0).firstInstallTime;
                    info = new AppInfo(label, pkg, label, activity, installedDate);
                    searchResultItem = new SearchResultItem(info, null, false);
                }
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return searchResultItem;
    }

    @Override
    public void removePakcage(String pkg) {
        mRecentlyInstalledAppsList.remove(pkg);
    }

}
