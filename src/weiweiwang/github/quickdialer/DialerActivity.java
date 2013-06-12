
package weiweiwang.github.quickdialer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import weiweiwang.github.search.AbstractSearchService;
import weiweiwang.github.search.SearchCallback;
import weiweiwang.github.search.SearchService;
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
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.CharacterStyle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;

public class DialerActivity extends Activity implements AdapterView.OnItemClickListener {
    private static final String TAG = DialerActivity.class.getSimpleName();

    private static final int MAX_HITS = 20;

    private StringBuilder mInputStrBuilder = new StringBuilder();

    private AbstractSearchService mSearchService;

    private Gallery mList;

    private ResultAdapter mResultAdapter = new ResultAdapter();

    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quicksearch);
        initView();
        doAsyncBuild();
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
        mSearchService = SearchService.getInstance(this);
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
                    final List<Map<String, Object>> result) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mResultAdapter.setItems(result);
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

    private static class ViewHolder {
        public ImageView icon;

        public TextView name;

        public TextView number;
    }

    private SpannableStringBuilder getKeyboardNumberStr(String bigStr, String smallStr) {
        SpannableStringBuilder spannable = new SpannableStringBuilder();
        spannable.append(bigStr).append(smallStr);
        CharacterStyle spanBig = new AbsoluteSizeSpan(30);
        CharacterStyle spanSmall = new AbsoluteSizeSpan(20);
        int bigEnd = bigStr.length() - 1;
        int smallStart = bigEnd + 1;
        int smallEnd = smallStart + smallStr.length();

        spannable.setSpan(spanBig, 0, bigEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(spanSmall, smallStart, smallEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }

    private class ResultAdapter extends BaseAdapter {
        private List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();

        @Override
        public synchronized int getCount() {
            return items.size();
        }

        public synchronized void setItems(List<Map<String, Object>> items) {
            this.items = items;
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            if (rowView == null) {
                rowView = LayoutInflater.from(getBaseContext()).inflate(R.layout.list_item, parent,
                        false);
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.name = (TextView) rowView.findViewById(R.id.name);
                viewHolder.number = (TextView) rowView.findViewById(R.id.number);
                viewHolder.icon = (ImageView) rowView.findViewById(R.id.icon);
                rowView.setTag(viewHolder);
            }
            ViewHolder holder = (ViewHolder) rowView.getTag();
            holder.name.setText(getNameStr(position));
            if (checkIsApp(position)) {
                holder.icon.setVisibility(View.VISIBLE);
                holder.icon.setImageDrawable(getAppIcon(position));
                holder.number.setVisibility(View.GONE);
            } else {
                holder.icon.setVisibility(View.GONE);
                holder.number.setText(getNumberStr(position));
                holder.number.setVisibility(View.VISIBLE);
            }
            return rowView;
        }

        protected Spanned getNameStr(int position) {
            Map<String, Object> searchRes = items.get(position);
            StringBuilder nameBuilder = new StringBuilder();
            if (searchRes.containsKey(SearchService.FIELD_NAME)) {
                nameBuilder.append(searchRes.get(SearchService.FIELD_NAME).toString());
            } else {
                nameBuilder.append("Not contains*****");
            }
            nameBuilder.append(' ');
            if (searchRes.containsKey(SearchService.FIELD_PINYIN)) {
                nameBuilder.append(searchRes.get(SearchService.FIELD_PINYIN).toString());
            }
            return Html.fromHtml(nameBuilder.toString());
        }

        protected Spanned getNumberStr(int position) {
            Map<String, Object> searchRes = items.get(position);
            StringBuilder numberBuilder = new StringBuilder();
            if (searchRes.containsKey(SearchService.FIELD_HIGHLIGHTED_NUMBER)) {
                numberBuilder.append(searchRes.get(SearchService.FIELD_HIGHLIGHTED_NUMBER)
                        .toString());
            } else if (searchRes.containsKey(SearchService.FIELD_NUMBER)) {
                numberBuilder.append(searchRes.get(SearchService.FIELD_NUMBER));
            }
            return Html.fromHtml(numberBuilder.toString());
        }

        protected boolean checkIsApp(int position) {
            Map<String, Object> searchRes = items.get(position);
            boolean ret = false;
            if (searchRes.containsKey(SearchService.FIELD_PKG)
                    && searchRes.containsKey(SearchService.FIELD_ACTIVITY)) {
                ret = true;
            }
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "checkIsApp:" + ret);
            }
            return ret;
        }

        protected Drawable getAppIcon(int position) {
            Map<String, Object> searchRes = items.get(position);
            Drawable icon = null;
            try {
                PackageManager pm = DialerActivity.this.getPackageManager();
                String pkg = searchRes.get(SearchService.FIELD_PKG).toString();
                Log.e(TAG, "getAppIcon: pkg:" + pkg);
                icon = pm.getApplicationIcon(pkg);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
            return icon;
        }

        protected void naviToApp(int position) {
            Map<String, Object> searchRes = items.get(position);
            String pkg = searchRes.get(SearchService.FIELD_PKG).toString();
            String activity = searchRes.get(SearchService.FIELD_ACTIVITY).toString();
            Log.e(TAG, "naviToApp: pkg:" + pkg + "; activity:" + activity);
            ComponentName name = new ComponentName(pkg, activity);
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            i.setComponent(name);
            startActivity(i);
        }

        protected void naviTocall(int position) {
            Map<String, Object> searchRes = items.get(position);
            String number = searchRes.get(SearchService.FIELD_NUMBER).toString();
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.fromParts("tel", number, null));
            startActivity(intent);
        }

        public void handleClick(int position) {
            if (checkIsApp(position)) {
                naviToApp(position);
            } else {
                naviTocall(position);
            }
        }

        protected void resetData() {
            items.clear();
            notifyDataSetChanged();
        }

    }

}
