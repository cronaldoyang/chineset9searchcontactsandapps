
package weiweiwang.github.quickdialer;

import java.util.ArrayList;
import java.util.List;
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
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;

public class DialerActivity extends Activity implements AdapterView.OnItemClickListener {
    private static final String TAG = DialerActivity.class.getSimpleName();

    private static final int MAX_HITS = 20;

    /**
     * Called when the activity is first created.
     */
    private AbstractSearchService mSearchService;

    private EditText mEditText;

    private Gallery mList;

    private ResultAdapter mResultAdapter = new ResultAdapter();

    private Handler mHandler = new Handler(Looper.getMainLooper());

    TextWatcher mTextWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int arg1, int arg2, int arg3) {
        }

        @Override
        public void onTextChanged(CharSequence s, int arg1, int arg2, int arg3) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            String text = s.toString();
            if (TextUtils.isEmpty(text)) {
                search(null);
            } else if ("*#*".equals(text)) {
                mSearchService.asyncRebuild(true);
            } else if (text.indexOf('*') == -1) {
                search(text);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mEditText = (EditText) findViewById(R.id.input_widget);
        mEditText.addTextChangedListener(mTextWatcher);
        mList = ((Gallery) findViewById(R.id.list));
        mList.setAdapter(mResultAdapter);
        mList.setOnItemClickListener(this);
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
                holder.icon.setImageDrawable(getAppIcon(position));
                holder.number.setVisibility(View.GONE);
            } else {
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

    }

}
