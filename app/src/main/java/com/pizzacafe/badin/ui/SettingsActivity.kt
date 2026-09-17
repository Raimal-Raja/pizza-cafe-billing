package com.pizzacafe.badin.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pizzacafe.badin.data.DeliveryZone
import com.pizzacafe.badin.data.Repository
import com.pizzacafe.badin.data.Rider
import com.pizzacafe.badin.data.Waiter
import com.pizzacafe.badin.databinding.ActivitySettingsBinding
import com.pizzacafe.badin.databinding.DialogDeliveryZoneBinding
import com.pizzacafe.badin.databinding.DialogStaffBinding
import com.pizzacafe.badin.ui.adapters.RiderAdapter
import com.pizzacafe.badin.ui.adapters.WaiterAdapter
import com.pizzacafe.badin.ui.adapters.ZoneAdapter
import com.pizzacafe.badin.util.Prefs
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var repo: Repository
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        repo = Repository(this)
        prefs = Prefs(this)

        binding.etName.setText(prefs.restaurantName)
        binding.etPhone.setText(prefs.restaurantPhone)
        binding.etAddress.setText(prefs.restaurantAddress)
        binding.etTableCount.setText(prefs.tableCount.toString())
        binding.etThreshold.setText(prefs.freeDeliveryThreshold.toString())

        binding.btnSaveInfo.setOnClickListener {
            prefs.restaurantName = binding.etName.text?.toString().orEmpty()
            prefs.restaurantPhone = binding.etPhone.text?.toString().orEmpty()
            prefs.restaurantAddress = binding.etAddress.text?.toString().orEmpty()
            prefs.tableCount = binding.etTableCount.text?.toString()?.toIntOrNull() ?: prefs.tableCount
            prefs.freeDeliveryThreshold = binding.etThreshold.text?.toString()?.toDoubleOrNull() ?: prefs.freeDeliveryThreshold
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        }

        setupZones()
        setupRiders()
        setupWaiters()
    }

    private fun setupZones() {
        val zoneAdapter = ZoneAdapter { zone ->
            lifecycleScope.launch { repo.zoneDao.delete(zone) }
        }
        binding.rvZones.adapter = zoneAdapter
        binding.rvZones.layoutManager = LinearLayoutManager(this)
        repo.zoneDao.getAll().observe(this) { zones -> zoneAdapter.submit(zones) }

        binding.btnAddZone.setOnClickListener { showAddZoneDialog() }
    }

    private fun showAddZoneDialog() {
        val dialogBinding = DialogDeliveryZoneBinding.inflate(LayoutInflater.from(this))
        AlertDialog.Builder(this)
            .setTitle("Add delivery zone")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val name = dialogBinding.etZoneName.text?.toString()?.trim().orEmpty()
                val charge = dialogBinding.etZoneCharge.text?.toString()?.toDoubleOrNull() ?: 0.0
                if (name.isBlank()) {
                    Toast.makeText(this, "Enter a zone name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch { repo.zoneDao.insert(DeliveryZone(name = name, charge = charge)) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ---- Riders ----

    private fun setupRiders() {
        val riderAdapter = RiderAdapter { rider, active ->
            lifecycleScope.launch { repo.riderDao.update(rider.copy(active = active)) }
        }
        binding.rvRiders.adapter = riderAdapter
        binding.rvRiders.layoutManager = LinearLayoutManager(this)
        repo.riderDao.getAllRiders().observe(this) { riders -> riderAdapter.submit(riders) }

        binding.btnAddRider.setOnClickListener { showAddStaffDialog(isRider = true) }
    }

    // ---- Waiters ----

    private fun setupWaiters() {
        val waiterAdapter = WaiterAdapter { waiter, active ->
            lifecycleScope.launch { repo.waiterDao.update(waiter.copy(active = active)) }
        }
        binding.rvWaiters.adapter = waiterAdapter
        binding.rvWaiters.layoutManager = LinearLayoutManager(this)
        repo.waiterDao.getAllWaiters().observe(this) { waiters -> waiterAdapter.submit(waiters) }

        binding.btnAddWaiter.setOnClickListener { showAddStaffDialog(isRider = false) }
    }

    /** Shared "add rider / add waiter" dialog — same two fields (name + phone) either way. */
    private fun showAddStaffDialog(isRider: Boolean) {
        val dialogBinding = DialogStaffBinding.inflate(LayoutInflater.from(this))
        AlertDialog.Builder(this)
            .setTitle(if (isRider) "Add rider" else "Add waiter")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val name = dialogBinding.etStaffName.text?.toString()?.trim().orEmpty()
                val phone = dialogBinding.etStaffPhone.text?.toString()?.trim().takeUnless { it.isNullOrBlank() }
                if (name.isBlank()) {
                    Toast.makeText(this, "Enter a name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    if (isRider) repo.riderDao.insert(Rider(name = name, phone = phone))
                    else repo.waiterDao.insert(Waiter(name = name, phone = phone))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
