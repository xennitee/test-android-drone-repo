package app3cm.mg2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.i18next.android.I18Next;

import org.json.JSONException;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MG2";

    private Context mContext;
    private I18Next mI18Next;
    private static final String I18NEXT_APP_EN_FILE = "i18next/android_common_en.json";
    private static final String I18NEXT_APP_CH_FILE = "i18next/android_common_ch.json";
    private static final String I18NEXT_CH_FILE = "ch.json";

    private static final int REQUEST_SIGN_IN_CODE = 33333;
    private static boolean isSignInFinished = false;

    public MainActivity() {
        mContext = this;
        mI18Next = I18Next.getInstance();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(CommonUtil.TAG, "onCreate isSignInFinished: " + isSignInFinished);

        if (getIntent().getBooleanExtra("SHOULD_FINISH_APP", false)) {
            finish();
            return;
        }

        initUi();
        //startLoginActivity();
        if (!isSignInFinished) { startLoginActivity(); }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(CommonUtil.TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(CommonUtil.TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(CommonUtil.TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(CommonUtil.TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(CommonUtil.TAG, "onDestroy");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Log.d(CommonUtil.TAG, "requestCode: " + requestCode);
        Log.d(CommonUtil.TAG, "resultCode: " + resultCode);

        if (requestCode == REQUEST_SIGN_IN_CODE) {
            isSignInFinished = true;
            startGamingActivity();
        }
    }

    private void initUi() {
        setContentView(R.layout.activity_main);

        mI18Next = I18Next.getInstance();
        try {
            mI18Next.loader()
                    .from(CommonUtil.getAssetJsonData(mContext, I18NEXT_APP_CH_FILE))
                    .lang("ch")
                    .namespace("generic")
                    .load();

            mI18Next.loader()
                    .from(CommonUtil.getAssetJsonData(mContext, I18NEXT_CH_FILE))
                    .lang("ch")
                    .namespace("play")
                    .load();

            mI18Next.loader()
                    .from(CommonUtil.getAssetJsonData(mContext, I18NEXT_APP_EN_FILE))
                    .lang("en")
                    .namespace("generic")
                    .load();
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
        mI18Next.getOptions()
                .setInterpolationPrefix("{{")
                .setInterpolationSuffix("}}")
                .setFallbackLanguage("en")
                .setLanguage("ch")
                .setDefaultNamespace("generic")
                .setNamespaces("play", "generic");

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        mI18Next.saveInPreference(sharedPref);
    }

    private void startLoginActivity() {
        Log.d(CommonUtil.TAG, "startLoginActivity");
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, REQUEST_SIGN_IN_CODE);
    }

    private void startGamingActivity() {
        Intent intent = new Intent(this, GamingActivity.class);
        startActivity(intent);
    }

}
