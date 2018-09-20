package com.ninja.playtogether;

import android.app.ActionBar;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.chaos.view.PinView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.hbb20.CountryCodePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import datastore.Store;
import extras.PlayApplication;
import model.User;

public class PhoneAuthActivity extends AppCompatActivity implements
        View.OnClickListener {

    private static final String TAG = "PhoneAuthActivity";

    private static final String KEY_VERIFY_IN_PROGRESS = "key_verify_in_progress";

    private static final int STATE_INITIALIZED = 1;
    private static final int STATE_CODE_SENT = 2;
    private static final int STATE_VERIFY_FAILED = 3;
    private static final int STATE_VERIFY_SUCCESS = 4;
    private static final int STATE_SIGNIN_FAILED = 5;
    private static final int STATE_SIGNIN_SUCCESS = 6;

    private FirebaseAuth mAuth;

    private boolean mVerificationInProgress = false;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    private ViewGroup mPhoneNumberViews;
    private ViewGroup mVerificationViews;

    private TextView mStatusText;
    private TextView mDetailText;
    private TextView mVerificationTitle;
    private TextView mShowVerificationMsg;

    private EditText mPhoneNumberField;
    private PinView mVerificationField;

    private Button mStartButton;
    private Button mVerifyButton;
    private Button mResendButton;
    private Button mChangeButton;

    private CountryCodePicker ccp;

    private Toolbar mTopToolbar;
    private Dialog progressDialog;
    Store store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_auth);

        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState);
        }

        mTopToolbar =  findViewById(R.id.toolbar);
        mVerificationTitle = (TextView) findViewById(R.id.title);
        setSupportActionBar(mTopToolbar);

        progressDialog = new Dialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setContentView(R.layout.progress_layout);

        store = PlayApplication.store;

        // Assign views
        mPhoneNumberViews = findViewById(R.id.phone_auth_fields);
        mVerificationViews = findViewById(R.id.verificationView);

        mStatusText = findViewById(R.id.status);
        mDetailText = findViewById(R.id.detail);
        mShowVerificationMsg = findViewById(R.id.showVerificationMsg);
        mVerificationTitle = findViewById(R.id.title);

        mPhoneNumberField = findViewById(R.id.field_phone_number);
        mVerificationField = findViewById(R.id.field_verification_code);

        ccp = findViewById(R.id.ccp);
        ccp.registerCarrierNumberEditText(mPhoneNumberField);

        mStartButton = findViewById(R.id.button_start_verification);
        mChangeButton = findViewById(R.id.button_change_phone);
        mVerifyButton = findViewById(R.id.button_verify_phone);
        mResendButton = findViewById(R.id.button_resend);

        // Assign click listeners
        mStartButton.setOnClickListener(this);
        mChangeButton.setOnClickListener(this);
        mVerifyButton.setOnClickListener(this);
        mResendButton.setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                Log.d(TAG, "onVerificationCompleted:" + credential);
                mVerificationInProgress = false;

                updateUI(STATE_VERIFY_SUCCESS, credential);
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w(TAG, "onVerificationFailed", e);
                mVerificationInProgress = false;

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    mPhoneNumberField.setError("Invalid phone number.");
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    Snackbar.make(findViewById(android.R.id.content), "Quota exceeded.",
                            Snackbar.LENGTH_SHORT).show();
                }
                if(e instanceof FirebaseNetworkException){
                    Snackbar.make(findViewById(android.R.id.content), "Unable to connect to server. Please check internet connection.",
                            Snackbar.LENGTH_SHORT).show();
                }
                updateUI(STATE_VERIFY_FAILED);
                progressDialog.dismiss();
            }

            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d(TAG, "onCodeSent:" + verificationId);

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;

                updateUI(STATE_CODE_SENT);
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);

        if (mVerificationInProgress && validatePhoneNumber()) {
            startPhoneNumberVerification(ccp.getFormattedFullNumber());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_VERIFY_IN_PROGRESS, mVerificationInProgress);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mVerificationInProgress = savedInstanceState.getBoolean(KEY_VERIFY_IN_PROGRESS);
    }


    private void startPhoneNumberVerification(String phoneNumber) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                mCallbacks);

        mVerificationInProgress = true;
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void resendVerificationCode(String phoneNumber,
                                        PhoneAuthProvider.ForceResendingToken token) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                mCallbacks,
                token);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");

                            FirebaseUser user = task.getResult().getUser();
                            updateUI(STATE_SIGNIN_SUCCESS, user);//
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                mVerificationField.setError("Invalid code.");
                            }
                            updateUI(STATE_SIGNIN_FAILED);//invalid
                        }
                    }
                });
    }

    private void updateUI(int uiState) {
        updateUI(uiState, mAuth.getCurrentUser(), null);
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            updateUI(STATE_SIGNIN_SUCCESS, user);
        } else {
            updateUI(STATE_INITIALIZED);
        }
    }

    private void updateUI(int uiState, FirebaseUser user) {
        updateUI(uiState, user, null);
    }

    private void updateUI(int uiState, PhoneAuthCredential cred) {
        updateUI(uiState, null, cred);
    }

    private void updateUI(int uiState, FirebaseUser user, PhoneAuthCredential cred) {
        switch (uiState) {
            case STATE_INITIALIZED:
                enableViews(mStartButton, mPhoneNumberField);
                disableViews(mVerifyButton, mResendButton, mVerificationField);
                mDetailText.setText(null);
                break;
            case STATE_CODE_SENT:
                enableViews(mVerifyButton, mResendButton, mPhoneNumberField, mVerificationField);
                disableViews(mStartButton);
                mDetailText.setText(R.string.status_code_sent);
                progressDialog.dismiss();
                mPhoneNumberViews.setVisibility(View.GONE);
                mVerificationViews.setVisibility(View.VISIBLE);
                break;
            case STATE_VERIFY_FAILED:
                enableViews(mStartButton, mVerifyButton, mResendButton, mPhoneNumberField,
                        mVerificationField);
                mDetailText.setText(R.string.status_verification_failed);
                break;
            case STATE_VERIFY_SUCCESS:
                disableViews(mStartButton, mVerifyButton, mResendButton, mPhoneNumberField,
                        mVerificationField);
                mDetailText.setText(R.string.status_verification_succeeded);

                if (cred != null) {
                    if (cred.getSmsCode() != null) {
                        mVerificationField.setText(cred.getSmsCode());
                    } else {
                        mVerificationField.setText(R.string.instant_validation);
                    }
                }

                break;
            case STATE_SIGNIN_FAILED:
                mDetailText.setText(R.string.status_sign_in_failed);
                break;
            case STATE_SIGNIN_SUCCESS:
                PlayApplication.self = new User(user.getUid(),user.getPhoneNumber(),user.getPhoneNumber());
                PlayApplication.self.state = "Online";
                PlayApplication.self.last_changed = (new SimpleDateFormat("yyyy-MM-dd HH:mm a"))
                        .format(Calendar.getInstance().getTime());
                store.saveUser(PlayApplication.self);
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                finish();

                // Signed in
                mPhoneNumberViews.setVisibility(View.GONE);
                mVerificationViews.setVisibility(View.VISIBLE);

                enableViews(mPhoneNumberField, mVerificationField);
                mPhoneNumberField.setText(null);
                mVerificationField.setText(null);

                mStatusText.setText(R.string.signed_in);
                break;
        }
    }

    private boolean validatePhoneNumber() {
        String phoneNumber = ccp.getFormattedFullNumber();
        if (TextUtils.isEmpty(phoneNumber) || phoneNumber.length()!=15) {
            mPhoneNumberField.setError("Invalid phone number.");
            return false;
        }

        return true;
    }

    private void enableViews(View... views) {
        for (View v : views) {
            v.setEnabled(true);
        }
    }

    private void disableViews(View... views) {
        for (View v : views) {
            v.setEnabled(false);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_start_verification:
                mVerificationTitle.setText(" Verifying "+ccp.getFormattedFullNumber());
                mShowVerificationMsg.setText("Waiting to automatically detect an SMS sent to \n"+ccp.getFormattedFullNumber()+".");

                if (!validatePhoneNumber()) {
                    return;
                }
                startPhoneNumberVerification(ccp.getFormattedFullNumber());
                progressDialog.show();
                break;
            case R.id.button_verify_phone:
                String code = mVerificationField.getText().toString();
                if (TextUtils.isEmpty(code)) {
                    mVerificationField.setError("Cannot be empty.");
                    return;
                }

                verifyPhoneNumberWithCode(mVerificationId, code);
                break;
            case R.id.button_resend:
                resendVerificationCode(ccp.getFormattedFullNumber(), mResendToken);
            case R.id.button_change_phone:
                mPhoneNumberViews.setVisibility(View.VISIBLE);
                mVerificationViews.setVisibility(View.GONE);
                break;
        }
    }
}
