package com.wstro.virtuallocation.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.CoordinateConverter;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemChildClickListener;
import com.google.gson.Gson;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VirtualLocationManager;
import com.lody.virtual.remote.vloc.VCell;
import com.lody.virtual.remote.vloc.VLocation;
import com.lody.virtual.remote.vloc.VWifi;
import com.suke.widget.SwitchButton;
import com.wstro.app.common.base.BaseAppToolbarActivity;
import com.wstro.app.common.utils.SPUtils;
import com.wstro.virtuallocation.Constants;
import com.wstro.virtuallocation.R;
import com.wstro.virtuallocation.component.LocationInfo;
import com.wstro.virtuallocation.component.MapViewActivity;
import com.wstro.virtuallocation.data.model.AppInfo;
import com.wstro.virtuallocation.data.model.CellInfo;
import com.wstro.virtuallocation.data.model.RealData;
import com.wstro.virtuallocation.data.model.WifiInfo;
import com.wstro.virtuallocation.ui.adapter.CollectAdapter;
import com.wstro.virtuallocation.ui.presenter.AppDetailPresenter;
import com.wstro.virtuallocation.ui.view.AppDetailView;
import com.wstro.virtuallocation.utils.AppUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public class AppDetailActivity extends BaseAppToolbarActivity implements AppDetailView, CompoundButton.OnCheckedChangeListener {

    AppDetailPresenter presenter;

    @BindView(R.id.iv_icon)
    ImageView ivIcon;
    @BindView(R.id.rv)
    RecyclerView rv;
    @BindView(R.id.tv_name)
    TextView tvName;
    @BindView(R.id.tv_address)
    TextView tvAddress;
    @BindView(R.id.tv_address_collect)
    TextView tvAddressCollect;//收藏位置
    @BindView(R.id.cb_collect)
    CheckBox cbCollect;
    @BindView(R.id.switch_button)
    SwitchButton switchButton;
    @BindView(R.id.tv_clear)
    TextView tvClear;


    AppInfo appInfo;

    boolean hasLocation;

    private final static String cacheName = "cache";

    LocationInfo locInfo;
    private SharedPreferences cache;
    private RealData data;//当前的位置信息
    private CollectData collectDatas;//收藏的位置信息
    private CollectAdapter adapter;

    public static void start(Context context, AppInfo appInfo) {
        Intent starter = new Intent(context, AppDetailActivity.class);
        starter.putExtra("data", appInfo);
        context.startActivity(starter);
    }


    @Override
    protected int getLayoutId() {
        return R.layout.activity_app_detail;
    }

    @Override
    protected void initViewsAndEvents(Bundle bundle) {
        titleText.setText("虚拟操作");

        appInfo = getIntent().getParcelableExtra("data");

        cache = getSharedPreferences(cacheName, 0);


        if (appInfo == null)
            return;


        cbCollect.setOnCheckedChangeListener(this);
        tvName.setText(appInfo.getAppName());
        Drawable icon = appInfo.getIcon();
        if (icon == null) {
            icon = AppUtils.getApplicationIcon(context, appInfo.getPackageName());
        }

        ivIcon.setImageDrawable(icon);


        int mode = VirtualLocationManager.get().getMode(Constants.appUserId, appInfo.getPackageName());
        hasLocation = mode != 0;
        switchButton.setChecked(hasLocation);

        if (hasLocation) {
            String name = (String) SPUtils.get(this, appInfo.getPackageName(), "");
            if (!"".equals(name)) {
                tvAddress.setText("位置：" + name);
            }
        }

        switchButton.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                int mode = isChecked ? VirtualLocationManager.MODE_USE_SELF : VirtualLocationManager.MODE_CLOSE;

                VirtualLocationManager.get().setMode(0, appInfo.getPackageName(), mode);
            }
        });

        collectDatas = getCollectDatas();
        adapter = new CollectAdapter(collectDatas.datas, this);
        adapter.setListener(new CollectAdapter.OnItemClickListener() {
            @Override
            public void click(RealData data) {

                List<CellInfo> cellList = data.getCellInfoList();
                List<WifiInfo> wifiList = data.getWifiInfoList();

                List<VCell> vCellList = new ArrayList<>();
                List<VWifi> vWifiList = new ArrayList<>();

                for (int i = 0; i < cellList.size(); i++) {
                    vCellList.add(transferCell(cellList.get(i)));
                }

                for (int i = 0; i < wifiList.size(); i++) {
                    vWifiList.add(transferWifi(wifiList.get(i)));
                }
                String packageName = appInfo.getPackageName();
                if (vCellList.size() > 0) {
                    VirtualLocationManager.get().setCell(Constants.appUserId,
                            packageName, vCellList.get(0));
                    VirtualLocationManager.get().setAllCell(Constants.appUserId,
                            packageName, vCellList);
                }

                VirtualLocationManager.get().setAllWifi(Constants.appUserId,
                        packageName, vWifiList);

                LocationInfo locInfo = data.getLocationInfo();


                VirtualLocationManager.get().setLocation(Constants.appUserId,
                        packageName, transferLocation(locInfo));
                SPUtils.put(AppDetailActivity.this, packageName, locInfo.getAddrStr());


                onGetRealDataSuccess(data);
            }
        });
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(manager);
        rv.setAdapter(adapter);

    }

    @Override
    protected void initData() {
        presenter = new AppDetailPresenter();
        presenter.attachView(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (presenter != null)
            presenter.detachView();
    }


    @OnClick({R.id.ll_item, R.id.rl_position, R.id.tv_clear})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.ll_item:
                launch(appInfo.getPackageName());
                break;
            case R.id.rl_position:
                if (!hasLocation) {
                    switchButton.setChecked(true);
                }
                MapViewActivity.start(this, null);
                break;
            case R.id.tv_clear:
                if (collectDatas!=null){
                    if (collectDatas.datas!=null) {
                        collectDatas.datas.clear();
                        adapter.notifyDataSetChanged();
                    }
                }
                break;
        }
    }

    public void launch(final String packageName) {
        final int userId = appInfo.getUserId() != 0 ? appInfo.getUserId() : Constants.appUserId;

        final Intent intent = VirtualCore.get().getLaunchIntent(packageName, userId);
        if (intent == null)
            return;

        VirtualCore.get().setUiCallback(intent, mUiCallback);

        showProgressDialog("启动中...");


        VActivityManager.get().startActivity(intent, userId);

    }


    private final VirtualCore.UiCallback mUiCallback = new VirtualCore.UiCallback() {

        @Override
        public void onAppOpened(String packageName, int userId) throws RemoteException {
            stopProgressDialog();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) return;

        locInfo = data.getParcelableExtra("data");


        if (locInfo == null)
            return;

        showProgressDialog("获取虚拟数据中...");
        presenter.getNetRealInfo(locInfo, appInfo.getPackageName());


    }

    @Override
    public void onGetRealDataFail(String error) {
        stopProgressDialog();
        showToast("获取虚拟数据失败，" + error);
    }

    @Override
    public void onGetRealDataSuccess(RealData data) {
        stopProgressDialog();

        if (data == null)
            return;

        this.data = data;


        List<CellInfo> cellList = data.getCellInfoList();
        List<WifiInfo> wifiList = data.getWifiInfoList();

        int cellNumber = cellList != null ? cellList.size() : 0;
        int wifiNumber = wifiList != null ? wifiList.size() : 0;

        showToast(String.format("虚拟GPS、基站(%d)、WIFI(%d)成功", cellNumber, wifiNumber));


        if (data.getLocationInfo()==null){
            data.setLocationInfo(locInfo);
        }

        tvAddress.setText("位置：" + data.getLocationInfo().getAddrStr());

        tvAddressCollect.setText("位置：" +  data.getLocationInfo().getAddrStr());

    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if (data == null) {
            return;
        }

        if (collectDatas == null) {
            showToast("收藏的位置信息为空");
            return;
        }
        if (data == null) {
            showToast("尚未获取到位置信息");
            return;
        }


        if (data.getLocationInfo() == null) {
            data.setLocationInfo(locInfo);
        }
        if (isChecked) {
            int i = collectDatas.datas.indexOf(data);
            if (i < 0) {
                collectDatas.datas.add(0, data);
            }
        } else {
            collectDatas.datas.remove(data);
        }
        adapter.notifyDataSetChanged();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (collectDatas != null) {
            String s = new Gson().toJson(collectDatas);
            cache.edit().putString(cacheName, s).apply();
        }
    }

    public static class CollectData {
        public List<RealData> datas;
    }


    private CollectData getCollectDatas() {
        String string = cache.getString(cacheName, "");
        CollectData collectData;
        if (TextUtils.isEmpty(string)) {
            collectData = new CollectData();
        } else {
            collectData = new Gson().fromJson(string, CollectData.class);
        }

        if (collectData.datas == null) {
            collectData.datas = new ArrayList<>();
        }
        return collectData;
    }

    private VCell transferCell(CellInfo cellInfo) {
        if (cellInfo == null)
            return null;

        VCell obj = new VCell();
        obj.mcc = 460;
        obj.mnc = cellInfo.getMnc();
        obj.cid = cellInfo.getCi();
        obj.lac = cellInfo.getLac();
        obj.psc = -1;

        return obj;
    }

    private VWifi transferWifi(WifiInfo wifiInfo) {
        if (wifiInfo == null)
            return null;

        VWifi obj = new VWifi();
        obj.bssid = wifiInfo.getMac();
        obj.level = wifiInfo.getAcc();

        return obj;
    }

    private VLocation transferLocation(LocationInfo locInfo) {
        if (locInfo == null)
            return null;

        VLocation vLocation = new VLocation();

//        CoordinateConverter converter = new CoordinateConverter();
//        converter.from(CoordinateConverter.CoordType.BD09LL);
//        LatLng ll = new LatLng(locInfo.getLatitude(), locInfo.getLongitude());
////        converter.coord(ll);
//        ll = converter.convert();

        vLocation.latitude = locInfo.getLatitude();
        vLocation.longitude = locInfo.getLongitude();
        vLocation.address = locInfo.getAddrStr();
        vLocation.city = locInfo.getCityName();

        return vLocation;
    }
}
