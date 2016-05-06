package jp.ac.titech.itpro.sdl.suggest;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Xml;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import org.xmlpull.v1.XmlPullParser;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private EditText inputText;
    private ArrayAdapter<String> resultAdapter;
    private ArrayList<String> result;
    private final static String KEY_RESULT = "MainActivity.result";

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        @SuppressWarnings("unchecked")
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case MSG_RESULT:
                    resultAdapter.clear();
                    resultAdapter.addAll((List<String>) message.obj);
                    break;
            }
            return false;
        }
    });

    private final static int MSG_RESULT = 1111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputText = (EditText) findViewById(R.id.input_text);
        ListView resultList = (ListView) findViewById(R.id.result_list);
        assert resultList != null;

        if (savedInstanceState != null)
            result = savedInstanceState.getStringArrayList(KEY_RESULT);
        if (result == null)
            result = new ArrayList<>();

        resultAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, result);
        resultList.setAdapter(resultAdapter);
        resultList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                String text = (String) parent.getItemAtPosition(pos);
                Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                intent.putExtra(SearchManager.QUERY, text);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(KEY_RESULT, result);
    }

    public void onClickSuggestButton(View view) {
        new SuggestionsThread().start();
    }

    private class SuggestionsThread extends Thread {
        @Override
        public void run() {
            List<String> result = new ArrayList<>();
            HttpURLConnection conn = null;
            String error = null;
            try {
                String query = URLEncoder.encode(inputText.getText().toString().trim(), "UTF-8");
                URL url = new URL(getString(R.string.suggest_url, query));
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setDoInput(true);
                conn.connect();
                XmlPullParser xpp = Xml.newPullParser();
                xpp.setInput(conn.getInputStream(), "UTF-8");
                for (int et = xpp.getEventType(); et != XmlPullParser.END_DOCUMENT; et = xpp.next()) {
                    if (et == XmlPullParser.START_TAG && xpp.getName().equalsIgnoreCase("suggestion")) {
                        for (int i = 0; i < xpp.getAttributeCount(); i++)
                            if (xpp.getAttributeName(i).equalsIgnoreCase("data"))
                                result.add(xpp.getAttributeValue(i));
                    }
                }
            }
            catch (Exception e) {
                error = e.toString();
            }
            finally {
                if (conn != null)
                    conn.disconnect();
            }
            if (error != null) {
                result.clear();
                result.add(error);
            }
            if (result.size() == 0)
                result.add(getString(R.string.no_suggestions));
            handler.sendMessage(handler.obtainMessage(MSG_RESULT, result));
        }
    }
}
