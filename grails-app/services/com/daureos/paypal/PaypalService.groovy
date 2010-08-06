package com.daureos.paypal

import org.springframework.beans.factory.InitializingBean

import com.paypal.sdk.core.nvp.NVPDecoder
import com.paypal.sdk.core.nvp.NVPEncoder
import com.paypal.sdk.profiles.APIProfile
import com.paypal.sdk.profiles.ProfileFactory
import com.paypal.sdk.services.NVPCallerServices

class PaypalService implements InitializingBean {
	
	boolean transactional = false
	
	// Injected by grails
	def grailsApplication
	
	// Some constants (used as default values)
	public static final String API_VERSION = "63.0"
	public static final String CURRENCY_CODE = "EUR"
	public static final String MAXFAILEDPAYMENTS = "1"
	public static final String AUTOBILLAMT = "AddToNextBilling" // The alternative is NoAutoBill
	public static final String IPN_HANDLER_SERVICE_NAME = "ipnHandlerService"
	
	// Attributes	
	private NVPCallerServices caller = null
	private String apiVersion 
	private String currencyCode
	private String maxFaliedPayments
	private String autobillAmt
	
	/**
	* It initializes the attribute caller with the config data needed.
	*/
	void afterPropertiesSet() {
		caller = new NVPCallerServices()
		APIProfile profile = ProfileFactory.createSignatureAPIProfile()

		// Set up the API credentials, PayPal end point, API operation and version.
		if(!grailsApplication.config.grails.paypal?.username || !grailsApplication.config.grails.paypal?.password ||
			!grailsApplication.config.grails.paypal?.signature || !grailsApplication.config.grails.paypal?.environment) {
			throw IllegalArgumentException("Paypal configuration missed, please set the following config values: grails.paypal.username, grails.paypal.password, grails.paypal.signature, grails.paypal.environment")
		} else {
			profile.setAPIUsername(grailsApplication.config.grails.paypal.username)
			profile.setAPIPassword(grailsApplication.config.grails.paypal.password)
			profile.setSignature(grailsApplication.config.grails.paypal.signature)
			profile.setEnvironment(grailsApplication.config.grails.paypal.environment)
			profile.setSubject("")
			caller.setAPIProfile(profile)
			
			// Setting some configurable properties with available default values
			apiVersion = grailsApplication.config.grails.paypal?.apiVersion ?: API_VERSION
			currencyCode = grailsApplication.config.grails.paypal?.currencyCode ?: CURRENCY_CODE
			maxFaliedPayments = grailsApplication.config.grails.paypal?.MAXFAILEDPAYMENTS ?: MAXFAILEDPAYMENTS
			
			autobillAmt = grailsApplication.config.grails.paypal?.AUTOBILLAMT ?: AUTOBILLAMT
			
			// The handler of the IPN's
			def serviceName = grailsApplication.config.paypal?.ipnHanderService ?: IPN_HANDLER_SERVICE_NAME
			def service = grailsApplication.mainContext
			// TODO Sacar service como atributo y llamarlo desde invokeIPNHandlers
		}
	}
	
	/**
	 * This method invokes the paypal method SetExpressCheckout using the NVP API.
	 *
	 * @param callback A map with 'successController', 'successAction', 'cancelController', 'cancelAction'. Needed
	 * to create the urls used by Paypal to redirect the user from the Paypal page.
	 * @param description A little (127 chars) with information of the sell. The customer will see it in Paypal.
	 * @param billingAgreement true if this call is the start of a recurring payment and false if the transaction does not include a one-time purchase
	 * @param amount The money
	 * @return The token of success associated with the checkout of null
	 */
	public def setExpressCheckoutCode(callback, description, billingAgreement = true, amount = 0) {
		def decoder
		def args = [:]
		def result
		
		def g = new org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib()
		String returnURL = g.createLink(controller:callback.successController, action:callback.successAction, absolute:true)
		String cancelURL = g.createLink(controller:callback.cancelController, action:callback.cancelAction, absolute:true)
		
		args.METHOD = "SetExpressCheckout"
		args.CURRENCYCODE = currencyCode
		args.RETURNURL = returnURL
		args.CANCELURL = cancelURL
		args.AMT = amount.toString()
		if(billingAgreement){
			args.L_BILLINGTYPE0="RecurringPayments"
			args.L_BILLINGAGREEMENTDESCRIPTION0 = description
		} else {
			args.PAYMENTREQUEST_0_DESC = description
		}
		
		return apiCall(args)
	}
	
	/**
	 * This method call the Paypal API method GetExpressCheckoutDetails, using the token
	 * passed as argument.
	 * @param token
	 * @return A map with all the elements of the Paypal response or null (with errors)
	 */
	public def getExpressCheckoutDetails(token) {
		def args = [:]
		
		args.METHOD = "GetExpressCheckoutDetails"
		args.TOKEN = token
		
		return apiCall(args)
	}
	
