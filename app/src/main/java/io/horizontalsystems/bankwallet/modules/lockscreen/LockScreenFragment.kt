package io.horizontalsystems.bankwallet.modules.lockscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.ratelist.RatesListFragment
import io.horizontalsystems.bankwallet.modules.ratelist.RatesTopListFragment
import io.horizontalsystems.pin.PinFragment
import io.horizontalsystems.pin.PinInteractionType
import io.horizontalsystems.pin.PinModule
import kotlinx.android.synthetic.main.fragment_lockscreen.*

class LockScreenFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_lockscreen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragments = listOf(
                PinFragment(),
                RatesListFragment(),
                RatesTopListFragment()
        )

        viewPager.offscreenPageLimit = 2
        viewPager.adapter = LockScreenViewPagerAdapter(fragments, parentFragmentManager)

        circleIndicator.setViewPager(viewPager)
        subscribeFragmentResult()
    }

    private fun subscribeFragmentResult() {
        parentFragmentManager.setFragmentResultListener(PinModule.requestKey, this) { requestKey, bundle ->
            val resultType = bundle.getParcelable<PinInteractionType>(PinModule.requestType)
            if (resultType == PinInteractionType.UNLOCK) {
                when (bundle.getInt(PinModule.requestResult)) {
                    PinModule.RESULT_OK -> activity?.setResult(PinModule.RESULT_OK)
                    PinModule.RESULT_CANCELLED -> activity?.setResult(PinModule.RESULT_CANCELLED)
                }

                activity?.finish()
            }
        }
    }

}
