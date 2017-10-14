package android.bemodel.com.bemodel.messages;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bemodel.com.bemodel.adapter.MessageChatAdapter;
import android.bemodel.com.bemodel.base.BaseActivity;
import android.bemodel.com.bemodel.bean.ChatUser;
import android.bemodel.com.bemodel.content.MyMessageReceiver;
import android.bemodel.com.bemodel.util.CommonUtils;
import android.bemodel.com.bemodel.widget.EmoticonsEditText;
import android.bemodel.com.bemodel.widget.MySecondTitlebar;
import android.bemodel.com.bemodel.widget.xlist.XListView;
import android.bemodel.com.bemodel.config.BmobConstants;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.bemodel.com.bemodel.R;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.EventListener;
import java.util.List;

import butterknife.BindView;
import cn.bmob.im.BmobChatManager;
import cn.bmob.im.BmobRecordManager;
import cn.bmob.im.bean.BmobMsg;
import cn.bmob.im.config.BmobConfig;
import cn.bmob.im.db.BmobDB;
import cn.bmob.im.inteface.OnRecordChangeListener;
import cn.bmob.im.inteface.UploadListener;
import cn.bmob.im.util.BmobLog;


public class ChatActivity extends BaseActivity implements View.OnClickListener, XListView.IXListViewListener, EventListener {

    @BindView(R.id.mListView)
    XListView mXListView;

    @BindView(R.id.btn_chat_add)
    Button btn_chat_add;
    @BindView(R.id.btn_chat_emo)
    Button btn_chat_emo;
    @BindView(R.id.btn_chat_keyboard)
    Button btn_chat_keyboard;
    @BindView(R.id.btn_chat_voice)
    Button btn_chat_voice;
    @BindView(R.id.btn_chat_send)
    Button btn_chat_send;
    @BindView(R.id.btn_speak)
    Button btn_speak;

    @BindView(R.id.layout_more)
    LinearLayout layout_more;
    @BindView(R.id.layout_emo)
    LinearLayout layout_emo;
    @BindView(R.id.layout_add)
    LinearLayout layout_add;

    @BindView(R.id.edit_user_comment)
    EmoticonsEditText edit_user_comment;

    @BindView(R.id.tv_picture)
    TextView tv_picture;
    @BindView(R.id.tv_camera)
    TextView tv_camera;
    @BindView(R.id.tv_location)
    TextView tv_location;
    @BindView(R.id.tv_phone)
    TextView tv_phone;

    @BindView(R.id.vp_emo)
    ViewPager pager_emo;

    // 语音有关
    @BindView(R.id.layout_record)
    RelativeLayout layout_record;
    @BindView(R.id.tv_voice_tips)
    TextView tv_voice_tips;
    @BindView(R.id.iv_record)
    ImageView iv_record;

    private String targetId = "";

    private ChatUser targetUser;

    private static int MsgPagerNum;

    private MySecondTitlebar mySecondTitlebar;

    private NewBroadcastReceiver receiver;

    private String localCameraPath = "";    // 拍照后得到的图片地址

    private Drawable[] drawable_Anims;  // 话筒动画

    private BmobRecordManager recordManager;
    private BmobChatManager manager;
    private MessageChatAdapter mAdapter;
    private Toast toast;

