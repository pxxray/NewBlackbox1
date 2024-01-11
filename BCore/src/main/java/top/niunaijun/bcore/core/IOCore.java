package top.niunaijun.bcore.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Process;
import android.text.TextUtils;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import top.niunaijun.bcore.BlackBoxCore;
import top.niunaijun.bcore.app.BActivityThread;
import top.niunaijun.bcore.core.env.BEnvironment;
import top.niunaijun.bcore.utils.FileUtils;
import top.niunaijun.bcore.utils.TrieTree;

@SuppressLint("SdCardPath")
public class IOCore {
    public static final String TAG = "IOCore";

    private static final IOCore sIOCore = new IOCore();
    private static final TrieTree mTrieTree = new TrieTree();
    private static final TrieTree sBlackTree = new TrieTree();
    private final Map<String, String> mRedirectMap = new LinkedHashMap<>();

    public static IOCore get() {
        return sIOCore;
    }

    // 路径前缀匹配，重定向
    // /data/data/com.google/  ----->  /data/data/com.virtual/data/com.google/
    public void addRedirect(String origPath, String redirectPath) {
        if (TextUtils.isEmpty(origPath) || TextUtils.isEmpty(redirectPath) || mRedirectMap.get(origPath) != null) {
            return;
        }
        // Add the key to TrieTree
        mTrieTree.add(origPath);
        mRedirectMap.put(origPath, redirectPath);
        File redirectFile = new File(redirectPath);
        if (!redirectFile.exists()) {
            FileUtils.mkdirs(redirectPath);
        }
        NativeCore.addIORule(origPath, redirectPath);
    }

    public void addBlackRedirect(String path) {
        if (TextUtils.isEmpty(path)) {
            return;
        }
        sBlackTree.add(path);
        NativeCore.addWhiteList(path);
    }

    public String redirectPath(String path) {
        if (TextUtils.isEmpty(path)) {
            return path;
        }
        if (path.contains("/blackbox/")) {
            return path;
        }
        String search = sBlackTree.search(path);
        if (!TextUtils.isEmpty(search)) {
            return search;
        }

        // Search the key from TrieTree
        String key = mTrieTree.search(path);
        if (!TextUtils.isEmpty(key)) {
            path = path.replace(key, Objects.requireNonNull(mRedirectMap.get(key)));
        }
        return path;
    }

    public File redirectPath(File path) {
        if (path == null) {
            return null;
        }
        String pathStr = path.getAbsolutePath();
        return new File(redirectPath(pathStr));
    }

    public String redirectPath(String path, Map<String, String> rule) {
        if (TextUtils.isEmpty(path)) {
            return path;
        }
        // Search the key from TrieTree
        String key = mTrieTree.search(path);
        if (!TextUtils.isEmpty(key)) {
            path = path.replace(key, Objects.requireNonNull(rule.get(key)));
        }
        return path;
    }

