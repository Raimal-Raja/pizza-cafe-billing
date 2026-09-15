package com.pizzacafe.badin.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pizzacafe.badin.R
import com.pizzacafe.badin.data.OrderStatus
import com.pizzacafe.badin.data.OrderType
import com.pizzacafe.badin.data.Repository
import com.pizzacafe.badin.databinding.ActivityBillBinding
import com.pizzacafe.badin.util.Currency
import com.pizzacafe.badin.util.Prefs
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BillActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ORDER_ID = "order_id"
    }

    private lateinit var binding: ActivityBillBinding
    private lateinit var repo: Repository
    private lateinit var prefs: Prefs
    private var orderId: Long = -1L
    private var receiptHtml: String = ""

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

        loadBill()
    }

    private fun loadBill() {
        lifecycleScope.launch {
            val order = repo.orderDao.getOrderById(orderId) ?: return@launch
            val items = repo.orderDao.getItemsForOrderSync(orderId)

            binding.tvBillHeader.text = prefs.restaurantName.ifBlank { "Pizza Cafe" }
            binding.tvBillSub.text = "${prefs.restaurantAddress}\n${prefs.restaurantPhone}"

            val timeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val meta = StringBuilder()
            val invoiceLabel = if (order.invoiceNumber > 0) "Invoice #${order.invoiceNumber} (${order.orderDate})" else "Bill #${order.id}"
            meta.append("$invoiceLabel   ${timeFormat.format(Date(order.createdAt))}\n")
            meta.append(
                when (order.type) {
                    OrderType.DINE_IN -> "Dine-in — Table ${order.tableNumber ?: "-"}"
                    OrderType.DELIVERY -> "Delivery — ${order.customerName ?: ""}  ${order.phone ?: ""}\n${order.address ?: ""}"
                    else -> "Takeaway — ${order.customerName ?: "Walk-in"}  ${order.phone ?: ""}"
                }
            )
            binding.tvBillMeta.text = meta.toString()

            binding.billItemsContainer.removeAllViews()
            val htmlItems = StringBuilder()
            items.forEach { item ->
                val row = TextView(this@BillActivity)
                val flavorText = if (!item.flavorNote.isNullOrBlank()) " (${item.flavorNote})" else ""
                row.text = "${item.qty} x ${item.itemName}$flavorText — ${Currency.format(item.lineTotal)}"
                row.textSize = 14f
                row.setPadding(0, 4, 0, 4)
                binding.billItemsContainer.addView(row)

                htmlItems.append("<tr><td>${item.qty} x ${item.itemName}$flavorText</td><td style='text-align:right'>${Currency.format(item.lineTotal)}</td></tr>")
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

            binding.btnComplete.visibility =
                if (order.status == OrderStatus.COMPLETED) View.GONE else View.VISIBLE

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

    private fun buildReceiptHtml(order: com.pizzacafe.badin.data.Order, itemsHtml: String, metaText: String): String {
        val logoTag = logoBase64().let {
            // object-fit:contain (not cover) keeps the transparent logo un-cropped on the
            // white bill background instead of zooming/cutting off its edges.
            if (it.isBlank()) "" else "<div style='text-align:center;background:#ffffff;'><img src='data:image/png;base64,$it' style='width:100px;height:100px;object-fit:contain;'/></div>"
        }
        val discountRow = if (order.discountAmount > 0) {
            val note = order.discountNote?.let { " ($it)" } ?: ""
            "<tr><td>Discount$note</td><td style=\"text-align:right;\">-${Currency.format(order.discountAmount)}</td></tr>"
        } else ""
        return """
            <html><body style="font-family: monospace; width: 280px; background:#ffffff;">
            $logoTag
            <h2 style="text-align:center;margin-bottom:0;">${prefs.restaurantName}</h2>
            <p style="text-align:center;margin-top:4px;">${prefs.restaurantAddress}<br/>${prefs.restaurantPhone}</p>
            <hr/>
            <p>${metaText.replace("\n", "<br/>")}</p>
            <hr/>
            <table style="width:100%;">$itemsHtml</table>
            <hr/>
            <table style="width:100%;">
              <tr><td>Subtotal</td><td style="text-align:right;">${Currency.format(order.subtotal)}</td></tr>
              <tr><td>Delivery</td><td style="text-align:right;">${if (order.deliveryCharge > 0) Currency.format(order.deliveryCharge) else "Free"}</td></tr>
              $discountRow
              <tr><td><b>Total</b></td><td style="text-align:right;"><b>${Currency.format(order.total)}</b></td></tr>
            </table>
            <p style="text-align:center;margin-top:12px;">Thank you for ordering!</p>
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
}
