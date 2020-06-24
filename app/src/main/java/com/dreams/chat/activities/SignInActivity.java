package com.dreams.chat.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSpinner;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dreams.chat.BaseApplication;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.dreams.chat.R;
import com.dreams.chat.models.Country;
import com.dreams.chat.models.User;
import com.dreams.chat.utils.Helper;
import com.dreams.chat.utils.KeyboardUtil;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.toptoche.searchablespinnerlibrary.SearchableSpinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SignInActivity extends AppCompatActivity {
    private SearchableSpinner spinnerCountryCodes;
    private EditText etPhone;
    private static final int REQUEST_CODE_SMS = 123;
    private Helper helper;
    private EditText otpCode;
    private KeyboardUtil keyboardUtil;
    private String phoneNumberInPrefs = null, verificationCode = null;
    private ProgressDialog progressDialog;
    private TextView verificationMessage, retryTimer, myOtpexpiresTXT;
    private CountDownTimer countDownTimer;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private FirebaseAuth mAuth;
    private boolean authInProgress;
    private TextView myCountryCodeTXT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        helper = new Helper(this);

        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.VIBRATE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.MODIFY_AUDIO_SETTINGS,
                        Manifest.permission.READ_PHONE_STATE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            // do you work now
                        }

                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // permission is denied permenantly, navigate user to app settings
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .onSameThread()
                .check();

        User user = helper.getLoggedInUser();
        if (user != null) {    //Check if user if logged in
//            done();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            //init vars
            phoneNumberInPrefs = helper.getPhoneNumberForVerification();
            keyboardUtil = KeyboardUtil.getInstance(this);
            progressDialog = new ProgressDialog(this);

            //if there is number to authenticate in preferences then initiate
            setContentView(TextUtils.isEmpty(phoneNumberInPrefs) ? R.layout.activity_sign_in_1 : R.layout.activity_sign_in_2);
            if (TextUtils.isEmpty(phoneNumberInPrefs)) {
                //setup number selection
                spinnerCountryCodes = findViewById(R.id.countryCode);
                etPhone = findViewById(R.id.phoneNumber);
                myCountryCodeTXT = findViewById(R.id.layout_registration_country_code_TXT);
                setupCountryCodes();
            } else {
                //initiate authentication
                mAuth = FirebaseAuth.getInstance();
                retryTimer = findViewById(R.id.resend);
                myOtpexpiresTXT = findViewById(R.id.otp_expires_txt);
                verificationMessage = findViewById(R.id.verificationMessage);
                otpCode = findViewById(R.id.otp);
                findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        back();
                    }
                });
                findViewById(R.id.changeNumber).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        helper.clearPhoneNumberForVerification();
                        recreate();
                    }
                });
                initiateAuth(phoneNumberInPrefs);
            }
            findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (TextUtils.isEmpty(phoneNumberInPrefs)) {
                        submit();
                    } else {
                        //force authenticate

                        String otp = otpCode.getText().toString();
                        if (!TextUtils.isEmpty(otp) && !TextUtils.isEmpty(mVerificationId))
                            signInWithPhoneAuthCredential(PhoneAuthProvider.getCredential(mVerificationId, otp));

                        //verifyOtp(otpCode[0].getText().toString() + otpCode[1].getText().toString() + otpCode[2].getText().toString() + otpCode[3].getText().toString());
                    }
                }
            });
        }
    }

    private void showProgress(int i) {
        String title = (i == 1) ? "Sending otp" : "Verifying otp";
        String message = (i == 1) ? ("One time password is being send to:\n" + phoneNumberInPrefs) : "Verifying otp...";
        progressDialog.setTitle(title);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void initiateAuth(String phone) {
        showProgress(1);
        PhoneAuthProvider.getInstance().verifyPhoneNumber(phone, 60, TimeUnit.SECONDS, this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onCodeAutoRetrievalTimeOut(String s) {
                        super.onCodeAutoRetrievalTimeOut(s);
                    }

                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                        progressDialog.dismiss();
                        signInWithPhoneAuthCredential(phoneAuthCredential);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        authInProgress = false;
                        progressDialog.dismiss();
                        countDownTimer.cancel();
                        Log.e("ERR_MESSAGE", "Something went wrong" + ((e.getMessage() != null) ? ("\n" + e.getMessage()) : ""));
                        if ((e.getMessage() != null) && e.getMessage().contains("E.164")) {
                            verificationMessage.setText("Something went wrong! The format of the phone number provided is incorrect");
                        } else {
                            verificationMessage.setText("Something went wrong" + ((e.getMessage() != null) ? ("\n" + e.getMessage()) : ""));
                        }

                        retryTimer.setVisibility(View.VISIBLE);
                        retryTimer.setText("RESEND CODE");
                        myOtpexpiresTXT.setText("Didn't receive the code?");
                        retryTimer.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                initiateAuth(phoneNumberInPrefs);
                            }
                        });
