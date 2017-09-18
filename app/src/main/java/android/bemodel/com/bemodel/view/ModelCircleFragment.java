package android.bemodel.com.bemodel.view;

import android.app.Activity;
import android.bemodel.com.bemodel.adapter.ModelCircleAdapter;
import android.bemodel.com.bemodel.db.ModelCircleInfo;
import android.bemodel.com.bemodel.util.HttpUtil;
import android.bemodel.com.bemodel.util.Utility;
import android.content.Context;
import android.graphics.PorterDuff;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.bemodel.com.bemodel.R;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import rx.Subscriber;


public class ModelCircleFragment extends Fragment implements View.OnClickListener {

    private static final String TAG ="BeModel";
    private View view;
    private Context context;

    private RecyclerView recyclerView;
    public List<ModelCircleInfo> modelCircleInfoList;
    private ModelCircleAdapter modelCircleAdapter;
    private SwipeRefreshLayout swipeRefresh;

    private TextView tvTitleText;
    private Button btnLeft;
    private Button btnRight;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_model_circle, container, false);
        this.context = inflater.getContext();

        initViews();

        swipeRefresh = (SwipeRefreshLayout)view.findViewById(R.id.swipe_refresh);
//        swipeRefresh.setColorSchemeColors(R.color.colorPrimary);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshModelCicleInfo();
            }
        });

        return view;
    }

    protected void initViews() {
        //展示逻辑
        recyclerView = (RecyclerView)view.findViewById(R.id.rv_model_Circle);
        tvTitleText = (TextView)view.findViewById(R.id.title_text);
        btnLeft = (Button)view.findViewById(R.id.left_btn);
        btnRight = (Button)view.findViewById(R.id.right_btn);

        tvTitleText.setText("模特圈");
        btnLeft.setText("全部");
        btnRight.setVisibility(View.GONE);

        queryModelCircleInfoData();
        if (modelCircleInfoList != null) {
            modelCircleAdapter = new ModelCircleAdapter(context, modelCircleInfoList);
        } else {
            modelCircleAdapter = new ModelCircleAdapter(context, null);
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(modelCircleAdapter);

    }
    //从服务器获取数据
    public void queryModelCircleInfoData() {
        BmobQuery<ModelCircleInfo> query = new BmobQuery<ModelCircleInfo>();

        query.setLimit(50);
        query.order("createdAt");
        //判断是否有缓存
        boolean isCache = query.hasCachedResult(ModelCircleInfo.class);
        if (isCache) {
            query.setCachePolicy(BmobQuery.CachePolicy.CACHE_ELSE_NETWORK);
        } else {
            query.setCachePolicy(BmobQuery.CachePolicy.NETWORK_ELSE_CACHE);
        }

        query.findObjectsObservable(ModelCircleInfo.class)
                .subscribe(new Subscriber<List<ModelCircleInfo>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable throwable) {
                        loge(throwable);
                    }

                    @Override
                    public void onNext(List<ModelCircleInfo> modelCircleInfos) {
                        modelCircleInfoList = modelCircleInfos;
                    }
                });

    }

    //下拉刷新
    private void refreshModelCicleInfo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ((Activity)context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initViews();
                        modelCircleAdapter.notifyDataSetChanged();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        }).start();
    }

    public void loge(Throwable e) {
        Log.i(TAG,"===============================================================================");
        if(e instanceof BmobException){
            Log.e(TAG, "错误码："+((BmobException)e).getErrorCode()+",错误描述："+((BmobException)e).getMessage());
        }else{
            Log.e(TAG, "错误描述："+e.getMessage());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.left_btn:
                queryModelCircleInfoData();
                break;
        }
    }
}
