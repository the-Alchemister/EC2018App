package com.manan.dev.ec2018app.Xunbao;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.Profile;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.manan.dev.ec2018app.Fragments.FragmentFbLogin;
import com.manan.dev.ec2018app.LoginActivity;
import com.manan.dev.ec2018app.R;
import com.manan.dev.ec2018app.Utilities.ConnectivityReciever;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.valdesekamdem.library.mdtoast.MDToast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;


public class QuestionFragment extends Fragment implements XunbaoActivity.loadQuestionFragment, ConnectivityReciever.ConnectivityReceiverListener {
    TextView question, contestEnd, refreshText, level, loginText;
    ImageView xunbaoimg, refreshButton;
    LinearLayout submit;
    EditText ans;
    String queURL, ansURL, statusURL;
    StringRequest stat;
    StringRequest jobReq;
    RequestQueue queue;
    RelativeLayout queLayout;
    private ProgressBar bar, barImage;
    //ProgressDialog progressBar;
    int xstatus = 2;
    private String currFbid;
    private LoginButton loginButton;
    private CallbackManager callbackManager;
    private FirebaseAuth mAuth;
    private Context mContext;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_question, container, false);

        if(mContext == null){
            mContext = getActivity();
        }

        mAuth = FirebaseAuth.getInstance();

        bar = (ProgressBar) view.findViewById(R.id.pb_question);
        bar.getIndeterminateDrawable().setColorFilter(mContext.getResources().getColor(R.color.pb_xunbao), android.graphics.PorterDuff.Mode.MULTIPLY);
        barImage = (ProgressBar) view.findViewById(R.id.pb_image);
        barImage.getIndeterminateDrawable().setColorFilter(mContext.getResources().getColor(R.color.pb_xunbao), android.graphics.PorterDuff.Mode.MULTIPLY);
        bar.setVisibility(View.VISIBLE);
        barImage.setVisibility(View.GONE);



        loginButton = (LoginButton) view.findViewById(R.id.login_button);
        loginText = (TextView) view.findViewById(R.id.tv_log_in);

        loginText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in = new Intent(mContext, LoginActivity.class);
                in.putExtra("parent", "xunbao");
                startActivity(in);
            }
        });

        final String EMAIL = "email";

        callbackManager = CallbackManager.Factory.create();

        loginButton.setReadPermissions(Arrays.asList(EMAIL));
        loginButton.setFragment(this);

        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                AccessToken accessToken = loginResult.getAccessToken();
                FragmentFbLogin.fbLoginButton activity = (FragmentFbLogin.fbLoginButton) mContext;
                handleFacebookAccessToken(loginResult.getAccessToken());
                activity.fbStatus(true, accessToken.getUserId());
                MDToast.makeText(mContext, "Facebook Login Successful!", MDToast.LENGTH_SHORT, MDToast.TYPE_SUCCESS).show();
            }

            @Override
            public void onCancel() {
                Toast.makeText(mContext, "Facebook login cancelled!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException exception) {
            }
        });

        queURL = mContext.getResources().getString(R.string.xunbao_get_question_api);
        ansURL = mContext.getResources().getString(R.string.xunbao_check_answer_api);
        statusURL = mContext.getResources().getString(R.string.xunbao_status);
        queLayout = view.findViewById(R.id.question_layout);
        level = view.findViewById(R.id.tv_question_number);
        question = view.findViewById(R.id.tv_question_text);
        xunbaoimg = view.findViewById(R.id.iv_xunbao_question_image);
        ans = view.findViewById(R.id.et_xunbao_answer);
        submit = view.findViewById(R.id.ll_submit);
        contestEnd = view.findViewById(R.id.contest_ends);
        refreshButton = view.findViewById(R.id.refresh_button);
        refreshText = view.findViewById(R.id.refresh_text);


        queLayout.setVisibility(View.GONE);
        contestEnd.setVisibility(View.GONE);
        refreshButton.setVisibility(View.GONE);
        refreshText.setVisibility(View.GONE);
        loginButton.setVisibility(View.GONE);
        loginText.setVisibility(View.GONE);


        queue = Volley.newRequestQueue(mContext);

        refreshButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        reload();
                    }
                }
        );

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bar.setVisibility(View.VISIBLE);
                //progressBar.show();
                submitAnswer();
            }
        });
        return view;
    }

    private void submitAnswer() {

        AccessToken accessToken = AccessToken.getCurrentAccessToken();

        final StringRequest answ = new StringRequest(Request.Method.POST, ansURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("xunbao", "onResponse: " + response);
                        bar.setVisibility(View.GONE);
                        try {
                            //progressBar.dismiss();
                            JSONObject resp = new JSONObject(response);
                            String end = resp.getString("response");
                            contestEnd.setVisibility(View.VISIBLE);
                            if(end.equals("1")) {
                                MDToast.makeText(mContext, "Congrats! Right answer!", MDToast.LENGTH_SHORT, MDToast.TYPE_SUCCESS).show();
                                ans.setText("");
                                reload();
                            }
                            else
                                MDToast.makeText(mContext, "Wrong answer!", MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR).show();

                        } catch (JSONException e) {

                            MDToast.makeText(mContext, "Problem submitting answer!", MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR).show();
                            //progressBar.dismiss();
                            //Log.e("xunbao", "onResponse: " + e.getMessage());
                            Log.d("xunbao", "onResponse: " + response.toString());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        bar.setVisibility(View.GONE);
                        //progressBar.dismiss();
                        MDToast.makeText(mContext, "Problem submitting answer!", MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR).show();
                    }
                })
        {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {

                Map<String, String> map = new HashMap<String, String>();
                map.put("email", currFbid);
                map.put("skey", mContext.getResources().getString(R.string.skey));
                map.put("ans", ans.getText().toString());
                return map;
            }
        };
        queue.add(answ);
    }

    public void reload() {
        //progressBar.show();
        bar.setVisibility(View.VISIBLE);
        queLayout.setVisibility(View.GONE);
        contestEnd.setVisibility(View.GONE);
        refreshButton.setVisibility(View.GONE);
        refreshText.setVisibility(View.GONE);
        loginButton.setVisibility(View.GONE);
        loginText.setVisibility(View.GONE);
        checkStatus();
    }

    public void checkStatus() {
        stat = new StringRequest(Request.Method.GET, statusURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        bar.setVisibility(View.GONE);
                        refreshButton.setVisibility(View.GONE);
                        xstatus = Integer.parseInt(response);
                        if (xstatus == 1) {
                            //progressBar.dismiss();
                            contestEnd.setText("KEEP CALM! CONTEST YET TO START!");
                            contestEnd.setVisibility(View.VISIBLE);
                        } else if (xstatus == 2) {
                            if (AccessToken.getCurrentAccessToken() != null) {
                                bar.setVisibility(View.VISIBLE);
                                queue.add(jobReq);
                                refreshText.setVisibility(View.GONE);
                                loginButton.setVisibility(View.GONE);
                                loginText.setVisibility(View.GONE);
                            } else {
                                try {
                                    refreshText.setVisibility(View.VISIBLE);
                                    SharedPreferences prefs = mContext.getSharedPreferences(getResources().getString(R.string.sharedPrefName), MODE_PRIVATE);
                                    String phoneNumber = prefs.getString("Phone", null);
                                    if(phoneNumber == null){
                                        loginText.setVisibility(View.VISIBLE);
                                    } else {
                                        loginButton.setVisibility(View.VISIBLE);
                                    }

                                }catch (Exception e){}


                            }
                        } else if (xstatus == 3) {
                            //progressBar.dismiss();
                            contestEnd.setText("THE CONTEST IS OVER! THANKS FOR PLAYING. IF YOU HAVE WON, WE WILL CONTACT YOU SHORTLY.");
                            contestEnd.setVisibility(View.VISIBLE);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                bar.setVisibility(View.GONE);
                //progressBar.dismiss();
                refreshButton.setVisibility(View.VISIBLE);
//                MDToast.makeText(mContext, "Problem loading!", MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR).show();
            }
        });
        queue.add(stat);
    }

    public void getQuestion() {
        JSONArray jsonArray = new JSONArray();
        JSONObject params = new JSONObject();
        try {
            params.put("fid", currFbid);
            if(Profile.getCurrentProfile()!=null) {
                params.put("skey", mContext.getResources().getString(R.string.skey));
                params.put("fname",Profile.getCurrentProfile().getFirstName());
                params.put("lname",Profile.getCurrentProfile().getLastName());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        jsonArray.put(params);

        jobReq = new StringRequest(Request.Method.POST, queURL, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        bar.setVisibility(View.GONE);

                        JSONObject resp;
                        refreshButton.setVisibility(View.GONE);
                        try {
                            JSONArray re = new JSONArray(response);
                            Log.d("yatin", response);
                            resp = re.getJSONObject(0);
                            try {
                                String end = resp.getString("response");
                                contestEnd.setVisibility(View.VISIBLE);
                                contestEnd.setText("YOU HAVE SUCCESSFULLY COMPLETED ALL THE QUESTIONS.\n WE WILL ANNOUNCE THE WINNERS ON 14th APRIL, 2018.\nIF YOU HAVE WON, WE WILL CONTACT YOU SHORTLY");
                                refreshButton.setVisibility(View.GONE);
                            } catch (Exception e) {

                                bar.setVisibility(View.GONE);
                                queLayout.setVisibility(View.VISIBLE);
                                String imgUrl = resp.getString("image");
                                String que = resp.getString("desc");
                                Integer level = resp.getInt("val");
                                question.setText(Html.fromHtml(que));
                                QuestionFragment.this.level.setText("Level " + String.valueOf(level));
                                if(!imgUrl.equals("null")) {
                                    barImage.setVisibility(View.GONE);
                                    Picasso.with(mContext).load("http://xunbao.elementsculmyca.com" + imgUrl).fit().into(xunbaoimg, new Callback() {
                                        @Override
                                        public void onSuccess() {
                                            barImage.setVisibility(View.GONE);
                                        }

                                        @Override
                                        public void onError() {
                                            barImage.setVisibility(View.GONE);
                                        }
                                    });
                                } else {
                                    Log.d("yatin", "yess");
                                    xunbaoimg.setVisibility(View.GONE);
                                }
                            }
                        } catch (JSONException e) {
                            bar.setVisibility(View.GONE);
                            //progressBar.dismiss();
                            refreshButton.setVisibility(View.VISIBLE);
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        //progressBar.dismiss();

                        bar.setVisibility(View.GONE);
                        refreshButton.setVisibility(View.VISIBLE);

                        volleyError.printStackTrace();
                    }
                }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {

                Map<String, String> map = new HashMap<String, String>();
                map.put("fid", currFbid);
                map.put("skey", mContext.getResources().getString(R.string.skey));
                map.put("fname", Profile.getCurrentProfile().getFirstName());
                map.put("lname", Profile.getCurrentProfile().getLastName());
                return map;
            }
        };
        queue.add(stat);
    }


    @Override
    public void makeQuestionVisible(String fbId) {
        currFbid = fbId;

        if(isNetworkAvailable()) {
            reload();
            if (!currFbid.equals("notLoggedIn"))
                getQuestion();
        } else {
            MDToast.makeText(mContext.getApplicationContext(), "Connect to Internet", Toast.LENGTH_SHORT, MDToast.TYPE_ERROR).show();
        }
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener((Activity) mContext, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                        } else {
                            // If sign in fails, display a message to the user.
                            MDToast.makeText(mContext, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        if(isConnected){
            checkStatus();
            if (!currFbid.equals("notLoggedIn"))
                getQuestion();
        } else {
            MDToast.makeText(mContext.getApplicationContext(), "Connect to Internet", Toast.LENGTH_SHORT, MDToast.TYPE_ERROR).show();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mContext = context;
    }
}
