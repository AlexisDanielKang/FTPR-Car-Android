package com.example.myapitest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.minhaprimeiraapi.service.RetrofitClient
import com.example.minhaprimeiraapi.service.safeApiCall
import com.example.myapitest.adapter.CarAdapter
import com.example.myapitest.databinding.ActivityMainBinding
import com.example.myapitest.model.Car
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestLocationPermission()
        setupView()
    }

    override fun onResume() {
        super.onResume()
        fetchItems()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                onLogout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = true
            fetchItems()
        }
        binding.addCta.setOnClickListener {
            startActivity(NewCarActivity.newIntent(this))
        }
    }

    private fun requestLocationPermission() {
        // TODO
    }

    private fun fetchItems() {
       CoroutineScope(Dispatchers.IO).launch {
           val result = safeApiCall { RetrofitClient.apiService.getCars() }

           withContext(Dispatchers.Main) {
               binding.swipeRefreshLayout.isRefreshing = false
               when (result) {
                   is com.example.minhaprimeiraapi.service.Result.Error ->{}
                   is com.example.minhaprimeiraapi.service.Result.Success -> handleOnSuccess(result.data)
               }
           }
       }
    }

    private fun handleOnSuccess(data: List<Car>) {
        val adapter = CarAdapter(data) { car ->

            startActivity(CarDetailActivity.newIntent(
                this,
                car.id
            ))
        }
        binding.recyclerView.adapter = adapter
    }

    private fun onLogout() {
        FirebaseAuth.getInstance().signOut()
        val intent = LoginActivity.newIntent(this)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {

        fun newIntent(context: Context) = Intent(context, MainActivity::class.java)
    }
}
