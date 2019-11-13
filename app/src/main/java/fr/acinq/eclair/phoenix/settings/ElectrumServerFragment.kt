/*
 * Copyright 2019 ACINQ SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.acinq.eclair.phoenix.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.google.common.base.Strings
import fr.acinq.eclair.phoenix.BaseFragment
import fr.acinq.eclair.phoenix.R
import fr.acinq.eclair.phoenix.databinding.FragmentSettingsElectrumServerBinding
import fr.acinq.eclair.phoenix.utils.BindingHelpers
import fr.acinq.eclair.phoenix.utils.Prefs
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ElectrumServerFragment : BaseFragment() {

  override val log: Logger = LoggerFactory.getLogger(this::class.java)
  private lateinit var mBinding: FragmentSettingsElectrumServerBinding
  private lateinit var model: ElectrumServerViewModel

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    mBinding = FragmentSettingsElectrumServerBinding.inflate(inflater, container, false)
    mBinding.lifecycleOwner = this
    return mBinding.root
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    model = ViewModelProvider(this).get(ElectrumServerViewModel::class.java)
    appKit.nodeData.observe(viewLifecycleOwner, Observer {
      context?.let { ctx ->
        val currentPrefsAddress = Prefs.getElectrumServer(ctx)
        mBinding.currentState.text = if (Strings.isNullOrEmpty(it.electrumAddress)) {
          if (Strings.isNullOrEmpty(currentPrefsAddress)) {
            resources.getString(R.string.electrum_connecting)
          } else {
            resources.getString(R.string.electrum_connecting_to_custom, currentPrefsAddress)
          }
        } else {
          resources.getString(R.string.electrum_connected, it.electrumAddress)
        }
      }
    })
    mBinding.model = model
  }

  override fun onStart() {
    super.onStart()
    mBinding.changeServerButton.setOnClickListener {
      val alertDialog: AlertDialog? = activity?.let {
        val view = layoutInflater.inflate(R.layout.dialog_electrum, null)
        val sslWarning = view.findViewById<TextView>(R.id.elec_dialog_ssl)
        val checkbox = view.findViewById<CheckBox>(R.id.elec_dialog_checkbox)
        val inputLabel = view.findViewById<TextView>(R.id.elec_dialog_input_label)
        val inputValue = view.findViewById<TextInputEditText>(R.id.elec_dialog_input_value)
        val currentPrefsAddress = Prefs.getElectrumServer(it)

        fun updateState(isChecked: Boolean) {
          BindingHelpers.enableOrFade(inputLabel, isChecked)
          BindingHelpers.enableOrFade(inputValue, isChecked)
          sslWarning.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        checkbox.setOnCheckedChangeListener { v, isChecked -> updateState(isChecked) }

        if (Strings.isNullOrEmpty(currentPrefsAddress)) {
          checkbox.isChecked = false
          updateState(false)
        } else {
          checkbox.isChecked = true
          inputValue.setText(currentPrefsAddress)
          updateState(true)
        }

        val builder = AlertDialog.Builder(it)
        builder.setView(view)
          .setPositiveButton(R.string.btn_confirm) { dialog, id ->
            val address = inputValue.text.toString()
            if (checkbox.isChecked && Strings.isNullOrEmpty(address)) {
              Toast.makeText(context, "Invalid address", Toast.LENGTH_SHORT).show()
            } else {
              Prefs.saveElectrumServer(it, if (checkbox.isChecked) inputValue.text.toString() else "")
              appKit.shutdown()
            }
          }
          .setNegativeButton(R.string.btn_cancel) { _, _ -> }
        builder.create()
      }
      alertDialog?.show()

    }
  }
}

enum class ElectrumServerState {
  INIT, IN_PROGRESS, DONE, ERROR
}

class ElectrumServerViewModel : ViewModel() {

  private val log = LoggerFactory.getLogger(this::class.java)
  val state = MutableLiveData<ElectrumServerState>()

  init {
    state.value = ElectrumServerState.INIT
  }

}
