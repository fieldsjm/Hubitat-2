/*
 * Import URL: https://raw.githubusercontent.com/jakelehner/Hubitat/master/WyzeHub/drivers/wyzehub-camera-group-driver.groovy
 *
 * DON'T BE A DICK PUBLIC LICENSE
 *
 * Version 1.1, December 2016
 *
 * Copyright (C) 2021 Jake Lehner
 * 
 * Everyone is permitted to copy and distribute verbatim or modified
 * copies of this license document.
 * 
 * DON'T BE A DICK PUBLIC LICENSE
 * TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
 *
 * 1. Do whatever you like with the original work, just don't be a dick.
 * 
 *    Being a dick includes - but is not limited to - the following instances:
 *
 *    1a. Outright copyright infringement - Don't just copy this and change the name.
 *    1b. Selling the unmodified original with no work done what-so-ever, that's REALLY being a dick.
 *    1c. Modifying the original work to contain hidden harmful content. That would make you a PROPER dick.
 *
 * 2. If you become rich through modifications, related works/services, or supporting the original work,
 *    share the love. Only a dick would make loads off this work and not buy the original work's
 *    creator(s) a pint.
 * 
 * 3. Code is provided with no warranty. Using somebody else's code and bitching when it goes wrong makes
 *    you a DONKEY dick. Fix the problem yourself. A non-dick would submit the fix back.
 *
 */

import groovy.transform.Field

public static String version() {  return "v1.7"  }

public String deviceModel() { return '' }

public String groupTypeId() { return 1 }

metadata {
	definition(
		name: "WyzeHub Camera Group",
		namespace: "jakelehner",
		author: "Jake Lehner",
		importUrl: "https://raw.githubusercontent.com/fieldsjm/Hubitat-2/master/WyzeHub/drivers/wyzehub-camera-group-driver.groovy"
	) {
		capability "Outlet"
		capability "Refresh"
		capability "Switch"

		attribute "allOn", "enum", ["true", "false"]
		attribute "allOff", "enum", ["true", "false"]

		command "updateGroupState", [[
			"name":"Description",
			"description":"Recalculate group switch, allOn, and allOff states from current child camera states",
			"type":"STRING"
		]]
    }

	preferences {
		input "SWITCH_MODE", "enum", title: "Switch 'on' when...",
			description: "Determines when the group switch reports 'on'",
			options: [["any": "Any camera is on"], ["all": "All cameras are on"]],
			defaultValue: "any", required: true, displayDuringSetup: true
	}
}

void installed() {
    app = getApp()
	logDebug("installed()")

	sendEvent(name: 'switch', value: 'off')
	sendEvent(name: 'allOn', value: 'false')
	sendEvent(name: 'allOff', value: 'true')

	refresh()
	initialize()
}

void updated() {
    app = getApp()
	logDebug("updated()")
	updateGroupState()
    initialize()
}

void initialize() {
    app = getApp()
	logDebug("initialize()")
}

void parse(String description) {
	app = getApp()
	logWarn("Running unimplemented parse for: '${description}'")
}

def refresh() {
	getChildDevices().each { childDevice ->
		childDevice.settingsRefresh()
		childDevice.refresh()
	}
	runIn(15, 'updateGroupState')
}

def on() {
	getChildDevices().each { childDevice ->
		childDevice.on()
	}
	runIn(10, 'updateGroupState')
}

def off() {
	getChildDevices().each { childDevice ->
		childDevice.off()
	}
	runIn(10, 'updateGroupState')
}

void updateGroupState() {
	logDebug("updateGroupState()")

	def children = getChildDevices()
	if (!children) {
		logDebug("No child devices found")
		return
	}

	int onCount = children.count { it.currentValue("switch") == "on" }
	int totalCount = children.size()

	String allOnValue = (onCount == totalCount) ? "true" : "false"
	String allOffValue = (onCount == 0) ? "true" : "false"

	String switchMode = settings.SWITCH_MODE ?: "any"
	String switchValue
	if (switchMode == "all") {
		switchValue = (onCount == totalCount) ? "on" : "off"
	} else {
		switchValue = (onCount > 0) ? "on" : "off"
	}

	logDebug("Group state: ${onCount}/${totalCount} on, mode=${switchMode}, switch=${switchValue}, allOn=${allOnValue}, allOff=${allOffValue}")

	if (device.currentValue("allOn") != allOnValue) {
		sendEvent(name: "allOn", value: allOnValue, descriptionText: "${device.displayName} allOn is ${allOnValue}")
	}
	if (device.currentValue("allOff") != allOffValue) {
		sendEvent(name: "allOff", value: allOffValue, descriptionText: "${device.displayName} allOff is ${allOffValue}")
	}
	if (device.currentValue("switch") != switchValue) {
		sendEvent(name: "switch", value: switchValue, descriptionText: "${device.displayName} switch is ${switchValue}")
	}
}

private getApp() {
	app = getParent()
	while(app && app.name != "WyzeHub") {
		app = app.getParent()
	}
	return app
}

private void logDebug(message) {
	app = getApp()
	app.logDebug("[${device.label}] " + message)
}

private void logInfo(message) {
	app = getApp()
	app.logInfo("[${device.label}] " + message)
}

private void logWarn(message) {
	app = getApp()
	app.logWarn("[${device.label}] " + message)
}

private void logError(message) {
	app = getApp()
	app.logError("[${device.label}] " + message)
}
