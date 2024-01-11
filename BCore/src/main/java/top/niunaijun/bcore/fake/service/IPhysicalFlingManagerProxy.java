package top.niunaijun.bcore.fake.service;

import android.content.Context;
import android.os.IBinder;

import black.android.os.ServiceManager;
import black.oem.vivo.IPhysicalFlingManager;
import top.niunaijun.bcore.fake.hook.BinderInvocationStub;
import top.niunaijun.bcore.fake.service.base.PkgMethodProxy;

/**
 * @author Findger
 * @function
 * @date :2023/10/8 20:11
 **/
public class IPhysicalFlingManagerProxy extends BinderInvocationStub {
    public IPhysicalFlingManagerProxy() {
        super(ServiceManager.getService.call("physical_fling_service"));
    }

    @Override
    protected Object getWho() {
        return IPhysicalFlingManager.Stub.asInterface.call(ServiceManager.getService.call("physical_fling_service"));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("physical_fling_service");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    protected void onBindMethod() {
        addMethodHook(new PkgMethodProxy("isSupportPhysicalFling"));
    }
}
