metadata {
	// Automatically generated. Make future change here.
	definition (name: "TED Pro MTU", namespace: "joer293", author: "joer@noface.net") {

		capability "Energy Meter"
        capability "Power Meter"
        capability "Voltage Measurement"
        capability "Refresh"

        attribute "power", "number" 
        attribute "energy", "number" 
        attribute "PF", "number"
        attribute "voltage", "number"

	}

	simulator {
		}
        
	tiles(scale: 2) {
		standardTile("Refresh", "device.refresh", width: 1, height: 1, inactiveLabel: false, decoration: "flat") {
			state "default", label:'   ', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		 
        valueTile("Power", "device.power", width: 2, height: 2) {
            state "power", label:'${currentValue} kW',
            backgroundColors:[
					[value: 0, color: "#151515"],
					[value: 100, color: "#1e9cbb"],
					[value: 500, color: "#90d2a7"],
					[value: 1000, color: "#44b621"],
					[value: 2000, color: "#f1d801"],
					[value: 4000, color: "#d04e00"],
					[value: 8000, color: "#bc2323"]
				]
            }
        valueTile("Voltage", "device.voltage", width: 3, height: 1) {
            state "voltage", label:'${currentValue} Volts'
            }        
        valueTile("Energy", "device.energy", width: 2, height: 1) {
            state "energy", label:'${currentValue} kVA'
            }
        valueTile("Power Factor", "device.PF", width: 2, height: 1) {
            state "PF", label:'power factor ${currentValue}',
             backgroundColors:[
            [value: 0, color: "#ff0000"],
            [value: 85, color: "#e86d13"],
            [value: 100, color: "#00ff00"]
        ]
            }
        htmlTile(name:"graphHTML",
			action: "getGraphHTML",
			refreshInterval: 1,
			width: 6,
			height: 4,
			whitelist: ["www.gstatic.com"])


		main(["Power"])
		details(["graphHTML", "Power", "Energy", "Power Factor", "Voltage", "Refresh"])
	}
}

mappings {
	path("/getGraphHTML") {action: [GET: "getGraphHTML"]}
}

void refresh() {
	log.debug "Executing 'refresh'"
    parent.pollMTUs()
}

String getDataString(Integer seriesIndex) {
	def dataString = ""
	def dataTable = []
	def startOfToday = timeToday("00:00", location.timeZone)
    switch (seriesIndex) {
		case 1:
			
            def powerData = device.statesBetween("PF", startOfToday - 1, startOfToday, [max: 1440])
   	powerData.reverse().each() {
					dataTable.add([it.date.format("H", location.timeZone),it.date.format("m", location.timeZone),it.value])
				}
			break
		case 2:
			
            def powerData = device.statesBetween("power", startOfToday - 1, startOfToday, [max: 1440])
   	powerData.reverse().each() {
					dataTable.add([it.date.format("H", location.timeZone),it.date.format("m", location.timeZone),it.value])
				}
			break
		case 3:
			
            def energyData = device.statesSince("PF", startOfToday, [max: 1440])
   	energyData.reverse().each() {
					dataTable.add([it.date.format("H", location.timeZone),it.date.format("m", location.timeZone),it.value])
				}
			break
		case 4:
			
            def powerData = device.statesSince("power", startOfToday, [max: 1440])
   	powerData.reverse().each() {
					dataTable.add([it.date.format("H", location.timeZone),it.date.format("m", location.timeZone),it.value])
				}
			break
	}
    
	//}
    
	dataTable.each() {
		def dataArray = [[it[0],it[1],0],null,null,null,null]
		dataArray[seriesIndex] = it[2]
		dataString += dataArray.toString().replaceAll("\\s","") + ","
	}
  //  log.debug "data string: ${dataString}"
	return dataString
}
def getStartTime() {
	def startTime = 0
	
	
	return startTime
}

def getGraphHTML() {
	def html = """
		<!DOCTYPE html>
			<html>
				<head>
					<meta http-equiv="cache-control" content="max-age=0"/>
					<meta http-equiv="cache-control" content="no-cache"/>
					<meta http-equiv="expires" content="0"/>
					<meta http-equiv="expires" content="Tue, 01 Jan 1980 1:00:00 GMT"/>
					<meta http-equiv="pragma" content="no-cache"/>
					<meta name="viewport" content="width = device-width">
					<meta name="viewport" content="initial-scale = 1.0, user-scalable=no">
					<style type="text/css">body,div {margin:0;padding:0}</style>
					<script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
					<script type="text/javascript">
						google.charts.load('current', {packages: ['corechart']});
						google.charts.setOnLoadCallback(drawGraph);
						function drawGraph() {
							var data = new google.visualization.DataTable();
							data.addColumn('timeofday', 'time');
							data.addColumn('number', 'PF (Yesterday)');
							data.addColumn('number', 'Power (Yesterday)');
							data.addColumn('number', 'PF (Today)');
							data.addColumn('number', 'Power (Today)');
							data.addRows([
								${getDataString(1)}
								${getDataString(2)}
								${getDataString(3)}
								${getDataString(4)}
							]);
							var options = {
								fontName: 'San Francisco, Roboto, Arial',
								height: 240,
                                animation: {duration: 1000, startup: true, easing: 'out'},
								hAxis: {
									format: 'H:mm',
									minValue: [${getStartTime()},0,0],
									slantedText: false
								},
								series: {
									0: {targetAxisIndex: 1, color: '#FFD2C2', areaOpacity: 0, lineWidth: 1},
									1: {targetAxisIndex: 0, color: '#D1DFFF', lineWidth: 0},
									2: {targetAxisIndex: 1, color: '#EF3020', areaOpacity: 0, lineWidth: 1},
									3: {targetAxisIndex: 0, color: '#004CFF', lineWidth: 1}
								},
								vAxes: {
									0: {
										title: 'Power (W)',
										format: 'decimal',
										textStyle: {color: '#004CFF'},
										titleTextStyle: {color: '#004CFF'},
										viewWindow: {min: 0}
									},
									1: {
										title: 'Power Factor',
										format: 'decimal',
										textStyle: {color: '#FF0000'},
										titleTextStyle: {color: '#FF0000'},
										viewWindow: {min: 0}
									}
								},
								curveType: 'function',
                                legend: {
									position: 'none'
								},
								chartArea: {
									width: '72%',
									height: '85%'
								}
							};
							var chart = new google.visualization.AreaChart(document.getElementById('chart_div'));
							chart.draw(data, options);
						}
					</script>
				</head>
				<body>
					<div id="chart_div"></div>
				</body>
			</html>
		"""
	render contentType: "text/html", data: html, status: 200
}