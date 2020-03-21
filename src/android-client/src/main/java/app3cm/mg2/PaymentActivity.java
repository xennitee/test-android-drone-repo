package app3cm.mg2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.i18next.android.I18Next;
import com.stripe.android.ApiResultCallback;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.PaymentIntentResult;
import com.stripe.android.Stripe;
import com.stripe.android.model.ConfirmPaymentIntentParams;
import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodCreateParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class PaymentActivity extends AppCompatActivity {

    private Context mContext;
    private I18Next mI18Next;

    // TODO: this should be the domain of the backend server
    private static final String BACKEND_URL = "http://10.0.2.2:4242/";

    // Google Pay guide uses requestCode 991, but it says it is arbitrarily-picked
    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 991;
    // stripe SDK 14.1.0 uses requestCode 50000
    private static final int STRIPE_PAYMENT_API = 50000;
    private PaymentsClient mPaymentsClient;
    private OkHttpClient okHttpClient = new OkHttpClient();
    private Stripe stripe;
    private String paymentIntentClientSecret;

    private String merchPrice = "10.00";
    private String merchName = "Example Merchant";

    private Button mPayButton;
    private TextView tvGooglePayStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initUi();

        // Initialize a Google Pay API client for an environment suitable for testing.
        // It's recommended to create the PaymentsClient object inside of the onCreate method.
        // TODO: change ENVIRONMENT_TEST into ENVIRONMENT_PRODUCTION to go live
        mPaymentsClient = PaymentUtil.createPaymentsClient(this);
        isReadyToPay();

        // TODO: need a criteria to determine using webhook or not
        if (PaymentUtil.stripeFlowType == PaymentUtil.STRIPE_WITHOUT_USING_WEBHOOK) {
            getPublishableKey();
        } else {
            startCheckout();
        }
    }

    private void initUi() {
        setContentView(R.layout.activity_pay);

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        mI18Next = I18Next.getInstance();
        mI18Next.loadFromPreference(sharedPref);

        mPayButton = findViewById(R.id.button_pay);
        mPayButton.setClickable(false);
        tvGooglePayStatus = findViewById(R.id.text_googlepay_status);
        tvGooglePayStatus.setText(mI18Next.t("pay.prepare_for_google_pay"));

        ((RadioButton) findViewById(R.id.radio_pay_item_one_ntd)).setChecked(true);
        merchName = "1 TWD";
        merchPrice = "2000";

        mPayButton.setOnClickListener((view) -> {
            mPayButton.setClickable(false);
            payWithGoogle();
        });
    }

    public void onRadioButtonClicked(View view) {
        Log.d(CommonUtil.TAG, "onRadioButtonClicked");
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        Log.d(CommonUtil.TAG, "checked: " + checked);

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_pay_item_one_ntd:
                if (checked) {
                    merchName = "1 TWD";
                    merchPrice = "2000";
                }
                break;
            case R.id.radio_pay_item_five_ntd:
                if (checked) {
                    merchName = "5 TWD";
                    merchPrice = "2000";
                }
                break;
            case R.id.radio_pay_item_ten_ntd:
                if (checked) {
                    merchName = "10 TWD";
                    merchPrice = "2000";
                }
                break;
            case R.id.radio_pay_item_fifty_ntd:
                if (checked) {
                    merchName = "50 TWD";
                    merchPrice = "2000";
                }
                break;
            case R.id.radio_pay_item_ninenine_ntd:
                if (checked) {
                    merchName = "99 TWD";
                    merchPrice = "2000";
                }
                break;
            case R.id.radio_pay_item_infinite_ntd:
                if (checked) {
                    merchName = "Drain card";
                    merchPrice = "2000";
                }
                break;
        }
        Log.d(CommonUtil.TAG, "merchName: " + merchName);
    }

    private void isReadyToPay() {
        Log.d(CommonUtil.TAG, "isReadyToPay");
        IsReadyToPayRequest request = IsReadyToPayRequest.fromJson("");
        try {
            request = PaymentUtil.createIsReadyToPayRequest();
            Log.d(CommonUtil.TAG, "request " + request.toJson());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mPaymentsClient.isReadyToPay(request)
                .addOnCompleteListener((@NonNull Task<Boolean> task) -> {
                    Log.d(CommonUtil.TAG, "onComplete");
                    if (task.isSuccessful()) {
                        Log.d(CommonUtil.TAG, "task.isSuccessful()");
                        Log.d(CommonUtil.TAG, "task.getResult(): " + task.getResult());
                        // show Google Pay as payment option
                        if (task.getResult()) {
                            mPayButton.setClickable(true);
                            tvGooglePayStatus
                                    .setText(I18Next.getInstance().t("ready_for_google_pay"));
                        } else {
                            Log.w(CommonUtil.TAG, "PaymentsClient.isReadyToPay did not return true," +
                                    "usually the configuration went wrong");
                        }
                    } else {
                        // hide Google Pay as payment option
                        tvGooglePayStatus.setText(I18Next.getInstance().t("error_on_preparing_for_google_pay"));
                        Log.w(CommonUtil.TAG, "isReadyToPay failed, e:" + task.getException());
                    }
                });
    }

    private void payWithGoogle() {
        Log.d(CommonUtil.TAG, "payWithGoogle");
        PaymentDataRequest request = PaymentDataRequest.fromJson("");
        try {
            request = PaymentUtil.createPaymentDataRequest(this, merchName, merchPrice);
            Log.d(CommonUtil.TAG, "request: " + request.toJson());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        AutoResolveHelper.resolveTask(
                mPaymentsClient.loadPaymentData(request),
                this,
                LOAD_PAYMENT_DATA_REQUEST_CODE
        );
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Log.d(CommonUtil.TAG, "onActivityResult requestCode: " + requestCode + " resultCode: " + resultCode);
        // Handle any other startActivityForResult calls you may have made.
        if (requestCode == LOAD_PAYMENT_DATA_REQUEST_CODE) {
            switch (resultCode) {
                case Activity.RESULT_OK: {
                    if (intent != null) {
                        onGooglePayResult(intent);
                    }
                    break;
                }
                case Activity.RESULT_CANCELED: {
                    // Canceled
                    break;
                }
                case AutoResolveHelper.RESULT_ERROR: {
                    // Log the status for debugging
                    // Generally there is no need to show an error to
                    // the user as the Google Payment API will do that
                    Status status = AutoResolveHelper.getStatusFromIntent(intent);
                    Log.w(CommonUtil.TAG, "loadPaymentData failed");
                    Log.w(CommonUtil.TAG, "" + String.format("Error code: %d", status.getStatusCode()));
                    // TODO: google pay returns RESULT_ERROR, allow retrying
                    logPaymentEvent();
                    break;
                }
                default: {
                    // Do nothing.
                }
            }
        } else if (requestCode == STRIPE_PAYMENT_API) {
            if (PaymentUtil.stripeFlowType == PaymentUtil.STRIPE_WITHOUT_USING_WEBHOOK) {
                // Handle the result of stripe.confirmPayment or stripe.handleNextActionForPayment
                stripe.onPaymentResult(requestCode, intent, new StripeHandleNextActionForPaymentResultCallback(this));
            } else {
                // Handle the result of stripe.confirmPayment
                stripe.onPaymentResult(requestCode, intent,
                        new PaymentActivity.StripeConfirmPaymentResultCallback(this));
            }
        }
    }

    private void onGooglePayResult(@NonNull Intent intent) {
        Log.d(CommonUtil.TAG, "onGooglePayResult");
        // paymentData is a response object returned by Google after a payer approves payment.
        final PaymentData paymentData = PaymentData.getFromIntent(intent);
        if (paymentData == null) {
            Log.w(CommonUtil.TAG, "No paymentData, wth google?");
            // TODO: maybe treat this one as RESULT_ERROR?
            logPaymentEvent();
            return;
        }
        Log.d(CommonUtil.TAG, "paymentData: " + paymentData.toJson());

        try {
            PaymentMethodCreateParams paymentMethodCreateParams =
                    PaymentMethodCreateParams.createFromGooglePay(new JSONObject(paymentData.toJson()));

            if (PaymentUtil.stripeFlowType == PaymentUtil.STRIPE_WITHOUT_USING_WEBHOOK) {
                Log.d(CommonUtil.TAG, "STRIPE_WITHOUT_USING_WEBHOOK");
                stripe.createPaymentMethod(paymentMethodCreateParams,
                        new ApiResultCallback<PaymentMethod>() {
                            @Override
                            public void onSuccess(@NonNull PaymentMethod paymentMethod) {
                                Log.d(CommonUtil.TAG, "ApiResultCallback onSuccess");
                                // Create and confirm the PaymentIntent by calling the sample server's /pay endpoint.
                                pay(paymentMethod.id, null);
                            }

                            @Override
                            public void onError(@NonNull Exception e) {
                                Log.d(CommonUtil.TAG, "ApiResultCallback onError");
                                logPaymentEvent();
                                Toast.makeText(PaymentActivity.this, "Error: " + e.toString(), Toast.LENGTH_LONG)
                                        .show();
                            }
                        }
                );
            } else {
                Log.d(CommonUtil.TAG, "STRIPE_USING_WEBHOOK");
                ConfirmPaymentIntentParams confirmParams = ConfirmPaymentIntentParams
                        .createWithPaymentMethodCreateParams(
                                paymentMethodCreateParams, paymentIntentClientSecret);
                stripe.confirmPayment(this, confirmParams);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void logPaymentEvent() {

        mPayButton.setClickable(true);
    }

    private void onRetrievedKey(@NonNull String stripePublishableKey) {
        // Configure the SDK with your Stripe publishable key so that it can make requests to the Stripe API
        final Context applicationContext = getApplicationContext();
        PaymentConfiguration.init(applicationContext, stripePublishableKey);
        stripe = new Stripe(applicationContext, stripePublishableKey);
    }



    // payment using webhook
    //
    //
    //
    private void startCheckout() {
        // Create a PaymentIntent by calling the sample server's /create-payment-intent endpoint.
        MediaType mediaType = MediaType.get("application/json; charset=utf-8");
        String json = "{"
                + "\"currency\":\"twd\","
                + "\"items\":["
                + "{\"id\":" + "\"" + merchName + "\"}"
                + "]"
                + "}";
        RequestBody body = RequestBody.create(json, mediaType);
        Request request = new Request.Builder()
                .url(BACKEND_URL + "create-payment-intent")
                .post(body)
                .build();
        okHttpClient.newCall(request)
                .enqueue(new PaymentActivity.CreatePiCallback(this));
    }

    private static final class CreatePiCallback implements Callback {

        @NonNull private final WeakReference<PaymentActivity> activityRef;

        CreatePiCallback(@NonNull PaymentActivity activity) {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
            final PaymentActivity activity = activityRef.get();

            if (activity == null) {
                return;
            }

            activity.runOnUiThread(() ->
                    Toast.makeText(activity, "Error: " + e.toString(), Toast.LENGTH_LONG)
                            .show()
            );
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull final Response response) throws IOException {
            final PaymentActivity activity = activityRef.get();

            if (activity == null) {
                return;
            }

            if (response.isSuccessful()) {
                activity.onConnectToBackendSuccess(response);
            } else {
                activity.runOnUiThread(() ->
                        Toast.makeText(activity, "Error: " + response.toString(), Toast.LENGTH_LONG)
                                .show()
                );
            }
        }

    }

    private void onConnectToBackendSuccess(@NonNull final Response response) throws IOException {
        Log.d(CommonUtil.TAG, "onConnectToBackendSuccess");
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> responseMap = gson.fromJson(
                Objects.requireNonNull(response.body()).string(),
                type
        );

        // The response from the server includes the Stripe publishable key and
        // PaymentIntent details.
        // For added security, our sample app gets the publishable key from the server
        String stripePublishableKey = responseMap.get("publishableKey");
        paymentIntentClientSecret = responseMap.get("clientSecret");

        Log.d(CommonUtil.TAG, responseMap.toString());

        if (stripePublishableKey != null) {
            onRetrievedKey(stripePublishableKey);
        } else {
            Log.w(CommonUtil.TAG, "publishableKey is null");
            // TODO: disable pay button functions, maybe retry accessing server again
            mPayButton.setClickable(false);
            tvGooglePayStatus.setText(mI18Next.t("pay.did_not_retrieve_publishable_key"));
        }
        // Configure the SDK with your Stripe publishable key so that it can make requests to the Stripe API
        //stripe = new Stripe(getApplicationContext(), Objects.requireNonNull(stripePublishableKey));
    }

    // callback of Stripe PaymentIntents result (using webhook)
    private static final class StripeConfirmPaymentResultCallback implements ApiResultCallback<PaymentIntentResult> {

        @NonNull private final WeakReference<PaymentActivity> activityRef;

        StripeConfirmPaymentResultCallback(PaymentActivity activity) {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        public void onSuccess(@NonNull PaymentIntentResult result) {
            final PaymentActivity activity = activityRef.get();
            if (activity == null) {
                return;
            }

            PaymentIntent paymentIntent = result.getIntent();
            PaymentIntent.Status status = paymentIntent.getStatus();
            if (status == PaymentIntent.Status.Succeeded) {
                // Payment completed successfully
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Log.d(CommonUtil.TAG, "Payment completed " + gson.toJson(paymentIntent));
                // TODO: (stripe console has the record, and it should notify the server with webhook)
                activity.logPaymentEvent();
                //activity.displayAlert(
                //        "Payment completed",
                //        gson.toJson(paymentIntent),
                //        true
                //);
            } else if (status == PaymentIntent.Status.RequiresPaymentMethod) {
                // Payment failed
                Log.d(CommonUtil.TAG, "Payment failed "
                        + Objects.requireNonNull(paymentIntent.getLastPaymentError()).getMessage());
                Log.w(CommonUtil.TAG, "Google Pay was successful, but stripe.confirmPayment failed");
                // TODO: need to determine what to do with this situation
                // TODO: error code
                activity.logPaymentEvent();
                //activity.displayAlert(
                //        "Payment failed",
                //        Objects.requireNonNull(paymentIntent.getLastPaymentError()).getMessage(),
                //        false
                //);
            }
        }

        @Override
        public void onError(@NonNull Exception e) {
            final PaymentActivity activity = activityRef.get();
            if (activity == null) {
                return;
            }

            // Payment request failed – allow retrying using the same payment method
            Log.d(CommonUtil.TAG, "Error " + e.toString());
            Log.w(CommonUtil.TAG, "Google Pay was successful, but there was an error on stripe.confirmPayment");
            // TODO: retry stripe.confirmPayment?
            // TODO: error code
            activity.logPaymentEvent();
            //activity.displayAlert("Error", e.toString(), false);
        }

    }



    // payment without webhook
    //
    //
    //
    private void getPublishableKey() {
        Log.d(CommonUtil.TAG, "getPublishableKey");
        // For added security, our sample app gets the publishable key from the server
        Request request = new Request.Builder()
                .url(BACKEND_URL + "stripe-key")
                .get()
                .build();
        okHttpClient.newCall(request)
                .enqueue(new PaymentActivity.StripeKeyCallback(this));
    }

    private static final class StripeKeyCallback implements Callback {

        @NonNull private final WeakReference<PaymentActivity> activityRef;

        private StripeKeyCallback(@NonNull PaymentActivity activity) {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
            Log.d(CommonUtil.TAG, "StripeKeyCallback onFailure");
            final PaymentActivity activity = activityRef.get();
            if (activity == null) {
                return;
            }

            activity.runOnUiThread(() -> {
                // TODO: failed to retrieving the publishable key, maybe retry?
                // TODO: error code
                activity.mPayButton.setClickable(false);
                activity.tvGooglePayStatus
                        .setText(activity.mI18Next.t("pay.did_not_retrieve_publishable_key"));
                Toast.makeText(activity, "Error: " + e.toString(), Toast.LENGTH_LONG)
                        .show();
            });
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull final Response response) throws IOException {
            final PaymentActivity activity = activityRef.get();
            Log.d(CommonUtil.TAG, "StripeKeyCallback onResponse");
            if (activity == null) {
                return;
            }

            if (response.isSuccessful()) {
                Log.d(CommonUtil.TAG, "response.isSuccessful()");
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, String>>(){}.getType();
                final ResponseBody responseBody = response.body();
                final Map<String, String> responseMap;
                if (responseBody != null) {
                    responseMap = gson.fromJson(responseBody.string(), type);
                } else {
                    responseMap = new HashMap<>();
                }

                final String stripePublishableKey = responseMap.get("publishableKey");
                if (stripePublishableKey != null) {
                    activity.runOnUiThread(() ->
                            activity.onRetrievedKey(stripePublishableKey));
                }
            } else {
                Log.d(CommonUtil.TAG, "response.isSuccessful() does not return true");
                // TODO: failed to retrieving the publishable key, maybe retry?
                // TODO: error code
                activity.runOnUiThread(() -> {
                    activity.mPayButton.setClickable(false);
                    activity.tvGooglePayStatus
                            .setText(activity.mI18Next.t("pay.did_not_retrieve_publishable_key"));
                    Toast.makeText(activity, "Error: " + response.toString(), Toast.LENGTH_LONG)
                            .show();
                });
            }
        }

    }

    private void pay(@Nullable String paymentMethodId, @Nullable String paymentIntentId) {
        final MediaType mediaType = MediaType.get("application/json; charset=utf-8");
        final String json;
        if (paymentMethodId != null) {
            json = "{"
                    + "\"useStripeSdk\":true,"
                    + "\"paymentMethodId\":" + "\"" + paymentMethodId + "\","
                    + "\"currency\":\"usd\","
                    + "\"items\":["
                    + "{\"id\":" + "\"" + merchName + "\"}"
                    + "]"
                    + "}";
        } else {
            json = "{"
                    + "\"paymentIntentId\":" +  "\"" + paymentIntentId + "\""
                    + "}";
        }
        RequestBody body = RequestBody.create(json, mediaType);
        Request request = new Request.Builder()
                .url(BACKEND_URL + "pay")
                .post(body)
                .build();
        okHttpClient
                .newCall(request)
                .enqueue(new PaymentActivity.RequestServerToPayCallback(this, stripe));
        Log.d(CommonUtil.TAG, "pay() paymentMethodId: " + paymentMethodId + " paymentIntentId: " + paymentIntentId);
    }

    private static final class RequestServerToPayCallback implements Callback {

        @NonNull private final WeakReference<PaymentActivity> activityRef;
        @NonNull private final Stripe stripe;

        private RequestServerToPayCallback(@NonNull PaymentActivity activity, @NonNull Stripe stripe) {
            activityRef = new WeakReference<>(activity);
            this.stripe = stripe;
        }

        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
            final PaymentActivity activity = activityRef.get();
            if (activity == null) {
                return;
            }

            activity.runOnUiThread(() ->
                    Toast.makeText(activity, "Error: " + e.toString(), Toast.LENGTH_LONG)
                            .show()
            );
            // TODO: error code
            Log.w(CommonUtil.TAG, "Failed at requesting server to pay with stripe");
            activity.logPaymentEvent();
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull final Response response) throws IOException {
            final PaymentActivity activity = activityRef.get();
            if (activity == null) {
                return;
            }

            if (response.isSuccessful()) {
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, String>>(){}.getType();
                final ResponseBody responseBody = response.body();
                final Map<String, String> responseMap;
                if (responseBody != null) {
                    responseMap = gson.fromJson(responseBody.string(), type);
                } else {
                    responseMap = new HashMap<>();
                }

                String error = responseMap.get("error");
                String paymentIntentClientSecret = responseMap.get("clientSecret");
                String requiresAction = responseMap.get("requiresAction");

                Log.d(CommonUtil.TAG, responseMap.toString());

                if (error != null) {
                    // TODO: error code
                    Log.d(CommonUtil.TAG, "Error " + error);
                    activity.logPaymentEvent();
                    //activity.displayAlert("Error", error, false);
                } else if (paymentIntentClientSecret != null) {
                    if ("true".equals(requiresAction)) {
                        activity.runOnUiThread(() ->
                                stripe.handleNextActionForPayment(activity, paymentIntentClientSecret));
                    } else {
                        Log.d(CommonUtil.TAG, "Payment succeeded " + paymentIntentClientSecret);
                        activity.logPaymentEvent();
                        //activity.displayAlert("Payment succeeded", paymentIntentClientSecret, true);
                    }
                }
            } else {
                activity.runOnUiThread(() ->
                        Toast.makeText(activity, "Error: " + response.toString(), Toast.LENGTH_LONG)
                                .show());
                Log.w(CommonUtil.TAG, "Google Pay was successful, but it failed at requesting server to pay with stripe");
                // TODO: need to determine what to do with this situation
                // TODO: error code
                activity.logPaymentEvent();
            }
        }

    }

    private static final class StripeHandleNextActionForPaymentResultCallback
            implements ApiResultCallback<PaymentIntentResult> {
        private final WeakReference<PaymentActivity> activityRef;

        StripeHandleNextActionForPaymentResultCallback(@NonNull PaymentActivity activity) {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        public void onError(@NonNull Exception e) {
            final PaymentActivity activity = activityRef.get();
            if (activity == null) {
                return;
            }

            // Payment request failed, allow retrying using the same payment method
            Log.d(CommonUtil.TAG, "Error " + e.toString());
            //activity.displayAlert("Error", e.toString(), false)
            Log.w(CommonUtil.TAG, "Google Pay was successful, but there was an error on stripe.handleNextActionForPayment");
            // TODO: retry stripe.handleNextActionForPayment?
            // TODO: error code
            activity.logPaymentEvent();
        }

        @Override
        public void onSuccess(@NonNull PaymentIntentResult result) {
            final PaymentActivity activity = activityRef.get();
            if (activity == null) {
                return;
            }

            PaymentIntent paymentIntent = result.getIntent();
            PaymentIntent.Status status = paymentIntent.getStatus();

            if (status == PaymentIntent.Status.Succeeded) {
                // Payment completed successfully for stripe.confirmPayment
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Log.d(CommonUtil.TAG, "Payment completed " + gson.toString());
                //activity.displayAlert("Payment completed", gson.toJson(paymentIntent), true);
                activity.logPaymentEvent();
            } else if (status == PaymentIntent.Status.RequiresPaymentMethod) {
                // Payment failed – allow retrying using a different payment method
                final PaymentIntent.Error error = paymentIntent.getLastPaymentError();
                final String errorMessage;
                if (error != null && error.getMessage() != null) {
                    errorMessage = error.getMessage();
                } else {
                    errorMessage = "Unknown error";
                }
                //activity.displayAlert("Payment failed", errorMessage, false);
                Log.d(CommonUtil.TAG, "Payment failed " + errorMessage);
                Log.w(CommonUtil.TAG, "Google Pay was successful, but it failed at requesting server to pay with stripe");
                // TODO: need to determine what to do with this situation
                // TODO: error code
                activity.logPaymentEvent();
            } else if (status == PaymentIntent.Status.RequiresConfirmation) {
                // After handling a required action on the client, the status of the PaymentIntent is
                // requires_confirmation. You must send the PaymentIntent ID to your backend
                // and confirm it to finalize the payment. This step enables your integration to
                // synchronously fulfill the order on your backend and return the fulfillment result
                // to your client.
                Log.d(CommonUtil.TAG, "Payment required confirmation");
                activity.pay(null, paymentIntent.getId());
            }
        }

    }

}
