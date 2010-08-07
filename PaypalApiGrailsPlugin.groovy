import com.daureos.paypal.TransactionType
import grails.util.GrailsNameUtils

class PaypalApiGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3.3 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def author = "Jesús Lanchas"
    def authorEmail = "jesus.lanchas@daureos.com"
    def title = "Plugin summary/headline"
    def description = '''\\
Brief description of the plugin.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/paypal-api"

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before 
    }

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { applicationContext ->
		String ipnHandlerServiceClassName
		String defaultIpnHandlerServiceClassName = "IpnHandlerService"
		def service, mc, declaredMethod
		def methodName
		
		// Recovering the name of the ipn handler service
		ipnHandlerServiceClassName = application.config.paypal?.ipnHanderService ?: defaultIpnHandlerServiceClassName
		
		log.debug("Searching the bean ${GrailsNameUtils.getPropertyName(ipnHandlerServiceClassName)}")

		if(!applicationContext.containsBean(GrailsNameUtils.getPropertyName(ipnHandlerServiceClassName))) {
			log.warn("Not IPN handler defined! (class name: ${ipnHandlerServiceClassName})")
		} else {
			service = applicationContext.getBean(GrailsNameUtils.getPropertyName(ipnHandlerServiceClassName))
			log.debug("Using ${ipnHandlerServiceClassName} as IPN handler")
			
			// Adding needed methods
			mc = service.getClass().metaClass
			log.debug("Methods in ${ipnHandlerServiceClassName}: ${mc.methods.name}")
			TransactionType.values().each {transactionType ->
				methodName = "on${GrailsNameUtils.getClassNameRepresentation(transactionType.toString())}" as String // to be used in the contains method then
				declaredMethod = mc.methods.name.contains(methodName)
				
				log.debug("Does the method ${methodName} exist in the IPN handler? ${declaredMethod}")
				
				if(!declaredMethod) {
					// Adding an empty implementation
					mc."${methodName}" = {log.debug("Nothing to do with a ${transactionType} IPN")}
					
					log.debug("Method ${methodName} added to the IPN handler")
				}
			}
			mc.methodMissing = {name,args -> log.debug("Called the IPN Handler for an unknown event: ${name}")}
			
			// Setting the handler in the paypal service
			applicationContext.getBean("paypalService").ipnHandler = service
		}
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
