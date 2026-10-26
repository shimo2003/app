package com.aistudio.docscanpro.zqwk.billing

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import com.aistudio.docscanpro.zqwk.model.PlanDuration
import com.aistudio.docscanpro.zqwk.model.TrialState
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class SubscriptionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "docscan_subscription_prefs"
        private const val KEY_TRIAL_START = "key_trial_start_epoch"
        private const val KEY_PRO_UNLOCKED = "key_pro_unlocked"
        private const val KEY_ACTIVATION_ID = "key_activation_id"
        private const val KEY_PURCHASE_DATE = "key_purchase_date"

        const val PAYPAL_EMAIL = "shimo2003@gmail.com"
        const val TRIAL_DAYS = 15
        const val TRIAL_DURATION_MS = TRIAL_DAYS * 24 * 60 * 60 * 1000L
    }

    init {
        // Initialize trial start if first time
        if (!prefs.contains(KEY_TRIAL_START)) {
            prefs.edit().putLong(KEY_TRIAL_START, System.currentTimeMillis()).apply()
        }
    }

    fun getTrialState(): TrialState {
        val isPro = prefs.getBoolean(KEY_PRO_UNLOCKED, false)
        val startTime = prefs.getLong(KEY_TRIAL_START, System.currentTimeMillis())
        val endTime = startTime + TRIAL_DURATION_MS
        val now = System.currentTimeMillis()

        val diffMs = endTime - now
        val isTrialActive = diffMs > 0

        val daysRemaining = if (diffMs > 0) {
            (diffMs / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(0)
        } else {
            0
        }

        val hoursRemaining = if (diffMs > 0) {
            ((diffMs % (24 * 60 * 60 * 1000L)) / (60 * 60 * 1000L)).toInt().coerceAtLeast(0)
        } else {
            0
        }

        return TrialState(
            daysRemaining = if (isTrialActive && daysRemaining == 0) 1 else daysRemaining,
            hoursRemaining = hoursRemaining,
            isTrialActive = isTrialActive,
            isProUnlocked = isPro,
            trialStartEpochMs = startTime,
            trialEndEpochMs = endTime
        )
    }

    fun unlockPro(transactionOrCode: String): Boolean {
        prefs.edit()
            .putBoolean(KEY_PRO_UNLOCKED, true)
            .putString(KEY_ACTIVATION_ID, transactionOrCode)
            .putLong(KEY_PURCHASE_DATE, System.currentTimeMillis())
            .apply()
        return true
    }

    fun revokePro() {
        prefs.edit()
            .putBoolean(KEY_PRO_UNLOCKED, false)
            .remove(KEY_ACTIVATION_ID)
            .remove(KEY_PURCHASE_DATE)
            .apply()
    }

    fun resetTrial() {
        prefs.edit()
            .putLong(KEY_TRIAL_START, System.currentTimeMillis())
            .putBoolean(KEY_PRO_UNLOCKED, false)
            .apply()
    }

    fun expireTrialForTesting() {
        // Set trial start to 16 days ago
        val sixteenDaysAgo = System.currentTimeMillis() - (16 * 24 * 60 * 60 * 1000L)
        prefs.edit()
            .putLong(KEY_TRIAL_START, sixteenDaysAgo)
            .putBoolean(KEY_PRO_UNLOCKED, false)
            .apply()
    }

    fun createPayPalPaymentIntent(plan: PlanDuration): Intent {
        val amount = when (plan) {
            PlanDuration.MONTHLY -> "4.99"
            PlanDuration.ANNUAL -> "29.99"
            PlanDuration.LIFETIME -> "49.99"
        }
        val itemName = URLEncoder.encode("DocScan Pro - ${plan.title}", StandardCharsets.UTF_8.toString())
        val url = "https://www.paypal.com/cgi-bin/webscr?cmd=_xclick&business=$PAYPAL_EMAIL&item_name=$itemName&currency_code=USD&amount=$amount"

        return Intent(Intent.ACTION_VIEW, Uri.parse(url))
    }

    fun createSendEmailIntent(): Intent {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$PAYPAL_EMAIL")
            putExtra(Intent.EXTRA_SUBJECT, "DocScan Pro Subscription Payment Inquiry")
            putExtra(
                Intent.EXTRA_TEXT,
                "Hello,\n\nI would like to activate my DocScan Pro license using PayPal account: $PAYPAL_EMAIL.\n\nThank you!"
            )
        }
        return intent
    }
}
