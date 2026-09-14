package com.pizzacafe.badin.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pizzacafe.badin.data.MenuItem
import com.pizzacafe.badin.data.Repository
import com.pizzacafe.badin.databinding.ActivityMenuManageBinding
import com.pizzacafe.badin.databinding.DialogAddMenuItemBinding
import com.pizzacafe.badin.ui.adapters.MenuManageAdapter
import kotlinx.coroutines.launch

class MenuManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuManageBinding
    private lateinit var repo: Repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuManageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        repo = Repository(this)

        val adapter = MenuManageAdapter(
            onEdit = { item -> showItemDialog(item) },
            onDelete = { item -> confirmDelete(item) }
        )
        binding.rvMenu.adapter = adapter
        binding.rvMenu.layoutManager = LinearLayoutManager(this)

        repo.menuDao.getAll().observe(this) { items -> adapter.submit(items) }

        binding.fabAdd.setOnClickListener { showItemDialog(null) }
    }

    private fun showItemDialog(existing: MenuItem?) {
        val dialogBinding = DialogAddMenuItemBinding.inflate(LayoutInflater.from(this))
        existing?.let {
            dialogBinding.etCategory.setText(it.category)
            dialogBinding.etName.setText(it.name)
            dialogBinding.etPrice.setText(it.price.toString())
            dialogBinding.cbFlavor.isChecked = it.hasFlavorOption
        }

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Add menu item" else "Edit menu item")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val category = dialogBinding.etCategory.text?.toString()?.trim().orEmpty()
                val name = dialogBinding.etName.text?.toString()?.trim().orEmpty()
                val price = dialogBinding.etPrice.text?.toString()?.toDoubleOrNull()
                val hasFlavor = dialogBinding.cbFlavor.isChecked

                if (category.isBlank() || name.isBlank() || price == null) {
                    Toast.makeText(this, "Fill category, name and a valid price", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    if (existing == null) {
                        repo.menuDao.insert(MenuItem(category = category, name = name, price = price, hasFlavorOption = hasFlavor))
                    } else {
                        repo.menuDao.update(existing.copy(category = category, name = name, price = price, hasFlavorOption = hasFlavor))
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(item: MenuItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete ${item.name}?")
            .setMessage("This only removes it from the menu — past bills are unaffected.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch { repo.menuDao.delete(item) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
