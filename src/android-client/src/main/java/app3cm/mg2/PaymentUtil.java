package app3cm.mg2;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.stripe.android.GooglePayConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PaymentUtil {

    public static final int STRIPE_USING_WEBHOOK = 0;
    public static final int STRIPE_WITHOUT_USING_WEBHOOK = 1;
    public static int stripeFlowType = STRIPE_USING_WEBHOOK;

    private static final String COUNTRY_CODE = "TW";
    private static final String CURRENCY_CODE = "TWD";

    public static PaymentsClient createPaymentsClient(Context context) {
        // TODO: change ENVIRONMENT_TEST into ENVIRONMENT_PRODUCTION to go live
        return Wallet.getPaymentsClient(context,
                new Wallet.WalletOptions.Builder()
                        .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                        .build());
    }

    @NonNull
    public static IsReadyToPayRequest createIsReadyToPayRequest() throws JSONException {
        final JSONObject isReadyToPayRequestJson = getBaseApiRequest();
        isReadyToPayRequestJson.put("allowedPaymentMethods", new JSONArray()
                .put(getBaseCardPaymentMethod()));

        return IsReadyToPayRequest.fromJson(isReadyToPayRequestJson.toString());
    }

    @NonNull
    public static PaymentDataRequest createPaymentDataRequest(Context context, String merchName, String merchPrice) throws JSONException {
        final JSONObject tokenizationSpec =
                new GooglePayConfig(context).getTokenizationSpecification();

        final JSONObject paymentDataRequest = getBaseApiRequest();
        final JSONObject cardPaymentMethod = getBaseCardPaymentMethod();
        cardPaymentMethod.put("tokenizationSpecification", tokenizationSpec);
        paymentDataRequest
                .put("allowedPaymentMethods", new JSONArray().put(cardPaymentMethod))
                .put("transactionInfo", getTransactionInfo(merchPrice))
                .put("merchantInfo", getMerchantInfo(merchName))

                // require email address
                .put("emailRequired", true);

        return PaymentDataRequest.fromJson(paymentDataRequest.toString());
    }

    private static JSONObject getBaseApiRequest() throws JSONException {
        return new JSONObject()
                .put("apiVersion", 2)
                .put("apiVersionMinor", 0);
    }

    private static JSONObject getBaseCardPaymentMethod() throws JSONException {
        final JSONObject parameters = new JSONObject()
                .put("allowedAuthMethods", new JSONArray()
                        .put("PAN_ONLY")
                        .put("CRYPTOGRAM_3DS"))
                .put("allowedCardNetworks", new JSONArray()
                        .put("AMEX")
                        .put("DISCOVER")
                        .put("JCB")
                        .put("MASTERCARD")
                        .put("VISA"))

                // Optionally, you can add billing address/phone number associated with a CARD payment method.
                .put("billingAddressRequired", true)
                .put("billingAddressParameters", new JSONObject()
                                .put("format", "FULL")
                        //.put("phoneNumberRequired", true)
                );

        return new JSONObject()
                .put("type", "CARD")
                .put("parameters", parameters);
    }

    private static JSONObject getTransactionInfo(String merchPrice) throws JSONException {
        return new JSONObject()
                .put("totalPrice", merchPrice)
                .put("totalPriceStatus", "FINAL")
                .put("countryCode", COUNTRY_CODE)
                .put("currencyCode", CURRENCY_CODE);
    }

    private static JSONObject getMerchantInfo(String merchName) throws JSONException {
        return new JSONObject().put("merchantName", merchName);
    }

}
