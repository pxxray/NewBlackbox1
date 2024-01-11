package top.niunaijun.bcore.core.system.am;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import top.niunaijun.bcore.BlackBoxCore;
import top.niunaijun.bcore.core.IEmpty;
import top.niunaijun.bcore.core.system.BProcessManagerService;
import top.niunaijun.bcore.core.system.ProcessRecord;
import top.niunaijun.bcore.core.system.pm.BPackageManagerService;
import top.niunaijun.bcore.entity.UnbindRecord;
import top.niunaijun.bcore.entity.am.RunningServiceInfo;
import top.niunaijun.bcore.proxy.ProxyManifest;
import top.niunaijun.bcore.proxy.record.ProxyServiceRecord;

@SuppressLint("NewApi")
public class ActiveServices {
    public static final String TAG = "ActiveServices";
    private final Map<Intent.FilterComparison, RunningServiceRecord> mRunningServiceRecords = new HashMap<>();
    private final Map<IBinder, RunningServiceRecord> mRunningTokens = new HashMap<>();
    private final Map<IBinder, ConnectedServiceRecord> mConnectedServices = new HashMap<>();

    public void startService(Intent intent, String resolvedType, int userId) {
        ResolveInfo resolveInfo = resolveService(intent, resolvedType, userId);
        if (resolveInfo == null) {
            return;
        }

        ServiceInfo serviceInfo = resolveInfo.serviceInfo;
        ProcessRecord processRecord = BProcessManagerService.get().startProcessLocked(serviceInfo.packageName, serviceInfo.processName, userId, -1, Binder.getCallingPid());
        if (processRecord == null) {
            throw new RuntimeException("Unable to create " + serviceInfo.name);
        }

        RunningServiceRecord runningServiceRecord = getOrCreateRunningServiceRecord(intent);
        runningServiceRecord.mServiceInfo = serviceInfo;
        runningServiceRecord.getAndIncrementStartId();

        final Intent stubServiceIntent = createStubServiceIntent(intent, serviceInfo, processRecord, runningServiceRecord);
        new Thread(() -> {
            try {
                BlackBoxCore.getContext().startService(stubServiceIntent);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }).start();
    }

    public int stopService(Intent intent, String resolvedType, int userId) {
        synchronized (mRunningServiceRecords) {
            RunningServiceRecord runningServiceRecord = findRunningServiceRecord(intent);
            if (runningServiceRecord == null) {
                return 0;
            }

            if (runningServiceRecord.mBindCount.get() > 0) {
                Log.d(TAG, "There are also connections");
                return 0;
            }

            runningServiceRecord.mStartId.set(0);
            ResolveInfo resolveInfo = resolveService(intent, resolvedType, userId);
            if (resolveInfo == null) {
                return 0;
            }

            ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            ProcessRecord processRecord = BProcessManagerService.get().startProcessLocked(serviceInfo.packageName, serviceInfo.processName, userId, -1, Binder.getCallingPid());
            if (processRecord == null) {
                return 0;
            }

            try {
                processRecord.bActivityThread.stopService(intent);
            } catch (RemoteException ignored) { }
        }
        return 0;
    }

    public Intent bindService(Intent intent, final IBinder binder, String resolvedType, int userId) {
        ResolveInfo resolveInfo = resolveService(intent, resolvedType, userId);
        if (resolveInfo == null) {
            return intent;
        }

        ServiceInfo serviceInfo = resolveInfo.serviceInfo;
        ProcessRecord processRecord = BProcessManagerService.get().startProcessLocked(serviceInfo.packageName, serviceInfo.processName,
                userId, -1, Binder.getCallingPid());
        if (processRecord == null) {
            throw new RuntimeException("Unable to create " + serviceInfo.name);
        }

        RunningServiceRecord runningServiceRecord;
        synchronized (mRunningServiceRecords) {
            runningServiceRecord = getOrCreateRunningServiceRecord(intent);
            runningServiceRecord.mServiceInfo = serviceInfo;

            if (binder != null) {
                ConnectedServiceRecord connectedService = mConnectedServices.get(binder);
                boolean isBound = false;
                if (connectedService != null) {
                    isBound = true;
                } else {
                    connectedService = new ConnectedServiceRecord();
                    try {
                        binder.linkToDeath(new IBinder.DeathRecipient() {
                            @Override
                            public void binderDied() {
                                binder.unlinkToDeath(this, 0);
                                mConnectedServices.remove(binder);
                            }
                        }, 0);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    connectedService.mIntent = intent;
                    mConnectedServices.put(binder, connectedService);
                }

                if (!isBound) {
                    runningServiceRecord.incrementBindCountAndGet();
                }
            }
        }
        return createStubServiceIntent(intent, serviceInfo, processRecord, runningServiceRecord);
    }

    public void unbindService(IBinder binder) {
        ConnectedServiceRecord connectedService = mConnectedServices.get(binder);
        if (connectedService == null) {
            return;
        }

        RunningServiceRecord runningServiceRecord = getOrCreateRunningServiceRecord(connectedService.mIntent);
        runningServiceRecord.mBindCount.decrementAndGet();
        mConnectedServices.remove(binder);
    }

    public void stopServiceToken(IBinder token, int userId) {
        RunningServiceRecord runningServiceByToken = findRunningServiceByToken(token);
        if (runningServiceByToken != null) {
            stopService(runningServiceByToken.mIntent, null, userId);
        }
    }

    public void onServiceDestroy(Intent proxyIntent) {
        if (proxyIntent == null) {
            return;
        }

        ProxyServiceRecord proxyServiceRecord = ProxyServiceRecord.create(proxyIntent);
        if (proxyServiceRecord.mServiceIntent != null) {
            proxyIntent = proxyServiceRecord.mServiceIntent;
        }

        RunningServiceRecord remove = mRunningServiceRecords.remove(new Intent.FilterComparison(proxyIntent));
        if (remove != null) {
            mRunningTokens.remove(remove);
        }
    }

    public UnbindRecord onServiceUnbind(Intent proxyIntent) {
        if (proxyIntent == null) {
            return null;
        }

        ProxyServiceRecord proxyServiceRecord = ProxyServiceRecord.create(proxyIntent);
        ComponentName component = proxyServiceRecord.mServiceIntent.getComponent();
        RunningServiceRecord runningServiceRecord = findRunningServiceRecord(proxyServiceRecord.mServiceIntent);
        if (runningServiceRecord == null) {
            return null;
        }

        UnbindRecord record = new UnbindRecord();
        record.setComponentName(component);
        record.setBindCount(runningServiceRecord.mBindCount.get());
        record.setStartId(runningServiceRecord.mStartId.get());
        return record;
    }

    private Intent createStubServiceIntent(Intent targetIntent, ServiceInfo serviceInfo, ProcessRecord processRecord, RunningServiceRecord runningServiceRecord) {
        Intent stub = new Intent();
        ComponentName stubComp = new ComponentName(BlackBoxCore.getHostPkg(), ProxyManifest.getProxyService(processRecord.bPID));
        stub.setComponent(stubComp);
        stub.setAction(UUID.randomUUID().toString());
        ProxyServiceRecord.saveStub(stub, targetIntent, serviceInfo, runningServiceRecord, processRecord.userId, runningServiceRecord.mStartId.get());
        return stub;
    }

    private RunningServiceRecord getOrCreateRunningServiceRecord(Intent intent) {
        RunningServiceRecord runningServiceRecord = findRunningServiceRecord(intent);
        if (runningServiceRecord == null) {
            runningServiceRecord = new RunningServiceRecord();
            runningServiceRecord.mIntent = intent;
            mRunningServiceRecords.put(new Intent.FilterComparison(intent), runningServiceRecord);
            mRunningTokens.put(runningServiceRecord, runningServiceRecord);
        }
        return runningServiceRecord;
    }

    private RunningServiceRecord findRunningServiceRecord(Intent intent) {
        return mRunningServiceRecords.get(new Intent.FilterComparison(intent));
    }

    private RunningServiceRecord findRunningServiceByToken(IBinder token) {
        return mRunningTokens.get(token);
    }

    public RunningServiceInfo getRunningServiceInfo(String callerPackage, int userId) {
        ActivityManager manager = (ActivityManager) BlackBoxCore.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
        Map<Integer, ActivityManager.RunningServiceInfo> serviceInfoMap = new HashMap<>();
        for (ActivityManager.RunningServiceInfo runningService : runningServices) {
            serviceInfoMap.put(runningService.pid, runningService);
        }

        RunningServiceInfo info = new RunningServiceInfo();
        for (RunningServiceRecord value : mRunningServiceRecords.values()) {
            ServiceInfo serviceInfo = value.mServiceInfo;
            ProcessRecord processRecord = BProcessManagerService.get().findProcessRecord(callerPackage, serviceInfo.processName, userId);
            if (processRecord == null) {
                continue;
            }

            ActivityManager.RunningServiceInfo runningServiceInfo = serviceInfoMap.get(processRecord.pid);
            if (runningServiceInfo != null) {
                runningServiceInfo.process = processRecord.processName;
                runningServiceInfo.service = new ComponentName(serviceInfo.packageName, serviceInfo.name);
                info.mRunningServiceInfoList.add(runningServiceInfo);
            }
        }
        return info;
    }

    public IBinder peekService(Intent intent, String resolvedType, int userId) {
        ResolveInfo resolveInfo = resolveService(intent, resolvedType, userId);
        if (resolveInfo == null) {
            return null;
        }

        ProcessRecord processRecord = BProcessManagerService.get().findProcessRecord(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.processName,
                userId);
        if (processRecord == null) {
            return null;
        }

        try {
            return processRecord.bActivityThread.peekService(intent);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ResolveInfo resolveService(Intent intent, String resolvedType, int userId) {
        return BPackageManagerService.get().resolveService(intent, 0, resolvedType, userId);
    }

    public static class RunningServiceRecord extends IEmpty.Stub {
        private final AtomicInteger mStartId = new AtomicInteger(1);
        private final AtomicInteger mBindCount = new AtomicInteger(0);

        private ServiceInfo mServiceInfo;
        private Intent mIntent;

        public void getAndIncrementStartId() {
            mStartId.getAndIncrement();
        }

        public void incrementBindCountAndGet() {
            mBindCount.incrementAndGet();
        }
    }

    public static class ConnectedServiceRecord {
        private Intent mIntent;
    }
}
