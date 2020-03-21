package app3cm.mg2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.i18next.android.I18Next;

public class GamingActivity extends AppCompatActivity {

    private Context mContext;
    private I18Next mI18Next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initUi();
    }

    private void initUi() {
        setContentView(R.layout.activity_gaming);

        mContext = this;

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        mI18Next = I18Next.getInstance();
        mI18Next.loadFromPreference(sharedPref);

        findViewById(R.id.button_buy).setOnClickListener((view) -> startBillingActivity());
    }

    private void startPayActivity() {
        Intent intent = new Intent(this, PaymentActivity.class);
        startActivity(intent);
    }

    private void startBillingActivity() {
        Intent intent = new Intent(this, BillingActivity.class);
        startActivity(intent);
    }

    public void onBackPressed() {
        // do not return to MainActivity
        CommonUtil.promptLeaveAppDialog(mContext, mI18Next);
    }

}
