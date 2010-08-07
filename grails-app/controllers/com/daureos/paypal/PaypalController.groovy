package com.daureos.paypal

/**
 * Based on the paypal plugin. Thanks again Graeme
 * @author jesus.lanchas
 *
 */
class PaypalController {
	
	static allowedMethods = [notify: 'POST']
	
	// Injected by grails
	def paypalService

	/**
	 * This is the action invoked by Paypal when they send us an IPN
	 */
	def notify = {
		log.debug "Received IPN notification from PayPal Server ${params}"
		def config = grailsApplication.config.grails.paypal
		def server = config.server
		def login = params.email ?: config.email
		if (!server || !login) throw new IllegalStateException("Paypal misconfigured! You need to specify the Paypal server URL and/or account email. Refer to documentation.")

		// Verifying the IPN request
		params.cmd = "_notify-validate"
		def queryString = params.toQueryString()[1..-1]

		log.debug "Sending back query $queryString to PayPal server $server"
		def url = new URL(server)
		def conn = url.openConnection()
		conn.doOutput = true
		def writer = new OutputStreamWriter(conn.getOutputStream())
		writer.write queryString
		writer.flush()

		def result = conn.inputStream.text?.trim()

		log.debug "Got response from PayPal IPN $result"

		if (result != 'VERIFIED') {
			log.debug "Error with PayPal IPN response: [$result]"
		} else if (params.receiver_email != login) {
			// OK, the IPN was sent by Paypal ... but not for us
			log.warn """WARNING: receiver_email parameter received from PayPal does not match configured e-mail. This request is possibly fraudulent!
	REQUEST INFO: ${params}
				"""
		} else if(IPN.findByTxnId(params.txn_id)) {
			log.warn """WARNING: Request tried to re-use and old PayPal transaction id. This request is possibly fraudulent!
	REQUEST INFO: ${params} """
		} else {
			// The IPN is new and it is for us
			def ipn = new IPN(params)
			if(ipn.save(flush:true)) {
				log.info("New IPN saved: ${ipn}")
				paypalService.invokeIPNHandler(ipn)
				render "OK" // Paypal needs a response, otherwise it will send the notification several times!
			} else {
				log.error("Error saving a new IPN: ${ipn}")
			}
		}
	}
}