    // 由于正常情况Application已完成重定向，以下重定向是怕代码写死。
    public void enableRedirect(Context context) {
        Map<String, String> rule = new LinkedHashMap<>();
        Set<String> blackRule = new HashSet<>();

        try {
            // 修改所有已安装的路径, 支持xposed module
            int systemUserId = BlackBoxCore.getHostUserId();
            List<ApplicationInfo> installedApplications = BlackBoxCore.getBPackageManager().getInstalledApplications(PackageManager.GET_META_DATA, BActivityThread.getUserId());
            for (ApplicationInfo packageInfo : installedApplications) {
                rule.put(String.format("/data/data/%s/lib", packageInfo.packageName), packageInfo.nativeLibraryDir);
                rule.put(String.format("/data/user/%d/%s/lib", systemUserId, packageInfo.packageName), packageInfo.nativeLibraryDir);

                rule.put(String.format("/data/data/%s", packageInfo.packageName), packageInfo.dataDir);
                rule.put(String.format("/data/user/%d/%s", systemUserId, packageInfo.packageName), packageInfo.dataDir);
            }

            if (BlackBoxCore.getContext().getExternalCacheDir() != null && context.getExternalCacheDir() != null) {
                File external = BEnvironment.getExternalUserDir(BActivityThread.getUserId());

                // sdcard
                rule.put("/sdcard", external.getAbsolutePath());
                rule.put(String.format("/storage/emulated/%d", systemUserId), external.getAbsolutePath());

                blackRule.add("/sdcard/Pictures");
                blackRule.add(String.format("/storage/emulated/%d/Pictures", systemUserId));
                blackRule.add(String.format("/storage/emulated/%d/%s", systemUserId, Environment.DIRECTORY_PODCASTS));
                blackRule.add(String.format("/storage/emulated/%d/%s", systemUserId, Environment.DIRECTORY_RINGTONES));
                blackRule.add(String.format("/storage/emulated/%d/%s", systemUserId, Environment.DIRECTORY_ALARMS));
                blackRule.add(String.format("/storage/emulated/%d/%s", systemUserId, Environment.DIRECTORY_NOTIFICATIONS));
                blackRule.add(String.format("/storage/emulated/%d/%s", systemUserId, Environment.DIRECTORY_PICTURES));
                blackRule.add(String.format("/storage/emulated/%d/%s", systemUserId, Environment.DIRECTORY_MOVIES));
                blackRule.add(String.format("/storage/emulated/%d/%s", systemUserId, Environment.DIRECTORY_DOWNLOADS));
                blackRule.add(String.format("/storage/emulated/%d/%s", systemUserId, Environment.DIRECTORY_DCIM));
                blackRule.add(String.format("/storage/emulated/%d/%s", systemUserId, Environment.DIRECTORY_MUSIC));
                blackRule.add(String.format("/sdcard/%s", Environment.DIRECTORY_PODCASTS));
                blackRule.add(String.format("/sdcard/%s", Environment.DIRECTORY_RINGTONES));
                blackRule.add(String.format("/sdcard/%s", Environment.DIRECTORY_ALARMS));
                blackRule.add(String.format("/sdcard/%s", Environment.DIRECTORY_NOTIFICATIONS));
                blackRule.add(String.format("/sdcard/%s", Environment.DIRECTORY_PICTURES));
                blackRule.add(String.format("/sdcard/%s", Environment.DIRECTORY_MOVIES));
                blackRule.add(String.format("/sdcard/%s", Environment.DIRECTORY_DOWNLOADS));
                blackRule.add(String.format("/sdcard/%s", Environment.DIRECTORY_DCIM));
                blackRule.add(String.format("/sdcard/%s", Environment.DIRECTORY_MUSIC));
            }

            if (BlackBoxCore.get().isHideRoot()) {
                hideRoot(rule);
            }
            proc(rule);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (String key : rule.keySet()) {
            get().addRedirect(key, rule.get(key));
        }
        for (String s : blackRule) {
            get().addBlackRedirect(s);
        }
        NativeCore.enableIO();
    }

    private void hideRoot(Map<String, String> rule) {
        rule.put("/system/app/Superuser.apk", "/system/app/Superuser.apk-fake");
        rule.put("/sbin/su", "/sbin/su-fake");
        rule.put("/system/bin/su", "/system/bin/su-fake");
        rule.put("/system/xbin/su", "/system/xbin/su-fake");
        rule.put("/data/local/xbin/su", "/data/local/xbin/su-fake");
        rule.put("/data/local/bin/su", "/data/local/bin/su-fake");
        rule.put("/system/sd/xbin/su", "/system/sd/xbin/su-fake");
        rule.put("/system/bin/failsafe/su", "/system/bin/failsafe/su-fake");
        rule.put("/data/local/su", "/data/local/su-fake");
        rule.put("/su/bin/su", "/su/bin/su-fake");
    }

    private void proc(Map<String, String> rule) {
        int appPid = BActivityThread.getAppPid();
        int pid = Process.myPid();
        String selfProc = "/proc/self/";
        String proc = "/proc/" + pid + "/";

        String cmdline = new File(BEnvironment.getProcDir(appPid), "cmdline").getAbsolutePath();
        rule.put(proc + "cmdline", cmdline);
        rule.put(selfProc + "cmdline", cmdline);
    }
}
