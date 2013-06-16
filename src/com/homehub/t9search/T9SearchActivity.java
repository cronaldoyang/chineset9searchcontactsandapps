
package com.homehub.t9search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.homehub.t9search.dao.DBManager;
import com.homehub.t9search.mode.SearchResultItem;
import com.homehub.t9search.pubapi.IAsyncBuildComplete;
import com.homehub.t9search.pubapi.ISearchComplet;
import com.homehub.t9search.service.AbstractSearchService;
import com.homehub.t9search.service.SearchService;

public class T9SearchActivity extends Activity implements IAsyncBuildComplete {
    private static final String TAG = T9SearchActivity.class.getSimpleName();

    private static final int MAX_HITS = 20;

    private StringBuilder mInputStrBuilder = null;

    private AbstractSearchService mSearchService;

    private HorizontalScrollView mList;

    private TextView mNoSearchResultPage;

    private LinearLayout mSearchResultContainer;

    private List<SearchResultItem> mResults = new ArrayList<SearchResultItem>();

    private Handler mHandler = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quicksearch);
        initVariables();
        initView();
        doAsyncBuild();
    }

    private void initVariables() {
        mHandler = new Handler(Looper.getMainLooper());
        mSearchService = SearchService.getInstance(this);
        mSearchService.setAsyncBuildCompleteCallback(this);
        mInputStrBuilder = new StringBuilder();
    }

    private void initView() {
        initSoftKeyboard();
        mNoSearchResultPage = (TextView) findViewById(R.id.no_searchresultpage);
        mNoSearchResultPage.setOnClickListener(mBtnClickListener);
        mList = ((HorizontalScrollView) findViewById(R.id.list));
        mSearchResultContainer = (LinearLayout) findViewById(R.id.search_result_container);
    }

    private void initSoftKeyboard() {
        View clearV = (View) findViewById(R.id.btn_numpad_clear);
        View num2V = (View) findViewById(R.id.btn_numpad_2);
        View num3V = (View) findViewById(R.id.btn_numpad_3);
        View num4V = (View) findViewById(R.id.btn_numpad_4);
        View num5V = (View) findViewById(R.id.btn_numpad_5);
        View num6V = (View) findViewById(R.id.btn_numpad_6);
        View num7V = (View) findViewById(R.id.btn_numpad_7);
        View num8V = (View) findViewById(R.id.btn_numpad_8);
        View num9V = (View) findViewById(R.id.btn_numpad_9);
        setNumpadBtnNumber(num2V, "2", "ABC");
        setNumpadBtnNumber(num3V, "3", "DEF");
        setNumpadBtnNumber(num4V, "4", "GHI");
        setNumpadBtnNumber(num5V, "5", "JKL");
        setNumpadBtnNumber(num6V, "6", "MNO");
        setNumpadBtnNumber(num7V, "7", "PQRS");
        setNumpadBtnNumber(num8V, "8", "TUV");
        setNumpadBtnNumber(num9V, "9", "WXYZ");

        num2V.setOnClickListener(mBtnClickListener);
        num3V.setOnClickListener(mBtnClickListener);
        num4V.setOnClickListener(mBtnClickListener);
        num5V.setOnClickListener(mBtnClickListener);
        num6V.setOnClickListener(mBtnClickListener);
        num7V.setOnClickListener(mBtnClickListener);
        num8V.setOnClickListener(mBtnClickListener);
        num9V.setOnClickListener(mBtnClickListener);
        clearV.setOnClickListener(mBtnClickListener);
    }

    private void setNumpadBtnNumber(View v, String number, String number_chars) {
        TextView numberV = (TextView) v.findViewById(R.id.lbl_number);
        TextView enChars = (TextView) v.findViewById(R.id.lbl_en_chars);
        numberV.setText(number);
        enChars.setText(number_chars);
    }

    private String getNumberByView(View v) {
        return ((TextView) v.findViewById(R.id.lbl_number)).getText().toString();
    }

    private void doAsyncBuild() {
        mSearchService.asyncRebuild(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onStop");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onResume");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDestroy");
        }
    }

    View.OnClickListener mBtnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_numpad_clear:
                    resetIputStr();
                    break;
                case R.id.no_searchresultpage:
                    resetIputStr();
                    break;
                default:
                    buildSearchStr(getNumberByView(view));
                    break;
            }
        }
    };

    private void buildSearchStr(String c) {
        c = c.toLowerCase(Locale.CHINA);
        mInputStrBuilder.append(c);
        if (BuildConfig.DEBUG) {
            Log.e(TAG, "addChar: str:" + mInputStrBuilder.toString());
        }
        search(mInputStrBuilder.toString());
    }

    private void resetIputStr() {
        setTheVisibilityOfNoSearchResultPage(false);
        mInputStrBuilder.delete(0, mInputStrBuilder.length());
        if (BuildConfig.DEBUG) {
            Log.e(TAG, "resetIputStr: length:" + mInputStrBuilder.length());
        }
        mSearchResultContainer.removeAllViews();
    }

    private void search(String query) {
        ISearchComplet searchCallback = new ISearchComplet() {
            private long start = System.currentTimeMillis();

            @Override
            public void onSearchResult(String query, long hits, final List<SearchResultItem> result) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        boolean showNoSearchResultPage = result.size() > 0 ? false : true;
                        if (!showNoSearchResultPage) {
                            bindSearachResultData(result);
                        }
                        setTheVisibilityOfNoSearchResultPage(showNoSearchResultPage);
                    }
                });
                if (BuildConfig.DEBUG) {
                    Log.v(SearchService.TAG, "query:" + query + ",result: " + result.size()
                            + ",time used:" + (System.currentTimeMillis() - start));
                }
            }
        };
        mSearchService.query(query, MAX_HITS, true, searchCallback);
    }

    private void setTheVisibilityOfNoSearchResultPage(boolean visibility) {
        if (visibility) {
            mNoSearchResultPage.setVisibility(View.VISIBLE);
            mList.setVisibility(View.GONE);
        } else {
            mNoSearchResultPage.setVisibility(View.GONE);
            mList.setVisibility(View.VISIBLE);
        }
    }

    private void bindRecentlyInstalledApps(List<SearchResultItem> recentlyInstalledAppsList) {
        bindSearachResultData(recentlyInstalledAppsList);
    }

    private void bindSearachResultData(List<SearchResultItem> result) {
        mResults.clear();
        mResults.addAll(result);
        mSearchResultContainer.removeAllViews();

        for (int i = 0; i < result.size(); i++) {
            View v = getSearchResulItemView(i);
            final int position = i;
            v.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    handleClick(position);
                }
            });
            mSearchResultContainer.addView(v);
        }
    }

    private View getSearchResulItemView(int position) {
        View rowView = LayoutInflater.from(this).inflate(R.layout.list_item, null, false);
        if (BuildConfig.DEBUG) {
            Log.e(TAG, "getSearchResultItemView:" + position + "; name:" + getNameStr(position));
        }
        ((TextView) rowView.findViewById(R.id.name)).setText(getNameStr(position));
        Drawable icon = null;
        if (checkIsApp(position)) {
            icon = getAppIcon(position);
        } else {
            icon = DBManager.getPeoplePhotoByPhoneNumber(this, getPhoneNumberStr(position));
        }
        ((ImageView) rowView.findViewById(R.id.icon)).setImageDrawable(icon);
        return rowView;
    }

    protected Spanned getNameStr(int position) {
        SearchResultItem searchRes = mResults.get(position);
        StringBuilder nameBuilder = new StringBuilder();

        String name = searchRes.appInfo.name;
        String pingyin = searchRes.appInfo.pingyin;
        if (name != null && !name.equals("")) {
            nameBuilder.append(searchRes.appInfo.name);
        }

        if (pingyin != null && !pingyin.equals("")) {
            nameBuilder.append(searchRes.appInfo.pingyin);
        }
        return Html.fromHtml(nameBuilder.toString());
    }

    protected String getPhoneNumberStr(int position) {
        SearchResultItem searchRes = mResults.get(position);
        return searchRes != null ? searchRes.number : "";
    }

    protected boolean checkIsApp(int position) {
        SearchResultItem searchRes = mResults.get(position);
        boolean ret = searchRes.isContacts ? false : true;
        return ret;
    }

    protected Drawable getAppIcon(int position) {
        SearchResultItem searchRes = mResults.get(position);
        Drawable icon = null;
        try {
            PackageManager pm = this.getPackageManager();
            String pkg = searchRes.appInfo.pkg;
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "getAppIcon: pkg:" + pkg);
            }
            icon = pm.getApplicationIcon(pkg);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return icon;
    }

    protected void naviToApp(int position) {
        SearchResultItem searchRes = mResults.get(position);
        String pkg = searchRes.appInfo.pkg;
        String activity = searchRes.appInfo.activity;
        if (BuildConfig.DEBUG) {
            Log.e(TAG, "naviToApp: pkg:" + pkg + "; activity:" + activity);
        }
        ComponentName name = new ComponentName(pkg, activity);
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        i.setComponent(name);
        this.startActivity(i);
    }

    protected void naviTocall(int position) {
        SearchResultItem searchRes = mResults.get(position);
        String number = searchRes.number;
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.fromParts("tel", number, null));
        this.startActivity(intent);
    }

    public void handleClick(int position) {
        if (checkIsApp(position)) {
            naviToApp(position);
        } else {
            naviTocall(position);
        }
    }

    @Override
    public void onAsyncBuildComplete(final List<SearchResultItem> result) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onAsyncBuildComplete: result:" + result.size());
            Log.d(TAG, "onAsyncBuildComplete: itemname:" + result.get(0));
        }
        mResults.clear();
        mResults.addAll(result);
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                bindRecentlyInstalledApps(result);
            }
        });
    }

}
