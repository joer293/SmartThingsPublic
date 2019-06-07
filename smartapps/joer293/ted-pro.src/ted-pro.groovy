/**
 *  Ted Pro (Connect)
 *
 *  Copyright 2016 Joe Rose
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 
import physicalgraph.device.*;
 
definition(
    name: "Ted Pro",
    namespace: "joer293",
    author: "Joe Rose",
    description: "Connects 'read only' to your Ted (The Energy Detective) Pro home unit. Discover MTU, spyder and circuits to create devices from.  Polls the TED devices for energy usage at set intervals. ",
    category: "Green Living",
    iconUrl: "http://www.theenergydetective.com/media/magenzone/shopnow/default/logo.png",
    iconX2Url: "http://www.theenergydetective.com/media/magenzone/shopnow/default/logo.png",
	singleInstance: true
    ) {
    appSetting "host"
    appSetting "port"
    appSetting "hub"
}


preferences {
	page(name: "pageConnectionSetup", title: "Connection Setup", content: "pageConnectionSetup", install: true, uninstall: true)
    // page(name: "pageDeviceSelection", content: "pageDeviceSelection", install: true)
}

def pageConnectionSetup() {
    // verifyDevices()
    dynamicPage(name: "pageConnectionSetup") {
        section("Please add the TED network information") {
            input "host", "text", title: "host", defaultValue: "192.168.1.2", required: true
            input "port", "text", title: "port", defaultValue: 80
            input "hub", "hub", title: "Hub", required: false
        }
    }

}

def pageDeviceSelection() {
    verifyDevices()
    def refreshInterval = 0
    return dynamicPage(name: "pageDeviceSelection", title: "$state.GatewayDescription ($state.GatewayID)", refreshInterval: refreshInterval) {
        section("Please choose subdevice") {
            input "MTU", "enum", title: "MTU", options: [MTU0description: body.MTUs.MTU[0].MTUDescription.text(), MTU0description: body.MTUs.MTU[1].MTUDescription.text(),MTU0description: body.MTUs.MTU[2].MTUDescription.text(),MTU0description: body.MTUs.MTU[3].MTUDescription.text()]
        }
    }

}


def installed() {
	log.debug "Installed with settings: ${settings}"
	verifyDevices()
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
   //verifyDevices()
   unschedule()
    initialize()
    //unsubscribe()

}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
	
    log.debug "Initialized with settings: ${settings}"
	runEvery1Minute(pollMTUs)
    runEvery1Minute(pollSpyders)
   // runEvery1Minute(pollECC)

}

// TODO: implement event handlers

void verifyDevices() {
        log.debug "Verify devices"
         def verifyResult = new physicalgraph.device.HubAction(
    		method: "GET",
    		path: "/api/SystemSettings.xml",
    		headers: [
        		"HOST" : "${host}:${port}"],
                host,
                [callback: discoverDevices]
		)
        // log.debug result.toString()
        sendHubCommand(verifyResult);
        log.debug "sent verify"
}
void pollECC(){
	pollMTUs()
    pollSpyders()
 }
 
void pollSpyders(){
      //poll Spyder
             log.debug "Poll Spyders"
         def pollSpyderResult = new physicalgraph.device.HubAction(
    		method: "GET",
    		path: "/api/SpyderData.xml",
    		headers: [
        		"HOST" : "${host}:${port}"],
                host,
                [callback: pollSpyder]
		)
        // log.debug result.toString()
        sendHubCommand(pollSpyderResult)
       // log.debug "sent Spyder poll"
}

void pollMTUs(){
   //poll MTU
        log.debug "Poll MTUs"
         def pollMTUResult = new physicalgraph.device.HubAction(
    		method: "GET",
    		path: "/api/SystemOverview.xml",
    		headers: [
        		"HOST" : "${host}:${port}"],
                host,
                [callback: pollMTU]
		)
        // log.debug result.toString()
        sendHubCommand(pollMTUResult)
      //  log.debug "sent MTU poll"
}

void discoverDevices(physicalgraph.device.HubResponse hubResponse) {
    log.debug "Response Handler"
    def body = hubResponse.xml
    state.GatewayDescription = body.Gateway.GatewayDescription.text()
    state.GatewayID = body.Gateway.GatewayID.text()
    state.GatewayZipcode = body.ZipCode.text()
    log.debug "Gateway: ${state.GatewayID} - ${state.GatewayDescription} - ${state.GatewayZipcode}"
 
     //discover MTUs
    state.NumMTUs = body.NumberMTU.text()
    log.debug "Adding MTU devices: ${state.NumMTUs.toInteger()}"
	for (int i = 0; i < state.NumMTUs.toInteger(); i++) {
    log.debug "MTU: ${i}"
    addChildDevice("joer293", "TED Pro MTU", "${state.GatewayID}/${body.MTUs.MTU[i].MTUID}", $hub, [name: "${body.MTUs.MTU[i].MTUID}", label: "${body.MTUs.MTU[i].MTUDescription}", completedSetup: true])
	}
    
    //discover Spyders
    log.debug "Adding Spyder devices"
	body.Spyders.Spyder.eachWithIndex { spyder, s ->
 	  spyder.Group.eachWithIndex { group, g ->
		if (group.Description != "") {
        log.debug "Spyder: ${group.Description}"
        log.debug "Spyder: ${s}/${g}"
        addChildDevice("joer293", "TED Pro Spyder", "${state.GatewayID}/${s}/${g}", $hub, [name: "${group.Description}", label: "${group.Description}", completedSetup: true])
       		}
		}
    }
}

void pollMTU(physicalgraph.device.HubResponse hubResponse) {
// log.debug "MTU Response Handler"
    state.mresp = hubResponse.xml
  
       	//MTU 1   
    getChildDevice("${state.GatewayID}/16081C").sendEvent(name: "power", value: state.mresp.MTUVal.MTU1.Value, unit: "W", displayed:true)
    getChildDevice("${state.GatewayID}/16081C").sendEvent(name: "energy", value: state.mresp.MTUVal.MTU1.KVA, unit: "VA", displayed:true)
    getChildDevice("${state.GatewayID}/16081C").sendEvent(name: "PF", value: state.mresp.MTUVal.MTU1.PF, unit: "PF", displayed:true)
    getChildDevice("${state.GatewayID}/16081C").sendEvent(name: "voltage", value: state.mresp.MTUVal.MTU1.Voltage, unit: "V", displayed:true)
    
    //MTU 2
    getChildDevice("${state.GatewayID}/160AFD").sendEvent(name: "power", value: state.mresp.MTUVal.MTU2.Value.text(), unit: "W", displayed:true)
    getChildDevice("${state.GatewayID}/160AFD").sendEvent(name: "energy", value: state.mresp.MTUVal.MTU2.KVA.text(), unit: "VA", displayed:true)
    getChildDevice("${state.GatewayID}/160AFD").sendEvent(name: "PF", value: state.mresp.MTUVal.MTU2.PF.text(), unit: "PF", displayed:true)
    getChildDevice("${state.GatewayID}/160AFD").sendEvent(name: "voltage", value: state.mresp.MTUVal.MTU2.Voltage.text(), unit: "V", displayed:true)
               
    }



def pollSpyder(physicalgraph.device.HubResponse hubResponse){   
  //  log.debug "Spyder Response Handler"
    state.sresp = hubResponse.xml

//push Spyder data to child device
     //  log.debug "Spyder data loop"
	
    	def devices = getChildDevices()
      //  	log.debug "device list = ${devices}"
        devices.each {
         
          //  log.debug "${it.typeName}"
            switch (it.typeName){
            case "TED Pro MTU":
           // log.debug "ted pro MTU skipped"
         
            break
            
            case "TED Pro Spyder":
            
            log.debug "spyder device ID = ${it.deviceNetworkId}"
            int s = it.deviceNetworkId.tokenize('/')[1].toInteger()
            int g = it.deviceNetworkId.tokenize('/')[2].toInteger()
           it.sendEvent(name: "power", value: state.sresp.Spyder[s].Group[g].Now, unit: "W", displayed:true)
           it.sendEvent(name: "energy", value: state.sresp.Spyder[s].Group[g].MTD, unit: "W", displayed:false)
           it.sendEvent(name: "TDY", value: state.sresp.Spyder[s].Group[g].TDY, unit: "W", displayed:false)
            it.sendEvent(name: "MTD", value: state.sresp.Spyder[s].Group[g].MTD, unit: "W", displayed:false)
        //  log.debug "sent events to child spyder device(s)"
           break
            }
       }
}
