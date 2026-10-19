package com.life.management;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private WebView webView;
    private CredentialManager credentialManager;
    private final Executor executor = Executors.newSingleThreadExecutor();

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        credentialManager = CredentialManager.create(this);

        webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Xác nhận")
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> result.cancel())
                        .setOnCancelListener(dialog -> result.cancel())
                        .show();
                return true;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Thông báo")
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                        .setOnCancelListener(dialog -> result.confirm())
                        .show();
                return true;
            }
        });

        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setBackgroundColor(0xFF071018);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Add JavaScript Interface for Native Credential Manager Google Sign-In
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.loadUrl("file:///android_asset/index.html");
        setContentView(webView);
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void signInWithGoogle() {
            runOnUiThread(() -> {
                try {
                    GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId("702741285264-v1092837410823741.apps.googleusercontent.com")
                            .setAutoSelectEnabled(false)
                            .build();

                    GetCredentialRequest request = new GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build();

                    credentialManager.getCredentialAsync(
                            MainActivity.this,
                            request,
                            null,
                            executor,
                            new androidx.credentials.CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                                @Override
                                public void onResult(GetCredentialResponse result) {
                                    try {
                                        androidx.credentials.Credential credential = result.getCredential();
                                        GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.getData());
                                        String email = googleIdTokenCredential.getId();
                                        String displayName = googleIdTokenCredential.getDisplayName();
                                        String photoUrl = googleIdTokenCredential.getProfilePictureUri() != null ? googleIdTokenCredential.getProfilePictureUri().toString() : "";

                                        runOnUiThread(() -> {
                                            String js = "if(window.onGoogleSignInSuccess){window.onGoogleSignInSuccess('" + 
                                                    escapeJs(email) + "', '" + 
                                                    escapeJs(displayName != null ? displayName : email) + "', '" + 
                                                    escapeJs(photoUrl) + "');}";
                                            webView.evaluateJavascript(js, null);
                                        });
                                    } catch (Exception e) {
                                        Log.e("GoogleSignIn", "Credential parsing error", e);
                                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Lỗi đăng nhập Google: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                    }
                                }

                                @Override
                                public void onError(GetCredentialException e) {
                                    Log.e("GoogleSignIn", "Credential Manager error", e);
                                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Đăng nhập Google thất bại hoặc đã hủy.", Toast.LENGTH_SHORT).show());
                                }
                            }
                    );
                } catch (Exception e) {
                    Log.e("GoogleSignIn", "Exception starting CredentialManager", e);
                    Toast.makeText(MainActivity.this, "Không thể khởi động Credential Manager.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String escapeJs(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
