package com.mycelium.bequant.signup

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.remote.SignRepository
import com.mycelium.bequant.remote.client.models.TotpActivateRequest
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.poovam.pinedittextfield.PinField
import kotlinx.android.synthetic.main.fragment_bequant_sign_in_two_factor.*


class SignUpTwoFactorPasscodeFragment : Fragment(R.layout.fragment_bequant_sign_in_two_factor) {

    val args by navArgs<SignUpTwoFactorPasscodeFragmentArgs>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.title = getString(R.string.bequant_page_title_two_factor_auth)
        (activity as AppCompatActivity?)?.supportActionBar?.setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))
        pasteFromClipboard.setOnClickListener {
            pinCode.setText(Utils.getClipboardString(requireContext()))
        }
        pinCode.onTextCompleteListener = object : PinField.OnTextCompleteListener {
            override fun onTextComplete(enteredText: String): Boolean {
                loader(true)
                SignRepository.repository.totpActivate(this@SignUpTwoFactorPasscodeFragment, TotpActivateRequest(args.otp.otpId, enteredText), {
                    findNavController().navigate(SignUpTwoFactorPasscodeFragmentDirections.actionNext())
                }, error = { _, message ->
                    ErrorHandler(requireContext()).handle(message)
                }, finallyBlock = {
                    loader(false)
                })
                return true
            }
        }
    }
}