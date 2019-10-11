package io.horizontalsystems.bankwallet.modules.ratelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_rates.*
import kotlinx.android.synthetic.main.view_holder_coin_rate.*
import java.util.*

class RatesFragment : Fragment() {

    private lateinit var adapter: CoinRatesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_rates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val presenter = ViewModelProviders.of(this, RateListModule.Factory()).get(RateListPresenter::class.java)
        observeView(presenter.view)
        presenter.viewDidLoad()

        adapter = CoinRatesAdapter(presenter)
        coinRatesRecyclerView.adapter = adapter
    }

    private fun observeView(view: RateListView) {
        view.currentDate.observe(viewLifecycleOwner, Observer {
            dateText.text = DateHelper.formatDate(Date(), "MMMM dd")
        })
        view.reloadLiveEvent.observe(viewLifecycleOwner, Observer {
            adapter.notifyDataSetChanged()
        })
    }
}


class CoinRatesAdapter(private val presenter: RateListPresenter) : RecyclerView.Adapter<ViewHolderCoinRate>() {

    override fun getItemCount(): Int {
        return presenter.itemsCount
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderCoinRate {
        return ViewHolderCoinRate(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin_rate, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolderCoinRate, position: Int) {
        holder.bind(presenter.getViewItem(position), position == itemCount - 1)
    }

}

class ViewHolderCoinRate(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(viewItem: RateViewItem, isLast: Boolean) {
        coinIcon.bind(viewItem.coin)
        txCoinCode.text = viewItem.coin.code
        txCoinName.text = viewItem.coin.title

        txValueInFiat.text = if (viewItem.loadingStatus != RateLoadingStatus.Loading) "----" else ""
        txValueInFiat.setTextColor(containerView.context.getColor(R.color.grey))
        viewItem.rate?.let { exchangeValue ->
            val rateString = App.numberFormatter.format(exchangeValue, trimmable = true, canUseLessSymbol = false)
            txValueInFiat.text = rateString
            txValueInFiat.setTextColor(ContextCompat.getColor(containerView.context, if (viewItem.rateExpired) R.color.grey_50 else R.color.grey))
        }

        txDiff.visibility = View.GONE
        txDiffNa.visibility = View.GONE

        if (viewItem.loadingStatus == RateLoadingStatus.Loading) {
            loadingSpinner.visibility = View.VISIBLE
        } else {
            loadingSpinner.visibility = View.GONE

            viewItem.diff?.let {
                txDiff.bind(it, containerView.context)
                txDiff.visibility = View.VISIBLE
                txDiffNa.visibility = View.GONE
            } ?: run {
                txDiff.visibility = View.GONE
                txDiffNa.visibility = View.VISIBLE
            }
        }

        bottomShade.visibility = if (isLast) View.VISIBLE else View.GONE
    }
}