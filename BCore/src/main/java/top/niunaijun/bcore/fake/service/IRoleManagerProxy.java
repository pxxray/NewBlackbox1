package top.niunaijun.bcore.fake.service;

import android.content.Context;
import android.os.IBinder;

import black.android.os.IPowerManager;
import black.android.os.ServiceManager;
import black.android.role.IRoleManager;
import top.niunaijun.bcore.fake.hook.BinderInvocationStub;
import top.niunaijun.bcore.fake.service.base.PkgMethodProxy;
import top.niunaijun.bcore.fake.service.base.ValueMethodProxy;

/**
 * @author Findger
 * @function
 * @date :2023/10/8 19:45
 **/
public class IRoleManagerProxy extends BinderInvocationStub {

    public IRoleManagerProxy() {
        super(ServiceManager.getService.call(Context.ROLE_SERVICE));
    }

    @Override
    protected Object getWho() {
        return IRoleManager.Stub.asInterface.call(ServiceManager.getService.call(Context.ROLE_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.ROLE_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    protected void onBindMethod() {
        addMethodHook(new PkgMethodProxy("isRoleHeld"));
    }
}
