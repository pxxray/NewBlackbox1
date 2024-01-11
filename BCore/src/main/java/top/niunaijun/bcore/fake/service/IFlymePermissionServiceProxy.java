package top.niunaijun.bcore.fake.service;

import android.os.IBinder;

import black.android.os.ServiceManager;
import black.oem.flyme.IFlymePermissionService;
import top.niunaijun.bcore.fake.hook.BinderInvocationStub;
import top.niunaijun.bcore.fake.service.base.PkgMethodProxy;

/**
 * @author Findger
 * @function
 * @date :2023/10/9 12:34
 **/
public class IFlymePermissionServiceProxy extends BinderInvocationStub {
    public IFlymePermissionServiceProxy() {
        super(ServiceManager.getService.call("flyme_permission"));
    }

    @Override
    protected Object getWho() {
        return IFlymePermissionService.Stub.TYPE;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        addMethodHook(new PkgMethodProxy("noteIntentOperation"));
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
