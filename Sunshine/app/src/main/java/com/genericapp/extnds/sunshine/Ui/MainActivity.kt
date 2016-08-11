package com.genericapp.extnds.sunshine.Ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.preference.PreferenceManager
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.genericapp.extnds.mozillarecpro.DividerItemDecoration
import com.genericapp.extnds.sunshine.Models.Retrofit.Forcast
import com.genericapp.extnds.sunshine.Models.SugarORM.Location
import com.genericapp.extnds.sunshine.R
import com.genericapp.extnds.sunshine.Settings.SettingsActivity
import com.genericapp.extnds.sunshine.Utils.API.apiService
import com.genericapp.extnds.sunshine.Utils.Database.DatabaseService
import com.orm.SugarRecord
import kotlinx.android.synthetic.main.action_bar.*
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity() {

    companion object
    {
        const val TAG = "MainActivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(action_bar)
        getForcast()

    }

    private fun getForcast()
    {
        progress_bar.visibility = View.VISIBLE
        sunshine_main_weather_list.adapter=null

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val cityName = sharedPrefs.getString("location_preference", "Kolkata")
        val units = sharedPrefs.getString("units_preference", "metric")

        sunshine_main_weather_list.layoutManager = LinearLayoutManager(this@MainActivity)
        sunshine_main_weather_list.addItemDecoration(DividerItemDecoration(this@MainActivity, DividerItemDecoration.VERTICAL_LIST))

        apiService.forcastQuery(cityName, units).enqueue(object : Callback<Forcast> {
            override fun onResponse(call: Call<Forcast>, response: Response<Forcast>) {

                if(response.isSuccessful) {
                    progress_bar.visibility = View.GONE
                    DatabaseService.addForcasts(response.body())
                    sunshine_main_weather_list.adapter= WeatherListAdapter(response.body().list!!)
                    Toast.makeText(this@MainActivity,"Data Successfully Fetched", Toast.LENGTH_SHORT).show()
                }
                else {
                    unsuccessfulFetch(cityName)
                }
            }

            override fun onFailure(call: Call<Forcast>, t: Throwable) {
                unsuccessfulFetch(cityName)
            }
        })
    }

    private fun unsuccessfulFetch(cityName : String){
        progress_bar.visibility = View.GONE
        val dbLoc = SugarRecord.find(Location::class.java,"name = ?",cityName)
        if(dbLoc.isEmpty())
            Toast.makeText(this@MainActivity,"Couldn't fetch data. Try again", Toast.LENGTH_SHORT).show()
        else{
            sunshine_main_weather_list.adapter = WeatherListAdapter(forcastList = dbLoc[0].getForcastsForLastFetch())
            Toast.makeText(this@MainActivity,"Couldn't fetch data. Showing Cache", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item!!.itemId)
        {
            R.id.refresh -> {
                getForcast()
                return true
            }
            R.id.settings -> {

                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
            R.id.map -> {
                val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
                val cityName = sharedPrefs.getString("location_preference", "Kolkata")
                val geoLocation = Uri.parse("geo:0,0?").buildUpon()
                    .appendQueryParameter("q",cityName)
                    .build()

                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = geoLocation
                if(intent.resolveActivity(packageManager) != null)
                    startActivity(intent)
                else
                    Log.d(TAG,"No app to run Maps")
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }

        }

    }
}
