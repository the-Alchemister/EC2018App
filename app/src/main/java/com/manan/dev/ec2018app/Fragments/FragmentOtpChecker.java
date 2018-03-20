package com.manan.dev.ec2018app.Fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.manan.dev.ec2018app.R;
import com.manan.dev.ec2018app.Utilities.IncomingSms;
import com.manan.dev.ec2018app.Utilities.SmsListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by yatindhingra on 02/03/18.
 */

public class FragmentOtpChecker extends DialogFragment {

    EditText et1, et2, et3, et4, et5, et6;
    TextView submitOtp, resendOtp;
    String otp;
    private String otpNum;
    private String phoneNum;
    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    private ProgressBar bar;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.otp_checker_fragment_layout, container, false);
        phoneNum = getArguments().getString("phone");
        bar = (ProgressBar) rootView.findViewById(R.id.pb_otp);
        et1 = (EditText) rootView.findViewById(R.id.et_otp_dig_1);
        et2 = (EditText) rootView.findViewById(R.id.et_otp_dig_2);
        et3 = (EditText) rootView.findViewById(R.id.et_otp_dig_3);
        et4 = (EditText) rootView.findViewById(R.id.et_otp_dig_4);
        et5 = (EditText) rootView.findViewById(R.id.et_otp_dig_5);
        et6 = (EditText) rootView.findViewById(R.id.et_otp_dig_6);
        submitOtp = (TextView) rootView.findViewById(R.id.tv_check_otp);
        resendOtp = (TextView) rootView.findViewById(R.id.tv_otp_resend);

        addOnTextChangeListener();
        otpNum = getOTP();
        sendSMS(phoneNum,otpNum);
        submitOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                checkOtp();
            }
        });

        resendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendSMS(phoneNum,otpNum);
            }
        });

        return rootView;
    }

    private String getOTP(){
        //get otp
        Random rnd = new Random();
        Integer num = 100000 + rnd.nextInt(900000);
        return num.toString();
    }

    private void sendSMS(final String phone, final String otpNum) {
        bar.setVisibility(View.VISIBLE);
        String url = getResources().getString(R.string.send_sms_api);

        Log.e("TAG", "sendSMS url: " + url );

        RequestQueue queue = Volley.newRequestQueue(getActivity());
        StringRequest smsReq = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override

                    public void onResponse(String response) {
                        // TODO
                        Log.v("sms","response: " + response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO
                bar.setVisibility(View.GONE);
                Log.v("sms","error: " + error.getMessage());
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("From", "CLMYCA");
                params.put("To", phone);
                params.put("TemplateName", "culmyca-otp");
                params.put("VAR1", otpNum);
                return params;
            }
        };
        queue.add(smsReq);
        if(checkAndRequestPermissions()) {
            IncomingSms.bindListener(new SmsListener() {
                @Override
                public void messageReceived(String messageText) {
                    Log.e("TAG", "messageReceived: " + messageText);
                    if(messageText.contains("Culmyca")) {
                        otp = messageText.substring(0, 6);
                        Log.e("TAG" , "messageReceived OTP : " + otp );
                        if(otp.length() == 6){
                            Log.d("yatin", otp.substring(0, 1));
                            et1.setText(otp.substring(0, 1));
                            et2.setText(otp.substring(1, 2));
                            et3.setText(otp.substring(2, 3));
                            et4.setText(otp.substring(3, 4));
                            et5.setText(otp.substring(4, 5));
                            et6.setText(otp.substring(5, 6));
                            Log.e("TAG", "setOtpText: " + et1.getText().toString() + et2.getText().toString() + et3.getText().toString() + et4.getText().toString()
                                    + et5.getText().toString() + et6.getText().toString());
                        } else {
                            bar.setVisibility(View.GONE);
                        }
                    } else {
                        bar.setVisibility(View.GONE);
                    }
                }
            });
        } else {
            bar.setVisibility(View.GONE);
        }
    }

    private  boolean checkAndRequestPermissions() {
        int receiveSMS = ContextCompat.checkSelfPermission(getActivity(),
                android.Manifest.permission.RECEIVE_SMS);

        int readSMS = ContextCompat.checkSelfPermission(getActivity(),
                android.Manifest.permission.READ_SMS);
        List<String> listPermissionsNeeded = new ArrayList<>();

        if (receiveSMS != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.RECEIVE_MMS);
        }
        if (readSMS != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.READ_SMS);
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(getActivity(),
                    listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                    REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    private void addOnTextChangeListener() {
        et1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() == 1) {
                    et2.requestFocus();
                }
            }
        });

        et2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() == 1) {
                    et3.requestFocus();
                } else if (s.toString().length() == 0) {
                    et2.requestFocus();
                }
            }
        });

        et3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() == 1) {
                    et4.requestFocus();
                } else if (s.toString().length() == 0) {
                    et3.requestFocus();
                }
            }
        });

        et4.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() == 1) {
                    et5.requestFocus();
                } else if (s.toString().length() == 0) {
                    et4.requestFocus();
                }
            }
        });

        et5.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() == 1) {
                    et6.requestFocus();
                } else if (s.toString().length() == 0) {
                    et5.requestFocus();
                }
            }
        });


        et6.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() == 0) {
                    et6.requestFocus();
                } else if (s.toString().length() == 1) {
                    checkOtp();
                }
            }
        });

        et2.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                deleteOtp();
                return false;
            }
        });

        et3.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                deleteOtp();
                return false;
            }
        });

        et4.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                deleteOtp();
                return false;
            }
        });

        et5.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                deleteOtp();
                return false;
            }
        });

        et6.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                deleteOtp();
                return false;
            }
        });
    }

    private void deleteOtp() {
        et1.setText("");
        et2.setText("");
        et3.setText("");
        et4.setText("");
        et5.setText("");
        et6.setText("");
        et1.requestFocus();
    }

    private void checkOtp(){
        retrieveEnteredText();
        try {
            Thread.sleep(1000);
        } catch (Exception e){
            Log.e("TAG", e.getMessage());
        }
        otpCheckStatus activity = (otpCheckStatus) getActivity();
        try {
            Thread.sleep(1000);
        } catch (Exception e){
            Log.e("TAG", "checkOtp: " + "error");
        }
        if (otp.equals(otpNum)) {
            activity.updateResult(true);
            bar.setVisibility(View.GONE);
            dismiss();
        } else {
            bar.setVisibility(View.GONE);
            Toast.makeText(getActivity(), "Incorrect OTP!", Toast.LENGTH_SHORT).show();
        }
    }

    private void retrieveEnteredText() {
        otp = "";
        otp += et1.getText().toString();
        otp += et2.getText().toString();
        otp += et3.getText().toString();
        otp += et4.getText().toString();
        otp += et5.getText().toString();
        otp += et6.getText().toString();
    }

    public interface otpCheckStatus {
        void updateResult(boolean status);
    }

    @Override
    public void onDestroy() {
        otpCheckStatus activity = (otpCheckStatus) getActivity();
        activity.updateResult(false);
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        otpCheckStatus activity = (otpCheckStatus) getActivity();
        activity.updateResult(false);
        super.onDestroyView();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        // request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }
}
