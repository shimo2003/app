package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.SubscriptionManager
import com.example.data.SerialRepository
import com.example.model.SerialBatch
import com.example.model.SerialItem
import com.example.util.ExcelGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("DocScan Pro", appName)
  }

  @Test
  fun `test 15 day free trial and paypal email`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val subManager = SubscriptionManager(context)
    val state = subManager.getTrialState()

    // 15 day free trial should be active on first run
    assertTrue(state.isTrialActive)
    assertTrue(state.daysRemaining in 1..15)
    assertEquals("shimo2003@gmail.com", SubscriptionManager.PAYPAL_EMAIL)
  }

  @Test
  fun `test serial scanning to excel conversion`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = SerialRepository(context)
    val batches = repo.batches.value
    assertTrue(batches.isNotEmpty())

    val batch = batches.first()
    val testXlsx = File(context.cacheDir, "test_serials.xlsx")
    val testCsv = File(context.cacheDir, "test_serials.csv")

    val xlsxSuccess = ExcelGenerator.exportToXlsx(batch, testXlsx)
    val csvSuccess = ExcelGenerator.exportToCsv(batch, testCsv)

    assertTrue(xlsxSuccess)
    assertTrue(testXlsx.exists() && testXlsx.length() > 0)
    assertTrue(csvSuccess)
    assertTrue(testCsv.exists() && testCsv.length() > 0)
  }
}
