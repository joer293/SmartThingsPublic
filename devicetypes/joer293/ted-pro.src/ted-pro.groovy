 
metadata {
	// Automatically generated. Make future change here.
	definition (name: "TED Pro", author: "joer@noface.net") {

		capability "Energy Meter"
        capability "Power Meter"
		capability "Polling"
		capability "Refresh"

        attribute "now", "string"
        attribute "tdy", "string"
        attribute "mtd", "string"
        attribute "avg", "string"
        attribute "proj", "string"
        attribute "voltage", "string"
        attribute "phase", "string"

	}

	preferences {

    input("ip", "text", title: "IP", description: "The IP of your TED6000 device", required: true)
    input("port", "text", title: "port", description: "The port of your TED6000 device", required: true)
    input("usr", "text", title: "Username", description: "The username configured in Network Settings on your TED6000", required: false)
    input("pass", "password", title: "Password", description: "The password configured in Network Settings on your TED6000", required: false)
	
	}



	simulator {
		}
        
	tiles {
        
        chartTile(name: "EnergyChart", attribute: "device.energy.currentState.value")
        valueTile("Energy", "device.energy") {
            state "energy", label:'${currentValue} Wh'
            }
        valueTile("Power", "device.power") {
            state "power", label:'${currentValue} Wh'
            }
        valueTile("TDY", "device.tdy") {
            state "tdy", label:'${currentValue} Wh'
            }
       valueTile("MTD", "device.mtd") {
            state "mtd", label:'${currentValue} Wh'
            }
        valueTile("Avg", "device.avg") {
            state "avg", label:'${currentValue} Wh'
            }
        valueTile("Proj", "device.proj") {
            state "proj", label:'${currentValue} Wh'
            }
        valueTile("Phase", "device.phase") {
            state "phase", label:'${currentValue}'
            }
        standardTile("Refresh", "device.image") {
			state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main(["Energy"])
		details(["EnergyChart", "Energy", "Power", "Refresh", "TDY", "MTD", "Avg", "Proj", "Phase"])
	}
}



def poll() {
   
    log.debug "poll Ted Pro Home"
    goImportTed()
}

def refresh() {
	log.debug "refresh Ted Pro Home"
   return goImportTed()
   log.debug hubAction
}

def goImportTed(){
 
 log.debug "trying hubaction 25"
	
    def hubAction = new physicalgraph.device.HubAction(method:"GET", path:"/api/DashData.xml", headers:[HOST: "10.0.1.22:80"])
    return hubAction
    
}

def parse(description) {
	def msg = parseLanMessage(description)
    log.debug "${msg.data}"
    log.debug "${msg.data.name()}"
    
    sendEvent(name: "now", value: msg.data.Now.text(), IsStateChange: "true")
  	sendEvent(name: "power", value: msg.data.Now.text(), IsStateChange: "true")
    sendEvent(name: "tdy", value:  msg.data.TDY.text(), IsStateChange: "true")
    sendEvent(name: "mtd", value: msg.data.MTD.text(), IsStateChange: "true")
	sendEvent(name: "energy", value: msg.data.MTD.text(), IsStateChange: "true")
    sendEvent(name: "avg", value: msg.data.Avg.text(), IsStateChange: "true")
    sendEvent(name: "proj", value: msg.data.Proj.text(), IsStateChange: "true")
    sendEvent(name: "voltage", value: msg.data.Voltage.text(), IsStateChange: "true")
    sendEvent(name: "phase", value: msg.data.Phase.text(), IsStateChange: "true")
}


// This next method is only used from the simulator
def zwaveEvent(physicalgraph.zwave.commands.meterv1.MeterReport cmd) {
	if (cmd.scale == 0) {
		[name: "energy", value: cmd.scaledMeterValue, unit: "kWh"]
	} else if (cmd.scale == 1) {
		[name: "energy", value: cmd.scaledMeterValue, unit: "kVAh"]
	}
	else {
		[name: "power", value: Math.round(cmd.scaledMeterValue), unit: "W"]
	}
}





private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    log.debug "Port entered is $port and the converted hex code is $hexport"
    return hexport
}

private Integer convertHexToInt(hex) {
	return Integer.parseInt(hex, 16)
}


private String convertHexToIP(hex) {
	return [convertHexToInt(hex[0..1]), convertHexToInt(hex[2..3]), convertHexToInt(hex[4..5]), convertHexToInt(hex[6..7])].join(".")
}

private getHostAddress() {
    def ip = getDataValue("ip")
    def port = getDataValue("port")

    if (!ip || !port) {
        def parts = device.deviceNetworkId.split(":")
        if (parts.length == 2) {
            ip = parts[0]
            port = parts[1]
        } else {
            log.warn "Can't figure out ip and port for device: ${device.id}"
        }
    }

    log.debug "Using IP: $ip and port: $port for device: ${device.id}"
    return convertHexToIP(ip) + ":" + convertHexToInt(port)
}

// gets the address of the hub
private getCallBackAddress() {
    return device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
}
