package com.mycelium.giftbox.purchase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.loadImage
import com.mycelium.giftbox.purchase.viewmodel.GiftboxBuyResultViewModel
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.txdetails.*
import com.mycelium.wallet.activity.txdetails.BtcDetailsFragment.Companion.newInstance
import com.mycelium.wallet.activity.txdetails.BtcvDetailsFragment.Companion.newInstance
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.FragmentGiftboxBuyResultBinding
import com.mycelium.wapi.wallet.TransactionSummary
import com.mycelium.wapi.wallet.btcvault.hd.BitcoinVaultHdAccount
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.fio.FioAccount
import kotlinx.android.synthetic.main.giftcard_send_info.*
import java.text.DateFormat
import java.util.*


class GiftBoxBuyResultFragment : Fragment() {
    private lateinit var tx: TransactionSummary
    private val viewModel: GiftboxBuyResultViewModel by viewModels()
    private var binding: FragmentGiftboxBuyResultBinding? = null

    val args by navArgs<GiftBoxBuyResultFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        FragmentGiftboxBuyResultBinding.inflate(inflater).apply {
            binding = this
            lifecycleOwner = this@GiftBoxBuyResultFragment
            vm = viewModel
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.totalAmountFiatString.value = args.totalFiat?.toStringWithUnit()
        viewModel.totalAmountCryptoString.value = "~" + args.totalCrypto?.toStringWithUnit()
        viewModel.minerFeeFiat.value = args.minerFeeFiat?.toStringWithUnit()
        viewModel.minerFeeCrypto.value = "~" + args.minerFeeCrypto?.toStringWithUnit()
        loadProduct()
        viewModel.setOrder(args.orderResponse)
        binding?.more?.setOnClickListener {
            viewModel.more.value = !viewModel.more.value!!
        }
        args.accountId?.let { accountId ->
            val walletManager = MbwManager.getInstance(requireContext()).getWalletManager(false)
            val account = walletManager.getAccount(accountId)
            args.transaction?.id?.let { txId ->
                tx = account?.getTxSummary(txId)!!
                if (childFragmentManager.findFragmentById(R.id.spec_details_fragment) == null) {
                    val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
                    if (account is EthAccount || account is ERC20Account) {
                        transaction.add(R.id.spec_details_fragment, EthDetailsFragment.newInstance(tx))
                    } else if (account is FioAccount) {
                        transaction.add(R.id.spec_details_fragment, FioDetailsFragment.newInstance(tx))
                    } else if (account is BitcoinVaultHdAccount) {
                        transaction.add(R.id.spec_details_fragment, newInstance(tx, accountId))
                    } else {
                        transaction.add(R.id.spec_details_fragment, newInstance(tx, false, accountId))
                    }
                    transaction.commit()
                }
                updateUi()
            }
        }?:run {
            binding?.more?.visibility = View.GONE
        }
    }

    private fun loadProduct() {
        args.productResponse?.let {
            binding?.detailsHeader?.ivImage?.loadImage(it.cardImageUrl)
            viewModel.setProduct(it)
        } ?: GitboxAPI.giftRepository.getProduct(lifecycleScope, args.orderResponse.productCode!!, {
            binding?.detailsHeader?.ivImage?.loadImage(it?.product?.cardImageUrl)
            viewModel.setProduct(it?.product!!)
        }, { _, msg ->
            Toaster(this).toast(msg, true)
        })
    }

    private fun updateUi() {
        // Set Hash
        binding?.txDetails?.tvHash?.run {
            setColuMode(false)
            setTransaction(tx)
        }

        // Set Confirmed
        val confirmations = tx.getConfirmations()
        var confirmed: String
        confirmed = if (confirmations > 0) {
            resources.getString(R.string.confirmed_in_block, tx.getHeight())
        } else {
            resources.getString(R.string.no)
        }

        // check if tx is in outgoing queue
        if (tx != null && tx.isQueuedOutgoing()) {
            binding?.txDetails?.tcdConfirmations?.setNeedsBroadcast()
            binding?.txDetails?.tvConfirmations?.text = ""
            confirmed = resources.getString(R.string.transaction_not_broadcasted_info)
        } else {
            binding?.txDetails?.tcdConfirmations?.setConfirmations(confirmations)
            binding?.txDetails?.tvConfirmations?.text = confirmations.toString()
        }
        binding?.txDetails?.tvConfirmed?.text = confirmed

        // Set Date & Time
        val date = Date(tx.getTimestamp() * 1000L)
        val locale = resources.configuration.locale
        val dayFormat = DateFormat.getDateInstance(DateFormat.LONG, locale)
        val dateString = dayFormat.format(date)
        binding?.txDetails?.tvDate?.text = dateString
        val hourFormat = DateFormat.getTimeInstance(DateFormat.LONG, locale)
        val timeString = hourFormat.format(date)
        binding?.txDetails?.tvTime?.text = timeString
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}