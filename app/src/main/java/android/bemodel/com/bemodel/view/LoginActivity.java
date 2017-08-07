package android.bemodel.com.bemodel.view;

import android.bemodel.com.bemodel.BaseActivity;
import android.bemodel.com.bemodel.activity.MainActivity;
import android.bemodel.com.bemodel.db.UserInfo;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.print.PrinterId;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bemodel.com.bemodel.R;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import cn.bmob.v3.BmobObject;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.SaveListener;
import rx.Subscriber;

public class LoginActivity extends BaseActivity implements OnClickListener {

    private EditText etAccount;
    private EditText etPassword;

    private Button btn_login;
    private Button btn_register;

    private TextView tvTitleText;
    private Button btnLeft;
    private Button btnRight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        initViews(savedInstanceState);
    }

    @Override
    protected void initVariables() {

    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        etAccount = (EditText)findViewById(R.id.et_phone_number_login);
        etPassword = (EditText)findViewById(R.id.et_password_login);

        btn_login = (Button)findViewById(R.id.bt_sign_in);
        btn_register = (Button)findViewById(R.id.bt_register);

        tvTitleText = (TextView)findViewById(R.id.title_text);
        btnLeft = (Button)findViewById(R.id.left_btn);
        btnRight = (Button)findViewById(R.id.right_btn);

        tvTitleText.setText("登录");
        btnLeft.setText("取消");
        btnRight.setVisibility(View.GONE);

//        btn_login.setOnClickListener(this);
//        btn_register.setOnClickListener(this);


    }

    @Override
    protected void loadData() {

    }

    String account;
    String password;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_sign_in:
                loginHandle();
                break;

            case R.id.bt_register:
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                break;

            case R.id.left_btn:
                finish();
                break;

        }
    }

    private void loginHandle() {
        account = etAccount.getText().toString().trim();
        password = etPassword.getText().toString().trim();
        if (account.equals("")) {
            toast("请填写你的电话号码");
            return;
        }
        if (password.equals("")) {
            toast("请填写你的密码");
            return;
        }
        BmobUser bmobUser = new BmobUser();
        bmobUser.setMobilePhoneNumber(account);
        bmobUser.setPassword(password);

        //v3.5.0开始新增加的rx风格的Api
        bmobUser.loginObservable(BmobUser.class).subscribe(new Subscriber<BmobUser>() {
            @Override
            public void onCompleted() {
                log("----onCompleted----");
            }

            @Override
            public void onError(Throwable throwable) {
                loge(new BmobException(throwable));
            }

            @Override
            public void onNext(BmobUser bmobUser) {
                toast(bmobUser.getUsername() + "登陆成功");
                getCurrentUser();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    //获取本地用户
    private void getCurrentUser() {

    }

}
