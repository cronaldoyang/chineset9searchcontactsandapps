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

public class DialerActivity extends Activity implements
		AdapterView.OnItemClickListener {
	private static final String TAG = DialerActivity.class.getSimpleName();

	private static final int MAX_HITS = 20;

	/**
	 * Called when the activity is first created.
	 */
	private AbstractSearchService mSearchService;

	private TextView mInput;

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
		mInput = (TextView) findViewById(R.id.digits);
		mList = ((Gallery) findViewById(R.id.list));
		mList.setAdapter(mResultAdapter);
		mList.setOnItemClickListener(this);
	}

	private void initSoftKeyboard() {
		((TextView) findViewById(R.id.btn1)).setText(getKeyboardNumberStr("C", "lear"));
		((TextView) findViewById(R.id.btn2)).setText(getKeyboardNumberStr("2", "ABC"));
		((TextView) findViewById(R.id.btn3)).setText(getKeyboardNumberStr("3", "DEF"));
		((TextView) findViewById(R.id.btn4)).setText(getKeyboardNumberStr("4", "GHI"));
		((TextView) findViewById(R.id.btn5)).setText(getKeyboardNumberStr("5", "JKL"));
		((TextView) findViewById(R.id.btn6)).setText(getKeyboardNumberStr("6", "MNO"));
		((TextView) findViewById(R.id.btn7)).setText(getKeyboardNumberStr("7", "PQRS"));
		((TextView) findViewById(R.id.btn8)).setText(getKeyboardNumberStr("8", "TUV"));
		((TextView) findViewById(R.id.btn9)).setText(getKeyboardNumberStr("9", "WXYZ"));
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

	public void onBtnClick(View view) {
		switch (view.getId()) {
		case R.id.deleteButton:
			delChar();
			break;
		default:
			addChar(((TextView) view).getText().toString());
		}
		String searchText = mInput.getText().toString();
		if (TextUtils.isEmpty(searchText)) {
			search(null);
		} else if ("*#*".equals(searchText)) {
			mSearchService.asyncRebuild(true);
		} else if (searchText.indexOf('*') == -1) {
			search(searchText);
		}
	}

	private void addChar(String c) {
		c = c.toLowerCase(Locale.CHINA);
		mInput.setText(mInput.getText() + String.valueOf(c.charAt(0)));
	}

	private void delChar() {
		String text = mInput.getText().toString();
		if (text.length() > 0) {
			text = text.substring(0, text.length() - 1);
			mInput.setText(text);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
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
				Log.v(SearchService.TAG,
						"query:" + query + ",result: " + result.size()
								+ ",time used:"
								+ (System.currentTimeMillis() - start));
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

		spannable.setSpan(spanBig, 0, bigEnd,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		spannable.setSpan(spanSmall, smallStart, smallEnd,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
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
				rowView = LayoutInflater.from(getBaseContext()).inflate(
						R.layout.list_item, parent, false);
				ViewHolder viewHolder = new ViewHolder();
				viewHolder.name = (TextView) rowView.findViewById(R.id.name);
				viewHolder.number = (TextView) rowView
						.findViewById(R.id.number);
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
				nameBuilder.append(searchRes.get(SearchService.FIELD_NAME)
						.toString());
			} else {
				nameBuilder.append("Not contains*****");
			}
			nameBuilder.append(' ');
			if (searchRes.containsKey(SearchService.FIELD_PINYIN)) {
				nameBuilder.append(searchRes.get(SearchService.FIELD_PINYIN)
						.toString());
			}
			return Html.fromHtml(nameBuilder.toString());
		}

		protected Spanned getNumberStr(int position) {
			Map<String, Object> searchRes = items.get(position);
			StringBuilder numberBuilder = new StringBuilder();
			if (searchRes.containsKey(SearchService.FIELD_HIGHLIGHTED_NUMBER)) {
				numberBuilder.append(searchRes.get(
						SearchService.FIELD_HIGHLIGHTED_NUMBER).toString());
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
			String activity = searchRes.get(SearchService.FIELD_ACTIVITY)
					.toString();
			Log.e(TAG, "naviToApp: pkg:" + pkg + "; activity:" + activity);
			ComponentName name = new ComponentName(pkg, activity);
			Intent i = new Intent(Intent.ACTION_MAIN);
			i.addCategory(Intent.CATEGORY_LAUNCHER);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			i.setComponent(name);
			startActivity(i);
		}

		protected void naviTocall(int position) {
			Map<String, Object> searchRes = items.get(position);
			String number = searchRes.get(SearchService.FIELD_NUMBER)
					.toString();
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
