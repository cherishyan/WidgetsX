package com.jinq.widgetsx.widget;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import com.jinq.widgetsx.R;
import com.jinq.widgetsx.bean.TransferBean.TransferModelBean;
import com.jinq.widgetsx.constant.transfer.TransferConstant;
import com.jinq.widgetsx.event.TransferEvent;
import com.jinq.widgetsx.service.WebService;
import com.jinq.widgetsx.util.WifiConnectChangedReceiver;
import com.jinq.widgetsx.util.WifiUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;


/**
 * Created by masel on 2016/10/10.
 */

public class PopupMenuDialog {
    Unbinder mUnbinder;
    @BindView(R.id.popup_menu_title)
    TextView mTxtTitle;
    @BindView(R.id.popup_menu_subtitle)
    TextView mTxtSubTitle;
    @BindView(R.id.shared_wifi_state)
    ImageView mImgLanState;
    @BindView(R.id.shared_wifi_state_hint)
    TextView mTxtStateHint;
    @BindView(R.id.shared_wifi_address)
    TextView mTxtAddress;
    @BindView(R.id.shared_wifi_settings)
    Button mBtnWifiSettings;
    @BindView(R.id.shared_wifi_button_split_line)
    View mButtonSplitLine;
    WifiConnectChangedReceiver mWifiConnectChangedReceiver = new WifiConnectChangedReceiver();
    private Context context;
    private Dialog dialog;
    private Display display;

    public PopupMenuDialog(Context context) {
        this.context = context;
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        display = windowManager.getDefaultDisplay();
        EventBus.getDefault().register(this);
    }

    public PopupMenuDialog builder() {
        View view = LayoutInflater.from(context).inflate(
                R.layout.layout_popup_menu_dialog, null);

        view.setMinimumWidth(display.getWidth());

        dialog = new Dialog(context, R.style.PopupMenuDialogStyle);
        dialog.setContentView(view);
        mUnbinder = ButterKnife.bind(this, dialog);
        dialog.setOnDismissListener(this::onDialogDismiss);

        Window dialogWindow = dialog.getWindow();
        dialogWindow.setGravity(Gravity.LEFT | Gravity.BOTTOM);
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.x = 0;
        lp.y = 0;
        dialogWindow.setAttributes(lp);

        return this;
    }

    public PopupMenuDialog setCancelable(boolean cancel) {
        dialog.setCancelable(cancel);
        return this;
    }

    public PopupMenuDialog setCanceledOnTouchOutside(boolean cancel) {
        dialog.setCanceledOnTouchOutside(cancel);
        return this;
    }

    public void show() {
        checkWifiState(WifiUtils.getWifiConnectState(context));
        dialog.show();
        registerWifiConnectChangedReceiver();
    }

    @OnClick({R.id.shared_wifi_cancel, R.id.shared_wifi_settings})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.shared_wifi_cancel:
                dialog.dismiss();
                break;
            case R.id.shared_wifi_settings:
                context.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                break;
        }
    }

    void registerWifiConnectChangedReceiver() {
        IntentFilter intentFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        context.registerReceiver(mWifiConnectChangedReceiver, intentFilter);
    }

    void unregisterWifiConnectChangedReceiver() {
        context.unregisterReceiver(mWifiConnectChangedReceiver);
    }


    void checkWifiState(NetworkInfo.State state) {
        if (state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.CONNECTING) {
            if (state == NetworkInfo.State.CONNECTED) {
                String ip = WifiUtils.getWifiIp(context);
                if (!TextUtils.isEmpty(ip)) {
                    onWifiConnected(ip);
                    return;
                }
            }
            onWifiConnecting();
            return;
        }
        onWifiDisconnected();
    }

    void onWifiDisconnected() {
        mTxtTitle.setText(R.string.transfer_wlan_disabled);
        mTxtTitle.setTextColor(context.getResources().getColor(android.R.color.black));
        mTxtSubTitle.setVisibility(View.VISIBLE);
        mImgLanState.setImageResource(R.drawable.shared_wifi_shut_down);
        mTxtStateHint.setText(R.string.fail_to_start_http_service);
        mTxtAddress.setVisibility(View.GONE);
        mButtonSplitLine.setVisibility(View.VISIBLE);
        mBtnWifiSettings.setVisibility(View.VISIBLE);
    }

    void onWifiConnecting() {
        mTxtTitle.setText(R.string.transfer_wlan_enabled);
        mTxtTitle.setTextColor(context.getResources().getColor(R.color.baseTheme));
        mTxtSubTitle.setVisibility(View.GONE);
        mImgLanState.setImageResource(R.drawable.shared_wifi_enable);
        mTxtStateHint.setText(R.string.retrofit_wlan_address);
        mTxtAddress.setVisibility(View.GONE);
        mButtonSplitLine.setVisibility(View.GONE);
        mBtnWifiSettings.setVisibility(View.GONE);
    }

    void onWifiConnected(String ipAddr) {
        mTxtTitle.setText(R.string.transfer_wlan_enabled);
        mTxtTitle.setTextColor(context.getResources().getColor(R.color.baseTheme));
        mTxtSubTitle.setVisibility(View.GONE);
        mImgLanState.setImageResource(R.drawable.shared_wifi_enable);
        mTxtStateHint.setText(R.string.pls_input_the_following_address_in_pc_browser);
        mTxtAddress.setVisibility(View.VISIBLE);
        mTxtAddress.setText(String.format(context.getString(R.string.http_address), ipAddr, TransferConstant.HTTP_PORT));
        mButtonSplitLine.setVisibility(View.GONE);
        mBtnWifiSettings.setVisibility(View.GONE);
    }

    void onDialogDismiss(DialogInterface dialog) {
        Log.d("tag","dialog dismiss!");
        if (mUnbinder != null) {
            mUnbinder.unbind();

            TransferEvent event = new TransferEvent();
            event.setType(TransferConstant.RxBusEventType.POPUP_MENU_DIALOG_SHOW_DISMISS);
            event.setIntState(TransferConstant.MSG_DIALOG_DISMISS);
            EventBus.getDefault().post(event);

            unregisterWifiConnectChangedReceiver();
           EventBus.getDefault().unregister(this);
        }
    }

    @Subscribe
    public void onEvent(TransferEvent event){
        Log.e("transfer", "回调onEvent");
        if (event != null) {
            if(event.getType().equals(TransferConstant.RxBusEventType.WIFI_CONNECT_CHANGE_EVENT)) {
                checkWifiState(event.getState());
            }
        }
    }
}
