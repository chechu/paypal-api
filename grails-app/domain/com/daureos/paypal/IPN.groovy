package com.daureos.paypal

import grails.converters.JSON

/**
 * This class represents an IPN message from Paypal
 * 
 * @author jesus.lanchas
 *
 */
class IPN {
	
	static mapping = {
		table 'paypal_ipn'
	}
	
	static transients = ['asMap']
	
	// The default constructor
	public IPN(){}
	
	// Receiving the request params
	public IPN(params){
		raw = params.toQueryString()[1..-1]
		asMap = params
		populateFromPaypal(params)
	}
	
	//Auto timestamping
	Date dateCreated
	Date lastUpdated
	
	/**
	 * The IPN request made by Paypal to our server
	 */
	private String raw
	
	/**
	 * All the request params in JSON format
	 */
	private String asJson
	
	/**
	 * All the request params in map format (transient)
	 */
	private Map asMap = [:]
	
	/**
	 * The type of transaction
	 */
	private TransactionType transactionType
	
	/**
	 * The id of the transaction
	 */
	private String txnId
	
	/**
	 * Primary email address of the payment recipient
	 */
	private String receiverEmail
	
	/**
	 * The email of the payer
	 */
	private String payerEmail
	
	/**
	 * The id of the payer
	 */
	private String payerId
	
	/**
	 * True if the payer address is confirmed and false otherwise
	 */
	private Boolean addressConfirmed
	
	static constraints = {
		asJSON(nullable:true, maxSize:1000)
		txnId(unique:true)
	}
	
	/**
	 * This method will be called when an external user uses ipnInstance.key
	 */
	public def get(String key) {
		return asMap[key]
	}
	
	private void populateFromPaypal(Map paypalArgs) {
		if(paypalArgs.txn_type) {
			try {
				transactionType = TransactionType.valueOf(paypalArgs.txn_type.toUpperCase())
			} catch (IllegalArgumentException ex){
				log.warn("Received an invalid IPN message: ${paypalArgs}")
			}
		}
		txnId = paypalArgs.txn_id
		receiverEmail = paypalArgs.receiver_email
		payerEmail = paypalArgs.payer_email
		payerId = paypalArgs.payer_id
		addressConfirmed = (paypalArgs.address_status == 'confirmed')
	}
	
	/**
	 * This method is called before the first insert in the database.
	 */
	def beforeInsert() {
		asJSON = asMap ? asMap as JSON : null
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
		asMap = asJSON ? JSON.parse(asJSON) : [:]
	}
}