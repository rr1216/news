package com.app.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.app.R
import com.app.util.showToast

class TestFragment(private var testName: String) : Fragment() {

    private lateinit var testNameView: TextView
    private lateinit var btn: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_test, container, false)
        testNameView = view.findViewById(R.id.test_name)
        btn = view.findViewById(R.id.test_btn)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        testNameView.text = testName
        btn.setOnClickListener {
            btn.text.toString().showToast()
        }
    }
}