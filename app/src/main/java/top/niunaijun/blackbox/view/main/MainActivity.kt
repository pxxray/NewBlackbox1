package top.niunaijun.blackbox.view.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.viewpager2.widget.ViewPager2
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import top.niunaijun.bcore.BlackBoxCore
import top.niunaijun.blackbox.R
import top.niunaijun.blackbox.app.App
import top.niunaijun.blackbox.app.AppManager
import top.niunaijun.blackbox.databinding.ActivityMainBinding
import top.niunaijun.blackbox.util.Resolution
import top.niunaijun.blackbox.util.ViewBindingEx.inflate
import top.niunaijun.blackbox.view.apps.AppsFragment
import top.niunaijun.blackbox.view.base.LoadingActivity
import top.niunaijun.blackbox.view.fake.FakeManagerActivity
import top.niunaijun.blackbox.view.list.ListActivity
import top.niunaijun.blackbox.view.setting.SettingActivity

class MainActivity : LoadingActivity() {
    private val viewBinding: ActivityMainBinding by inflate()
    private lateinit var mViewPagerAdapter: ViewPagerAdapter
    private val fragmentList = mutableListOf<AppsFragment>()
    private var currentUser = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        initToolbar(viewBinding.toolbarLayout.toolbar, R.string.app_name)
        initViewPager()
        initFab()
        initToolbarSubTitle()
    }

    private fun initToolbarSubTitle() {
        updateUserRemark(0)

        viewBinding.toolbarLayout.toolbar.getChildAt(1).setOnClickListener {
            MaterialDialog(this).show {
                title(res = R.string.userRemark)
                input(
                    hintRes = R.string.userRemark,
                    prefill = viewBinding.toolbarLayout.toolbar.subtitle
                ) { _, input ->
                    AppManager.mRemarkSharedPreferences.edit {
                        putString("Remark$currentUser", input.toString())
                        viewBinding.toolbarLayout.toolbar.subtitle = input
                    }
                }
                positiveButton(res = R.string.done)
                negativeButton(res = R.string.cancel)
            }
        }
    }

    private fun initViewPager() {
        val userList = BlackBoxCore.get().users
        userList.forEach {
            fragmentList.add(AppsFragment.newInstance(it.id))
        }

        currentUser = userList.firstOrNull()?.id ?: 0
        fragmentList.add(AppsFragment.newInstance(userList.size))

        mViewPagerAdapter = ViewPagerAdapter(this)
        mViewPagerAdapter.replaceData(fragmentList)
        viewBinding.viewPager.adapter = mViewPagerAdapter

        viewBinding.dotsIndicator.attachTo(viewBinding.viewPager)
        viewBinding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentUser = fragmentList[position].userID

                updateUserRemark(currentUser)
                showFloatButton(true)
            }
        })
    }

    private fun initFab() {
        viewBinding.fab.setOnClickListener {
            val userId = viewBinding.viewPager.currentItem
            val intent = Intent(this, ListActivity::class.java)

            intent.putExtra("userID", userId)
            apkPathResult.launch(intent)
        }
    }

    fun showFloatButton(show: Boolean) {
        val tranY: Float = Resolution.convertDpToPixel(120F, App.getContext())
        val time = 200L

        if (show) {
            viewBinding.fab.animate().translationY(0f).alpha(1f).setDuration(time)
                .start()
        } else {
            viewBinding.fab.animate().translationY(tranY).alpha(0f).setDuration(time)
                .start()
        }
    }

    fun scanUser() {
        val userList = BlackBoxCore.get().users

        if (fragmentList.size == userList.size) {
            fragmentList.add(AppsFragment.newInstance(fragmentList.size))
        } else if (fragmentList.size > userList.size + 1) {
            fragmentList.removeLast()
        }
        mViewPagerAdapter.notifyDataSetChanged()
    }

    private fun updateUserRemark(userId: Int) {
        var remark = AppManager.mRemarkSharedPreferences.getString("Remark$userId", "User $userId")
        if (remark.isNullOrEmpty()) {
            remark = "User $userId"
        }
        viewBinding.toolbarLayout.toolbar.subtitle = remark
    }

    private val apkPathResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                it.data?.let { data ->
                    val userId = data.getIntExtra("userID", 0)
                    val source = data.getStringExtra("source")

                    if (source != null) {
                        fragmentList[userId].installApk(source)
                    }
                }
            }
        }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.main_git -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Monster-GM/NewBlackbox"))
                startActivity(intent)
            }

            R.id.main_setting -> {
                SettingActivity.start(this)
            }

            R.id.main_tg -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/NewBlackbox"))
                startActivity(intent)
            }

            R.id.fake_location -> {
                val intent = Intent(this, FakeManagerActivity::class.java)
                intent.putExtra("userID", currentUser)
                startActivity(intent)
            }
        }
        return true
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }
}
