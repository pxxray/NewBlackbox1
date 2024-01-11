package top.niunaijun.blackbox.view.fake

import android.view.View
import android.view.ViewGroup
import cbfg.rvadapter.RVHolder
import cbfg.rvadapter.RVHolderFactory
import top.niunaijun.bcore.fake.frameworks.BLocationManager
import top.niunaijun.blackbox.R
import top.niunaijun.blackbox.bean.FakeLocationBean
import top.niunaijun.blackbox.databinding.ItemFakeBinding
import top.niunaijun.blackbox.util.ResUtil.getString

class FakeLocationAdapter : RVHolderFactory() {
    override fun createViewHolder(parent: ViewGroup?, viewType: Int, item: Any): RVHolder<out Any> {
        return FakeLocationVH(inflate(R.layout.item_fake, parent))
    }

    class FakeLocationVH(itemView: View) : RVHolder<FakeLocationBean>(itemView) {
        private val binding = ItemFakeBinding.bind(itemView)

        override fun setContent(item: FakeLocationBean, isSelected: Boolean, payload: Any?) {
            binding.icon.setImageDrawable(item.icon)
            binding.name.text = item.name

            if (item.fakeLocation == null || item.fakeLocationPattern == BLocationManager.CLOSE_MODE) {
                binding.fakeLocation.text = getString(R.string.real_location)
            } else {
                binding.fakeLocation.text = String.format("%f, %f", item.fakeLocation!!.latitude, item.fakeLocation!!.longitude)
            }
            binding.cornerLabel.visibility = View.VISIBLE
        }
    }
}
