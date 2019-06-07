metadata {
	// Automatically generated. Make future change here.
	definition (name: "TED Pro Spyder", namespace: "joer293", author: "joer@noface.net") {

        capability "Power Meter"
        capability "Energy Meter"
        capability "Health Check"
        capability "Refresh"

        attribute "power", "number" 
        attribute "TDY", "number" 
        attribute "MTD", "number"
        

	}

	simulator {
		}

tiles(scale: 2) {
        valueTile("Power", "device.power", width: 2, height: 2, canChangeIcon: true, canChangeBackground: true) {
            state "default", label:'${currentValue} W',
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
        valueTile("TDY", "device.TDY", width: 4, height: 1) {
            state "default", label:'${currentValue} Watts - Today'
            }
        valueTile("MTD", "device.MTD", width: 3, height: 1) {
            state "default", label:'${currentValue} Watts - This month'
            }
        standardTile("Refresh", "device.refresh", width: 1, height: 1, decoration: 'flat') {
			state "default", label:'   ', action:"refresh.refresh", icon:"st.secondary.refresh"
            }
     htmlTile(name:"chartHTML",
			action: "getChartHTML",
			refreshInterval: 1,
			width: 6,
			height: 4,
			whitelist: ["www.gstatic.com"])
        
		main(["Power"])
		details([ "chartHTML", "Power", "TDY", "MTD", "Refresh"])
	}
}


def ping(){
    return true
}

mappings {
	path("/getChartHTML") {action: [GET: "getChartHTML"]}
}

void refresh() {
	log.debug "Executing 'refresh'"
    parent.pollSpyders()
}

String getDataString(Integer seriesIndex) {
	def dataString = ""
	def dataTable = []
	def startOfToday = timeToday("00:00", location.timeZone)
    switch (seriesIndex) {
		case 1:
			 def powerData = device.statesBetween("power", startOfToday - 1, startOfToday, [max: 1440])
   	powerData.reverse().each() {
					dataTable.add([it.date.format("H", location.timeZone),it.date.format("m", location.timeZone),it.integerValue])
				}
			break
		case 2:
			def powerData = device.statesSince("power", startOfToday, [max: 1440])
   	powerData.reverse().each() {
					dataTable.add([it.date.format("H", location.timeZone),it.date.format("m", location.timeZone),it.integerValue])
				}
			break
	}
    
	//}
	dataTable.each() {
		def dataArray = [[it[0],it[1],0],null,null]
		dataArray[seriesIndex] = it[2]
		dataString += dataArray.toString() + ","
	}
    // log.debug "data string: ${dataString}"
	return dataString
}
def getStartTime() {
	def startTime = 0
	
	
	return startTime
}

def getChartHTML() {
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
						google.charts.setOnLoadCallback(drawChart);
						function drawChart() {
							var data = new google.visualization.DataTable();
							data.addColumn('timeofday', 'time');
							data.addColumn('number', 'Power (Yesterday)');
							data.addColumn('number', 'Power (Today)');
							data.addRows([
								${getDataString(1)}
								${getDataString(2)}
								]);
							var options = {
								curveType: 'function',
                                fontName: 'Arial',
								height: 240,
                                animation: {duration: 1000, startup: true, easing: 'out'},
								hAxis: {
									format: 'H:mm',
									minValue: [${getStartTime()},0,0],
									slantedText: false
								},
								series: {
									0: {targetAxisIndex: 0, color: '#FFAAAA', lineWidth: 0},
									1: {targetAxisIndex: 0, color: '#FF2222', lineWidth: 1}
								},
								vAxes: {
									0: {
										title: 'Power (W)',
										format: 'decimal',
										textStyle: {color: '#004CFF'},
										titleTextStyle: {color: '#004CFF'},
										viewWindow: {min: 0}
									}
								},
                                legend: {
									position: 'none'
								},
								chartArea: {
									width: '78%',
									height: '80%'
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