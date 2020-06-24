package com.dreams.chat.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dreams.chat.R;
import com.dreams.chat.utils.Constants;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class NameSignInActivity extends AppCompatActivity {
    GoogleSignInClient googleSignInClient;
    @BindView(R.id.iv_company_logo)
    ImageView ivCompanyLogo;
    @BindView(R.id.et_company_email_id)
    EditText etCompanyEmailId;
    @BindView(R.id.et_company_email_password)
    EditText etCompanyEmailPassword;
    @BindView(R.id.btn_login)
    Button btnLogin;
    @BindView(R.id.tv_or)
    TextView tvOr;
    @BindView(R.id.sign_in_button)
    SignInButton signInButton;
    @BindView(R.id.btn_google_sign_in)
    Button btnGoogleSignIn;
    private int RC_SIGN_IN = 108;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("740630944449-s3qqga56djgr2l0c1il5qqd9343oa6l2.apps.googleusercontent.com")
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void signIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            if (account != null) {
                createGAccount(account);
            }

        } catch (ApiException e) {
            Log.w("TAG", "signInResult:failed code=" + e.getStatusCode());
        }
    }

    private void createGAccount(final GoogleSignInAccount account) {

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("TAG", "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
//                            updateUI(user);
//                            SessionHandler.getInstance().save(NameSignInActivity.this, Constants.ACCOUNT_NAME, account.getDisplayName());
//                            SessionHandler.getInstance().save(NameSignInActivity.this, Constants.ACCOUNT_ID, user.getUid());
//                            SessionHandler.getInstance().save(NameSignInActivity.this, Constants.ACCOUNT_TOKEN, account.getIdToken());
//                            SessionHandler.getInstance().save(NameSignInActivity.this, Constants.ACCOUNT_EMAIL, user.getEmail());
                            Intent callIntent = new Intent(NameSignInActivity.this, MainActivity.class);
                            startActivity(callIntent);
                            finish();

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("TAG", "signInWithCredential:failure", task.getException());
                        }
                    }
                });


    }

    @OnClick({R.id.btn_login, R.id.btn_google_sign_in})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_login:

                if (etCompanyEmailId.getText().toString().isEmpty()) {
                    etCompanyEmailId.setError("Enter email id");
                    return;
                }

                if (etCompanyEmailPassword.getText().toString().isEmpty()) {
                    etCompanyEmailPassword.setError("Enter password");
                    return;
                }

                createAccount();
                break;
            case R.id.btn_google_sign_in:
                signOut();
                signInButton.performClick();
                signIn();
                break;
        }
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        googleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
//                        Log.d("TASK", task.getResult().toString());
                        String str = "";
                    }
                });
    }


    private void createAccount() {
        final String email = etCompanyEmailId.getText().toString();
        String password = etCompanyEmailPassword.getText().toString();

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(NameSignInActivity.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (!task.isSuccessful()) {
                    // Show error message (dialog or toast)
                    Toast.makeText(NameSignInActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                } else {
                    FirebaseUser user = mAuth.getCurrentUser();
//                            updateUI(user);

                    assert user != null;
//                    SessionHandler.getInstance().save(NameSignInActivity.this, Constants.ACCOUNT_NAME, user.getDisplayName());
//                    SessionHandler.getInstance().save(NameSignInActivity.this, Constants.ACCOUNT_ID, user.getUid());
//                    SessionHandler.getInstance().save(NameSignInActivity.this, Constants.ACCOUNT_EMAIL, user.getEmail());
                    Intent callIntent = new Intent(NameSignInActivity.this, MainActivity.class);
                    startActivity(callIntent);
                    finish();
                }
            }
        });
    }
}
