package app3cm.mg2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.BillingResponseCode;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.i18next.android.I18Next;

import java.util.ArrayList;
import java.util.List;

public class BillingActivity extends AppCompatActivity implements PurchasesUpdatedListener,
        BillingClientStateListener, SkuDetailsResponseListener {

    private Context mContext;
    private I18Next mI18Next;

    private BillingClient billingClient;
    private String idProduct = "";

    private Button mBillUpButton;
    private TextView tvGooglePlayBillingStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
        initUi();

        // Connect to Google Play
        createBillingClient();

    }

    private void initUi() {
        setContentView(R.layout.activity_billing);

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        mI18Next = I18Next.getInstance();
        mI18Next.loadFromPreference(sharedPref);

        mBillUpButton = findViewById(R.id.button_bill_up);
        mBillUpButton.setClickable(false);
        tvGooglePlayBillingStatus = findViewById(R.id.text_googleplaybilling_status);
        tvGooglePlayBillingStatus.setText(mI18Next.t("pay.prepare_for_google_pay"));

        ((RadioButton) findViewById(R.id.radio_billing_pkg_30)).setChecked(true);
        idProduct = "app.3cm.mg2.pkg1";

        /*mBillUpButton.setOnClickListener((view) -> {
            mBillUpButton.setClickable(false);
        });*/
    }

    public void onRadioButtonClicked(View view) {
        Log.d(CommonUtil.TAG, "onRadioButtonClicked");
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        Log.d(CommonUtil.TAG, "checked: " + checked);

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_billing_pkg_30:
                if (checked) {
                    idProduct = "android.test.purchased";
                }
                break;
            case R.id.radio_billing_pkg_60:
                if (checked) {
                    idProduct = "android.test.canceled";
                }
                break;
            case R.id.radio_billing_pkg_90:
                if (checked) {
                    idProduct = "android.test.item_unavailable";
                }
                break;
            case R.id.radio_billing_pkg_300:
                if (checked) {
                    idProduct = "android.test.refunded";
                }
                break;
            case R.id.radio_billing_pkg_999:
                if (checked) {
                    idProduct = "android.test.purchased";
                }
                break;
            case R.id.radio_billing_pkg_infinite_ntd:
                if (checked) {
                    idProduct = "android.test.canceled";
                }
                break;
        }
        Log.d(CommonUtil.TAG, "idProduct: " + idProduct);
    }

    private void createBillingClient() {
        Log.d(CommonUtil.TAG, "createBillingClient");
        billingClient = BillingClient.newBuilder(mContext)
                .setListener(this)
                .enablePendingPurchases()
                .build();
        billingClient.startConnection(this);
    }

    // BillingClient.startConnection(BillingClientStateListener listener)
    @Override
    public void onBillingSetupFinished(BillingResult billingResult) {
        Log.d(CommonUtil.TAG, "onBillingSetupFinished");
        Log.d(CommonUtil.TAG, "responseCode: " + billingResult.getResponseCode());
        if (billingResult.getResponseCode() ==  BillingResponseCode.OK) {
            // The BillingClient is ready. You can query purchases here.
            querySkuDetails();
        }
    }

    @Override
    public void onBillingServiceDisconnected() {
        Log.w(CommonUtil.TAG, "onBillingServiceDisconnected");
        // Try to restart the connection on the next request to
        // Google Play by calling the startConnection() method.
        billingClient.startConnection(this);
    }

    private void querySkuDetails() {
        List<String> skuList = new ArrayList<>();
        skuList.add("app.3cm.mg2.pkg1");
        skuList.add("app.3cm.mg2.pkg2");
        skuList.add("app.3cm.mg2.pkg3");
        skuList.add("android.test.purchased");
        skuList.add("android.test.canceled");
        skuList.add("android.test.item_unavailable");
        skuList.add("android.test.refunded");

        SkuDetailsParams params = SkuDetailsParams.newBuilder()
                .setType(SkuType.INAPP)
                .setSkusList(skuList)
                .build();

        billingClient.querySkuDetailsAsync(params, this);
    }

    // BillingClient.querySkuDetailsAsync(..., SkuDetailsResponseListener listener)
    @Override
    public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
        Log.d(CommonUtil.TAG, "onSkuDetailsResponse");
        if (billingResult == null) {
            Log.wtf(CommonUtil.TAG, "onSkuDetailsResponse: null BillingResult");
            return;
        }

        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Log.d(CommonUtil.TAG, "responseCode: " + responseCode);
        Log.d(CommonUtil.TAG, "debugMessage: " + debugMessage);

        // Process the result.
        switch (responseCode) {
            case BillingResponseCode.OK:
                if (skuDetailsList != null) {
                    mBillUpButton.setOnClickListener((view) -> launchBillingFlow(skuDetailsList));
                    mBillUpButton.setClickable(true);
                } else {
                    Log.w(CommonUtil.TAG, "onSkuDetailsResponse: null SkuDetails list");
                }
                break;
            case BillingResponseCode.USER_CANCELED:
                Log.i(CommonUtil.TAG, "onSkuDetailsResponse: " + responseCode + " " + debugMessage);
                break;
            case BillingResponseCode.SERVICE_DISCONNECTED:
            case BillingResponseCode.SERVICE_UNAVAILABLE:
            case BillingResponseCode.BILLING_UNAVAILABLE:
            case BillingResponseCode.ITEM_UNAVAILABLE:
            case BillingResponseCode.DEVELOPER_ERROR:
            case BillingResponseCode.ERROR:
                Log.e(CommonUtil.TAG, "onSkuDetailsResponse: " + responseCode + " " + debugMessage);
                break;
            // These response codes are not expected.
            case BillingResponseCode.FEATURE_NOT_SUPPORTED:
            case BillingResponseCode.ITEM_ALREADY_OWNED:
            case BillingResponseCode.ITEM_NOT_OWNED:
            default:
                Log.wtf(CommonUtil.TAG, "onSkuDetailsResponse: " + responseCode + " " + debugMessage);
        }
    }

    private void launchBillingFlow(List<SkuDetails> skuDetailsList) {
        if (!billingClient.isReady()) {
            Log.e(CommonUtil.TAG, "launchBillingFlow: BillingClient is not ready");
        }

        SkuDetails skuDetails = skuDetailsList.get(0);
        for (SkuDetails details : skuDetailsList) {
            String sku = details.getSku();
            String price = details.getPrice();
            Log.d(CommonUtil.TAG, sku);
            Log.d(CommonUtil.TAG, price);
            Log.d(CommonUtil.TAG, details.getTitle());
            Log.d(CommonUtil.TAG, details.getDescription());
            Log.d(CommonUtil.TAG, details.getType());
            if (sku.equals(idProduct)) {
                skuDetails = details;
            }
        }
        Log.d(CommonUtil.TAG, "launched billing sku: " + skuDetails.getSku());
        BillingFlowParams params = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build();

        BillingResult billingResult = billingClient
                .launchBillingFlow(BillingActivity.this, params);
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Log.d(CommonUtil.TAG, "launchBillingFlow: BillingResponse " + responseCode + " " + debugMessage);
    }

    // BillingClient.setListener(PurchasesUpdatedListener listener)
    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult == null) {
            Log.wtf(CommonUtil.TAG, "onPurchasesUpdated: null BillingResult");
            return;
        }
        Log.d(CommonUtil.TAG, "onPurchasesUpdated");
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Log.d(CommonUtil.TAG, "responseCode: " + responseCode);
        Log.d(CommonUtil.TAG, "debugMessage: " + debugMessage);
        switch (responseCode) {
            case BillingResponseCode.OK:
                if (purchases == null) {
                    Log.i(CommonUtil.TAG, "onPurchasesUpdated: null purchase list");
                    //processPurchases(null);
                } else {
                    //processPurchases(purchases);
                    acknowledgeConsumablePurchase(purchases);
                }
                break;
            case BillingResponseCode.USER_CANCELED:
                Log.i(CommonUtil.TAG, "onPurchasesUpdated: User canceled the purchase");
                break;
            case BillingResponseCode.ITEM_ALREADY_OWNED:
                Log.i(CommonUtil.TAG, "onPurchasesUpdated: The user already owns this item");
                break;
            case BillingResponseCode.DEVELOPER_ERROR:
                Log.e(CommonUtil.TAG, "onPurchasesUpdated: Developer error means that Google Play " +
                        "does not recognize the configuration. If you are just getting started, " +
                        "make sure you have configured the application correctly in the " +
                        "Google Play Console. The SKU product ID must match and the APK you " +
                        "are using must be signed with release keys."
                );
                break;
        }
    }

    private void acknowledgeConsumablePurchase(List<Purchase> purchases) {
        Log.d(CommonUtil.TAG, "acknowledgeConsumablePurchase");

        Log.d(CommonUtil.TAG, "purchases.size(): " + purchases.size());
        for (Purchase purchase : purchases) {
            ConsumeParams params = ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken())
                    .build();
            billingClient.consumeAsync(params, new ConsumeResponseListener() {
                @Override
                public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
                    Log.d(CommonUtil.TAG, "onConsumeResponse");
                    Log.d(CommonUtil.TAG, "purchaseToken: " + purchaseToken);
                    int responseCode = billingResult.getResponseCode();
                    String debugMessage = billingResult.getDebugMessage();
                    Log.d(CommonUtil.TAG, "responseCode: " + responseCode);
                    Log.d(CommonUtil.TAG, "debugMessage: " + debugMessage);
                }
            });
        }
    }

}
