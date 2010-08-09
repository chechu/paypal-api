package com.daureos.paypal

import grails.converters.JSON

/**
 * This class represents an IPN message from Paypal
 * 
 * @author jesus.lanchas
 *
 */
public class IPN {
	
	static mapping = {
		table 'paypal_ipn'
	}
	
	static transients = ['asMap']
	
	// The default constructor
	public IPN(){}
	
	//Auto timestamping
	Date dateCreated
	Date lastUpdated
	
	/**
	 * The IPN request made by Paypal to our server
	 */
	String raw
	
	/**
	 * All the request params in JSON format
	 */
	String asJson
	
	/**
	 * All the request params in map format (transient)
	 */
	Map asMap = [:]
	
	/**
	 * The type of transaction
	 */
	TransactionType transactionType
	
	/**
	 * The id of the transaction
	 */
	String txnId
	
	/**
	 * Primary email address of the payment recipient
	 */
	String receiverEmail
	
	/**
	 * The email of the payer
	 */
	String payerEmail
	
	/**
	 * The id of the payer
	 */
	String payerId
	
	/**
	 * True if the payer address is confirmed and false otherwise
	 */
	Boolean addressConfirmed
	
	static constraints = {
		asJson(nullable:true, maxSize:1000)
		raw(maxSize:1000)
		txnId(unique:true)
	}
	
	/**
	 * This method will be called when an external user uses ipnInstance.key
	 */
	public def get(String key) {
		return asMap[key]
	}
	
	void populateFromPaypal(paypalArgs) {
		raw = paypalArgs.toQueryString()[1..-1]
		asMap = paypalArgs
		
		log.debug("Populating IPN, with params: ${paypalArgs}")
		
		if(paypalArgs.txn_type) {
			try {
				transactionType = TransactionType.valueOf(paypalArgs.txn_type.toUpperCase())
			} catch (IllegalArgumentException ex){
				log.warn("Received an invalid IPN message: ${paypalArgs}")
			}
		}
		txnId = paypalArgs.txn_id ?: "TRANS-${System.currentTimeMillis()}"
		receiverEmail = paypalArgs.receiver_email
		payerEmail = paypalArgs.payer_email
		payerId = paypalArgs.payer_id
		addressConfirmed = (paypalArgs.address_status == 'confirmed')
		
		log.debug("Populated values: txnId:${txnId}; receiverEmail:${receiverEmail}; payerEmail:${payerEmail}; payerId:${payerId}; addressConfirmed:${addressConfirmed}")
	}
	
	/**
	 * This method is called before the first insert in the database.
	 */
	def beforeInsert() {
		asJson = asMap ? asMap as JSON : null
   }

	/**
	 * This method is called before any update to the database.
	 */
	def beforeUpdate() {
		beforeInsert()
	}
   
	/**
	 * This method is called after the object is loaded from the database.
	 */
	def afterLoad() {
		asMap = asJson ? JSON.parse(asJson) : [:]
	}
}