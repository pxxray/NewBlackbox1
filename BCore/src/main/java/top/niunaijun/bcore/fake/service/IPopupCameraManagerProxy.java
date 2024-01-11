package top.niunaijun.bcore.fake.service;

import android.content.Context;

import black.android.os.ServiceManager;
import black.oem.vivo.IPopupCameraManager;
import top.niunaijun.bcore.fake.hook.BinderInvocationStub;
import top.niunaijun.bcore.fake.service.base.PkgMethodProxy;

/**
 * @author Findger
 * @function
 * @date :2023/10/8 20:19
 **/
public class IPopupCameraManagerProxy extends BinderInvocationStub {

    public IPopupCameraManagerProxy() {
        super(ServiceManager.getService.call("popup_camera_service"));
    }

    @Override
    protected Object getWho() {
        return IPopupCameraManager.Stub.asInterface.call(ServiceManager.getService.call("popup_camera_service"));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("popup_camera_service");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    protected void onBindMethod() {
        addMethodHook(new PkgMethodProxy("notifyCameraStatus"));
    }
}
