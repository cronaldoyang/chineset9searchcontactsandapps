package com.homehub.t9search;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Gallery;
import android.widget.TextView;

import com.homehub.t9search.adapter.T9SearchResultAdapter;
import com.homehub.t9search.service.AbstractSearchService;
import com.homehub.t9search.service.SearchCallback;
import com.homehub.t9search.service.SearchService;

public class T9SearchActivity extends Activity implements AdapterView.OnItemClickListener {
    private static final String TAG = T9SearchActivity.class.getSimpleName();

    private static final int MAX_HITS = 20;

    private StringBuilder mInputStrBuilder = null;

    private AbstractSearchService mSearchService;

    private Gallery mList;

    private T9SearchResultAdapter mResultAdapter = null;

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
        mInputStrBuilder = new StringBuilder();
        mResultAdapter = new T9SearchResultAdapter(this);
    }

    private void initView() {
        initSoftKeyboard();
        mList = ((Gallery) findViewById(R.id.list));
        mList.setAdapter(mResultAdapter);
        mList.setOnItemClickListener(this);
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
        mInputStrBuilder.delete(0, mInputStrBuilder.length());
        if (BuildConfig.DEBUG) {
            Log.e(TAG, "resetIputStr: length:" + mInputStrBuilder.length());
        }
        mResultAdapter.resetData();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mResultAdapter.handleClick(position);
    }

    private void search(String query) {
        SearchCallback searchCallback = new SearchCallback() {
            private long start = System.currentTimeMillis();

            @Override
            public void onSearchResult(String query, long hits,
                    final List<Map<String, String>> result) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mResultAdapter.updateData(result);
                        mResultAdapter.notifyDataSetChanged();
                        mList.setSelection(0);
                    }
                });
                Log.v(SearchService.TAG, "query:" + query + ",result: " + result.size()
                        + ",time used:" + (System.currentTimeMillis() - start));
            }
        };
        mSearchService.query(query, MAX_HITS, true, searchCallback);
    }

}
