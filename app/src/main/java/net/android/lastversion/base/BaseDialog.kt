//package net.android.lastversion.base
//
//import android.app.Activity
//import android.app.Dialog
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.Window
//import androidx.databinding.DataBindingUtil
//import androidx.databinding.ViewDataBinding
//import net.android.lastversion.R
//import net.android.lastversion.utils.SystemUtils
//import net.android.lastversion.utils.showSystemUI
//
//
//abstract class BaseDialog <DB : ViewDataBinding>(var context : Activity, var canAble: Boolean) :
//    Dialog(context, R.style.BaseDialog) {
//
//    lateinit var binding: DB
//
//    abstract fun getContentView(): Int
//    abstract fun initView()
//    abstract fun bindView()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        SystemUtils.setLocale(context)
//        super.onCreate(savedInstanceState)
//        requestWindowFeature(Window.FEATURE_NO_TITLE)
//        binding = DataBindingUtil.inflate(LayoutInflater.from(context), getContentView(), null, false)
//        setContentView(binding.root)
//        setCancelable(canAble)
//        initView()
//        bindView()
//        setOnDismissListener {
//            context.showSystemUI()
//        }
//    }
//}