package com.example.flashcards

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.view.get
import androidx.lifecycle.asLiveData
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import com.example.flashcards.data.AppDataStore
import com.example.flashcards.fragments.SetsFragmentDirections
import kotlin.system.exitProcess

private const val TAG = "MainActivityTag"

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private lateinit var navController: NavController
    val PORTRAIT_ORIENTATION_MAX_DP = 850

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_container) as NavHostFragment
        navController = navHostFragment.navController

        val actionBar = supportActionBar
        actionBar!!.setDisplayShowTitleEnabled(false)
        actionBar.setDisplayShowCustomEnabled(true)

        val appBarView = LayoutInflater.from(this).inflate(R.layout.app_bar_view, null)
        actionBar.customView = appBarView

        setupActionBarWithNavController(this, navController)

        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels / displayMetrics.density
        val height = displayMetrics.heightPixels / displayMetrics.density

        if(width <= PORTRAIT_ORIENTATION_MAX_DP && height <= PORTRAIT_ORIENTATION_MAX_DP)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

//    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
//        menuInflater.inflate(R.menu.app_menu, menu)
//        return super.onPrepareOptionsMenu(menu)
//    }
//
////    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
////        menuInflater.inflate(R.menu.app_menu, menu)
////        return super.onCreateOptionsMenu(menu)
////    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//
//        when(item.itemId) {
//
//            R.id.track_progress_item -> { trackProgress() }
//
//            R.id.rate_us_item -> { rateUs() }
//
//            R.id.contact_item -> { contact() }
//
//            R.id.about_us_item -> { displayAboutUsDialog() }
//
//            R.id.exit_item -> { this.finishAffinity() }
//
//        }
//
//        return super.onOptionsItemSelected(item)
//    }
//
//    private fun trackProgress() {
//        if(navController.currentDestination == navController.findDestination(R.id.setsFragment)) {
//            val action = SetsFragmentDirections.actionSetsFragmentToTrackProgressFragment()
//            navController.navigate(action)
//        }
//    }
//
//    private fun rateUs() {
//        try {
//            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
//            startActivity(intent)
//        } catch (e: ActivityNotFoundException) {
//            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
//            startActivity(intent)
//        }
//    }
//
//    private fun contact() {
//        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
//            //.setType("text/plain")
//            .putExtra(Intent.EXTRA_EMAIL, arrayOf(resources.getString(R.string.email)))
//
//        startActivity(intent)
//
//    }
//
//    private fun displayAboutUsDialog() {
//
//        val aboutUsView = LayoutInflater.from(this).inflate(R.layout.about_us_dialog, null)
//
//        val aboutUsDialog = AlertDialog.Builder(this)
//            .setView(aboutUsView)
//            .create()
//
//        aboutUsDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//
//        aboutUsDialog.show()
//
//    }

}