package io.horizontalsystems.bankwallet.modules.swap.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.swap.DataState
import io.horizontalsystems.bankwallet.modules.swap.ResourceProvider
import io.horizontalsystems.bankwallet.modules.swap.SwapModule.ISwapService
import io.horizontalsystems.bankwallet.modules.swap.SwapModule.SwapError
import io.horizontalsystems.bankwallet.modules.swap.SwapModule.SwapState
import io.horizontalsystems.bankwallet.modules.swap.confirmation.ConfirmationPresenter
import io.horizontalsystems.bankwallet.modules.swap.model.AmountType
import io.horizontalsystems.bankwallet.modules.swap.model.PriceImpact
import io.horizontalsystems.bankwallet.modules.swap.model.Trade
import io.horizontalsystems.bankwallet.modules.swap.view.item.TradeViewItem
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal

class SwapViewModel(
        private val swapService: ISwapService,
        private val resourceProvider: ResourceProvider,
        private val numberFormatter: IAppNumberFormatter
) : ViewModel() {

    private val disposables = CompositeDisposable()

    val confirmationPresenter by lazy {
        ConfirmationPresenter(swapService, resourceProvider, numberFormatter)
    }

    // region Outputs
    private val _coinSending = MutableLiveData<Coin>()
    val coinSending: LiveData<Coin> = _coinSending

    private val _coinReceiving = MutableLiveData<Coin>()
    val coinReceiving: LiveData<Coin> = _coinReceiving

    private val _allowance = MutableLiveData<String?>()
    val allowance: LiveData<String?> = _allowance

    private val _allowanceLoading = MutableLiveData<Boolean>()
    val allowanceLoading: LiveData<Boolean> = _allowanceLoading

    private val _balance = MutableLiveData<String>()
    val balance: LiveData<String> = _balance

    private val _amountSending = MutableLiveData<String?>()
    val amountSending: LiveData<String?> = _amountSending

    private val _amountReceiving = MutableLiveData<String?>()
    val amountReceiving: LiveData<String?> = _amountReceiving

    private val _amountSendingLabelVisible = MutableLiveData<Boolean>()
    val amountSendingLabelVisible: LiveData<Boolean> = _amountSendingLabelVisible

    private val _amountReceivingLabelVisible = MutableLiveData<Boolean>()
    val amountReceivingLabelVisible: LiveData<Boolean> = _amountReceivingLabelVisible

    private val _tradeViewItem = MutableLiveData<TradeViewItem?>()
    val tradeViewItem: LiveData<TradeViewItem?> = _tradeViewItem

    private val _tradeViewItemLoading = MutableLiveData<Boolean>()
    val tradeViewItemLoading: LiveData<Boolean> = _tradeViewItemLoading

    private val _priceImpactColor = MutableLiveData<Int>()
    val priceImpactColor: LiveData<Int> = _priceImpactColor

    private val _allowanceColor = MutableLiveData<Int>()
    val allowanceColor: LiveData<Int> = _allowanceColor

    private val _amountSendingError = MutableLiveData<String?>()
    val amountSendingError: LiveData<String?> = _amountSendingError

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _approveButtonVisible = MutableLiveData<Boolean>()
    val approveButtonVisible: LiveData<Boolean> = _approveButtonVisible

    private val _proceedButtonVisible = MutableLiveData<Boolean>()
    val proceedButtonVisible: LiveData<Boolean> = _proceedButtonVisible

    private val _proceedButtonEnabled = MutableLiveData<Boolean>()
    val proceedButtonEnabled: LiveData<Boolean> = _proceedButtonEnabled

    private val _openConfirmation = MutableLiveData<Boolean>()
    val openConfirmation: LiveData<Boolean> = _openConfirmation

    private val _feeLoading = MutableLiveData<Boolean>()
    val feeLoading: LiveData<Boolean> = _feeLoading
    // endregion

    init {
        swapService.coinSendingObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    _coinSending.postValue(it)
                }
                .let { disposables.add(it) }

        swapService.coinReceivingObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    _coinReceiving.postValue(it.orElse(null))
                }
                .let { disposables.add(it) }

        swapService.balance
                .subscribeOn(Schedulers.io())
                .subscribe {
                    _balance.postValue(formatCoinAmount(it.value, it.coin))
                }
                .let { disposables.add(it) }

        swapService.amountReceivingObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    _amountReceiving.postValue(if (it.isPresent) it.get().toPlainString() else null)
                }
                .let { disposables.add(it) }

        swapService.amountSendingObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    _amountSending.postValue(if (it.isPresent) it.get().toPlainString() else null)
                }
                .let { disposables.add(it) }

        swapService.allowance
                .subscribeOn(Schedulers.io())
                .subscribe {
                    _allowance.postValue(it.dataOrNull?.let { coinValue ->
                        formatCoinAmount(coinValue.value, coinValue.coin)
                    })
                    _allowanceLoading.postValue(it is DataState.Loading)
                }
                .let { disposables.add(it) }

        swapService.tradeObservable
                .subscribeOn(Schedulers.io())
                .subscribe { dataState ->
                    _tradeViewItem.postValue(dataState.dataOrNull?.let { tradeViewItem(it) })
                    _tradeViewItemLoading.postValue(dataState is DataState.Loading)
                    _priceImpactColor.postValue(priceImpactColor(dataState.dataOrNull?.priceImpact))
                }
                .let { disposables.add(it) }

        swapService.errors
                .subscribeOn(Schedulers.io())
                .subscribe { errors ->
                    val amountSendingError = if (errors.contains(SwapError.InsufficientBalance)) resourceProvider.string(R.string.Swap_ErrorInsufficientBalance) else null
                    _amountSendingError.postValue(amountSendingError)

                    val allowanceColor = if (errors.contains(SwapError.InsufficientAllowance)) resourceProvider.colorLucian() else resourceProvider.color(R.color.grey)
                    _allowanceColor.postValue(allowanceColor)

                    _error.postValue(errorText(errors))
                }
                .let { disposables.add(it) }

        swapService.amountType
                .subscribeOn(Schedulers.io())
                .subscribe {
                    _amountSendingLabelVisible.postValue(it == AmountType.ExactReceiving)
                    _amountReceivingLabelVisible.postValue(it == AmountType.ExactSending)
                }
                .let { disposables.add(it) }

        swapService.state
                .subscribeOn(Schedulers.io())
                .subscribe {
                    _approveButtonVisible.postValue(it == SwapState.ApproveRequired)

                    _proceedButtonVisible.postValue(it != SwapState.ApproveRequired)
                    _proceedButtonEnabled.postValue(it == SwapState.ProceedAllowed)

                    _openConfirmation.postValue(it == SwapState.SwapAllowed)
                }
                .let { disposables.add(it) }

        swapService.fee
                .subscribeOn(Schedulers.io())
                .subscribe {
                    _feeLoading.postValue(it == DataState.Loading)
                }
                .let { disposables.add(it) }
    }

    // region Inputs
    fun setCoinSending(coin: Coin) {
        swapService.enterCoinSending(coin)
    }

    fun setCoinReceiving(coin: Coin) {
        swapService.enterCoinReceiving(coin)
    }

    fun setAmountSending(amount: String?) {
        swapService.enterAmountSending(if (amount.isNullOrBlank()) null else BigDecimal(amount))
    }

    fun setAmountReceiving(amount: String?) {
        swapService.enterAmountReceiving(if (amount.isNullOrBlank()) null else BigDecimal(amount))
    }

    fun onProceedClick() {
        swapService.proceed()
    }
    // endregion

    override fun onCleared() {
        disposables.dispose()
    }

    private fun formatCoinAmount(amount: BigDecimal, coin: Coin): String {
        val maxFraction = if (coin.decimal < 8) coin.decimal else 8
        return numberFormatter.formatCoin(amount, coin.code, 0, maxFraction)
    }

    private fun tradeViewItem(trade: Trade): TradeViewItem {
        val minMaxTitle: String?
        val minMaxAmount: String?

        when (trade.amountType) {
            AmountType.ExactSending -> {
                minMaxTitle = resourceProvider.string(R.string.Swap_MinimumReceived)
                minMaxAmount = trade.minMaxAmount?.let { formatCoinAmount(it, trade.coinReceiving) }
            }
            AmountType.ExactReceiving -> {
                minMaxTitle = resourceProvider.string(R.string.Swap_MaximumSold)
                minMaxAmount = trade.minMaxAmount?.let { formatCoinAmount(it, trade.coinSending) }
            }
        }
        return TradeViewItem(
                trade.executionPrice?.let { "${trade.coinSending.code} = ${formatCoinAmount(it, trade.coinReceiving)} " },
                trade.priceImpact?.value?.toPlainString()?.let { resourceProvider.string(R.string.Swap_Percent, it) },
                minMaxTitle,
                minMaxAmount
        )
    }

    private fun priceImpactColor(priceImpact: PriceImpact?): Int {
        return when (priceImpact?.level) {
            PriceImpact.Level.Normal -> resourceProvider.colorRemus()
            PriceImpact.Level.Warning -> resourceProvider.colorJacob()
            PriceImpact.Level.Forbidden -> resourceProvider.colorLucian()
            else -> resourceProvider.color(R.color.grey)
        }
    }

    private fun errorText(errors: List<SwapError>): String? {
        return when {
            errors.contains(SwapError.NoLiquidity) -> resourceProvider.string(R.string.Swap_ErrorNoLiquidity)
            errors.contains(SwapError.CouldNotFetchTrade) -> resourceProvider.string(R.string.Swap_ErrorCouldNotFetchTrade)
            errors.contains(SwapError.CouldNotFetchAllowance) -> resourceProvider.string(R.string.Swap_ErrorCouldNotFetchAllowance)
            errors.contains(SwapError.CouldNotFetchFee) -> resourceProvider.string(R.string.Swap_ErrorCouldNotFetchFee)
            else -> null
        }
    }

}
