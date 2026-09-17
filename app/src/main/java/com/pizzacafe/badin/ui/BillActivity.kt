package com.pizzacafe.badin.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pizzacafe.badin.R
import com.pizzacafe.badin.data.Order
import com.pizzacafe.badin.data.OrderStatus
import com.pizzacafe.badin.data.OrderType
import com.pizzacafe.badin.data.Repository
import com.pizzacafe.badin.databinding.ActivityBillBinding
import com.pizzacafe.badin.databinding.DialogDeleteReasonBinding
import com.pizzacafe.badin.util.Currency
import com.pizzacafe.badin.util.DateUtil
import com.pizzacafe.badin.util.Prefs
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class BillActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ORDER_ID = "order_id"
    }

    private lateinit var binding: ActivityBillBinding
    private lateinit var repo: Repository
    private lateinit var prefs: Prefs
    private var orderId: Long = -1L
    private var receiptHtml: String = ""
    private var currentOrder: Order? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBillBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        repo = Repository(this)
        prefs = Prefs(this)
        orderId = intent.getLongExtra(EXTRA_ORDER_ID, -1L)

        binding.btnPrint.setOnClickListener { printReceipt() }
        binding.btnComplete.setOnClickListener { markCompleted() }
        binding.btnEdit.setOnClickListener { editOrder() }
        binding.btnDelete.setOnClickListener { confirmDelete() }

        loadBill()
    }

    override fun onResume() {
        super.onResume()
        loadBill() // picks up edits made via "Edit Order" when we return to this screen
    }

    private fun loadBill() {
        lifecycleScope.launch {
            val order = repo.orderDao.getOrderById(orderId) ?: return@launch
            currentOrder = order
            val items = repo.orderDao.getItemsForOrderSync(orderId)

            binding.tvBillHeader.text = "PIZZA CAFE: TASTE OF BADIN"
            binding.tvBillSub.text = "${prefs.restaurantAddress}\n${prefs.restaurantPhone}"

            val meta = StringBuilder()
            val invoiceLabel = if (order.invoiceNumber > 0) "Invoice #${order.invoiceNumber} (${order.orderDate})" else "Bill #${order.id}"
            // Uses updatedAt — the moment this bill was last saved/printed — rather than
            // createdAt, so an edited order shows the correct current time, not a stale one.
            meta.append("$invoiceLabel   ${DateUtil.displayTime(order.updatedAt)}\n")
            meta.append(
                when (order.type) {
                    OrderType.DINE_IN -> {
                        val waiter = order.waiterName?.takeUnless { it.isBlank() }
                        "Dine-in — Table ${order.tableNumber ?: "-"}" + (waiter?.let { "\nWaiter: $it" } ?: "")
                    }
                    OrderType.DELIVERY -> {
                        val rider = order.riderName?.takeUnless { it.isBlank() }
                        "Delivery — ${order.customerName ?: ""}  ${order.phone ?: ""}\n${order.address ?: ""}" +
                            (rider?.let { "\nRider: $it" } ?: "")
                    }
                    else -> {
                        val waiter = order.waiterName?.takeUnless { it.isBlank() }
                        "Takeaway — ${order.customerName ?: "Walk-in"}  ${order.phone ?: ""}" + (waiter?.let { "\nWaiter: $it" } ?: "")
                    }
                }
            )
            binding.tvBillMeta.text = meta.toString()

            if (order.status == OrderStatus.CANCELLED) {
                binding.tvCancelledBanner.visibility = View.VISIBLE
                binding.tvCancelledBanner.text = "CANCELLED — ${order.deleteReason ?: "No reason given"}"
            } else {
                binding.tvCancelledBanner.visibility = View.GONE
            }

            binding.billItemsContainer.removeAllViews()
            val htmlItems = StringBuilder()
            items.forEach { item ->
                val row = TextView(this@BillActivity)
                val flavorText = if (!item.flavorNote.isNullOrBlank()) " (${item.flavorNote})" else ""
                row.text = "${item.qty} x ${item.itemName}$flavorText — ${Currency.format(item.lineTotal)}"
                row.textSize = 14f
                row.setPadding(0, 4, 0, 4)
                binding.billItemsContainer.addView(row)

                htmlItems.append(
                    "<tr><td class='qty'>${item.qty}</td><td>${item.itemName}$flavorText</td>" +
                        "<td class='amt'>${Currency.format(item.lineTotal)}</td></tr>"
                )
            }

            binding.tvSubtotal.text = "Subtotal: ${Currency.format(order.subtotal)}"
            binding.tvDeliveryCharge.text = if (order.deliveryCharge > 0)
                "Delivery: ${Currency.format(order.deliveryCharge)}" else "Delivery: Free"
            if (order.discountAmount > 0) {
                binding.tvDiscount.visibility = View.VISIBLE
                val note = order.discountNote?.let { " ($it)" } ?: ""
                binding.tvDiscount.text = "Discount$note: -${Currency.format(order.discountAmount)}"
            } else {
                binding.tvDiscount.visibility = View.GONE
            }
            binding.tvGrandTotal.text = "Total: ${Currency.format(order.total)}"

            val isCancelled = order.status == OrderStatus.CANCELLED
            val isCompleted = order.status == OrderStatus.COMPLETED
            binding.btnComplete.visibility = if (isCompleted || isCancelled) View.GONE else View.VISIBLE
            binding.btnEdit.visibility = if (isCancelled) View.GONE else View.VISIBLE
            binding.btnDelete.visibility = if (isCancelled) View.GONE else View.VISIBLE
            binding.btnPrint.isEnabled = !isCancelled

            receiptHtml = buildReceiptHtml(order, htmlItems.toString(), meta.toString())
        }
    }

    private fun logoBase64(): String {
        return try {
            val bitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.logo_pizza_cafe)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    private fun buildReceiptHtml(order: Order, itemsHtml: String, metaText: String): String {
        val logoTag = logoBase64().let {
            // object-fit:contain (not cover) keeps the transparent logo un-cropped on the
            // white bill background instead of zooming/cutting off its edges.
            if (it.isBlank()) "" else "<div style='text-align:center;background:#ffffff;'><img src='data:image/png;base64,$it' style='width:90px;height:90px;object-fit:contain;'/></div>"
        }
        val discountRow = if (order.discountAmount > 0) {
            val note = order.discountNote?.let { " ($it)" } ?: ""
            "<tr><td>Discount$note</td><td class='amt'>-${Currency.format(order.discountAmount)}</td></tr>"
        } else ""
        val cancelledBanner = if (order.status == OrderStatus.CANCELLED) {
            "<p style='text-align:center;background:#B0212E;color:#fff;font-weight:bold;padding:4px;'>CANCELLED — ${order.deleteReason ?: ""}</p>"
        } else ""
        // Clean, clearly-sectioned receipt: header, order meta (with rider/waiter baked into
        // metaText), a proper 3-column items table (qty / item / amount), then totals.
        return """
            <html><body style="font-family: monospace; width: 280px; background:#ffffff; font-size:13px;">
            $logoTag
            <h2 style="text-align:center;margin:2px 0 0 0;font-size:15px;white-space:nowrap;overflow:hidden;letter-spacing:0.3px;">PIZZA CAFE: TASTE OF BADIN</h2>
            <p style="text-align:center;margin:4px 0;">${prefs.restaurantAddress}<br/>${prefs.restaurantPhone}</p>
            $cancelledBanner
            <hr style="border:none;border-top:1px dashed #333;"/>
            <p style="margin:6px 0;">${metaText.replace("\n", "<br/>")}</p>
            <hr style="border:none;border-top:1px dashed #333;"/>
            <table style="width:100%;border-collapse:collapse;">
              <tr style="font-weight:bold;">
                <td class="qty">Qty</td><td>Item</td><td class="amt">Amount</td>
              </tr>
              $itemsHtml
            </table>
            <hr style="border:none;border-top:1px dashed #333;"/>
            <table style="width:100%;">
              <tr><td>Subtotal</td><td class="amt">${Currency.format(order.subtotal)}</td></tr>
              <tr><td>Delivery</td><td class="amt">${if (order.deliveryCharge > 0) Currency.format(order.deliveryCharge) else "Free"}</td></tr>
              $discountRow
              <tr><td><b>Total</b></td><td class="amt"><b>${Currency.format(order.total)}</b></td></tr>
            </table>
            <p style="text-align:center;margin-top:12px;">Thank you for ordering!</p>
            <style>
              td { padding: 2px 0; vertical-align: top; }
              td.qty { width: 28px; }
              td.amt { text-align: right; white-space: nowrap; }
            </style>
            </body></html>
        """.trimIndent()
    }

    private fun printReceipt() {
        if (receiptHtml.isBlank()) {
            Toast.makeText(this, "Bill not ready yet", Toast.LENGTH_SHORT).show()
            return
        }
        val webView = WebView(this)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                createPrintJob(webView)
            }
        }
        webView.loadDataWithBaseURL(null, receiptHtml, "text/HTML", "UTF-8", null)
    }

    private fun createPrintJob(webView: WebView) {
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "${prefs.restaurantName} - Bill #$orderId"
        val adapter = webView.createPrintDocumentAdapter(jobName)
        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }

    private fun markCompleted() {
        lifecycleScope.launch {
            val order = repo.orderDao.getOrderById(orderId) ?: return@launch
            repo.orderDao.updateOrder(order.copy(status = OrderStatus.COMPLETED, updatedAt = System.currentTimeMillis()))
            Toast.makeText(this@BillActivity, "Order completed", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun editOrder() {
        startActivity(Intent(this, NewOrderActivity::class.java).apply {
            putExtra(NewOrderActivity.EXTRA_ORDER_ID, orderId)
        })
        finish()
    }

    private fun confirmDelete() {
        val dialogBinding = DialogDeleteReasonBinding.inflate(LayoutInflater.from(this))
        AlertDialog.Builder(this)
            .setTitle("Delete this order?")
            .setView(dialogBinding.root)
            .setPositiveButton("Delete") { _, _ ->
                val reason = dialogBinding.etDeleteReason.text?.toString()?.trim()
                if (reason.isNullOrBlank()) {
                    Toast.makeText(this, "Please enter a reason", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                deleteOrder(reason)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteOrder(reason: String) {
        lifecycleScope.launch {
            val order = repo.orderDao.getOrderById(orderId) ?: return@launch
            repo.orderDao.updateOrder(
                order.copy(
                    status = OrderStatus.CANCELLED,
                    deleteReason = reason,
                    deletedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            Toast.makeText(this@BillActivity, "Order deleted", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
