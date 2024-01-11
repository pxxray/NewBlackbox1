package top.niunaijun.bcore.fake.frameworks;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.RemoteException;

import java.util.Collections;
import java.util.List;

import top.niunaijun.bcore.BlackBoxCore;
import top.niunaijun.bcore.app.BActivityThread;
import top.niunaijun.bcore.core.system.ServiceManager;
import top.niunaijun.bcore.core.system.pm.IBPackageManagerService;
import top.niunaijun.bcore.entity.pm.InstallOption;
import top.niunaijun.bcore.entity.pm.InstallResult;

public class BPackageManager extends BlackManager<IBPackageManagerService> {
    private static final BPackageManager sPackageManager = new BPackageManager();

    public static BPackageManager get() {
        return sPackageManager;
    }

    @Override
    protected String getServiceName() {
        return ServiceManager.PACKAGE_MANAGER;
    }

    public Intent getLaunchIntentForPackage(String packageName, int userId) {
        Intent intentToResolve = new Intent(Intent.ACTION_MAIN);
        intentToResolve.addCategory(Intent.CATEGORY_INFO);
        intentToResolve.setPackage(packageName);
        List<ResolveInfo> ris = queryIntentActivities(intentToResolve, 0,
                intentToResolve.resolveTypeIfNeeded(BlackBoxCore.getContext().getContentResolver()), userId);

        // Otherwise, try to find a main launcher activity.
        if (ris == null || ris.size() <= 0) {
            // Reuse the intent instance.
            intentToResolve.removeCategory(Intent.CATEGORY_INFO);
            intentToResolve.addCategory(Intent.CATEGORY_LAUNCHER);
            intentToResolve.setPackage(packageName);
            ris = queryIntentActivities(intentToResolve, 0, intentToResolve.resolveTypeIfNeeded(BlackBoxCore.getContext().getContentResolver()), userId);
        }

        if (ris == null || ris.size() <= 0) {
            return null;
        }

        Intent intent = new Intent(intentToResolve);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(ris.get(0).activityInfo.packageName, ris.get(0).activityInfo.name);
        return intent;
    }

    public ResolveInfo resolveService(Intent intent, int flags, String resolvedType, int userId) {
        try {
            return getService().resolveService(intent, flags, resolvedType, userId);
        } catch (RemoteException e) {
            crash(e);
        }
        return null;
    }

    public ResolveInfo resolveActivity(Intent intent, int flags, String resolvedType, int userId) {
        try {
            return getService().resolveActivity(intent, flags, resolvedType, userId);
        } catch (RemoteException e) {
            crash(e);
        }
        return null;
    }

    public ProviderInfo resolveContentProvider(String authority, int flags, int userId) {
        try {
            return getService().resolveContentProvider(authority, flags, userId);
        } catch (RemoteException e) {
            crash(e);
        }
        return null;
    }

    public ResolveInfo resolveIntent(Intent intent, String resolvedType, int flags, int userId) {
        try {
            return getService().resolveIntent(intent, resolvedType, flags, userId);
        } catch (RemoteException e) {
            crash(e);
        }
        return null;
    }

    public ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) {
        try {
            return getService().getApplicationInfo(packageName, flags, userId);
        } catch (RemoteException e) {
            crash(e);
        }
        return null;
    }

    public int getUidByPid(int pid) {
        try {
            return getService().getUidByPid(pid);
        } catch (RemoteException e) {
            crash(e);
        }
        return -1;
    }

    public PackageInfo getPackageInfo(String packageName, int flags, int userId) {
        try {
            return getService().getPackageInfo(packageName, flags, userId);
        } catch (RemoteException e) {
            crash(e);
        }
        return null;
    }

    public ServiceInfo getServiceInfo(ComponentName component, int flags, int userId) {
        try {
            return getService().getServiceInfo(component, flags, userId);
        } catch (RemoteException e) {
            crash(e);
        }
        return null;
    }

    public ActivityInfo getReceiverInfo(ComponentName componentName, int flags, int userId) {
        try {
            return getService().getReceiverInfo(componentName, flags, userId);
        } catch (RemoteException e) {
            crash(e);
        }
        return null;
    }

    public ActivityInfo getActivityInfo(ComponentName component, int flags, int userId) {
        try {
            return getService().getActivityInfo(component, flags, userId);
        } catch (RemoteException e) {
            crash(e);
        }
        return null;
    }

    public ProviderInfo getProviderInfo(ComponentName component, int flags, int userId) {
        try {
            return getService().getProviderInfo(component, flags, userId);
        } catch (RemoteException e) {
            crash(e);
        }
        return null;
    }

    public List<ResolveInfo> queryIntentActivities(Intent intent, int flags, String resolvedType, int userId) {
        try {
            return getService().queryIntentActivities(intent, flags, resolvedType, userId);
        } catch (RemoteException e) {
            crash(e);
        }
        return null;
    }

    public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags, String resolvedType, int userId) {
        try {
            return getService().queryBroadcastReceivers(intent, flags, resolvedType, userId);
        } catch (RemoteException e) {
            crash(e);
        }
        return null;
    }

    public List<ProviderInfo> queryContentProviders(String processName, int uid, int flags, int userId) {
        try {
            return getService().queryContentProviders(processName, uid, flags, userId);
        } catch (RemoteException e) {
            crash(e);
        }
        return null;
    }

    public InstallResult installPackageAsUser(String file, InstallOption option, int userId) {
        try {
            return getService().installPackageAsUser(file, option, userId);
        } catch (RemoteException e) {
            crash(e);
        }
        return null;
    }

    public List<ApplicationInfo> getInstalledApplications(int flags, int userId) {
        try {
            return getService().getInstalledApplications(flags, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public List<PackageInfo> getInstalledPackages(int flags, int userId) {
        try {
            return getService().getInstalledPackages(flags, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public void clearPackage(String packageName, int userId) {
        try {
            getService().clearPackage(packageName, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void stopPackage(String packageName, int userId) {
        try {
            getService().stopPackage(packageName, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void uninstallPackageAsUser(String packageName, int userId) {
        try {
            getService().uninstallPackageAsUser(packageName, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void uninstallPackage(String packageName) {
        try {
            getService().uninstallPackage(packageName);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean isInstalled(String packageName, int userId) {
        try {
            return getService().isInstalled(packageName, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String[] getPackagesForUid(int uid) {
        try {
            return getService().getPackagesForUid(uid, BActivityThread.getUserId());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return new String[]{};
    }

    private void crash(Throwable e) {
        e.printStackTrace();
    }
}
