package org.farring.gcs.fragments.account;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import org.farring.gcs.FishDroneGCSApp;
import org.farring.gcs.R;
import org.farring.gcs.activities.interfaces.AccountLoginListener;
import org.farring.gcs.fragments.account.Model.MyUser;
import org.farring.gcs.fragments.helpers.BaseFragment;
import org.farring.gcs.utils.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.LogInListener;
import cn.bmob.v3.listener.ResetPasswordByEmailListener;
import cn.bmob.v3.listener.SaveListener;

public class UserLoginFragment extends BaseFragment {

    private static final short DRONESHARE_MIN_PASSWORD = 8;
    @BindView(R.id.username)
    EditText username;
    @BindView(R.id.password)
    EditText password;
    @BindView(R.id.password_again)
    EditText passwordAgain;
    @BindView(R.id.email)
    EditText email;
    @BindView(R.id.signup_box)
    LinearLayout signupBox;
    @BindView(R.id.login_box)
    LinearLayout loginBox;
    private AccountLoginListener loginListener;

    // 显示进度条
    private ProgressDialog waitDialog;
    private Activity mActivity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!(activity instanceof AccountLoginListener)) {
            throw new IllegalStateException("Parent must implement " + AccountLoginListener.class.getName());
        }
        this.mActivity = activity;
        this.loginListener = (AccountLoginListener) mActivity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_droneshare_login, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View root, Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        loginBox.setVisibility(View.VISIBLE);
        signupBox.setVisibility(View.GONE);

        // Field validation - we need to add these after the fields have been set otherwise validation will fail on empty fields (ugly).
        username.addTextChangedListener(new TextValidator(username) {
            @Override
            public void validate(TextView textView, String text) {
                if (text.length() == 0)
                    textView.setError(getString(R.string.please_input_username));
                else
                    textView.setError(null);
            }
        });

        password.addTextChangedListener(new TextValidator(password) {
            @Override
            public void validate(TextView textView, String text) {
                if (loginBox.getVisibility() == View.VISIBLE && (text.length() < 1))
                    // Since some accounts have been created with < 7 and no digits, allow login
                    textView.setError(getString(R.string.please_input_password));
                else if (loginBox.getVisibility() == View.GONE && (text.length() < DRONESHARE_MIN_PASSWORD))
                    // New accounts require at least 7 characters and digit
                    textView.setError(getString(R.string.password_length));
                else
                    textView.setError(null);
            }
        });

        email.addTextChangedListener(new TextValidator(email) {
            @Override
            public void validate(TextView textView, String text) {
                if (text.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(text).matches())
                    textView.setError(getString(R.string.please_input_email));
                else
                    textView.setError(null);
            }
        });
    }

    private void showDialog() {
        waitDialog = new ProgressDialog(mActivity);
        waitDialog.setMessage(getString(R.string.wait));
        waitDialog.setCancelable(true);
        waitDialog.show();
    }

    private void hideSoftInput() {
        // Hide the soft keyboard and unfocus any active inputs.
        final Activity activity = mActivity;
        View view = activity.getCurrentFocus();
        if (view != null) {
            final InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputManager != null)
                inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private Boolean validateField(EditText field) {
        // Trigger text changed to see if we have any errors:
        field.setText(field.getText());
        return field.getError() == null;
    }

    @OnClick({R.id.signup_button, R.id.switch_to_login, R.id.login_button, R.id.switch_to_signup, R.id.find_password_tv})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.signup_button:
                // If we have all required fields...
                if (validateField(username) && validateField(password) && validateField(email) && Utils.isNetworkAvailable(getContext())) {
                    final String usernameText = username.getText().toString();
                    final String passwordText = password.getText().toString();
                    final String passwordAgainText = passwordAgain.getText().toString();
                    final String emailText = email.getText().toString();

                    // Hide the soft keyboard, otherwise can remain after logging in.
                    hideSoftInput();
                    // 两次输入密码是否相同
                    if (passwordText.equals(passwordAgainText)) {
                        // 【注册】
                        showDialog();
                        waitDialog.setTitle(getString(R.string.registing));
                        // 新建用户
                        final MyUser user = new MyUser();
                        user.setUsername(usernameText);
                        user.setPassword(passwordText);
                        user.setEmail(emailText);
                        user.signUp(mActivity, new SaveListener() {
                            @Override
                            public void onSuccess() {
                                // 注册成功
                                loginBox.setVisibility(View.VISIBLE);
                                signupBox.setVisibility(View.GONE);
                                Toast.makeText(mActivity, R.string.Registe_success, Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onFailure(int code, String msg) {
                                // 注册失败
                                Toast.makeText(mActivity, getString(R.string.registe_fail) + msg, Toast.LENGTH_LONG).show();
                            }
                        });
                        if (waitDialog.isShowing())
                            waitDialog.dismiss();
                    } else {
                        password.setText("");
                        passwordAgain.setText("");
                        Toast.makeText(mActivity, R.string.registe_password_different, Toast.LENGTH_SHORT).show();
                    }
                }
                break;

            case R.id.switch_to_login:
                loginBox.setVisibility(View.VISIBLE);
                signupBox.setVisibility(View.GONE);
                break;

            // 登陆逻辑
            case R.id.login_button:
                // Validate required fields
                if (validateField(username) && validateField(password) && Utils.isNetworkAvailable(getContext())) {
                    String usernameText = username.getText().toString();
                    String passwordText = password.getText().toString();

                    // Hide the soft keyboard, otherwise can remain after logging in.
                    hideSoftInput();
                    // 【登陆】
                    showDialog();

                    BmobUser.loginByAccount(mActivity, usernameText, passwordText, new LogInListener<MyUser>() {
                        @Override
                        public void done(MyUser myUser, BmobException e) {
                            if (myUser != null) {
                                // 登录成功
                                Toast.makeText(getActivity(), R.string.login_success, Toast.LENGTH_LONG).show();

                                if (loginListener != null)
                                    loginListener.onLogin();
                            } else {
                                // 登录失败
                                Toast.makeText(FishDroneGCSApp.getContext(), R.string.login_fail, Toast.LENGTH_LONG).show();
                                if (loginListener != null)
                                    loginListener.onFailedLogin();
                            }

                            if (waitDialog.isShowing())
                                waitDialog.dismiss();
                        }
                    });
                }
                break;

            // 登陆页面
            case R.id.switch_to_signup:
                loginBox.setVisibility(View.GONE);
                signupBox.setVisibility(View.VISIBLE);
                break;

            // 找回密码
            case R.id.find_password_tv:
                new MaterialDialog.Builder(mActivity)
                        .iconRes(R.drawable.ic_launcher)
                        .limitIconToDefaultSize() // limits the displayed icon size to 48dp
                        .title("找回注册密码")
                        .input("请输入你的注册邮箱地址", "", new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, CharSequence input) {
                                final String email = input.toString();
                                if (email.equals(""))
                                    return;
                                BmobUser.resetPasswordByEmail(mActivity, email, new ResetPasswordByEmailListener() {
                                    @Override
                                    public void onSuccess() {
                                        // 已发送一份重置密码的指令到用户的邮箱
                                        Toast.makeText(mActivity, "重置密码请求成功，请到" + email + "邮箱进行密码重置操作", Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void onFailure(int code, String e) {
                                        // 重置密码出错。
                                        Toast.makeText(mActivity, "重置密码出错", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }).show();
                break;
        }
    }

    private static abstract class TextValidator implements TextWatcher {
        // Wrapper for TextWatcher, providing a shorthand method of field specific validations
        private final TextView textView;

        public TextValidator(TextView textView) {
            this.textView = textView;
        }

        public abstract void validate(TextView textView, String text);

        @Override
        final public void afterTextChanged(Editable s) {
            String text = textView.getText().toString();
            validate(textView, text);
        }

        @Override
        final public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Don't care */ }

        @Override
        final public void onTextChanged(CharSequence s, int start, int before, int count) { /* Don't care */ }
    }
}
