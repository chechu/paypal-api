package com.daureos.paypal

/**
 * @see https://www.x.com/blogs/matt/2010/08/03/recurring-payments-ipns
 * @author jesus.lanchas
 *
 */
public enum TransactionType {

	ADJUSTMENT, CART, EXPRESS_CHECKOUT, MASSPAY, MERCH_PMT, NEW_CASE,
	SEND_MONEY, SUBSCR_CANCEL, SUBSCR_EOT, SUBSCR_FAILED, SUBSCR_MODIFY,
	SUBSCR_PAYMENT, SUBSCR_SIGNUP, VIRTUAL_TERMINAL, WEB_ACCEPT,
	
	// Recurring payment
	RECURRING_PAYMENT_PROFILE_CREATED, // When the profile is created
	RECURRING_PAYMENT, // for each successful payment
	RECURRING_PAYMENT_FAILED, // for each unsuccessful payment
	RECURRING_PAYMENT_SUSPEND_DUE_TO_MAX_FAILED_PAYMENT, // When the maximum number of failed payments is reached
	RECURRING_PAYMENT_PROFILE_CANCEL, // If the recurring payments profile is cancelled
	RECURRING_PAYMENT_OUTSTANDING_PAYMENT_FAILED, // If you call BillOutstandingAmount
	RECURRING_PAYMENT_OUTSTANDING_PAYMENT, // If you call BillOutstandingAmount
	RECURRING_PAYMENT_SKIPPED // PayPal was not able to process the recurring payment
	
	String toString() {
		return super.toString().replaceAll("_", "-").toLowerCase()
	}
}