//                        Toast.makeText(SignInActivity.this, "Something went wrong something went wrong! The format of the phone number provided is incorrect", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(verificationId, forceResendingToken);
                        authInProgress = true;
                        progressDialog.dismiss();
                        mVerificationId = verificationId;
                        mResendToken = forceResendingToken;
                        myOtpexpiresTXT.setText("You can Resend the OTP in");
                        verificationMessage.setText(String.format("Please type the verification code\nsent to %s", phoneNumberInPrefs));
//                        retryTimer.setVisibility(View.GONE);
                    }
                });
        startCountdown();
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential phoneAuthCredential) {
        showProgress(2);
        mAuth.signInWithCredential(phoneAuthCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if (task.isSuccessful()) {
                    progressDialog.setMessage("Logging you in!");
                    login();
                }

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                if (e.getMessage() != null && e.getMessage().contains("invalid")) {
                    Toast.makeText(SignInActivity.this, "Invalid OTP", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(SignInActivity.this, e.getMessage() != null ? "\n" + e.getMessage() : "", Toast.LENGTH_LONG).show();
                }
                progressDialog.dismiss();
                authInProgress = false;
            }
        });
    }

    private void login() {
        authInProgress = true;

        BaseApplication.getUserRef().child(phoneNumberInPrefs).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                progressDialog.dismiss();
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    try {
                        User user = dataSnapshot.getValue(User.class);
                        if (User.validate(user)) {
                            helper.setLoggedInUser(user);
                            done();
                        } else {
                            createUser(new User(phoneNumberInPrefs, phoneNumberInPrefs, getString(R.string.app_name), ""));
                        }
                    } catch (Exception ex) {
                        createUser(new User(phoneNumberInPrefs, phoneNumberInPrefs, getString(R.string.app_name), ""));
                    }
                } else {
                    createUser(new User(phoneNumberInPrefs, phoneNumberInPrefs, getString(R.string.app_name), ""));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void createUser(final User newUser) {
        BaseApplication.getUserRef().child(phoneNumberInPrefs).setValue(newUser).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                helper.setLoggedInUser(newUser);
                done();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(SignInActivity.this, "Something went wrong, unable to create user.", Toast.LENGTH_LONG).show();
            }
        });
    }


    private void back() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cancel verification");
        builder.setMessage("Verification is in progress! do you want to cancel and go back?");
        builder.setNegativeButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                helper.clearPhoneNumberForVerification();
                recreate();
                dialogInterface.dismiss();
            }
        });
        builder.setPositiveButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        if (progressDialog.isShowing() || authInProgress) {
            builder.create().show();
        } else {
            helper.clearPhoneNumberForVerification();
            recreate();
        }
    }

    private void startCountdown() {
        retryTimer.setOnClickListener(null);
        myOtpexpiresTXT.setText("You can Resend the OTP in");
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long l) {
                if (retryTimer != null) {
                    retryTimer.setText(String.valueOf(l / 1000));
                }
            }

            @Override
            public void onFinish() {
                if (retryTimer != null) {
                    retryTimer.setText("Resend");
                    myOtpexpiresTXT.setText("Didn't receive the code?");
                    retryTimer.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            initiateAuth(phoneNumberInPrefs);
                        }
                    });
                }
            }
        }.start();
    }

    private void setupCountryCodes() {
        ArrayList<Country> countries = getCountries();
        if (countries != null) {
           /* ArrayAdapter<Country> myAdapter = new ArrayAdapter<Country>(this, R.layout.item_country_spinner, countries);
            spinnerCountryCodes.setAdapter(myAdapter);
            spinnerCountryCodes.setPrompt("Select");
            spinnerCountryCodes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    Object item = parent.getItemAtPosition(pos);
                    final String aDialCode = ((Country) spinnerCountryCodes.getSelectedItem()).getDialCode();
                    myCountryCodeTXT.setText(aDialCode);
                }

                public void onNothingSelected(AdapterView<?> parent) {
                }
            });*/

            ArrayList<Country> aCountries1 = new ArrayList<>();
            Country country = new Country("", "Select Country", "");

            aCountries1 = getCountries();
            aCountries1.add(0, country);
//            aCountries1.addAll(aCountries1);

            final ArrayAdapter<Country> adapter = new ArrayAdapter<Country>(SignInActivity.this,
                    android.R.layout.simple_spinner_item, aCountries1);

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCountryCodes.setAdapter(adapter);

            spinnerCountryCodes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) {
                        myCountryCodeTXT.setText("");
                    } else {
                        final String aDialCode = ((Country) spinnerCountryCodes.getSelectedItem()).getDialCode();
                        myCountryCodeTXT.setText(aDialCode);
                    }

                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }


    }

    private ArrayList<Country> getCountries() {
        ArrayList<Country> toReturn = new ArrayList<>();
//        toReturn.add(new Country("RU", "Russia", "+7"));
//        toReturn.add(new Country("TJ", "Tajikistan", "+992"));
//        toReturn.add(new Country("US", "United States", "+1"));
//        return toReturn;

        try {
            JSONArray countrArray = new JSONArray(readEncodedJsonString(this));
            toReturn = new ArrayList<>();
            for (int i = 0; i < countrArray.length(); i++) {
                JSONObject jsonObject = countrArray.getJSONObject(i);
                String countryName = jsonObject.getString("name");
                String countryDialCode = jsonObject.getString("dial_code");
                String countryCode = jsonObject.getString("code");
                Country country = new Country(countryCode, countryName, countryDialCode);
                toReturn.add(country);
            }
            Collections.sort(toReturn, new Comparator<Country>() {
                @Override
                public int compare(Country lhs, Country rhs) {
                    return lhs.getName().compareTo(rhs.getName());
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return toReturn;
    }

    private String readEncodedJsonString(Context context) throws IOException {
        String base64 = context.getResources().getString(R.string.countries_code);
        byte[] data = Base64.decode(base64, Base64.DEFAULT);
        return new String(data, "UTF-8");
    }

    //Go to main activity
    private void done() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    public void submit() {
        //Validate and confirm number country codes selected
        try {
            if (spinnerCountryCodes.getSelectedItem() == null || (myCountryCodeTXT.getText().toString().trim().equalsIgnoreCase(""))) {
                Toast.makeText(this, "Select country code!", Toast.LENGTH_LONG).show();
                return;
            }
            if (TextUtils.isEmpty(etPhone.getText().toString())) {
                Toast.makeText(this, "Enter phone number!", Toast.LENGTH_LONG).show();
                return;
            }
            final String phoneNumber = ((Country) spinnerCountryCodes.getSelectedItem()).getDialCode() + etPhone.getText().toString().replaceAll("\\s+", "");

            if (isValidPhoneNumber(etPhone.getText().toString().replaceAll("\\s+", ""))) {
                /*boolean status = validateUsing_libphonenumber(((Country) spinnerCountryCodes.getSelectedItem()).getCode(),
                        etPhone.getText().toString().replaceAll("\\s+", ""));*/
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(phoneNumber);
                builder.setMessage("One time password will be sent on this number! Continue with this number?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        helper.setPhoneNumberForVerification(phoneNumber);
                        recreate();
                        dialogInterface.dismiss();
                    }
                });
                builder.setNegativeButton("Edit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        etPhone.requestFocus();
                        keyboardUtil.openKeyboard();
                        dialogInterface.dismiss();
                    }
                });
                builder.create().show();

            } else {
                Toast.makeText(this, "Invalid Mobile Number", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Invalid Mobile Number", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isValidPhoneNumber(CharSequence phoneNumber) {
        if (!TextUtils.isEmpty(phoneNumber)) {
            return Patterns.PHONE.matcher(phoneNumber).matches();
        }
        return false;
    }

    private boolean validateUsing_libphonenumber(String countryCode, String phNumber) {
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        //  String isoCode = phoneNumberUtil.getRegionCodeForCountryCode(Integer.parseInt(countryCode));
        Phonenumber.PhoneNumber phoneNumber = null;
        try {
            //phoneNumber = phoneNumberUtil.parse(phNumber, "IN");  //if you want to pass region code
            phoneNumber = phoneNumberUtil.parse(phNumber, countryCode);
        } catch (NumberParseException e) {
            System.err.println(e);
        }

        return phoneNumberUtil.isValidNumber(phoneNumber);
    }
}