	/**
	 * This method call the Paypal API method CreateRecurringPaymentsProfile
	 * @return
	 */
	public def createRecurringPaymentsProfile(token, description, amount, period, frequency, date = new Date()) {
		def args = [:]
		
		args.METHOD = "CreateRecurringPaymentsProfile"
		args.TOKEN = token
		args.PROFILESTARTDATE = date.format("yyyy-MM-dd'T'HH:mm:ss") // Now
		
		// Schedule Details Fields
		args.DESC = description
		args.MAXFAILEDPAYMENTS = maxFaliedPayments

		// Auto billing the outstanding balance
		args.AUTOBILLAMT = autobillAmt
		
		// Billing Period Details Fields
		args.BILLINGPERIOD = period
		args.BILLINGFREQUENCY = frequency
		
		args.CURRENCYCODE = currencyCode
		args.AMT = amount.toString()
		
		return apiCall(args)
	}
	
	/**
	 * This method returns the Paypal information associated with the profile id passed as parameter.
	 * If paypal doesn't return valid information for this profile, this method will return null.
	 *
	 * This method will call the API method GetRecurringPaymentsProfileDetails
	 *
	 * @return a map with information of the profile whose id is passed as parameter.
	 */
	public def getRecurringPaymentsProfileDetails(profileId) {
		def args = [:]
		
		args.METHOD = "GetRecurringPaymentsProfileDetails"
		args.PROFILEID = profileId
		
		return apiCall(args)
	}
	
	/**
	 * This method tries to cancel the Paypal profile subscription whose id is passed as parameter.
	 *
	 * This method will call the API method ManageRecurringPaymentsProfileStatus
	 *
	 * @return null if error and the decoder object generated from the Paypal response otherwise
	 */
	public def cancelRecurringPaymentsProfile(profileId, note = "") {
	   def args = [:]
	   
	   args.METHOD = "ManageRecurringPaymentsProfileStatus"
	   args.PROFILEID = profileId
	   args.ACTION = "Cancel"
	   if(note) args.NOTE = note
	   
	   return apiCall(args)
	}
   
	/**
	 * This method tries to suspend the Paypal profile subscription whose id is passed as parameter.
	 *
	 * This method will call the API method ManageRecurringPaymentsProfileStatus
	 *
	 * @return null if error and the decoder object generated from the Paypal response otherwise
	 */
	public def suspendRecurringPaymentsProfile(profileId, note = "") {
		def args = [:]
		 
		args.METHOD = "ManageRecurringPaymentsProfileStatus"
		args.PROFILEID = profileId
		args.ACTION = "Suspend"
		if(note) args.NOTE = note
		 
		return apiCall(args)
	}
	
	/**
	 * This method tries to suspend the Paypal profile subscription whose id is passed as parameter.
	 *
	 * This method will call the API method ManageRecurringPaymentsProfileStatus
	 *
	 * @return null if error and the decoder object generated from the Paypal response otherwise
	 */
	public def reactivateRecurringPaymentsProfile(profileId, note = "") {
	   def args = [:]
		
	   args.METHOD = "ManageRecurringPaymentsProfileStatus"
	   args.PROFILEID = profileId
	   args.ACTION = "Reactivate"
	   if(note) args.NOTE = note
		
	   return apiCall(args)
	}
	
	/**
	 * This method invokes the right method (according to the IPN parameter) in the
	 * right service (according to the configuration).
	 */
	public void invokeIPNHandlers(IPN ipn) {
		// TODO Llamar a service.on${ipnType} si existe
	}
	
	/**
	 * This method makes a Paypal call using the NVP API.
	 * @param args A map with the request params (METHOD, VERSION, etc.)
	 * @return null if it was an error and the decoder generated from the Paypal response otherwise.
	 */
	private NVPDecoder apiCall(args) {
		String nvpRequest, nvpResponse
		NVPEncoder encoder = new NVPEncoder()
		NVPDecoder decoder = new NVPDecoder()
		
		log.debug("Starting a paypal API call: ${args}")

		args.VERSION = apiVersion
		try {
			args.each {k,v ->
				encoder.add(k, v)
			}
   
			// Execute the API operation and obtain the response.
			nvpRequest = encoder.encode()
			log.debug("nvpRequest: ${nvpRequest}")
			
			nvpResponse = caller.call(nvpRequest)
			log.debug("nvpResponse: ${nvpResponse}")
			
			decoder.decode(nvpResponse)
			log.debug("Decoder generated: ${decoder}")
			
			// Loggin paypal info (errors: https://cms.paypal.com/es/cgi-bin/?&cmd=_render-content&content_ID=developer/e_howto_api_nvp_errorcodes)
			log.info("ACK:${decoder.ACK} ${decoder.L_ERRORCODE0 ? '(Err code:' + decoder.L_ERRORCODE0 + ') ':''}- CORRELATIONID:${decoder.CORRELATIONID} - TIMESTAMP:${decoder.TIMESTAMP}")

			if(!successResponse(decoder)){
				log.warn("The paypal response is not success")
				decoder = null
			}
		} catch (Exception ex) {
			decoder = null
			log.error(ex.message)
			log.error("Error invoking the paypal api call: ${args}")
			log.warn("The decoder object hasn't been created")
			ex.printStackTrace()
		}
		
		return decoder
	}
	
	/**
	 * This method returns true if the passed decoder is from a success paypal response
	 * (or success with warnings response)
	 * @param decoder
	 * @return
	 */
	private boolean successResponse(decoder) {
		return decoder && (decoder.ACK == "Success" || decoder.ACK == "SuccessWithWarning")
	}
}