    public static final int NEW_MESSAGE = 0x001;    //收到消息

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        manager = BmobChatManager.getInstance(this);
        MsgPagerNum = 0;
        //组装聊天对象
        targetUser = (ChatUser)getIntent().getSerializableExtra("user");
        targetId = targetUser.getObjectId();
        //注册广播接收器
        initNewMessageBroadCast();

    }

    private void initNewMessageBroadCast() {
        // 注册接收消息广播
        receiver = new NewBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(BmobConfig.BROADCAST_NEW_MESSAGE);
        //设置广播的优先级别大于Mainacitivity,这样如果消息来的时候正好在chat页面，直接显示消息，而不是提示消息未读
        intentFilter.setPriority(5);
        registerReceiver(receiver, intentFilter);
    }

    private class NewBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String from = intent.getStringExtra("fromId");
            String msgId = intent.getStringExtra("msgId");
            String msgTime = intent.getStringExtra("msgTime");
            // 收到这个广播的时候，message已经在消息表中，可直接获取
            if (!TextUtils.isEmpty(from) && !TextUtils.isEmpty(msgId) && !TextUtils.isEmpty(msgTime)) {
                BmobMsg msg = BmobChatManager.getInstance(ChatActivity.this).getMessage(msgId, msgTime);
                if (!from.equals(targetId)) {   // 如果不是当前正在聊天对象的消息，不处理
                    return;
                }
                //添加到当前页面
                mAdapter.add(msg);
                //定位
                mXListView.setSelection(mAdapter.getCount() - 1);
                //取消当前聊天对象的未读标示
                BmobDB.create(ChatActivity.this).resetUnread(targetId);

            }
            //截断广播
            abortBroadcast();
        }
    }

    @Override
    protected void initVariables() {

    }

    @Override
    protected void initViews() {
        mySecondTitlebar = (MySecondTitlebar)findViewById(R.id.udtb_chat);
        mySecondTitlebar.setTitleText(targetUser.getUsername());
        initButtomView();
        initXListView();
        initVoiceView();
    }


    private void initButtomView() {
        btn_chat_voice.setOnClickListener(this);
        btn_chat_keyboard.setOnClickListener(this);
        btn_chat_add.setOnClickListener(this);
        btn_chat_emo.setOnClickListener(this);
        btn_chat_send.setOnClickListener(this);
        initAddView();
        initEmoView();

        edit_user_comment.setOnClickListener(this);
        edit_user_comment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!TextUtils.isEmpty(s)) {    //==false 有字
                    btn_chat_send.setVisibility(View.VISIBLE);
                    btn_chat_keyboard.setVisibility(View.GONE);
//                    btn_chat_voice.setVisibility(View.GONE);
                    btn_chat_add.setVisibility(View.GONE);
                } else {
                    if (btn_chat_add.getVisibility() != View.VISIBLE) {
                        btn_chat_add.setVisibility(View.VISIBLE);
                        btn_chat_send.setVisibility(View.GONE);
                        btn_chat_keyboard.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void initAddView() {
        tv_picture.setOnClickListener(this);
        tv_location.setOnClickListener(this);
        tv_camera.setOnClickListener(this);
        tv_phone.setOnClickListener(this);
    }

//    List<FaceText> emos;
//    private void initEmoView() {
//        emos = FaceTextUtils.faceTexts;
//
//        List<View> views = new ArrayList<View>();
//        for (int i = 0; i< 2; i++) {
//            views.add(getGridView(i));
//        }
//        pager_emo.setAdapter(new EmoViewPagerAdapter(views));
//    }

    /**
     *
     * 初始化语音布局
     */
    private void initVoiceView() {
        btn_speak.setOnTouchListener(new VoiceTouchListen());
        initVoiceAnimRes();
        initRecordManager();
    }

    /**
     * 长按说话
     * @ClassName: VoiceTouchListen
     */
    class VoiceTouchListen implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (!CommonUtils.checkSdCard()) {
                        showToast("发送语音需要sdcard支持！");
                        return false;
                    }
                    try {
                        v.setPressed(true);
                        layout_record.setVisibility(View.VISIBLE);
                        tv_voice_tips.setText(getString(R.string.voice_cancel_tips));
                        // 开始录音
                        recordManager.startRecording(targetId);
                    } catch (Exception e) {
                    }
                    return true;

                case MotionEvent.ACTION_MOVE: {
                    if (event.getY() < 0) {
                        tv_voice_tips
                                .setText(getString(R.string.voice_cancel_tips));
                        tv_voice_tips.setTextColor(Color.RED);
                    } else {
                        tv_voice_tips.setText(getString(R.string.voice_up_tips));
                        tv_voice_tips.setTextColor(Color.WHITE);
                    }
                    return true;
                }

                case MotionEvent.ACTION_UP:
                    v.setPressed(false);
                    layout_record.setVisibility(View.INVISIBLE);
                    try {
                        if (event.getY() < 0) {// 放弃录音
                            recordManager.cancelRecording();
                            BmobLog.i("voice", "放弃发送语音");
                        } else {
                            int recordTime = recordManager.stopRecording();
                            if (recordTime > 1) {
                                // 发送语音文件
                                BmobLog.i("voice", "发送语音");
                                sendVoiceMessage(recordManager.getRecordFilePath(targetId), recordTime);
                            } else {// 录音时间过短，则提示录音过短的提示
                                layout_record.setVisibility(View.GONE);
                                showShortToast().show();
                            }
                        }
                    } catch (Exception e) {
                    }
                    return true;

                default:
                    return false;
            }
        }
    }

    /**
     * 初始化语音动画资源
     */
    private void initVoiceAnimRes() {
        drawable_Anims = new Drawable[]{
                getResources().getDrawable(R.drawable.chat_icon_voice2),
                getResources().getDrawable(R.drawable.chat_icon_voice3),
                getResources().getDrawable(R.drawable.chat_icon_voice4),
                getResources().getDrawable(R.drawable.chat_icon_voice5),
                getResources().getDrawable(R.drawable.chat_icon_voice6),
        };
    }

    private void initRecordManager() {
        recordManager = BmobRecordManager.getInstance(this);
        recordManager.setOnRecordChangeListener(new OnRecordChangeListener() {
            @Override
            public void onVolumnChanged(int i) {
                iv_record.setImageDrawable(drawable_Anims[i]);
            }

            @Override
            public void onTimeChanged(int recordTime, String localPath) {
                BmobLog.i("voice", "已录音长度:" + recordTime);
                if (recordTime >= BmobRecordManager.MAX_RECORD_TIME) {// 1分钟结束，发送消息
                    //需要重置按钮
                    btn_speak.setPressed(false);
                    btn_speak.setClickable(false);
                    //取消录音框
                    layout_record.setVisibility(View.INVISIBLE);
                    //发送语音信息
                    sendVoiceMessage(localPath, recordTime);
                    //是为了防止过了录音时间后，会多发一条语音出去的情况。
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            btn_speak.setClickable(true);
                        }
                    }, 1000);
                } else {

                }
            }
        });
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == NEW_MESSAGE) {
                BmobMsg message = (BmobMsg) msg.obj;
                String uid = message.getBelongId();
                BmobMsg m = BmobChatManager.getInstance(ChatActivity.this).getMessage(message.getConversationId(), message.getMsgTime());
                if (!uid.equals(targetId))// 如果不是当前正在聊天对象的消息，不处理
                    return;
                mAdapter.add(m);
                // 定位
                mXListView.setSelection(mAdapter.getCount() - 1);
                //取消当前聊天对象的未读标示
                BmobDB.create(ChatActivity.this).resetUnread(targetId);
            }
        }
    };



    private void sendVoiceMessage(String local, int length) {
        manager.sendVoiceMessage(targetUser, local, length, new UploadListener() {
            @Override
            public void onStart(BmobMsg bmobMsg) {
                refreshMessage(bmobMsg);
            }

            @Override
            public void onSuccess() {
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(int i, String s) {
                showToast("上传语音失败 -->s: " + s);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * 显示录音时间过短的Toast
     * @return
     */
    private Toast showShortToast() {
        if (toast == null) {
            toast = new Toast(this);
        }
        View view = LayoutInflater.from(this).inflate(R.layout.include_chat_voice_short, null);
        toast.setView(view);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setDuration(50);
        return toast;
    }

    /**
     * 刷新信息界面
     * @param bmobMsg
     */
    private void refreshMessage(BmobMsg bmobMsg) {
        //更新界面
        mAdapter.add(bmobMsg);
        mXListView.setSelection(mAdapter.getCount() - 1);
        edit_user_comment.setText("");
    }

    @Override
    protected void loadData() {

    }

    @Override
    public void onRefresh() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                MsgPagerNum++;
                int total = BmobDB.create(ChatActivity.this).queryChatTotalCount(targetId);
                BmobLog.i("记录总数: " + total);
                int currents = mAdapter.getCount();
                if (total <= currents) {
                    showToast("聊天记录加载完了");
                } else {
                    List<BmobMsg> msgList = initMsgData();
                    mAdapter.setList(msgList);
                    mXListView.setSelection(mAdapter.getCount() - currents - 1);
                }
                mXListView.stopRefresh();
            }
        }, 1000);

    }

    /**
     * 加载信息历史，从数据库中读出
     * @return
     */
    private List<BmobMsg> initMsgData() {
        List<BmobMsg> list = BmobDB.create(this).queryMessages(targetId, MsgPagerNum);
        return list;
    }

    @Override
    public void onLoadMore() {

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_chat_voice:       //触动语音按钮
                btn_chat_voice.setVisibility(View.GONE);
                btn_chat_keyboard.setVisibility(View.VISIBLE);
                edit_user_comment.setVisibility(View.GONE);
                btn_speak.setVisibility(View.VISIBLE);
                hideSoftInputView();    //隐藏软键盘
                break;

            case R.id.btn_chat_keyboard:    //触动键盘按钮
                showEditState(false);
                break;

            case R.id.btn_chat_emo:         //触动笑脸按钮
                if (layout_more.getVisibility() == View.GONE) {
                    showEditState(true);
                } else {
                    if (layout_add.getVisibility() == View.VISIBLE) {
                        layout_add.setVisibility(View.GONE);
                        layout_emo.setVisibility(View.VISIBLE);
                    } else {
                        layout_more.setVisibility(View.GONE);
                    }
                }
                break;

            case R.id.btn_chat_add:        //触动加号按钮
                if (layout_more.getVisibility() == View.GONE) {
                    layout_more.setVisibility(View.VISIBLE);
                    layout_add.setVisibility(View.VISIBLE);
                    layout_emo.setVisibility(View.GONE);
                    hideSoftInputView();//隐藏软键盘
                } else {
                    if (layout_emo.getVisibility() == View.VISIBLE) {
                        layout_emo.setVisibility(View.GONE);
                        layout_add.setVisibility(View.VISIBLE);
                    } else {
                        layout_more.setVisibility(View.GONE);
                    }
                }
                break;

            case R.id.btn_chat_send:        //触动发送按钮
                final String msg = edit_user_comment.getText().toString();
                if (msg.equals("")) {
                    showToast("请输入发送信息！");
                    return;
                }
                boolean isNetConnected = CommonUtils.isNetworkAvailable(this);
                if (!isNetConnected) {
                    showToast(R.string.network_tips);
                }
                //组装BmobMsg对象
                BmobMsg message = BmobMsg.createTagSendMsg(this, targetId, msg);
                message.setExtra("Bmob");
                //默认发送完成，将数据保存到本地消息表和最近会话表中
                manager.sendTextMessage(targetUser, message);
                //刷新信息界面
                refreshMessage(message);
                break;

            case R.id.edit_user_comment:    //触动点击文本输入框
                mXListView.setSelection(mXListView.getCount() - 1);
                if (layout_more.getVisibility() == View.VISIBLE) {
                    layout_add.setVisibility(View.GONE);
                    layout_emo.setVisibility(View.GONE);
                    layout_more.setVisibility(View.GONE);
                }
                showSoftInputView();
                break;

            case R.id.tv_camera:            //触动拍照按钮
                selectImageFromCamera();
                break;

            case R.id.tv_picture:           //触动图片按钮
                selectImageFromLocal();
                break;

            case R.id.tv_location:          //触动位置按钮
                selectLocationFromMap();
                break;

            case R.id.tv_phone:             //触动电话按钮
                selectcommunicateByphone();
                break;

            default:
                break;

        }
    }

    private void selectcommunicateByphone() {

    }

    /**
     * 启动地图
     */
    private void selectLocationFromMap() {
        Intent intent = new Intent(this, LocationActivity.class);
        intent.putExtra("type", "select");
        startActivityForResult(intent, BmobConstants.REQUESTCODE_TAKE_LOCATION);
    }

    /**
     * 从本地选择图片
     */
    private void selectImageFromLocal() {
        Intent intent;
        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
        } else {
            intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
        startActivityForResult(intent, BmobConstants.REQUESTCODE_TAKE_LOCAL);
    }

    /**
     * 启动相机拍照 startCamera
     */
    private void selectImageFromCamera() {
        Intent openCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File dir = new File(BmobConstants.BMOB_PICTURE_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, String.valueOf(System.currentTimeMillis()) + ".jpg");
        localCameraPath = file.getPath();
        Uri imageUri = Uri.fromFile(file);
        openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(openCameraIntent, BmobConstants.REQUESTCODE_TAKE_CAMERA);

    }

    private void sendImageMessage(String local) {
        if (layout_more.getVisibility() == View.VISIBLE) {
            layout_more.setVisibility(View.GONE);
            layout_add.setVisibility(View.GONE);
            layout_emo.setVisibility(View.GONE);
        }
        manager.sendImageMessage(targetUser, local, new UploadListener() {
            @Override
            public void onStart(BmobMsg bmobMsg) {
                showLog("开始上传onStart：" + bmobMsg.getContent() + ",状态：" + bmobMsg.getStatus());
                refreshMessage(bmobMsg);
            }

            @Override
            public void onSuccess() {
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(int i, String s) {
                showLog("上传失败 -->s：" + s);
                mAdapter.notifyDataSetChanged();
            }
        });
    }
    //----------------------------------------------------------------------------------------------
    /**
     *
     * @param isEmo 是否点击笑脸，false表示没点击笑脸，true表示点击笑脸
     */
    private void showEditState(boolean isEmo) {
        edit_user_comment.setVisibility(View.VISIBLE);
        btn_chat_keyboard.setVisibility(View.GONE);
        btn_chat_voice.setVisibility(View.VISIBLE);
        btn_speak.setVisibility(View.GONE);
        edit_user_comment.requestFocus();
        if (isEmo) {    //为true 表示点击笑脸
            layout_more.setVisibility(View.VISIBLE);
            layout_emo.setVisibility(View.VISIBLE);
            layout_add.setVisibility(View.GONE);
            hideSoftInputView();
        } else {    //为false 表示没点击笑脸
            layout_more.setVisibility(View.GONE);
            showSoftInputView();
        }
    }

    /**
     * 隐藏软键盘
     */
    public void hideSoftInputView() {
        InputMethodManager manager = ((InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE));
        if (getWindow().getAttributes().softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN) {
            if (getCurrentFocus() != null)
                manager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /**
     * 显示软键盘
     */
    public void showSoftInputView() {
        if (getWindow().getAttributes().softInputMode == WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN) {
            if (getCurrentFocus() != null)
                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(edit_user_comment, 0);
        }
    }

    //----------------------------------------------------------------------------------------------


    @Override
    protected void onPause() {
        super.onPause();
        MyMessageReceiver.ehList.remove(this);  //监听推送的信息
        //停止录音
        if (recordManager.isRecording()) {
            recordManager.cancelRecording();
            layout_record.setVisibility(View.GONE);
        }
        //停止播放录音
        if (NewRecordPlayClickListener.isPlaying && NewRecordPlayClickListener.currentPlayListener != null) {
            NewRecordPlayClickListener.currentPlayListener.stopPlayRecord();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideSoftInputView();
        try{
            unregisterReceiver(receiver);
        }catch (Exception e) {

        }
    }
}
