/*
 * Copyright © 2012 Intel Corporation
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice (including the next
 * paragraph) shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 * Authors:
 *    Lin A Jia <lin.a.jia@intel.com>
 *
 */
package com.intel.hdmi;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.content.Intent;
import android.preference.CheckBoxPreference;
import android.preference.DialogPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.telephony.TelephonyManager;

import com.intel.hdmi.OverscanPreference;
/**
 * Settings activity for HDMI settings and status information.
 * This is also the main entry point of HDMI in the application grid.
 */
public class HDMISettings extends PreferenceActivity
    implements OnPreferenceClickListener, OnPreferenceChangeListener
{
    private static final String TAG = "HdmiSettings";

    /** Keys for the settings */
    private static final String KEY_MODE = "hdmi_mode";
    private static final String KEY_HDMI_STATUS = "hdmi_status";
    private static final String KEY_CONNECT = "hdmi_connect";
    private static final String KEY_HDMI_DVI = "dvi_status";
    private static final String KEY_SCALE = "hdmi_scale";
    private static final String KEY_OVERSCAN = "hdmi_overscan";

    /** key for broadcast action */
    private static final String HDMI_Plug = "android.intent.action.HDMI_AUDIO_PLUG";
    private static final String HDMI_Observer_Info = "HdmiObserver.GET_HDMI_INFO";
    private static final String HDMI_Get_Info = "android.hdmi.GET_HDMI_INFO";
    private static final String HDMI_Set_Info = "android.hdmi.SET_HDMI_INFO";
    private static final String HDMI_Set_Scale = "android.hdmi.SET.HDMI_SCALE";
    private final String PHONE_INCALLSCREEN_FINISH = "com.android.phone_INCALLSCREEN_FINISH";
    private static final String HDMI_Get_DisplayBoot = "android.hdmi.GET_HDMI_Boot";
    private static final String HDMI_Set_DisplayBoot = "HdmiObserver.SET_HDMI_Boot";

    /** define information type: width, height, refresh, arrInterlace, arrRatio */
    private int[] arrWidth = null;
    private int[] arrHeight = null;
    private int[] arrRefresh = null;
    private int[] arrInterlace = null;
    private int[] arrRatio = null;
    private String[] infoString;

    /** record hdmi connected status */
    private boolean mHdmiStatus = false;
    private int mHdmiEnable = 1;
    private int mScaleType = 3;
    private int mWidth = 0;
    private int mHeight = 0;
    private int mRefresh = 0;
    private int mHDMIModeNum = 5;
    private final BroadcastReceiver mReceiver = new myBroadcastReceiver();
    private int mEdidChange =0;
    private int mDisplayBoot =0;
    private int state;
    private boolean HDMIDeviceStatus;
    private boolean HasIncomingCall = false;
    private boolean IncomingCallFinished = true;

    /** preference */
    private Preference hdmiStatusPref;
    private Preference hdmiDviPref;
    private CheckBoxPreference conPref;
    private ListPreference scalePreference;
    private ListPreference modePreference;
    private OverscanPreference osPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        intentFilter.addAction(HDMI_Plug);
        intentFilter.addAction(HDMI_Observer_Info);
        intentFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        intentFilter.addAction(PHONE_INCALLSCREEN_FINISH);
        intentFilter.addAction(HDMI_Set_DisplayBoot);

       //intentFilter.addAction(HDMI_Get_State);
        registerReceiver(mReceiver, intentFilter);
        //this.registerReceiver(mReceiver, intentFilter);
        addPreferencesFromResource(R.xml.hdmi_settings);

        hdmiStatusPref = (Preference)findPreference(KEY_HDMI_STATUS);
        hdmiStatusPref.setOnPreferenceClickListener(this);
        //hdmiStatusPref.setChecked(false);
        hdmiStatusPref.setEnabled(false);
        hdmiStatusPref.setSummary(R.string.hdmi_status_summary_off);
        hdmiStatusPref.setDependency(KEY_CONNECT);
       // hdmiStatusPref.setSelectable(false);

        hdmiDviPref = (Preference)findPreference(KEY_HDMI_DVI);
        hdmiDviPref.setOnPreferenceClickListener(this);
        //hdmiDviPref.setChecked(false);
        hdmiDviPref.setEnabled(false);
        hdmiDviPref.setSummary(R.string.dvi_status_summary_off);
        hdmiDviPref.setDependency(KEY_CONNECT);
       //hdmiDviPref.setSelectable(false);

        conPref = (CheckBoxPreference)findPreference(KEY_CONNECT);
        conPref.setOnPreferenceClickListener(this);

        scalePreference = (ListPreference)findPreference(KEY_SCALE);
        scalePreference.setOnPreferenceChangeListener(this);
        scalePreference.setDependency(KEY_HDMI_STATUS);

        osPreference = (OverscanPreference)findPreference(KEY_OVERSCAN);
        osPreference.setDependency(KEY_HDMI_STATUS);
        modePreference = (ListPreference)findPreference(KEY_MODE);
        /** init mode control*/
        CharSequence[] mEntries = null;
        CharSequence[] mValue = null;
        mEntries = new CharSequence[1];
        mEntries[0] = "0*0:0";
        mValue= new CharSequence[]{"0"};
        modePreference.setEntries(mEntries);
        modePreference.setEntryValues(mValue);
        restoreHDMISettingInfo();
        modePreference.setDependency(KEY_HDMI_STATUS);
        modePreference.setOnPreferenceChangeListener(this);

        Intent outIntent = new Intent(HDMI_Get_DisplayBoot);
        sendBroadcast(outIntent);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
    super.onDestroy();
    unregisterReceiver(mReceiver);
   }

    /**
     * From OnPreferenceClickListener
     */
    public boolean onPreferenceClick(Preference preference) {
        mHdmiEnable = ((CheckBoxPreference)preference).isChecked()? 1 : 0;
        Bundle bundle = new Bundle();
        bundle.putInt("Status", mHdmiEnable);
        Intent intent = new Intent(Intent.HDMI_SET_STATUS);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Log.i(TAG, "isHdmiEnable:"+ mHdmiEnable);
        sendBroadcast(intent);

        boolean prefStatus = ((CheckBoxPreference)preference).isChecked();
        if (HasIncomingCall) {
            hdmiStatusPref.setEnabled(false);
            hdmiStatusPref.setSummary(R.string.hdmi_status_summary_off);
        } else if (prefStatus && (state == 1)){
            hdmiStatusPref.setEnabled(true);
            hdmiStatusPref.setSummary(R.string.hdmi_status_summary_on);
        } else {
            hdmiStatusPref.setEnabled(false);
            hdmiStatusPref.setSummary(R.string.hdmi_status_summary_off);
        }

        /*int hPlug = 1;
        try {
            hPlug = Settings.System.getInt(getContentResolver(),
                Settings.System.HDMI_PLUG);
        } catch (SettingNotFoundException snfe) {
        }*/
        return true;
    }

    /**
     * From OnPreferenceChangeListener
     */
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // TODO Auto-generated method stub
        final String key = preference.getKey();
        osPreference.setEnabled(true);
        if (KEY_SCALE.equals(key)) {
        /** Scale control be click */
            //osPreference.setEnabled(true);
            int type = Integer.parseInt(newValue.toString());
            mScaleType = type;
            //osPreference.SetChange(true);
            Bundle bundle = new Bundle();
            bundle.putInt("Type", type);
            Intent intent = new Intent(HDMI_Set_Scale);
            intent.putExtras(bundle);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            sendBroadcast(intent);

            Log.i(TAG,"Scale Type");
            /** Disable center setp adjust */
            if (type == 2)
            {
                osPreference.setEnabled(false);
            }
            Log.i(TAG, "scale type" + type);
        }
        else {
        /** Mode contrl click */
            int index = Integer.parseInt(newValue.toString());
            mWidth = arrWidth[index];
            mHeight = arrHeight[index];
            mRefresh = arrRefresh[index];
            int mInterlace = arrInterlace[index];
            int mRatio = arrRatio[index];
            Bundle bundle = new Bundle();
            bundle.putInt("width", mWidth);
            bundle.putInt("height", mHeight);
            bundle.putInt("refresh", mRefresh);
            bundle.putInt("interlace", mInterlace);
            bundle.putInt("ratio", mRatio);
            Intent intent = new Intent(HDMI_Set_Info);
            intent.putExtras(bundle);
            sendBroadcast(intent);
            /** disable overscan when center Scale is set */
            if (mScaleType == 2)
            {
                osPreference.setEnabled(false);
            }
        }
        return true;
    }

    /**
     * Init mode infomation from hdmiObserver
    **/
    private void UpdateInfo(String[] info, ListPreference target) {
        // TODO Auto-generated method stub
        Log.i(TAG, "isHdmiConnected" + mHdmiStatus);
        if (mHdmiStatus) {
            Log.i(TAG, "updateInfo:" + info.length);
            CharSequence[] mEntries = new CharSequence[info.length];
            CharSequence[] mValue = new CharSequence[info.length];
            for(int i=0; i < info.length; i++)
            {
                mEntries[i] = info[i];
                mValue[i] = Integer.toString(i);
            Log.i(TAG, "updateInfo:"+ i+ ":"+ info[i]);
            }
            Log.i(TAG, "updateInfo:target="+ target+ ":");
            target.setEntries(mEntries);
            target.setEntryValues(mValue);
        }
    }

    private void clearStatus() {
        Log.i(TAG, "clear Status");
        scalePreference.setValueIndex(0);
        osPreference.SetChange(true);
        modePreference.setValueIndex(-1);
    }

    private void storeHDMISettingInfo(int count) {
        SharedPreferences preferences =
            modePreference.getContext().getSharedPreferences("com.intel.hdmi_preferences",
                Context.MODE_MULTI_PROCESS);
        Editor editor = preferences.edit();
        editor.putInt("count", count);
        for (int j = 0; j < count; j++) {
            String str = "infoString" + Integer.toString(j);
            editor.putString(str, infoString[j]);
        }
        editor.commit();
    }

    private void restoreHDMISettingInfo() {
        String[] infoStr;
        SharedPreferences preferences =
            modePreference.getContext().getSharedPreferences("com.intel.hdmi_preferences",
                Context.MODE_MULTI_PROCESS);
        int count = 0;
        if (preferences != null) {
                count = preferences.getInt("count", mHDMIModeNum);
                infoStr = new String[count];
                CharSequence[] mmEntries = new CharSequence[count];
                CharSequence[] mmValue = new CharSequence[count];
                for (int i = 0; i < count; i++) {
                     String str = "infoString" + Integer.toString(i);
                     infoStr[i] = preferences.getString(str, "0");
                     mmEntries[i] = infoStr[i];
                     mmValue[i] = Integer.toString(i);
                }
                modePreference.setEntries(mmEntries);
                modePreference.setEntryValues(mmValue);
        }
    }

        private class myBroadcastReceiver extends BroadcastReceiver {
                @Override
                public void onReceive(Context context, Intent intent) {
                // TODO Auto-generated method stub
                Log.i(TAG, "myBroadcaseReceiver:" + intent.toString());
                String action = intent.getAction();
                HDMIDeviceStatus = conPref.isChecked();
                //if (action.equals(HDMI_Plug)) {
                if (action.equals(Intent.ACTION_HDMI_AUDIO_PLUG)) {
                    /** HDMI Plugin*/
                    Log.i(TAG, "HDMI_PLUG");
                    Intent outIntent = new Intent(HDMI_Get_Info);
                    context.sendBroadcast(outIntent);
                    Log.i(TAG, "sendBroadcast hdmi_plug");
                    state = intent.getIntExtra("state", 0);
                    Log.i(TAG,"state:" + state);
                } else if (action.equals(HDMI_Set_DisplayBoot)) {
                    mDisplayBoot= intent.getIntExtra("DisplayBoot", 0);
                    Log.i(TAG,"mDisplayBoot:" + mDisplayBoot);
                    if(mDisplayBoot == 1)
                        conPref.setChecked(true);
                }
                else if (action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED) ||
                                action.equals(PHONE_INCALLSCREEN_FINISH)) {
                    if (action.equals(PHONE_INCALLSCREEN_FINISH)) {
                        IncomingCallFinished = true;
                        HasIncomingCall = false;
                        Log.i(TAG, "incoming call screen finished: " + IncomingCallFinished);
                    } else {
                        if (TelephonyManager.EXTRA_STATE == null ||
                                    TelephonyManager.EXTRA_STATE_RINGING == null)
                            return;
                        String extras = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                        if (extras == null)
                            return;
                        if (extras.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                            IncomingCallFinished = false;
                            HasIncomingCall = true;
                        } else if (extras.equals(TelephonyManager.EXTRA_STATE_IDLE))
                            HasIncomingCall = false;
                        else
                            return;
                        Log.i(TAG, "incoming call " +
                                (HasIncomingCall == true ? "initiated" : "terminated"));
                    }

                    if (HasIncomingCall) {
                        hdmiStatusPref.setEnabled(false);
                        mHdmiStatus = false;
                        hdmiStatusPref.setSummary(R.string.hdmi_status_summary_off);
                    } else if ((HDMIDeviceStatus && state == 1)
                        && ((!HasIncomingCall) && IncomingCallFinished)) {
                        mHdmiStatus = true;
                        hdmiStatusPref.setEnabled(true);
                        hdmiStatusPref.setSummary(R.string.hdmi_status_summary_on);
                    }
                }
                else if (action.equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                /** HDMI Plugout*/
                Log.i(TAG, "myBroadcaseReceiver:" + intent.toString());
                //hdmiStatusPref.setChecked(false);
                Dialog scalePreDialog = scalePreference.getDialog();
                Dialog modePreDialog = modePreference.getDialog();
                Dialog osPreDialog = osPreference.getDialog();
                if (scalePreDialog != null) {
                    scalePreDialog.dismiss();
                }
                if (modePreDialog != null) {
                    modePreDialog.dismiss();
                }
                if (osPreDialog != null) {
                    osPreDialog.dismiss();
                }
                hdmiStatusPref.setEnabled(false);
                mHdmiStatus = false;
                hdmiStatusPref.setSummary(R.string.hdmi_status_summary_off);
                //clearStatus();
            }
            else if (action.equals(HDMI_Observer_Info)) {
                Log.i(TAG, "get broadcast");
                /** Get mode infomation from hdmiobserver*/
                Log.i(TAG, "myBroadcaseReceiver:" + intent.toString());
                Bundle extras = intent.getExtras();
                if (extras == null)
                    return;
                int count = extras.getInt("count");
                mEdidChange = extras.getInt("EdidChange");
                HasIncomingCall = extras.getBoolean("hasIncomingCall");
                Log.i(TAG, "EdidChange: "+ mEdidChange);
                Log.i(TAG, "HasIncomingCall: "+ HasIncomingCall);

                infoString = new String[count];
                arrWidth = new int[count];
                arrWidth = (int[]) extras.getSerializable("width");
                arrHeight = new int[count];
                arrHeight = (int[]) extras.getSerializable("height");
                arrRefresh = new int[count];
                arrRefresh = (int[]) extras.getSerializable("refresh");
                arrInterlace = new int[count];
                arrInterlace = (int[]) extras.getSerializable("interlace");
                arrRatio = new int[count];
                arrRatio = (int[]) extras.getSerializable("ratio");

                for (int i = 0; i < count; i++){
                    infoString[i] = arrWidth[i] + "*" + arrHeight[i];
                    if (arrInterlace[i] != 0)
                        infoString[i] += "I";
                    else
                        infoString[i] += "P";

                    infoString[i] += "@" + arrRefresh[i]+ "Hz";
		    if (arrRatio[i] == 1)
			infoString[i] += " [16:9]";
		    else if (arrRatio[i] == 2)
			infoString[i] += " [4:3]";
                }

                if (HasIncomingCall) {
                    //hdmiStatusPref.setChecked(true);
                    hdmiStatusPref.setEnabled(false);
                    mHdmiStatus = false;
                }
                else {
                    hdmiStatusPref.setEnabled(true);
                    mHdmiStatus = true;
                }
                //UpdateInfo(infoString, (ListPreference)findPreference(KEY_MODE));
                UpdateInfo(infoString, modePreference);

                if (mDisplayBoot == 1) {
                    mScaleType = 3;
                } else {
                    mScaleType = Integer.parseInt(scalePreference.getValue());
                }
                if (mScaleType == 2) {
                    osPreference.setEnabled(false);
                } else {
                    osPreference.setEnabled(true);
                }

                Log.i(TAG, "After UpdateInfo,mDisplayBoot: " + mDisplayBoot);
                if((mEdidChange == 1) || (mDisplayBoot == 1)) {
                    modePreference.setValueIndex(0);
                    osPreference.SetChange(true);
                    scalePreference.setValueIndex(0);
                    mHdmiEnable = 1;
                    mEdidChange = 0;
                }
                if(mDisplayBoot == 1) {
                    mDisplayBoot = 0;
                }

                String value = modePreference.getValue();
                if(value != null){
                    int index = Integer.parseInt(value);
                    mWidth = arrWidth[index];
                    mHeight = arrHeight[index];
                    Log.i(TAG, "update mWidth");
                }
                Log.i(TAG, "mWidth:"+mWidth+", mHeight:"+mHeight);

                storeHDMISettingInfo(count);
            }
        }//onRecive
    }//myBroadcastReceiver
}

