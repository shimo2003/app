package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.SubscriptionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
}
