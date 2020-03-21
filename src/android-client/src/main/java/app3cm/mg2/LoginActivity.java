package app3cm.mg2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PlayGamesAuthProvider;
import com.i18next.android.I18Next;
import com.i18next.android.Operation;

public class LoginActivity extends AppCompatActivity {

    private Context mContext;

    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth = null;
    private FirebaseUser mFirebaseUser = null;
    private SignInButton mSignInButton;

    private I18Next mI18Next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(CommonUtil.TAG, "LoginActivity onCreate");

        mContext = this;
        mI18Next = I18Next.getInstance();
        initUi();
        getGoogleSignInClient();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Since the state of the signed in user can change when the activity is not active
        // it is recommended to try and sign in silently from when the app resumes.
        mFirebaseUser = mAuth.getCurrentUser();
        if (mFirebaseUser != null) {
            Log.d(CommonUtil.TAG, "mFirebaseUser != null");
            //signInSilently();
        } else {
            enableGoogleSignInButton(true);
        }
    }

    private void startSignInIntent() {
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(intent);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                onConnected(account);
            } catch (ApiException apiException) {
                String message = apiException.getMessage();
                if (message == null || message.isEmpty()) {
                    message = I18Next.getInstance().t("app.signin_other_error");
                }

                revokeAccess();

                new AlertDialog.Builder(this)
                        .setMessage(message)
                        .setNeutralButton(android.R.string.ok, null)
                        .show();
            }
        }
    }

    private void getGoogleSignInClient() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                        .requestServerAuthCode(getString(R.string.default_web_client_id))
                        //.requestEmail()
                        .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, signInOptions);
        mAuth = FirebaseAuth.getInstance();

        // re-sign-in even if we have a signed-in user
        mFirebaseUser = mAuth.getCurrentUser();
        if (mFirebaseUser != null) {
            Log.d(CommonUtil.TAG, "mFirebaseUser != null");
            signOut();
        }
    }

    private void signInSilently() {
        Log.d(CommonUtil.TAG, "signInSilently()");

        mGoogleSignInClient.silentSignIn()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(CommonUtil.TAG, "signInSilently(): success");
                        onConnected(task.getResult());
                    } else {
                        Log.d(CommonUtil.TAG, "signInSilently(): failure", task.getException());
                        revokeAccess();
                    }
                });
    }

    private void onConnected(GoogleSignInAccount googleSignInAccount) {

        // Set the greeting appropriately on main menu
        AuthCredential credential = PlayGamesAuthProvider.getCredential(googleSignInAccount.getServerAuthCode());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(CommonUtil.TAG, "signInWithCredential:success");
                        mFirebaseUser = mAuth.getCurrentUser();
                        enableGoogleSignInButton(false);
                        this.finish();
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(CommonUtil.TAG, "signInWithCredential:failure", task.getException());
                        handleException(task.getException(), I18Next.getInstance().t("app.players_exception"));
                        Toast.makeText(LoginActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                        revokeAccess();
                    }
                });

    }

    private void onDisconnected() {
        mFirebaseUser = null;
        enableGoogleSignInButton(true);
    }

    private void handleException(Exception e, String details) {
        int status = 0;

        if (e instanceof ApiException) {
            ApiException apiException = (ApiException) e;
            status = apiException.getStatusCode();
        }

        String message = I18Next.getInstance().t("app.status_exception_error", new Operation.SPrintF(details, status, e));

        new AlertDialog.Builder(LoginActivity.this)
                .setMessage(message)
                .setNeutralButton(android.R.string.ok, null)
                .show();
    }

    private boolean isSignedIn() {
        mFirebaseUser = mAuth.getCurrentUser();
        return (mFirebaseUser != null);
    }

    private void signOut() {
        Log.d(CommonUtil.TAG, "signOut()");

        if (!isSignedIn()) {
            Log.w(CommonUtil.TAG, "signOut() called, but was not signed in!");
            return;
        }

        // Firebase sign out
        mAuth.signOut();

        // Google sign out
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, task -> {
                    boolean successful = task.isSuccessful();
                    Log.d(CommonUtil.TAG, "signOut(): " + (successful ? "success" : "failed"));

                    onDisconnected();
                });
    }

    private void revokeAccess() {
        Log.d(CommonUtil.TAG, "revokeAccess()");

        // Firebase sign out
        mAuth.signOut();

        // Google revoke access
        mGoogleSignInClient.revokeAccess()
                .addOnCompleteListener(this, task -> onDisconnected());
    }

    private void initUi() {
        setContentView(R.layout.activity_login);

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        mI18Next = I18Next.getInstance();
        mI18Next.loadFromPreference(sharedPref);

        ((TextView) findViewById(R.id.text_sign_in_title)).setText(mI18Next.t("login.sign_in"));

        mSignInButton = findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener((view) -> startSignInIntent());

        findViewById(R.id.button_sign_out_test)
                .setOnClickListener((view) -> signOut());
        findViewById(R.id.button_disconnect_test)
                .setOnClickListener((view) -> revokeAccess());
    }

    private void enableGoogleSignInButton(boolean enable) {
        mSignInButton.setClickable(enable);
    }

    @Override
    public void onBackPressed() {
        // do not finish LoginActivity and back to MainActivity,
        // prompt a dialog to ask finishing the app
        Log.d(CommonUtil.TAG, "onBackPressed");
        CommonUtil.promptLeaveAppDialog(mContext, mI18Next);
    }

}
