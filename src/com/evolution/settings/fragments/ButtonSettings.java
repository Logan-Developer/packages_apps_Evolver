/*
 * Copyright (C) 2016 The CyanogenMod project
 * Copyright (C) 2017-2018 The LineageOS project
 * Copyright (C) 2018 The PixelExperience Project
 * Copyright (C) 2019-2021 The Evolution X Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolution.settings.fragments;

import static com.android.internal.util.custom.hwkeys.DeviceKeysConstants.*;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;

import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.custom.hardware.LineageHardwareManager;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.util.custom.NavbarUtils;
import com.android.internal.util.evolution.EvolutionUtils;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.evolution.settings.buttons.ButtonSettingsUtils;
import com.evolution.settings.buttons.preference.*;
import com.evolution.settings.preference.CustomDialogPreference;
import com.evolution.settings.preference.SystemSettingSwitchPreference;

import java.util.List;
import java.util.UUID;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ButtonSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "ButtonSettings";

    private static final String KEY_HWKEYS_ENABLED = "hardware_keys_enable";
    private static final String KEY_ANBI = "anbi_enabled";
    private static final String KEY_BUTTON_BACKLIGHT = "button_backlight";
    private static final String KEY_HOME_LONG_PRESS = "hardware_keys_home_long_press";
    private static final String KEY_HOME_DOUBLE_TAP = "hardware_keys_home_double_tap";
    private static final String KEY_BACK_LONG_PRESS = "hardware_keys_back_long_press";
    private static final String KEY_MENU_PRESS = "hardware_keys_menu_press";
    private static final String KEY_MENU_LONG_PRESS = "hardware_keys_menu_long_press";
    private static final String KEY_ASSIST_PRESS = "hardware_keys_assist_press";
    private static final String KEY_ASSIST_LONG_PRESS = "hardware_keys_assist_long_press";
    private static final String KEY_APP_SWITCH_PRESS = "hardware_keys_app_switch_press";
    private static final String KEY_APP_SWITCH_LONG_PRESS = "hardware_keys_app_switch_long_press";
    private static final String KEY_GESTURE_SYSTEM = "gesture_system_navigation";
    private static final String DISABLE_NAV_KEYS = "disable_nav_keys";
    private static final String KEY_SWAP_CAPACITIVE_KEYS = "swap_capacitive_keys";

    private static final String CATEGORY_HW = "hw_keys";
    private static final String CATEGORY_HOME = "home_key";
    private static final String CATEGORY_BACK = "back_key";
    private static final String CATEGORY_MENU = "menu_key";
    private static final String CATEGORY_ASSIST = "assist_key";
    private static final String CATEGORY_APPSWITCH = "app_switch_key";
    private static final String CATEGORY_CAMERA = "camera_key";
    private static final String CATEGORY_BACKLIGHT = "key_backlight";

    private static final String LAYOUT_SETTINGS = "navbar_layout_views";
    private static final String NAVIGATION_BAR_INVERSE = "navbar_inverse_layout";
    private static final String PIXEL_NAV_ANIMATION = "pixel_nav_animation";
    private static final String VOLUME_KEY_CURSOR_CONTROL = "volume_key_cursor_control";

    private ListPreference mHomeLongPressAction;
    private ListPreference mHomeDoubleTapAction;
    private ListPreference mBackLongPressAction;
    private ListPreference mMenuPressAction;
    private ListPreference mMenuLongPressAction;
    private ListPreference mAssistPressAction;
    private ListPreference mAssistLongPressAction;
    private ListPreference mAppSwitchPressAction;
    private ListPreference mAppSwitchLongPressAction;
    private SwitchPreference mCameraWakeScreen;
    private SwitchPreference mCameraSleepOnRelease;
    private SwitchPreference mCameraLaunch;
    private SwitchPreference mDisableNavigationKeys;
    private SwitchPreference mHardwareKeysEnable;
    private SwitchPreference mAnbi;
    private SwitchPreference mSwapCapacitiveKeys;
    private Preference mGestureSystemNavigation;
    private ButtonBacklightBrightness backlight;

    private Handler mHandler;

    private ListPreference mVolumeKeyCursorControl;
    private Preference mLayoutSettings;
    private SwitchPreference mSwapNavButtons;
    private SystemSettingSwitchPreference mPixelNavAnimation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.evolution_settings_button);

        final Resources res = getResources();
        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        final int deviceKeys = res.getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);
        final int deviceWakeKeys = res.getInteger(
                com.android.internal.R.integer.config_deviceHardwareWakeKeys);

        final boolean hasHomeKey = ButtonSettingsUtils.hasHomeKey(getActivity());
        final boolean hasBackKey = ButtonSettingsUtils.hasBackKey(getActivity());
        final boolean hasMenuKey = ButtonSettingsUtils.hasMenuKey(getActivity());
        final boolean hasAssistKey = ButtonSettingsUtils.hasAssistKey(getActivity());
        final boolean hasAppSwitchKey = ButtonSettingsUtils.hasAppSwitchKey(getActivity());
        final boolean hasCameraKey = ButtonSettingsUtils.hasCameraKey(getActivity());

        final boolean showHomeWake = ButtonSettingsUtils.canWakeUsingHomeKey(getActivity());
        final boolean showBackWake = ButtonSettingsUtils.canWakeUsingBackKey(getActivity());
        final boolean showMenuWake = ButtonSettingsUtils.canWakeUsingMenuKey(getActivity());
        final boolean showAssistWake = ButtonSettingsUtils.canWakeUsingAssistKey(getActivity());
        final boolean showAppSwitchWake = ButtonSettingsUtils.canWakeUsingAppSwitchKey(getActivity());
        final boolean showCameraWake = ButtonSettingsUtils.canWakeUsingCameraKey(getActivity());

        boolean hasAnyBindableKey = false;
        final PreferenceCategory hwCategory = prefScreen.findPreference(CATEGORY_HW);
        final PreferenceCategory homeCategory = prefScreen.findPreference(CATEGORY_HOME);
        final PreferenceCategory backCategory = prefScreen.findPreference(CATEGORY_BACK);
        final PreferenceCategory menuCategory = prefScreen.findPreference(CATEGORY_MENU);
        final PreferenceCategory assistCategory = prefScreen.findPreference(CATEGORY_ASSIST);
        final PreferenceCategory appSwitchCategory = prefScreen.findPreference(CATEGORY_APPSWITCH);
        final PreferenceCategory cameraCategory = prefScreen.findPreference(CATEGORY_CAMERA);

        mAnbi = (SwitchPreference) findPreference(KEY_ANBI);
        mGestureSystemNavigation = (Preference) findPreference(KEY_GESTURE_SYSTEM);
        mLayoutSettings = findPreference(LAYOUT_SETTINGS);
        mPixelNavAnimation = findPreference(PIXEL_NAV_ANIMATION);
        mSwapNavButtons = findPreference(NAVIGATION_BAR_INVERSE);

        mHandler = new Handler();

        // Force Navigation bar related options
        mDisableNavigationKeys = findPreference(DISABLE_NAV_KEYS);

        mHardwareKeysEnable = (SwitchPreference) findPreference(KEY_HWKEYS_ENABLED);
        if (mHardwareKeysEnable != null && isKeyDisablerSupported(getActivity())) {
            mHardwareKeysEnable.setOnPreferenceChangeListener(this);
        } else {
            mHardwareKeysEnable.setVisible(false);
        }

        mSwapCapacitiveKeys = findPreference(KEY_SWAP_CAPACITIVE_KEYS);
        if (mSwapCapacitiveKeys != null && !isKeySwapperSupported(getActivity())) {
            mSwapCapacitiveKeys.setVisible(false);
            mSwapCapacitiveKeys = null;
        }

        Action defaultHomeLongPressAction = Action.fromIntSafe(res.getInteger(
                com.android.internal.R.integer.config_longPressOnHomeBehavior));
        Action defaultHomeDoubleTapAction = Action.fromIntSafe(res.getInteger(
                com.android.internal.R.integer.config_doubleTapOnHomeBehavior));
        Action defaultBackLongPressAction = Action.fromIntSafe(res.getInteger(
                com.android.internal.R.integer.config_longPressOnBackBehavior));
        Action defaultAppSwitchPressAction = Action.fromIntSafe(res.getInteger(
                com.android.internal.R.integer.config_pressOnAppSwitchBehavior));
        Action defaultAppSwitchLongPressAction = Action.fromIntSafe(res.getInteger(
                com.android.internal.R.integer.config_longPressOnAppSwitchBehavior));
        Action defaultAssistPressAction = Action.fromIntSafe(res.getInteger(
                com.android.internal.R.integer.config_pressOnAssistBehavior));
        Action defaultAssistLongPressAction = Action.fromIntSafe(res.getInteger(
                com.android.internal.R.integer.config_longPressOnAssistBehavior));
        Action homeLongPressAction = Action.fromSettings(resolver,
                Settings.System.KEY_HOME_LONG_PRESS_ACTION,
                defaultHomeLongPressAction);
        Action homeDoubleTapAction = Action.fromSettings(resolver,
                Settings.System.KEY_HOME_DOUBLE_TAP_ACTION,
                defaultHomeDoubleTapAction);
        Action appSwitchLongPressAction = Action.fromSettings(resolver,
                Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION,
                defaultAppSwitchLongPressAction);

        // Only visible on devices that does not have a navigation bar already
        if (NavbarUtils.canDisable(getActivity())) {
            // Remove keys that can be provided by the navbar
            updateDisableNavkeysOption();
            updateDisableNavkeysCategories(mDisableNavigationKeys.isChecked());
            mDisableNavigationKeys.setDisableDependentsState(true);
        } else {
            mDisableNavigationKeys.setChecked(true);
            mDisableNavigationKeys.setEnabled(false);
        }

        if (hasHomeKey) {
            if (!showHomeWake) {
                homeCategory.removePreference(findPreference(Settings.System.HOME_WAKE_SCREEN));
            }

            mHomeLongPressAction = initList(KEY_HOME_LONG_PRESS, homeLongPressAction);
            mHomeDoubleTapAction = initList(KEY_HOME_DOUBLE_TAP, homeDoubleTapAction);

            hasAnyBindableKey = true;
        }
        if (!hasHomeKey || homeCategory.getPreferenceCount() == 0) {
            homeCategory.setVisible(false);
        }

        if (hasBackKey) {
            if (!showBackWake) {
                backCategory.removePreference(findPreference(Settings.System.BACK_WAKE_SCREEN));
            }

            Action backLongPressAction = Action.fromSettings(resolver,
                    Settings.System.KEY_BACK_LONG_PRESS_ACTION,
                    defaultBackLongPressAction);
            mBackLongPressAction = initList(KEY_BACK_LONG_PRESS, backLongPressAction);

            hasAnyBindableKey = true;
        }
        if (!hasBackKey || backCategory.getPreferenceCount() == 0) {
            backCategory.setVisible(false);
        }

        if (hasMenuKey) {
            if (!showMenuWake) {
                menuCategory.removePreference(findPreference(Settings.System.MENU_WAKE_SCREEN));
            }

            Action pressAction = Action.fromSettings(resolver,
                    Settings.System.KEY_MENU_ACTION, Action.MENU);
            mMenuPressAction = initList(KEY_MENU_PRESS, pressAction);

            Action longPressAction = Action.fromSettings(resolver,
                        Settings.System.KEY_MENU_LONG_PRESS_ACTION,
                        hasAssistKey ? Action.NOTHING : Action.APP_SWITCH);
            mMenuLongPressAction = initList(KEY_MENU_LONG_PRESS, longPressAction);

            hasAnyBindableKey = true;
        }
        if (!hasMenuKey || menuCategory.getPreferenceCount() == 0) {
            menuCategory.setVisible(false);
        }

        if (hasAssistKey) {
            if (!showAssistWake) {
                assistCategory.removePreference(findPreference(Settings.System.ASSIST_WAKE_SCREEN));
            }

            Action pressAction = Action.fromSettings(resolver,
                    Settings.System.KEY_ASSIST_ACTION, defaultAssistPressAction);
            mAssistPressAction = initList(KEY_ASSIST_PRESS, pressAction);

            Action longPressAction = Action.fromSettings(resolver,
                    Settings.System.KEY_ASSIST_LONG_PRESS_ACTION, defaultAssistLongPressAction);
            mAssistLongPressAction = initList(KEY_ASSIST_LONG_PRESS, longPressAction);

            hasAnyBindableKey = true;
        }
        if (!hasAssistKey || assistCategory.getPreferenceCount() == 0) {
            assistCategory.setVisible(false);
        }

        if (hasAppSwitchKey) {
            if (!showAppSwitchWake) {
                appSwitchCategory.removePreference(findPreference(
                        Settings.System.APP_SWITCH_WAKE_SCREEN));
            }

            Action pressAction = Action.fromSettings(resolver,
                    Settings.System.KEY_APP_SWITCH_ACTION, defaultAppSwitchPressAction);
            mAppSwitchPressAction = initList(KEY_APP_SWITCH_PRESS, pressAction);

            mAppSwitchLongPressAction = initList(KEY_APP_SWITCH_LONG_PRESS, appSwitchLongPressAction);

            hasAnyBindableKey = true;
        }
        if (!hasAppSwitchKey || appSwitchCategory.getPreferenceCount() == 0) {
            appSwitchCategory.setVisible(false);
        }

        if (hasCameraKey) {
            mCameraWakeScreen = findPreference(Settings.System.CAMERA_WAKE_SCREEN);
            mCameraSleepOnRelease = findPreference(Settings.System.CAMERA_SLEEP_ON_RELEASE);
            mCameraLaunch = findPreference(Settings.System.CAMERA_LAUNCH);

            if (!showCameraWake) {
                mCameraWakeScreen.setVisible(false);
            }
            // Only show 'Camera sleep on release' if the device has a focus key
            if (res.getBoolean(com.android.internal.R.bool.config_singleStageCameraKey)) {
                mCameraSleepOnRelease.setVisible(false);
            }
        }
        if (!hasCameraKey || cameraCategory.getPreferenceCount() == 0) {
            cameraCategory.setVisible(false);
        }

        backlight = findPreference(KEY_BUTTON_BACKLIGHT);
        if (!ButtonSettingsUtils.hasButtonBacklightSupport(getActivity())
                && !ButtonSettingsUtils.hasKeyboardBacklightSupport(getActivity())) {
            backlight.setVisible(false);
        }

        if (mCameraWakeScreen != null) {
            if (mCameraSleepOnRelease != null && !res.getBoolean(
                    com.android.internal.R.bool.config_singleStageCameraKey)) {
                mCameraSleepOnRelease.setDependency(Settings.System.CAMERA_WAKE_SCREEN);
            }
        }

        // On three button nav
        if (EvolutionUtils.isThemeEnabled("com.android.internal.systemui.navbar.threebutton")) {
            mGestureSystemNavigation.setSummary(getString(R.string.legacy_navigation_title));
        // On two button nav
        } else if (EvolutionUtils.isThemeEnabled("com.android.internal.systemui.navbar.twobutton")) {
            mGestureSystemNavigation.setSummary(getString(R.string.swipe_up_to_switch_apps_title));
        // On gesture nav
        } else {
            mGestureSystemNavigation.setSummary(getString(R.string.edge_to_edge_navigation_title));
            mLayoutSettings.setVisible(false);
            mPixelNavAnimation.setVisible(false);
            mSwapNavButtons.setVisible(false);
        }

        // Override key actions on Go devices in order to hide any unsupported features
        if (ActivityManager.isLowRamDeviceStatic()) {
            String[] actionEntriesGo = res.getStringArray(R.array.hardware_keys_action_entries_go);
            String[] actionValuesGo = res.getStringArray(R.array.hardware_keys_action_values_go);

            if (hasHomeKey) {
                mHomeLongPressAction.setEntries(actionEntriesGo);
                mHomeLongPressAction.setEntryValues(actionValuesGo);

                mHomeDoubleTapAction.setEntries(actionEntriesGo);
                mHomeDoubleTapAction.setEntryValues(actionValuesGo);
            }

            if (hasMenuKey) {
                mMenuPressAction.setEntries(actionEntriesGo);
                mMenuPressAction.setEntryValues(actionValuesGo);

                mMenuLongPressAction.setEntries(actionEntriesGo);
                mMenuLongPressAction.setEntryValues(actionValuesGo);
            }

            if (hasAssistKey) {
                mAssistPressAction.setEntries(actionEntriesGo);
                mAssistPressAction.setEntryValues(actionValuesGo);

                mAssistLongPressAction.setEntries(actionEntriesGo);
                mAssistLongPressAction.setEntryValues(actionValuesGo);
            }

            if (hasAppSwitchKey) {
                mAppSwitchPressAction.setEntries(actionEntriesGo);
                mAppSwitchPressAction.setEntryValues(actionValuesGo);

                mAppSwitchLongPressAction.setEntries(actionEntriesGo);
                mAppSwitchLongPressAction.setEntryValues(actionValuesGo);
            }
        }

        if (!hasHomeKey && !hasBackKey && !hasMenuKey && !hasAssistKey && !hasAppSwitchKey) {
            mAnbi.setVisible(false);
            mAnbi = null;
        } else if (isKeyDisablerSupported(getActivity())) {
            mAnbi.setEnabled(Settings.System.getIntForUser(resolver,
                    Settings.System.HARDWARE_KEYS_ENABLE, 1,
                    UserHandle.USER_CURRENT) == 1);
        }

        if (!hasHomeKey && !hasBackKey && !hasMenuKey && !hasAssistKey && !hasAppSwitchKey && !hasCameraKey) {
            hwCategory.setVisible(false);
        }

        // volume key cursor control
        mVolumeKeyCursorControl = findPreference(VOLUME_KEY_CURSOR_CONTROL);
        if (mVolumeKeyCursorControl != null) {
            mVolumeKeyCursorControl.setOnPreferenceChangeListener(this);
            int volumeRockerCursorControl = Settings.System.getInt(getContentResolver(),
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL, 0);
            mVolumeKeyCursorControl.setValue(Integer.toString(volumeRockerCursorControl));
            mVolumeKeyCursorControl.setSummary(mVolumeKeyCursorControl.getEntry());
        }
    }

    private static boolean isKeyDisablerSupported(Context context) {
        final LineageHardwareManager hardware = LineageHardwareManager.getInstance(context);
        return hardware.isSupported(LineageHardwareManager.FEATURE_KEY_DISABLE);
    }

    private static boolean isKeySwapperSupported(Context context) {
        final LineageHardwareManager hardware = LineageHardwareManager.getInstance(context);
        return hardware.isSupported(LineageHardwareManager.FEATURE_KEY_SWAP);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private ListPreference initList(String key, Action value) {
        return initList(key, value.ordinal());
    }

    private ListPreference initList(String key, int value) {
        ListPreference list = getPreferenceScreen().findPreference(key);
        if (list == null) return null;
        list.setValue(Integer.toString(value));
        list.setSummary(list.getEntry());
        list.setOnPreferenceChangeListener(this);
        return list;
    }

    private void handleListChange(ListPreference pref, Object newValue, String setting) {
        String value = (String) newValue;
        int index = pref.findIndexOfValue(value);
        pref.setSummary(pref.getEntries()[index]);
        Settings.System.putInt(getContentResolver(), setting, Integer.valueOf(value));
    }

    private void handleSystemListChange(ListPreference pref, Object newValue, String setting) {
        String value = (String) newValue;
        int index = pref.findIndexOfValue(value);
        pref.setSummary(pref.getEntries()[index]);
        Settings.System.putInt(getContentResolver(), setting, Integer.valueOf(value));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mHardwareKeysEnable) {
            boolean value = (Boolean) newValue;
            if (mAnbi != null) {
                mAnbi.setEnabled(!value);
            }
            if (backlight != null) {
                backlight.setEnabled(!value);
            }
            return true;
        } else if (preference == mHomeLongPressAction) {
            handleListChange((ListPreference) preference, newValue,
                    Settings.System.KEY_HOME_LONG_PRESS_ACTION);
            return true;
        } else if (preference == mHomeDoubleTapAction) {
            handleListChange((ListPreference) preference, newValue,
                    Settings.System.KEY_HOME_DOUBLE_TAP_ACTION);
            return true;
        } else if (preference == mBackLongPressAction) {
            handleListChange((ListPreference) preference, newValue,
                    Settings.System.KEY_BACK_LONG_PRESS_ACTION);
            return true;
        } else if (preference == mMenuPressAction) {
            handleListChange(mMenuPressAction, newValue,
                    Settings.System.KEY_MENU_ACTION);
            return true;
        } else if (preference == mMenuLongPressAction) {
            handleListChange(mMenuLongPressAction, newValue,
                    Settings.System.KEY_MENU_LONG_PRESS_ACTION);
            return true;
        } else if (preference == mAssistPressAction) {
            handleListChange(mAssistPressAction, newValue,
                    Settings.System.KEY_ASSIST_ACTION);
            return true;
        } else if (preference == mAssistLongPressAction) {
            handleListChange(mAssistLongPressAction, newValue,
                    Settings.System.KEY_ASSIST_LONG_PRESS_ACTION);
            return true;
        } else if (preference == mAppSwitchPressAction) {
            handleListChange(mAppSwitchPressAction, newValue,
                    Settings.System.KEY_APP_SWITCH_ACTION);
            return true;
        } else if (preference == mAppSwitchLongPressAction) {
            handleListChange((ListPreference) preference, newValue,
                    Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION);
            return true;
        } else if (preference == mVolumeKeyCursorControl) {
            String volumeKeyCursorControl = (String) newValue;
            int volumeKeyCursorControlValue = Integer.parseInt(volumeKeyCursorControl);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL, volumeKeyCursorControlValue);
            int volumeKeyCursorControlIndex = mVolumeKeyCursorControl
                    .findIndexOfValue(volumeKeyCursorControl);
            mVolumeKeyCursorControl
                    .setSummary(mVolumeKeyCursorControl.getEntries()[volumeKeyCursorControlIndex]);
            return true;
        }
        return false;
    }

    private void writeDisableNavkeysOption(boolean enabled) {
        NavbarUtils.setEnabled(getActivity(), enabled);
    }

    private void updateDisableNavkeysOption() {
        mDisableNavigationKeys.setChecked(NavbarUtils.isEnabled(getActivity()));
    }

    private void updateDisableNavkeysCategories(boolean navbarEnabled) {
        final PreferenceScreen prefScreen = getPreferenceScreen();

        /* Disable hw-key options if they're disabled */
        final PreferenceCategory homeCategory = prefScreen.findPreference(CATEGORY_HOME);
        final PreferenceCategory backCategory = prefScreen.findPreference(CATEGORY_BACK);
        final PreferenceCategory menuCategory = prefScreen.findPreference(CATEGORY_MENU);
        final PreferenceCategory assistCategory = prefScreen.findPreference(CATEGORY_ASSIST);
        final PreferenceCategory appSwitchCategory = prefScreen.findPreference(CATEGORY_APPSWITCH);
        final ButtonBacklightBrightness backlight = prefScreen.findPreference(KEY_BUTTON_BACKLIGHT);

        /* Toggle backlight control depending on navbar state, force it to
           off if enabling */
        if (backlight != null) {
            backlight.setEnabled(!navbarEnabled);
            backlight.updateSummary();
        }

        if (homeCategory != null) {
            homeCategory.setEnabled(!navbarEnabled);
        }
        if (backCategory != null) {
            backCategory.setEnabled(!navbarEnabled);
        }
        if (menuCategory != null) {
            menuCategory.setEnabled(!navbarEnabled);
        }
        if (assistCategory != null) {
            assistCategory.setEnabled(!navbarEnabled);
        }
        if (appSwitchCategory != null) {
            appSwitchCategory.setEnabled(!navbarEnabled);
        }
        if (mSwapCapacitiveKeys != null) {
            mSwapCapacitiveKeys.setEnabled(!navbarEnabled);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mDisableNavigationKeys) {
            mDisableNavigationKeys.setEnabled(false);
            writeDisableNavkeysOption(mDisableNavigationKeys.isChecked());
            updateDisableNavkeysOption();
            updateDisableNavkeysCategories(true);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        mDisableNavigationKeys.setEnabled(true);
                        updateDisableNavkeysCategories(mDisableNavigationKeys.isChecked());
                    }catch(Exception e){
                    }
                }
            }, 1000);
        }

        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.EVOLVER;
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference.getKey() == null) {
            // Auto-key preferences that don't have a key, so the dialog can find them.
            preference.setKey(UUID.randomUUID().toString());
        }
        DialogFragment f = null;
        if (preference instanceof CustomDialogPreference) {
            f = CustomDialogPreference.CustomPreferenceDialogFragment
                    .newInstance(preference.getKey());
        } else {
            super.onDisplayPreferenceDialog(preference);
            return;
        }
        f.setTargetFragment(this, 0);
        f.show(getFragmentManager(), "dialog_preference");
        onDialogShowing();
    }

    /**
     * For Search.
     */

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.evolution_settings_button);
}
