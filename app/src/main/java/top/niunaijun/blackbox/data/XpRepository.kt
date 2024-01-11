package top.niunaijun.blackbox.data

import android.net.Uri
import android.webkit.URLUtil
import androidx.lifecycle.MutableLiveData
import top.niunaijun.bcore.BlackBoxCore
import top.niunaijun.bcore.BlackBoxCore.getPackageManager
import top.niunaijun.blackbox.R
import top.niunaijun.blackbox.bean.XpModuleInfo
import top.niunaijun.blackbox.util.ResUtil.getString

class XpRepository {
    fun getInstallModules(modulesLiveData: MutableLiveData<List<XpModuleInfo>>) {
        val moduleList = BlackBoxCore.get().installedXPModules
        val result = mutableListOf<XpModuleInfo>()

        moduleList.forEach {
            val info = XpModuleInfo(it.name, it.desc, it.packageName, it.packageInfo.versionName, it.enable, it.application.loadIcon(getPackageManager()))
            result.add(info)
        }
        modulesLiveData.postValue(result)
    }

    fun installModule(source: String, resultLiveData: MutableLiveData<String>) {
        val blackBoxCore = BlackBoxCore.get()
        val installResult = if (URLUtil.isValidUrl(source)) {
            val uri = Uri.parse(source)
            blackBoxCore.installXPModule(uri)
        } else {
            // source == packageName
            blackBoxCore.installXPModule(source)
        }

        if (installResult.success) {
            resultLiveData.postValue(getString(R.string.install_success))
        } else {
            resultLiveData.postValue(getString(R.string.install_fail, installResult.msg))
        }
    }

    fun unInstallModule(packageName: String, resultLiveData: MutableLiveData<String>) {
        BlackBoxCore.get().uninstallXPModule(packageName)
        resultLiveData.postValue(getString(R.string.remove_success))
    }
}
